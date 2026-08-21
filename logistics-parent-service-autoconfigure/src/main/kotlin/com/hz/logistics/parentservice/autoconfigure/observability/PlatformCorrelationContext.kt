package com.hz.logistics.parentservice.autoconfigure.observability

import io.micrometer.tracing.Tracer
import org.springframework.beans.factory.ObjectProvider
import java.security.SecureRandom
import java.util.Locale

/**
 * Read-only access to the trace identifiers that are current for the calling
 * execution context.
 *
 * The platform deliberately depends on Micrometer's abstraction rather than
 * on an OpenTelemetry SDK type. This keeps errors and logging independent of
 * the selected web stack and allows Spring Boot's configured tracer to remain
 * application-owned. A tracer is optional because error handling can run
 * before tracing has established a span.
 */
open class PlatformCorrelationContext(
    private val tracerProvider: () -> Tracer? = { null },
    private val fallbackTraceIdSupplier: () -> String = ::newTraceId,
) {

    /**
     * Retains the lightweight constructor used by direct consumers and tests.
     * Auto-configuration instead uses the provider constructor so this shared
     * object can observe the Boot-created tracer after context startup.
     */
    constructor(
        tracer: Tracer?,
        fallbackTraceIdSupplier: () -> String = ::newTraceId,
    ) : this({ tracer }, fallbackTraceIdSupplier)

    /** Resolve the tracer lazily to avoid an early auto-configuration cycle. */
    constructor(
        tracerProvider: ObjectProvider<Tracer>,
        fallbackTraceIdSupplier: () -> String = ::newTraceId,
    ) : this(tracerProvider::getIfAvailable, fallbackTraceIdSupplier)

    /** The current valid W3C trace ID, or `null` when no trace is active. */
    open fun currentTraceId(): String? = currentTraceContext()?.traceId()?.normaliseW3cId(TRACE_ID_LENGTH)

    /** The current valid W3C span ID, or `null` when no span is active. */
    open fun currentSpanId(): String? = currentTraceContext()?.spanId()?.normaliseW3cId(SPAN_ID_LENGTH)

    /** A snapshot suitable for structured logging and error responses. */
    open fun current(): Correlation? {
        val traceId = currentTraceId() ?: return null
        return Correlation(traceId, currentSpanId())
    }

    /**
     * Return the current trace ID or create a valid local correlation value
     * when tracing has not been established yet.
     */
    open fun traceIdOrCreate(): String =
        currentTraceId() ?: fallbackTraceIdSupplier()
            .normaliseW3cId(TRACE_ID_LENGTH)
            ?: newTraceId()

    /** Alias that reads naturally at error and logging call sites. */
    open fun requiredTraceId(): String = traceIdOrCreate()

    /** Kotlin property-style access for integrations that prefer it. */
    val traceId: String?
        get() = currentTraceId()

    /** Kotlin property-style access for integrations that prefer it. */
    val spanId: String?
        get() = currentSpanId()

    data class Correlation(
        val traceId: String,
        val spanId: String?,
    )

    private fun currentTraceContext() =
        runCatching { tracerProvider()?.currentSpan()?.context() }.getOrNull()

    companion object {
        private const val TRACE_ID_LENGTH = 32
        private const val SPAN_ID_LENGTH = 16
        private val random = SecureRandom()

        /** Generate a lowercase, non-zero W3C trace ID. */
        @JvmStatic
        fun newTraceId(): String {
            val bytes = ByteArray(TRACE_ID_LENGTH / 2)
            do {
                random.nextBytes(bytes)
            } while (bytes.all { it.toInt() == 0 })
            return bytes.joinToString(separator = "") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
        }

        private fun String.normaliseW3cId(expectedLength: Int): String? {
            val candidate = trim().lowercase(Locale.ROOT)
            if (candidate.length != expectedLength || candidate.all { it == '0' }) {
                return null
            }
            return candidate.takeIf { it.all { character -> character in '0'..'9' || character in 'a'..'f' } }
        }
    }
}

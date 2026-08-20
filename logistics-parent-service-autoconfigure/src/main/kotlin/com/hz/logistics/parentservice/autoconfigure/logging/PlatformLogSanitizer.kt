package com.hz.logistics.parentservice.autoconfigure.logging

/**
 * Application-owned replacement point for structured log sanitization.
 *
 * A supplied implementation backs off only the platform sanitizer policy. The
 * logging pipeline remains responsible for preserving structured JSON,
 * correlation fields, and the immutable baseline redaction categories
 * (credentials, authorization, tokens, and secrets) before any sink receives
 * an event.
 */
fun interface PlatformLogSanitizer {

    /** Return a sanitized, preferably immutable view of [event]. */
    fun sanitize(event: PlatformLogEvent): PlatformLogEvent

    companion object {
        /** A pass-through implementation useful when composing test policies. */
        @JvmField
        val IDENTITY: PlatformLogSanitizer = PlatformLogSanitizer { it }
    }
}

/**
 * Stack-neutral structured event passed through the platform sanitizer SPI.
 * Values are intentionally represented as opaque objects so maps, lists, and
 * throwable projections can be sanitized without binding the public API to a
 * particular Logback implementation.
 */
data class PlatformLogEvent(
    val message: String,
    val arguments: List<Any?> = emptyList(),
    val fields: Map<String, Any?> = emptyMap(),
    val throwable: Throwable? = null,
    val traceId: String? = null,
    val spanId: String? = null,
)

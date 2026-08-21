package com.hz.logistics.parentservice.autoconfigure

import com.hz.logistics.parentservice.autoconfigure.errors.PlatformProblemDetailFactory
import com.hz.logistics.parentservice.autoconfigure.logging.PlatformLogEvent
import com.hz.logistics.parentservice.autoconfigure.logging.PlatformLogSanitizer
import com.hz.logistics.parentservice.autoconfigure.metrics.PlatformMetricsCustomizer
import com.hz.logistics.parentservice.autoconfigure.observability.PlatformCorrelationContext
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class CapabilityBackOffTest {

    @Test
    fun applicationMetricsOwnerLeavesErrorsLoggingAndCorrelationActive() {
        runner()
            .withUserConfiguration(ApplicationMetricsOwner::class.java)
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context.getBean(PlatformMetricsCustomizer::class.java))
                    .isSameAs(ApplicationMetricsOwner.CUSTOMIZER)
                assertThat(context).hasSingleBean(PlatformProblemDetailFactory::class.java)
                assertThat(context).hasSingleBean(PlatformLogSanitizer::class.java)
                assertThat(context).hasSingleBean(PlatformCorrelationContext::class.java)

                val registry = context.getBean(MeterRegistry::class.java)
                Counter.builder("backoff.metrics").register(registry).increment()
                assertThat(registry.find("backoff.metrics").counter()?.id?.tags)
                    .noneMatch { it.key == "platform" && it.value == "shared" }
            }
    }

    @Test
    fun applicationErrorOwnerLeavesMetricsLoggingAndCorrelationActive() {
        runner()
            .withUserConfiguration(ApplicationErrorOwner::class.java)
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context.getBeansOfType(PlatformProblemDetailFactory::class.java))
                    .containsOnlyKeys("applicationProblemDetailFactory")
                assertThat(context).hasSingleBean(PlatformLogSanitizer::class.java)
                assertThat(context).hasSingleBean(PlatformCorrelationContext::class.java)
                assertPlatformMetricTag(context.getBean(MeterRegistry::class.java))
            }
    }

    @Test
    fun applicationLoggingOwnerLeavesMetricsErrorsAndCorrelationActive() {
        runner()
            .withUserConfiguration(ApplicationLoggingOwner::class.java)
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context.getBean(PlatformLogSanitizer::class.java))
                    .isSameAs(ApplicationLoggingOwner.SANITIZER)
                assertThat(context).hasSingleBean(PlatformProblemDetailFactory::class.java)
                assertThat(context).hasSingleBean(PlatformCorrelationContext::class.java)
                assertPlatformMetricTag(context.getBean(MeterRegistry::class.java))
            }
    }

    private fun runner(): ApplicationContextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(PlatformCapabilityTestApplication::class.java)
            .withPropertyValues(
                "logistics.parent-service.security.enabled=false",
                "logistics.parent-service.metrics.common-tags.platform=shared",
                "logistics.parent-service.logging.console-enabled=false",
                "logistics.parent-service.logging.otel-enabled=false",
            )

    private fun assertPlatformMetricTag(registry: MeterRegistry) {
        Counter.builder("backoff.active").register(registry).increment()
        assertThat(registry.find("backoff.active").counter()?.id?.tags)
            .anyMatch { it.key == "platform" && it.value == "shared" }
    }
}

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
class PlatformCapabilityTestApplication {

    @Bean
    fun applicationMeterRegistry(): SimpleMeterRegistry = SimpleMeterRegistry()
}

@Configuration(proxyBeanMethods = false)
class ApplicationMetricsOwner {

    @Bean
    fun applicationMetricsCustomizer(): PlatformMetricsCustomizer = CUSTOMIZER

    companion object {
        val CUSTOMIZER = PlatformMetricsCustomizer { _, _ -> }
    }
}

@Configuration(proxyBeanMethods = false)
class ApplicationErrorOwner {

    @Bean
    fun applicationProblemDetailFactory(): PlatformProblemDetailFactory =
        PlatformProblemDetailFactory(PlatformCorrelationContext())
}

@Configuration(proxyBeanMethods = false)
class ApplicationLoggingOwner {

    @Bean
    fun applicationLogSanitizer(): PlatformLogSanitizer = SANITIZER

    companion object {
        val SANITIZER = PlatformLogSanitizer { event: PlatformLogEvent -> event }
    }
}

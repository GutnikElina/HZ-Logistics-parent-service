package com.hz.logistics.parentservice.autoconfigure

import com.hz.logistics.parentservice.autoconfigure.properties.PlatformProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.context.annotation.Configuration

class AutoConfigurationSelectionTest {

    @Test
    fun `keeps shared non-web defaults eligible without a web application`() {
        ApplicationContextRunner()
            .withUserConfiguration(AutoConfigurationTestApplication::class.java)
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context).hasSingleBean(PlatformProperties::class.java)
                assertThat(isFullMatch(context, MVC_SECURITY_AUTO_CONFIGURATION)).isFalse()
                assertThat(isFullMatch(context, WEBFLUX_SECURITY_AUTO_CONFIGURATION)).isFalse()
            }
    }

    @Test
    fun `activates only the Servlet branch for an explicit Servlet application`() {
        WebApplicationContextRunner()
            .withUserConfiguration(AutoConfigurationTestApplication::class.java)
            .withPropertyValues(*platformProperties(), "spring.main.web-application-type=servlet")
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(isFullMatch(context, MVC_SECURITY_AUTO_CONFIGURATION)).isTrue()
                assertThat(isFullMatch(context, WEBFLUX_SECURITY_AUTO_CONFIGURATION)).isFalse()
            }
    }

    @Test
    fun `activates only the Reactive branch for an explicit Reactive application`() {
        ReactiveWebApplicationContextRunner()
            .withUserConfiguration(AutoConfigurationTestApplication::class.java)
            .withPropertyValues(*platformProperties(), "spring.main.web-application-type=reactive")
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(isFullMatch(context, MVC_SECURITY_AUTO_CONFIGURATION)).isFalse()
                assertThat(isFullMatch(context, WEBFLUX_SECURITY_AUTO_CONFIGURATION)).isTrue()
            }
    }

    @Test
    fun `uses the explicitly selected branch when both web APIs are available`() {
        WebApplicationContextRunner()
            .withUserConfiguration(AutoConfigurationTestApplication::class.java)
            .withPropertyValues(*platformProperties(), "spring.main.web-application-type=servlet")
            .run { servletContext ->
                assertThat(servletContext).hasNotFailed()
                assertThat(isFullMatch(servletContext, MVC_SECURITY_AUTO_CONFIGURATION)).isTrue()
                assertThat(isFullMatch(servletContext, WEBFLUX_SECURITY_AUTO_CONFIGURATION)).isFalse()
            }

        ReactiveWebApplicationContextRunner()
            .withUserConfiguration(AutoConfigurationTestApplication::class.java)
            .withPropertyValues(*platformProperties(), "spring.main.web-application-type=reactive")
            .run { reactiveContext ->
                assertThat(reactiveContext).hasNotFailed()
                assertThat(isFullMatch(reactiveContext, MVC_SECURITY_AUTO_CONFIGURATION)).isFalse()
                assertThat(isFullMatch(reactiveContext, WEBFLUX_SECURITY_AUTO_CONFIGURATION)).isTrue()
            }
    }

    private fun isFullMatch(context: org.springframework.context.ConfigurableApplicationContext, source: String): Boolean =
        ConditionEvaluationReport.get(context.beanFactory)
            .conditionAndOutcomesBySource[source]
            ?.isFullMatch
            ?: false

    private fun platformProperties(): Array<String> = arrayOf(
        "logistics.parent-service.security.issuer=https://identity.example.test/realms/logistics",
        "logistics.parent-service.tracing.sampling-probability=1.0",
        "logistics.parent-service.metrics.common-tags.environment=selection-test",
    )

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    class AutoConfigurationTestApplication

    private companion object {
        const val MVC_SECURITY_AUTO_CONFIGURATION =
            "com.hz.logistics.parentservice.autoconfigure.security.mvc.PlatformMvcSecurityAutoConfiguration"
        const val WEBFLUX_SECURITY_AUTO_CONFIGURATION =
            "com.hz.logistics.parentservice.autoconfigure.security.reactive.PlatformWebFluxSecurityAutoConfiguration"
    }
}

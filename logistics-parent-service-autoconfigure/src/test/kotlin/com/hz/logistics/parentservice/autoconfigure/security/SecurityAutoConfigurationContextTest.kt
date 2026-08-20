package com.hz.logistics.parentservice.autoconfigure.security

import com.hz.logistics.parentservice.autoconfigure.PlatformAutoConfiguration
import com.hz.logistics.parentservice.autoconfigure.errors.PlatformProblemDetailFactory
import com.hz.logistics.parentservice.autoconfigure.observability.PlatformCorrelationContext
import com.hz.logistics.parentservice.autoconfigure.security.mvc.PlatformMvcSecurityAutoConfiguration
import com.hz.logistics.parentservice.autoconfigure.security.reactive.PlatformWebFluxSecurityAutoConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.reflect.Proxy

class SecurityAutoConfigurationContextTest {

    @Test
    fun `creates only the selected stack security chain`() {
        mvcRunner().run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context).hasSingleBean(SecurityFilterChain::class.java)
            assertThat(context).doesNotHaveBean(SecurityWebFilterChain::class.java)
        }

        webFluxRunner().run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context).hasSingleBean(SecurityWebFilterChain::class.java)
            assertThat(context).doesNotHaveBean(SecurityFilterChain::class.java)
        }
    }

    @Test
    fun `fails selected stack startup without a valid issuer`() {
        mvcRunner().withoutIssuer().run { context ->
            assertThat(context).hasFailed()
        }

        webFluxRunner().withPropertyValues("logistics.parent-service.security.issuer=issuer.example.test").run { context ->
            assertThat(context).hasFailed()
        }
    }

    @Test
    fun `fails startup for invalid public patterns while platform security is active`() {
        mvcRunner()
            .withPropertyValues("logistics.parent-service.security.public-endpoints[0]=/public/**/health")
            .run { context ->
                assertThat(context).hasFailed()
            }
    }

    @Test
    fun `application owned chains back off only their matching platform branch`() {
        mvcRunner()
            .withUserConfiguration(MvcApplicationSecurityConfiguration::class.java)
            .withoutIssuer()
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context).hasSingleBean(SecurityFilterChain::class.java)
                assertThat(context).hasBean("applicationSecurityFilterChain")
            }

        webFluxRunner()
            .withUserConfiguration(WebFluxApplicationSecurityConfiguration::class.java)
            .withoutIssuer()
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context).hasSingleBean(SecurityWebFilterChain::class.java)
                assertThat(context).hasBean("applicationSecurityWebFilterChain")
            }
    }

    @Test
    fun `reuses application decoders and authority converters without disabling default denial`() {
        mvcRunner()
            .withUserConfiguration(MvcDecoderAndConverterConfiguration::class.java)
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context).hasSingleBean(JwtDecoder::class.java)
                assertThat(context).hasSingleBean(Converter::class.java)
                assertThat(context).hasSingleBean(SecurityFilterChain::class.java)
            }

        webFluxRunner()
            .withUserConfiguration(WebFluxDecoderConfiguration::class.java)
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context).hasSingleBean(ReactiveJwtDecoder::class.java)
                assertThat(context).hasSingleBean(SecurityWebFilterChain::class.java)
            }
    }

    @Test
    fun `security disablement retains independent shared capabilities`() {
        mvcRunner()
            .withoutIssuer()
            .withPropertyValues("logistics.parent-service.security.enabled=false")
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context).doesNotHaveBean(SecurityFilterChain::class.java)
                assertThat(context).hasSingleBean(PlatformCorrelationContext::class.java)
                assertThat(context).hasSingleBean(PlatformProblemDetailFactory::class.java)
            }
    }

    private fun mvcRunner(): WebApplicationContextRunner =
        WebApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    PlatformAutoConfiguration::class.java,
                    PlatformMvcSecurityAutoConfiguration::class.java,
                    PlatformWebFluxSecurityAutoConfiguration::class.java,
                ),
            )
            .withPropertyValues(VALID_ISSUER)

    private fun webFluxRunner(): ReactiveWebApplicationContextRunner =
        ReactiveWebApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    PlatformAutoConfiguration::class.java,
                    PlatformMvcSecurityAutoConfiguration::class.java,
                    PlatformWebFluxSecurityAutoConfiguration::class.java,
                ),
            )
            .withPropertyValues(VALID_ISSUER)

    private fun WebApplicationContextRunner.withoutIssuer(): WebApplicationContextRunner =
        withPropertyValues("logistics.parent-service.security.issuer=")

    private fun ReactiveWebApplicationContextRunner.withoutIssuer(): ReactiveWebApplicationContextRunner =
        withPropertyValues("logistics.parent-service.security.issuer=")

    @Configuration(proxyBeanMethods = false)
    class MvcApplicationSecurityConfiguration {

        @Bean
        fun applicationSecurityFilterChain(): SecurityFilterChain =
            Proxy.newProxyInstance(
                SecurityFilterChain::class.java.classLoader,
                arrayOf(SecurityFilterChain::class.java),
            ) { _, method, _ ->
                when (method.name) {
                    "matches" -> false
                    "getFilters" -> emptyList<Any>()
                    else -> null
                }
            } as SecurityFilterChain
    }

    @Configuration(proxyBeanMethods = false)
    class WebFluxApplicationSecurityConfiguration {

        @Bean
        fun applicationSecurityWebFilterChain(): SecurityWebFilterChain = object : SecurityWebFilterChain {
            override fun matches(exchange: ServerWebExchange): Mono<Boolean> = Mono.just(false)

            override fun getWebFilters(): Flux<WebFilter> = Flux.empty()
        }
    }

    @Configuration(proxyBeanMethods = false)
    class MvcDecoderAndConverterConfiguration {

        @Bean
        fun applicationJwtDecoder(): JwtDecoder = JwtDecoder { throw JwtException("decoder is only a context fixture") }

        @Bean
        fun applicationJwtAuthenticationConverter(): Converter<Jwt, AbstractAuthenticationToken> =
            JwtAuthenticationConverter()
    }

    @Configuration(proxyBeanMethods = false)
    class WebFluxDecoderConfiguration {

        @Bean
        fun applicationReactiveJwtDecoder(): ReactiveJwtDecoder =
            ReactiveJwtDecoder { Mono.error(JwtException("decoder is only a context fixture")) }
    }

    private companion object {
        const val VALID_ISSUER =
            "logistics.parent-service.security.issuer=https://identity.example.test/realms/logistics"
    }
}

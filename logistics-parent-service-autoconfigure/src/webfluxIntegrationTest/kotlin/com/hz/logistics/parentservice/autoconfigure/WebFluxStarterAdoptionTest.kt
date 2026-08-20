package com.hz.logistics.parentservice.autoconfigure

import com.hz.logistics.parentservice.autoconfigure.properties.PlatformProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.web.server.reactive.context.ReactiveWebServerApplicationContext
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.test.context.ActiveProfiles
import reactor.core.publisher.Mono

@SpringBootTest(
    classes = [WebFluxStarterAdoptionTest.WebFluxConsumerApplication::class],
    webEnvironment = WebEnvironment.RANDOM_PORT,
)
@ActiveProfiles("webflux")
class WebFluxStarterAdoptionTest {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var platformProperties: PlatformProperties

    @Test
    fun `starts a WebFlux consumer through the public starter without a Servlet platform branch`() {
        assertThat(applicationContext).isInstanceOf(ReactiveWebServerApplicationContext::class.java)
        assertThat(applicationContext.getBeansOfType(SecurityWebFilterChain::class.java)).isNotEmpty
        assertThat(applicationContext.getBeansOfType(SecurityFilterChain::class.java)).isEmpty()
        assertThat(platformProperties.metrics.commonTags).containsEntry("environment", "adoption-test")
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    class WebFluxConsumerApplication {

        @Bean
        fun reactiveJwtDecoder(): ReactiveJwtDecoder = ReactiveJwtDecoder {
            Mono.error(JwtException("A token is not needed for the startup fixture"))
        }
    }
}

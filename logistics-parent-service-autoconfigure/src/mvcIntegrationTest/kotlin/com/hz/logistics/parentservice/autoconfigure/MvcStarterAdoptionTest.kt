package com.hz.logistics.parentservice.autoconfigure

import com.hz.logistics.parentservice.autoconfigure.properties.PlatformProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    classes = [MvcStarterAdoptionTest.MvcConsumerApplication::class],
    webEnvironment = WebEnvironment.RANDOM_PORT,
)
@ActiveProfiles("mvc")
class MvcStarterAdoptionTest {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var platformProperties: PlatformProperties

    @Test
    fun `starts an MVC consumer through the public starter without a Reactive platform branch`() {
        assertThat(applicationContext).isInstanceOf(ServletWebServerApplicationContext::class.java)
        assertThat(applicationContext.getBeansOfType(SecurityFilterChain::class.java)).isNotEmpty
        assertThat(applicationContext.getBeansOfType(SecurityWebFilterChain::class.java)).isEmpty()
        assertThat(platformProperties.metrics.commonTags).containsEntry("environment", "adoption-test")
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    class MvcConsumerApplication {

        @Bean
        fun jwtDecoder(): JwtDecoder = JwtDecoder {
            throw JwtException("A token is not needed for the startup fixture")
        }
    }
}

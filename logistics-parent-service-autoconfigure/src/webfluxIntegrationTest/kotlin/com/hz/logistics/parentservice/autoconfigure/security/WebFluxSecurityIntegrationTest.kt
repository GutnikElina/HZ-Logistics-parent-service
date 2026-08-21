package com.hz.logistics.parentservice.autoconfigure.security

import com.hz.logistics.parentservice.autoconfigure.support.mockJwt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@SpringBootTest(
    classes = [WebFluxSecurityFixtureApplication::class],
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = [
        "logistics.parent-service.security.issuer=https://identity.example.test/realms/logistics",
        "logistics.parent-service.security.public-endpoints[0]=/public/**",
        "logistics.parent-service.security.role-claims-path=realm_access.roles",
        "management.endpoints.web.base-path=/manage",
        "management.endpoints.web.exposure.include=health,info",
    ],
)
class WebFluxSecurityIntegrationTest(
) {

    @LocalServerPort
    private var port: Int = 0

    private val webTestClient: WebTestClient by lazy {
        WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    @Test
    fun `denies protected requests by default with a safe problem response`() {
        webTestClient.get().uri("/protected").exchange()
            .expectStatus().isUnauthorized
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo(401)
            .jsonPath("$.traceId").isNotEmpty()
    }

    @Test
    fun `permits configured public paths and default health and info endpoints`() {
        webTestClient.get().uri("/public/ping").exchange().expectStatus().isOk
        webTestClient.get().uri("/manage/health").exchange().expectStatus().isOk
        webTestClient.get().uri("/manage/info").exchange().expectStatus().isOk
    }

    @Test
    fun `accepts a mock jwt and applies nested role authorities`() {
        webTestClient.get().uri("/protected")
            .header(HttpHeaders.AUTHORIZATION, "Bearer valid")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.authorities[0]").isEqualTo("ROLE_dispatcher")
            .jsonPath("$.authorities[1]").isEqualTo("ROLE_planner")
    }

    @Test
    fun `accepts a nested string role with the default prefix`() {
        webTestClient.get().uri("/protected")
            .header(HttpHeaders.AUTHORIZATION, "Bearer nested-string")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.authorities[0]").isEqualTo("ROLE_driver")
    }

    @Test
    fun `rejects an invalid signature through the common problem contract`() {
        webTestClient.get().uri("/protected")
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-signature")
            .exchange()
            .expectStatus().isUnauthorized
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo(401)
    }

    @Test
    fun `rejects an expired token through the common problem contract`() {
        webTestClient.get().uri("/protected")
            .header(HttpHeaders.AUTHORIZATION, "Bearer expired")
            .exchange()
            .expectStatus().isUnauthorized
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo(401)
    }

    @Test
    fun `rejects an issuer mismatched token through the common problem contract`() {
        webTestClient.get().uri("/protected")
            .header(HttpHeaders.AUTHORIZATION, "Bearer issuer-mismatch")
            .exchange()
            .expectStatus().isUnauthorized
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo(401)
    }
}

@SpringBootTest(
    classes = [WebFluxSecurityFixtureApplication::class],
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = [
        "logistics.parent-service.security.issuer=https://identity.example.test/realms/logistics",
        "logistics.parent-service.security.role-claims-path=realm_access.roles",
        "logistics.parent-service.security.role-prefix=APP_",
    ],
)
class WebFluxSecurityCustomPrefixIntegrationTest(
) {

    @LocalServerPort
    private var port: Int = 0

    private val webTestClient: WebTestClient by lazy {
        WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    @Test
    fun `applies a custom role prefix in the active WebFlux bearer flow`() {
        webTestClient.get().uri("/protected")
            .header(HttpHeaders.AUTHORIZATION, "Bearer valid")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.authorities[0]").isEqualTo("APP_dispatcher")
            .jsonPath("$.authorities[1]").isEqualTo("APP_planner")
    }
}

@SpringBootTest(
    classes = [WebFluxSecurityFixtureApplication::class],
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = [
        "logistics.parent-service.security.issuer=https://identity.example.test/realms/logistics",
        "logistics.parent-service.security.public-actuator-endpoints=false",
        "management.endpoints.web.base-path=/manage",
        "management.endpoints.web.exposure.include=health,info",
    ],
)
class WebFluxSecurityActuatorOptOutIntegrationTest(
) {

    @LocalServerPort
    private var port: Int = 0

    private val webTestClient: WebTestClient by lazy {
        WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    @Test
    fun `protects actuator endpoints when their public default is disabled`() {
        webTestClient.get().uri("/manage/health").exchange().expectStatus().isUnauthorized
        webTestClient.get().uri("/manage/info").exchange().expectStatus().isUnauthorized
    }
}

@SpringBootTest(
    classes = [WebFluxApplicationOwnedSecurityFixtureApplication::class],
    webEnvironment = WebEnvironment.RANDOM_PORT,
)
class WebFluxApplicationOwnedSecurityIntegrationTest(
) {

    @LocalServerPort
    private var port: Int = 0

    private val webTestClient: WebTestClient by lazy {
        WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    @Autowired
    private lateinit var securityWebFilterChains: Map<String, SecurityWebFilterChain>

    @Test
    fun `backs off platform WebFlux security when the application supplies its own chain`() {
        assertThat(securityWebFilterChains).containsOnlyKeys("applicationSecurityWebFilterChain")
        webTestClient.get().uri("/protected").exchange().expectStatus().isOk
    }
}

@SpringBootConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration
@Import(WebFluxSecurityFixtureController::class)
class WebFluxSecurityFixtureApplication {

    @Bean
    fun reactiveJwtDecoder(): ReactiveJwtDecoder =
        ReactiveJwtDecoder { token ->
            when (token) {
                "trusted", "valid" -> Mono.just(
                    mockJwt(
                        tokenValue = token,
                        claims = mapOf("realm_access" to mapOf("roles" to listOf("dispatcher", "planner"))),
                    ),
                )
                "nested-string" -> Mono.just(
                    mockJwt(
                        tokenValue = token,
                        claims = mapOf("realm_access" to mapOf("roles" to "driver")),
                    ),
                )
                "invalid-signature" -> Mono.error(BadJwtException("invalid signature"))
                "expired" -> Mono.error(BadJwtException("token expired"))
                "issuer-mismatch" -> Mono.error(BadJwtException("issuer mismatch"))
                else -> Mono.error(BadJwtException("unknown test token"))
            }
        }
}

@SpringBootConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration
@Import(WebFluxSecurityFixtureController::class)
class WebFluxApplicationOwnedSecurityFixtureApplication {

    @Bean
    fun applicationSecurityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http.authorizeExchange { it.anyExchange().permitAll() }.build()
}

@RestController
class WebFluxSecurityFixtureController {

    @GetMapping("/public/ping")
    fun publicEndpoint(): Mono<Map<String, String>> = Mono.just(mapOf("status" to "ok"))

    @GetMapping("/protected")
    fun protectedEndpoint(authentication: Authentication?): Mono<Map<String, List<String>>> =
        Mono.just(mapOf("authorities" to authentication?.authorities?.mapNotNull { it.authority }.orEmpty()))
}

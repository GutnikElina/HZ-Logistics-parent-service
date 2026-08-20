package com.hz.logistics.parentservice.autoconfigure.security

import com.hz.logistics.parentservice.autoconfigure.support.mockJwt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootTest(
    classes = [MvcSecurityFixtureApplication::class],
    webEnvironment = WebEnvironment.MOCK,
    properties = [
        "logistics.parent-service.security.issuer=https://identity.example.test/realms/logistics",
        "logistics.parent-service.security.public-endpoints[0]=/public/**",
        "logistics.parent-service.security.role-claims-path=realm_access.roles",
        "management.endpoints.web.base-path=/manage",
        "management.endpoints.web.exposure.include=health,info",
    ],
)
@AutoConfigureMockMvc
class MvcSecurityIntegrationTest(
) {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `denies protected requests by default with a safe problem response`() {
        mockMvc.perform(get("/protected"))
            .andExpect(status().isUnauthorized)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.traceId").isNotEmpty())
    }

    @Test
    fun `permits configured public paths and default health and info endpoints`() {
        mockMvc.perform(get("/public/ping")).andExpect(status().isOk)
        mockMvc.perform(get("/manage/health")).andExpect(status().isOk)
        mockMvc.perform(get("/manage/info")).andExpect(status().isOk)
    }

    @Test
    fun `accepts a mock jwt and applies nested role authorities`() {
        mockMvc.perform(
            get("/protected")
                .with(
                    jwt().jwt { token ->
                        token.claim("realm_access", mapOf("roles" to listOf("dispatcher")))
                    },
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.authorities[0]").value("ROLE_dispatcher"))
    }

    @Test
    fun `rejects an invalid bearer token through the common problem contract`() {
        mockMvc.perform(get("/protected").header(HttpHeaders.AUTHORIZATION, "Bearer malformed"))
            .andExpect(status().isUnauthorized)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
    }
}

@SpringBootTest(
    classes = [MvcSecurityFixtureApplication::class],
    webEnvironment = WebEnvironment.MOCK,
    properties = [
        "logistics.parent-service.security.issuer=https://identity.example.test/realms/logistics",
        "logistics.parent-service.security.public-actuator-endpoints=false",
        "management.endpoints.web.base-path=/manage",
        "management.endpoints.web.exposure.include=health,info",
    ],
)
@AutoConfigureMockMvc
class MvcSecurityActuatorOptOutIntegrationTest(
) {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `protects actuator endpoints when their public default is disabled`() {
        mockMvc.perform(get("/manage/health")).andExpect(status().isUnauthorized)
        mockMvc.perform(get("/manage/info")).andExpect(status().isUnauthorized)
    }
}

@SpringBootTest(
    classes = [MvcApplicationOwnedSecurityFixtureApplication::class],
    webEnvironment = WebEnvironment.MOCK,
)
@AutoConfigureMockMvc
class MvcApplicationOwnedSecurityIntegrationTest(
) {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var securityFilterChains: Map<String, SecurityFilterChain>

    @Test
    fun `backs off platform MVC security when the application supplies its own chain`() {
        assertThat(securityFilterChains).containsOnlyKeys("applicationSecurityFilterChain")
        mockMvc.perform(get("/protected")).andExpect(status().isOk)
    }
}

@SpringBootConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration
@Import(MvcSecurityFixtureController::class)
class MvcSecurityFixtureApplication {

    @Bean
    fun jwtDecoder(): JwtDecoder = JwtDecoder { token ->
        if (token == "trusted") mockJwt() else throw JwtException("invalid test token")
    }
}

@SpringBootConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration
@Import(MvcSecurityFixtureController::class)
class MvcApplicationOwnedSecurityFixtureApplication {

    @Bean
    fun applicationSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http.authorizeHttpRequests { it.anyRequest().permitAll() }.build()
}

@RestController
class MvcSecurityFixtureController {

    @GetMapping("/public/ping")
    fun publicEndpoint(): Map<String, String> = mapOf("status" to "ok")

    @GetMapping("/protected")
    fun protectedEndpoint(authentication: Authentication): Map<String, List<String>> =
        mapOf("authorities" to authentication.authorities.map { it.authority })
}

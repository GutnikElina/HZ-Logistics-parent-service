package com.hz.logistics.parentservice.autoconfigure.security

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PublicEndpointPatternTest {

    @Test
    fun `matches exact literals question marks and segment stars`() {
        val patterns = PublicEndpointPattern.compileAll(
            listOf(
                "/status",
                "/docs/v?/index",
                "/assets/*.json",
            ),
        )

        assertThat(patterns.matches("/status")).isTrue()
        assertThat(patterns.matches("/docs/v1/index")).isTrue()
        assertThat(patterns.matches("/docs/v2/index")).isTrue()
        assertThat(patterns.matches("/assets/platform.json")).isTrue()
        assertThat(patterns.matches("/assets/.json")).isTrue()

        assertThat(patterns.matches("/status/extra")).isFalse()
        assertThat(patterns.matches("/docs/v10/index")).isFalse()
        assertThat(patterns.matches("/assets/releases/platform.json")).isFalse()
        assertThat(patterns.matches("/assets/platform.yaml")).isFalse()
    }

    @Test
    fun `terminal double star matches zero or more complete trailing segments`() {
        val patterns = PublicEndpointPattern.compileAll(listOf("/public/**"))

        assertThat(patterns.matches("/public")).isTrue()
        assertThat(patterns.matches("/public/")).isTrue()
        assertThat(patterns.matches("/public/images/logo.svg")).isTrue()
        assertThat(patterns.matches("/published")).isFalse()
    }

    @Test
    fun `matches application paths independently of query strings and fragments`() {
        val patterns = PublicEndpointPattern.compileAll(listOf("/docs/*.json"))

        assertThat(patterns.matches("/docs/openapi.json?download=true")).isTrue()
        assertThat(patterns.matches("/docs/openapi.json#section")).isTrue()
        assertThat(patterns.matches("/docs/openapi.yaml?download=true")).isFalse()
    }

    @Test
    fun `overlapping permits are a union and list order does not matter`() {
        val first = PublicEndpointPattern.compileAll(listOf("/public/**", "/public/health"))
        val second = PublicEndpointPattern.compileAll(listOf("/public/health", "/public/**"))
        val paths = listOf("/public/health", "/public/assets/logo.svg", "/private/health")

        paths.forEach { path ->
            assertThat(first.matches(path)).isEqualTo(second.matches(path))
        }
        assertThat(first.matches("/public/health")).isTrue()
        assertThat(first.matches("/public/assets/logo.svg")).isTrue()
        assertThat(first.matches("/private/health")).isFalse()
    }

    @ParameterizedTest(name = "rejects invalid public pattern [{0}]")
    @ValueSource(
        strings = [
            "status",
            "",
            "/",
            "//status",
            "/status//health",
            "/status/../health",
            "/status/%2Fhealth",
            "/status/%2fhealth",
            "/orders/{id}",
            "/orders/{id:[0-9]+}",
            "/public/**/health",
            "/public/**suffix",
        ],
    )
    fun `rejects patterns outside the public grammar`(pattern: String) {
        assertThatIllegalArgumentException()
            .isThrownBy { PublicEndpointPattern.compileAll(listOf(pattern)) }
    }
}

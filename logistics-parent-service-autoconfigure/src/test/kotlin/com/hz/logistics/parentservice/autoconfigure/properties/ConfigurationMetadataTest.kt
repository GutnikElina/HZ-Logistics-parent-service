package com.hz.logistics.parentservice.autoconfigure.properties

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConfigurationMetadataTest {

    private val mapper = ObjectMapper()

    @Test
    fun `generated metadata advertises every canonical public property`() {
        val generated = readMetadata("META-INF/spring-configuration-metadata.json")
        val additional = readMetadata("META-INF/additional-spring-configuration-metadata.json")
        val generatedProperties = generated.associateBy { it["name"].asText() }
        val properties = (generated + additional)
            .associateBy { it["name"].asText() }

        val expected = mapOf(
            "logistics.parent-service.security.enabled" to true,
            "logistics.parent-service.security.issuer" to null,
            "logistics.parent-service.security.public-endpoints" to null,
            "logistics.parent-service.security.public-actuator-endpoints" to true,
            "logistics.parent-service.security.role-claims-path" to null,
            "logistics.parent-service.security.role-prefix" to "ROLE_",
            "logistics.parent-service.tracing.enabled" to true,
            "logistics.parent-service.tracing.sampling-probability" to 0.1,
            "logistics.parent-service.tracing.otlp.endpoint" to null,
            "logistics.parent-service.tracing.otlp.protocol" to "HTTP_PROTOBUF",
            "logistics.parent-service.tracing.otlp.headers" to null,
            "logistics.parent-service.tracing.otlp.timeout" to "10s",
            "logistics.parent-service.tracing.otlp.compression" to "GZIP",
            "logistics.parent-service.metrics.enabled" to true,
            "logistics.parent-service.metrics.common-tags" to null,
            "logistics.parent-service.errors.enabled" to true,
            "logistics.parent-service.errors.detail-policy" to "GENERIC",
            "logistics.parent-service.errors.include-instance" to true,
            "logistics.parent-service.logging.enabled" to true,
            "logistics.parent-service.logging.console-enabled" to true,
            "logistics.parent-service.logging.otel-enabled" to true,
            "logistics.parent-service.logging.redaction-mask" to "[REDACTED]",
            "logistics.parent-service.logging.additional-sensitive-fields" to null,
            "logistics.parent-service.logging.additional-sensitive-paths" to null,
        )

        assertThat(properties.keys)
            .filteredOn { it.startsWith("logistics.parent-service.") }
            .containsAll(expected.keys)
        assertThat(generatedProperties.keys).containsAll(expected.keys)

        expected.forEach { (name, defaultValue) ->
            val metadata = requireNotNull(properties[name]) { "Missing metadata for $name" }
            assertThat(metadata["description"].asText())
                .isNotBlank()
            if (defaultValue != null) {
                assertThat(metadata["defaultValue"].asText())
                    .isEqualTo(defaultValue.toString())
            }
        }
    }

    @Test
    fun `metadata contains no alternate root`() {
        val generated = readMetadata("META-INF/spring-configuration-metadata.json")
        val additional = readMetadata("META-INF/additional-spring-configuration-metadata.json")
        val names = (generated + additional)
            .map { it["name"].asText() }

        assertThat(names).noneMatch { it.startsWith("hz.logistics.") }
    }

    private fun readMetadata(resource: String): List<JsonNode> {
        val stream = requireNotNull(javaClass.classLoader.getResourceAsStream(resource)) {
            "Missing metadata resource $resource"
        }
        stream.use { input ->
            return mapper.readTree(input)["properties"].toList()
        }
    }
}

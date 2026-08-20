plugins {
    `java-library`
    kotlin("jvm")
}

dependencies {
    api(enforcedPlatform(project(":logistics-parent-service-bom")))
    api(project(":logistics-parent-service-autoconfigure"))

    api("org.springframework.boot:spring-boot-starter-actuator")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-opentelemetry")
    api("org.springframework.security:spring-security-oauth2-resource-server")
    api("org.springframework.security:spring-security-oauth2-jose")
    api("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

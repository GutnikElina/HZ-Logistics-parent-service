import org.gradle.api.tasks.testing.Test

plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("kapt")
}

dependencies {
    implementation(enforcedPlatform(project(":logistics-parent-service-bom")))
    compileOnly(enforcedPlatform(project(":logistics-parent-service-bom")))
    kapt(enforcedPlatform(project(":logistics-parent-service-bom")))

    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-actuator-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("org.springframework.security:spring-security-core")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("org.springframework.security:spring-security-oauth2-resource-server")

    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly("org.springframework:spring-webmvc")
    compileOnly("org.springframework:spring-webflux")
    compileOnly("org.springframework.security:spring-security-config")
    compileOnly("org.springframework.security:spring-security-web")
    compileOnly("org.springframework.security:spring-security-oauth2-resource-server")
    compileOnly("org.springframework.security:spring-security-oauth2-jose")
    compileOnly("io.projectreactor:reactor-core")
    // The public starter supplies these APIs at runtime. Spring Boot does not
    // ship the OTel Logback appender, so it remains a separate platform input.
    compileOnly("ch.qos.logback:logback-classic")
    compileOnly("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    kapt("org.springframework.boot:spring-boot-configuration-processor")
}

val customTestSourceSets = listOf(
    "mvcIntegrationTest",
    "webfluxIntegrationTest",
    "loggingTest",
)

customTestSourceSets.forEach { sourceSetName ->
    val sourceSet = sourceSets.create(sourceSetName) {
        resources.srcDir("src/$sourceSetName/resources")
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += output + compileClasspath
    }

    configurations.named("${sourceSetName}Implementation") {
        extendsFrom(configurations.testImplementation.get())
    }
    configurations.named("${sourceSetName}RuntimeOnly") {
        extendsFrom(configurations.testRuntimeOnly.get())
    }

    tasks.register<Test>(sourceSetName) {
        description = "Runs the $sourceSetName test suite."
        group = "verification"
        testClassesDirs = sourceSet.output.classesDirs
        classpath = sourceSet.runtimeClasspath
        useJUnitPlatform()
    }
}

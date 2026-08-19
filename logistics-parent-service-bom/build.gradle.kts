plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

val springBootVersion: String by project
val projectGroup: String by project
val platformVersion: String by project

dependencies {
    api(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))

    constraints {
        api("$projectGroup:logistics-parent-service-autoconfigure:$platformVersion")
        api("$projectGroup:logistics-parent-service-starter:$platformVersion")

        api("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.21.0-alpha")
    }
}

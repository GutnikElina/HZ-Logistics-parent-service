import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test

plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

val springBootVersion: String by project
val projectGroup: String by project
val platformVersion: String by project

val bomTestCompileClasspath by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}
val bomTestRuntimeClasspath by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    extendsFrom(bomTestCompileClasspath)
}
val bomTestCompilerClasspath by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    api(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))

    constraints {
        api("$projectGroup:logistics-parent-service-autoconfigure:$platformVersion")
        api("$projectGroup:logistics-parent-service-starter:$platformVersion")

        api("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.21.0-alpha")
    }

    // A java-platform project cannot apply the Java or Kotlin JVM plugin, so
    // its Kotlin JUnit suite is compiled explicitly below.  The test consumes
    // this platform exactly as a downstream application would.
    bomTestCompileClasspath(enforcedPlatform(project(":logistics-parent-service-bom")))
    bomTestCompileClasspath("org.jetbrains.kotlin:kotlin-stdlib")
    bomTestCompileClasspath("org.jetbrains.kotlin:kotlin-test")
    bomTestCompileClasspath("org.junit.jupiter:junit-jupiter-api")
    bomTestCompileClasspath("org.springframework.boot:spring-boot-starter-opentelemetry")
    bomTestCompileClasspath("org.springframework.boot:spring-boot-starter-security")
    bomTestCompileClasspath("ch.qos.logback:logback-classic")
    bomTestCompileClasspath("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0")
    bomTestRuntimeClasspath("org.junit.jupiter:junit-jupiter-engine")
    bomTestRuntimeClasspath("org.junit.platform:junit-platform-launcher")
    bomTestCompilerClasspath("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.2.21")
}

val bomTestClassesDirectory = layout.buildDirectory.dir("classes/kotlin/bomTest")

val compileBomTestKotlin by tasks.registering(JavaExec::class) {
    description = "Compiles the Kotlin BOM dependency-resolution test suite."
    group = "verification"
    classpath = bomTestCompilerClasspath
    mainClass.set("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")

    val sourceFiles = fileTree("src/test/kotlin") { include("**/*.kt") }
    inputs.files(sourceFiles)
    outputs.dir(bomTestClassesDirectory)

    doFirst {
        bomTestClassesDirectory.get().asFile.mkdirs()
        args(
            "-no-stdlib",
            "-no-reflect",
            "-classpath",
            bomTestCompileClasspath.asPath,
            "-jvm-target",
            "17",
            "-d",
            bomTestClassesDirectory.get().asFile.absolutePath,
            *sourceFiles.files.map { it.absolutePath }.toTypedArray(),
        )
    }
}

val bomTest by tasks.registering(Test::class) {
    description = "Runs the BOM dependency-resolution test suite."
    group = "verification"
    dependsOn(compileBomTestKotlin)
    testClassesDirs = files(bomTestClassesDirectory)
    classpath = files(bomTestClassesDirectory) + bomTestRuntimeClasspath
    binaryResultsDirectory.set(layout.buildDirectory.dir("test-results/bomTest/binary"))
    reports.junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/bomTest"))
    reports.html.outputLocation.set(layout.buildDirectory.dir("reports/tests/bomTest"))
    useJUnitPlatform()
}

tasks.named("check") {
    dependsOn(bomTest)
}

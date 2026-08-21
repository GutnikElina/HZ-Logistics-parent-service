# HZ Logistics Parent Service

Shared Kotlin platform infrastructure for HZ Logistics services. The build is
an aggregator with exactly three platform modules:

- `logistics-parent-service-bom` aligns the Spring Boot, Kotlin, security,
  observability, logging, and test dependencies.
- `logistics-parent-service-autoconfigure` contains the implementation and
  conditional Spring Boot auto-configuration.
- `logistics-parent-service-starter` is the thin consumer entry point and has
  no implementation sources or forced web stack.

The baseline is Kotlin 2.2.21, Java 21, Gradle 9.3.0, and Spring Boot 4.0.7.
The root project is not published as a fourth platform module.

## Adoption model

Each consumer uses one enforced BOM and one starter. The consumer chooses its
own application model explicitly:

```kotlin
dependencies {
    implementation(enforcedPlatform("com.hz.logistics:logistics-parent-service-bom:0.1.0"))
    implementation("com.hz.logistics:logistics-parent-service-starter:0.1.0")

    // Choose exactly one when the service is an HTTP application:
    implementation("org.springframework.boot:spring-boot-starter-web")     // MVC
    // implementation("org.springframework.boot:spring-boot-starter-webflux") // WebFlux
}
```

For a non-web service, omit both web starters. The platform detects the
selected Spring application type and contributes only the matching MVC or
WebFlux branch. It never adds `spring-boot-starter-web` or
`spring-boot-starter-webflux` through the public starter.

## Canonical configuration

All public settings use the single `logistics.parent-service.*` namespace.
Capability settings are independent and may be combined as needed:

```yaml
logistics:
  parent-service:
    security:
      enabled: true
      issuer: https://identity.example.test/realms/logistics
      public-endpoints: [/status, /public/**]
      public-actuator-endpoints: true
      role-claims-path: realm_access.roles
      role-prefix: ROLE_
    tracing:
      enabled: true
      sampling-probability: 0.1
      otlp:
        endpoint: https://otel-collector.example.test/v1/traces
        protocol: HTTP_PROTOBUF
        timeout: 10s
        compression: GZIP
    metrics:
      enabled: true
      common-tags:
        service: shipment-api
        environment: production
    errors:
      enabled: true
      detail-policy: GENERIC
      include-instance: true
    logging:
      enabled: true
      console-enabled: true
      otel-enabled: true
      redaction-mask: "[REDACTED]"
```

OTLP header values are configured under `tracing.otlp.headers` when needed,
but are always treated as secrets and are never emitted in diagnostics,
ProblemDetails, structured logs, or telemetry attributes. The generated
configuration metadata documents the complete property set and rejects
alternate roots.

## Override points and back-off

Overrides are capability-scoped. An application can own a complete
`SecurityFilterChain` or `SecurityWebFilterChain`, supply a compatible JWT
decoder/converter, provide an OpenTelemetry owner, select its own
`MeterRegistry`, or replace the platform `PlatformMetricsCustomizer`,
`PlatformProblemDetailFactory`, or `PlatformLogSanitizer`. A compatible
application logging resource takes precedence over the default resource.

Only the corresponding platform contribution backs off; security, tracing,
metrics, errors, and logging do not disable one another. Security overrides
own authorization policy, while compatible error and logging overrides must
preserve the documented external contracts.

## Security, tracing, errors, and logging guarantees

- Security is deny-by-default when enabled, validates issuer-backed JWTs, and
  supports the same public-pattern grammar and nested-role mapping in MVC and
  WebFlux. `ROLE_` is the default role prefix.
- W3C `traceparent` is continued inbound and injected into managed outbound
  clients. Missing or malformed context starts a safe local trace. OTLP export
  is asynchronous and collector failures are diagnostic-only.
- Platform errors use `application/problem+json` with stable type, title,
  status, safe detail, optional sanitized instance, and nonempty `traceId`.
  MVC and WebFlux expose the same field contract.
- Structured Logback JSON is correlated with trace context. Redaction happens
  once at the immutable pre-sink fan-out boundary, so console and OpenTelemetry
  sinks receive the same sanitized event. JWTs, authorization values,
  passwords, tokens, secrets, and configured sensitive fields never reach a
  sink.

The stable contracts and release evidence are catalogued in
[`specs/001-shared-platform-starter/compatibility-review.md`](specs/001-shared-platform-starter/compatibility-review.md), with detailed contracts for
[configuration](specs/001-shared-platform-starter/contracts/configuration.md),
[security](specs/001-shared-platform-starter/contracts/security.md),
[observability](specs/001-shared-platform-starter/contracts/observability.md),
[ProblemDetail](specs/001-shared-platform-starter/contracts/problem-detail.md),
[logging](specs/001-shared-platform-starter/contracts/logging.md), and
[module/back-off behavior](specs/001-shared-platform-starter/contracts/module-and-backoff.md).

## Verification

Use JDK 21 and run the complete release gate:

```bash
./gradlew clean check
```

This includes the BOM suite, unit/context tests, MVC and WebFlux integration
suites, and the logging/redaction suite. The focused commands and acceptance
scenarios are listed in
[`specs/001-shared-platform-starter/quickstart.md`](specs/001-shared-platform-starter/quickstart.md).

## Versioning and migration policy

The current platform baseline is `0.1.0`. PATCH releases are compatible fixes;
MINOR releases add backward-compatible capabilities; MAJOR releases are
required for changes to module responsibilities, dependency direction,
configuration keys/defaults, security behavior, W3C/OTLP propagation,
ProblemDetail fields, logging/redaction behavior, or supported web-stack
selection. Every compatibility-sensitive change must update the compatibility
review, affected contract, regression evidence, and migration notes before
release. The current adoption migration is additive: add the BOM and starter,
select the web stack if applicable, and migrate only to the canonical
`logistics.parent-service.*` keys—there are no supported alternate aliases.

# Compatibility Review: Shared Platform Starter

**Review date:** 2026-08-21  
**Baseline:** `0.1.0` / Spring Boot `4.0.7` / Java `21` / Gradle `9.3.0`  
**Disposition:** Release-ready after the regression evidence listed below

## Scope and Semantic Versioning assessment

This feature establishes the first reusable shared-platform contract. It is an
initial `0.1.0` platform release, with no migration from an earlier supported
platform version. The implementation preserves the constitutional architecture:
the root is an aggregator, and the only platform modules are the BOM,
auto-configuration, and thin starter.

No compatibility-sensitive behavior was intentionally changed during the
final polish review. The only implementation correction was to make the BOM's
Kotlin standard library and test constraints explicitly use the pinned
`kotlinVersion` (`2.2.21`), so the resolved consumer graph matches the declared
baseline instead of allowing the imported Boot BOM to select `2.2.20`.

For subsequent releases:

- PATCH is for compatible defects, diagnostics, and documentation corrections.
- MINOR is for additive properties, opt-in capabilities, and compatible
  extension points.
- MAJOR is required for breaking changes to module responsibilities,
  dependency direction, Java/Spring Boot baseline, canonical property names or
  defaults, security denial/issuer behavior, W3C propagation, ProblemDetail
  fields, logging JSON/redaction, or selected-stack behavior.

## Stable-contract inventory and regression evidence

| Stable surface | Contract | Regression evidence | Migration note |
|---|---|---|---|
| Modules, BOM alignment, starter neutrality, and capability back-off | [`module-and-backoff.md`](contracts/module-and-backoff.md) | `BomAlignmentTest`, `StarterDependencyContractTest`, `AutoConfigurationSelectionTest`, `CapabilityBackOffTest`, `:starter:dependencies`, full `check` | Consumers add one enforced BOM and one starter; no fourth module or web-specific starter is supported. |
| Canonical properties, defaults, validation, and generated metadata | [`configuration.md`](contracts/configuration.md) | `PlatformPropertiesBindingTest`, `ConfigurationMetadataTest`, `SecurityAutoConfigurationContextTest`, `OtlpConfigurationTest` | Use only `logistics.parent-service.*`; there are no alternate-root aliases. Property rename/type/default changes require an explicit migration table. |
| Secure default denial, issuer validation, public patterns, roles, and stack equivalence | [`security.md`](contracts/security.md) | `PublicEndpointPatternTest`, `RoleClaimsAuthorityMapperTest`, `IssuerValidationTest`, `PlatformJwtAuthenticationConverterTest`, `SecurityAutoConfigurationContextTest`, `MvcSecurityIntegrationTest`, `WebFluxSecurityIntegrationTest` | A service-owned chain becomes responsible for its complete security policy; migrating a role path or prefix requires token/authority regression coverage. |
| W3C propagation, OTLP reliability, metrics API, and independent observability back-off | [`observability.md`](contracts/observability.md) | `W3cPropagationTest`, `OtlpConfigurationTest`, `TracingBackOffTest`, `MvcTracingIntegrationTest`, `WebFluxTracingIntegrationTest`, `MvcMetricsIntegrationTest`, `WebFluxMetricsIntegrationTest` | Keep W3C `traceparent` and Micrometer APIs when replacing application observability owners; OTLP headers remain secrets. |
| ProblemDetail media type, fields, safe details, correlation, and handlers | [`problem-detail.md`](contracts/problem-detail.md) | `PlatformProblemDetailFactoryTest`, `MvcProblemDetailIntegrationTest`, `WebFluxProblemDetailIntegrationTest` | Clients must consume the stable fields and `traceId`; changes to type URNs, status mapping, or media type require a major-version migration note. |
| Structured JSON, correlation, redaction, fan-out, and OTel logging | [`logging.md`](contracts/logging.md) | `LoggingRedactionTest`, `LoggingRuntimeCompatibilityTest`, `CapabilityBackOffTest`, `loggingTest` | Custom logging resources/sanitizers must preserve baseline JSON correlation and redaction; new sensitive fields must be added to the corpus and migration notes. |

Regression evidence covers non-web context selection, both supported web
stacks, application-owned overrides, dependency resolution, error responses,
trace correlation, metrics, and both configured logging sinks. The full
release gate is `./gradlew clean check`; the runnable matrix is maintained in
[`quickstart.md`](quickstart.md).

## Migration-note policy

Every compatibility-sensitive change must update this file and the affected
contract before release. The note must state:

1. the old and new behavior, property, dependency, or external field;
2. the SemVer classification and why the change is compatible or breaking;
3. required consumer changes, including MVC and WebFlux examples when both
   branches are affected;
4. deprecation timing, if an old form remains temporarily supported; and
5. the regression commands/tests proving the migration path.

The initial `0.1.0` migration is additive. A consumer should add the enforced
BOM and thin starter, choose one web starter for an HTTP service, and configure
the canonical namespace. `management.opentelemetry.*`, vendor-specific role
claim locations, alternate configuration roots, direct-client propagation, and
platform-provided business endpoints are not compatibility aliases or
supported migration paths.

## Final boundary review

- `settings.gradle.kts` includes exactly the three platform modules.
- The starter has no implementation sources and resolves no MVC/WebFlux starter.
- No domain model, persistence, business workflow, service endpoint, service
  metric, or service authorization policy is present.
- All public configuration is under `logistics.parent-service.*`.
- Spring Boot remains pinned to `4.0.7`; Kotlin is pinned to `2.2.21`; Java and
  bytecode target remain `21`.
- The quickstart filter corrections are documented and all corrected commands
  pass locally; see its validation notes for the launcher/toolchain detail.

# External Contract: Tracing and Metrics

## Tracing API and Implementation

The platform uses Micrometer Observation/Tracing for Spring application instrumentation and an OpenTelemetry 1.55.0 SDK bridge for trace implementation and OTLP export. Application code may use Micrometer observation/tracing APIs; it is not required to use the OpenTelemetry tracing API directly.

## W3C Propagation Contract

W3C Trace Context is the default and required cross-service format.

- A valid inbound `traceparent` is extracted as a remote parent and the server span continues its trace ID.
- A valid `tracestate` is preserved according to the W3C/OpenTelemetry propagator.
- A missing or invalid `traceparent` is ignored without throwing or rejecting the business request; instrumentation starts a safe new local trace.
- Outbound requests created from Spring Boot's managed `RestClient.Builder`, `RestTemplateBuilder`, or `WebClient.Builder` receive a valid `traceparent` for the current context.
- Directly constructed clients are outside the automatic propagation guarantee and are documented as unsupported for this contract.
- Reactive context propagation must preserve the current trace across supported Reactor execution boundaries; a stale/unrelated thread-local trace must never be attached.
- Trace and span IDs are available for structured logging. A problem response always receives a usable `traceId`, including failures before normal handler execution.

W3C validity rules apply: trace ID is 16 nonzero bytes, parent span ID is 8 nonzero bytes, and malformed fields are not trusted.

## OTLP Export Contract

| State | Required behavior |
|---|---|
| No OTLP endpoint | No trace exporter is created by the platform; local tracing/propagation/correlation continue. |
| Valid configured endpoint | Eligible sampled spans are exported asynchronously with configured protocol, headers, timeout, and compression. |
| Collector rejects or is unavailable | Export failure is diagnostic only; requests, error responses, logs, and local metrics continue. |
| Application exporter/customizer exists | Compatible application component is reused or the corresponding default backs off; other capabilities remain active. |
| `tracing.enabled=false` | Platform tracing contribution is absent; security, metrics, errors, and logging remain independently configured. |

OTLP headers are secrets. They must not appear in startup diagnostics, problem details, console JSON, or OpenTelemetry log attributes.

## Sampling

`logistics.parent-service.tracing.sampling-probability` configures the platform sampler in the inclusive range `0.0..1.0`, default `0.1`. Sampling controls recording/export, not whether a valid propagation/correlation context exists. Acceptance tests set it to `1.0` for deterministic export.

## Metrics Contract

Application custom metrics are recorded through Micrometer `MeterRegistry` and instrument builders.

Required supported examples:

- a monotonically increasing counter;
- a timer recording count and total duration;
- a gauge reading an application-owned numeric value.

The platform:

- enables Spring Boot/Micrometer infrastructure when `logistics.parent-service.metrics.enabled=true`;
- applies validated safe `common-tags` through a platform customizer;
- uses any application-selected registry and supports a `SimpleMeterRegistry` in tests;
- does not force Prometheus, OTLP metrics, or another vendor backend;
- does not require or document the OpenTelemetry Metrics API for business code;
- never places credential values or unbounded personal data into tags.

An application registry remains application-owned. A compatible `PlatformMetricsCustomizer` can replace the platform policy contribution without disabling tracing, errors, logging, or security.

## Correlation Contract

For one handled request with an active trace:

```text
inbound traceparent trace-id
        = local server traceId
        = outbound traceparent trace-id
        = ProblemDetail.traceId (if request fails)
        = structured log traceId
```

Span IDs may differ between server and outbound client spans as required by tracing semantics. Tests compare the trace ID, not an assumption that every span ID is identical.

## Verification Contract

Both MVC and WebFlux test source sets must prove:

- valid inbound W3C continuation and valid outbound injection;
- missing/invalid header starts a different valid trace and does not fail the request;
- reactive scheduling retains the correct trace context;
- no endpoint means no exporter but correlation still works;
- a controlled local OTLP HTTP collector receives a trace when configured and sampling is `1.0`;
- a closed/rejecting collector cannot turn a successful request into a failure;
- error response and captured JSON log have the same trace ID;
- application observability beans trigger only tracing back-off;
- counter, timer, and gauge are visible in `SimpleMeterRegistry` for both application types;
- metrics common tags apply and sensitive/high-cardinality tags are absent.

## Compatibility

Propagation format, outbound-client guarantee, sampling default, OTLP activation rule, correlation field names, Micrometer application API, and tracing/metrics back-off behavior are compatibility surfaces requiring review, migration notes, Semantic Versioning classification, and both-stack tests.

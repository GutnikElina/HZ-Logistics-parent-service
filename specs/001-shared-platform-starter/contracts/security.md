# External Contract: Security

## Default Policy

When `logistics.parent-service.security.enabled=true` and the selected web stack has no application-owned security chain:

1. OAuth2 Resource Server JWT authentication is enabled.
2. Every request is authenticated unless it matches an explicit public pattern or the enabled health/info default.
3. Session state is not used to weaken bearer-token authentication.
4. CSRF behavior is configured appropriately for a stateless bearer-token API.
5. Authentication (`401`) and authorization (`403`) failures use the common problem contract.

The platform supplies authentication infrastructure, not service-specific role requirements or endpoint authorization policies.

## Issuer and Token Validation

`logistics.parent-service.security.issuer` identifies the authorization server. The decoder must validate:

- a cryptographic signature from the issuer's discovered keys;
- `iss` equality with the configured issuer;
- expiry and not-before/time validity using Spring Security's standard validators;
- standard JWT structural validity.

The URI must be absolute HTTP(S) with no user-info, query, or fragment. HTTPS is expected outside controlled local test/development environments. A missing or malformed issuer fails startup when the platform default chain would activate. Providing an application-owned complete chain causes the platform chain and its conditional issuer requirement to back off.

Keycloak compatibility is standards-based issuer discovery and signature validation. No realm, client, `realm_access`, or `resource_access` claim location is hardcoded.

## Public Endpoint Pattern Grammar

`logistics.parent-service.security.public-endpoints` is a permit-only list using the same grammar and matcher in MVC and WebFlux.

Allowed forms:

| Form | Meaning | Example |
|---|---|---|
| Literal path | Exact path | `/status` |
| `?` inside a segment | Exactly one character | `/docs/v?/index` |
| `*` inside a segment | Zero or more characters, not `/` | `/docs/*.json` |
| Terminal `/**` | Zero or more complete trailing segments | `/public/**` |

Rules:

- Every pattern starts with `/` and is matched against the application path only.
- Query strings and fragments are not part of matching.
- `**` is allowed only as the final complete segment.
- URI-template variables, inline regex, relative paths, `..`, empty/double segments, and encoded slash tricks are rejected at startup.
- Patterns apply to all HTTP methods. A service that needs method-specific public access must own its security chain.
- Entries only permit; they never deny. Overlap is the set union, order is irrelevant, and every non-matching request remains authenticated.
- Parsed `PathPattern`/`RequestPath` semantics are used for segment-aware normalization in both branches.

When `public-actuator-endpoints=true`, present and exposed health/info actuator endpoints are public using Actuator endpoint matchers, including a customized management base path. This behavior can be disabled independently and does not add those paths to the configured pattern list.

## Nested JWT Role Mapping

`logistics.parent-service.security.role-claims-path` is an optional dot-separated list of nested JSON object keys. For example, `realm_access.roles` traverses claim `realm_access`, then key `roles`. It is configuration, not a hardcoded default.

Extraction algorithm:

1. If the path is absent, add no platform role authorities.
2. Traverse maps by exact key for every path segment.
3. Accept either one string or a collection whose emitted values are strings.
4. Trim values, discard blanks, and de-duplicate while preserving encounter order.
5. Prefix each role with `logistics.parent-service.security.role-prefix`, whose default is `ROLE_`.
6. If any path element is missing/null, traversal reaches a non-map, or the final value is neither a string nor a string collection, return no mapped roles.
7. A mixed-type collection is malformed and maps no roles; values are not coerced.

An empty configured prefix is allowed. The platform does not strip an existing prefix, infer a vendor role, or invent an authority when mapping fails.

## MVC and WebFlux Equivalence

| Client-observable case | MVC | WebFlux |
|---|---|---|
| Protected request, no bearer token | `401` problem body | Same |
| Invalid signature, expired token, not-yet-valid token, issuer mismatch | `401` problem body | Same |
| Valid JWT, missing required application role | `403` problem body | Same |
| Configured public path, no token | Allowed | Same |
| Non-matching path, no token | Denied | Same |
| Valid nested roles and default/custom prefix | Equivalent authorities | Same |
| Application chain present | Platform selected-stack chain absent | Same |

Headers such as `WWW-Authenticate` required by bearer-token standards remain present. When a response body is emitted, its content type and body follow [problem-detail.md](./problem-detail.md).

## Branch Conditions and Back-Off

- Servlet/MVC configuration requires MVC/Servlet security classes and a Servlet web application, and creates a default only when no application `SecurityFilterChain` exists.
- Reactive configuration requires WebFlux/reactive security classes and a Reactive web application, and creates a default only when no application `SecurityWebFilterChain` exists.
- If both API classpaths exist, selected application type is decisive; both chains must never be created.
- An application `JwtDecoder` or `ReactiveJwtDecoder` is reused by the corresponding platform chain.
- A documented application authority converter is reused without disabling default denial.
- Security back-off does not disable tracing, metrics, errors, or logging.

## Verification Contract

Both stack suites must use mock JWTs and controlled decoders to prove:

- default denial and configured public access;
- valid token success;
- invalid signature, expired/not-yet-valid, and issuer-mismatch rejection;
- absent, null, malformed, mixed-type, string, and list nested-role claims;
- default `ROLE_`, custom, and empty prefixes;
- health/info default and opt-out with a nondefault management base path;
- identical client status/problem shape;
- application-owned chain back-off and decoder/converter reuse;
- only the selected branch exists when both web API classpaths are present.

## Compatibility

Default access policy, issuer behavior, public pattern grammar, role mapping, prefix semantics, error shape, back-off trigger, or web-stack equivalence changes require explicit compatibility review, migration notes, Semantic Versioning classification, and both-stack regression evidence.

## Release regression evidence and migration note

Regression evidence is provided by `PublicEndpointPatternTest`,
`RoleClaimsAuthorityMapperTest`, `IssuerValidationTest`,
`PlatformJwtAuthenticationConverterTest`, `SecurityAutoConfigurationContextTest`,
`MvcSecurityIntegrationTest`, and `WebFluxSecurityIntegrationTest`. A service
that replaces the default chain must retain its required authorization policy;
the initial migration only adds the BOM/starter and canonical issuer/role
settings.

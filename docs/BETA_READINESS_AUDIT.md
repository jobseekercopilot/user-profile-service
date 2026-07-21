# Beta-readiness audit: user-profile service

Audit date: 18 July 2026

Status: **Not beta-ready.** PROFILE-02, PROFILE-03, PROFILE-06, PROFILE-08 and PROFILE-09 are
remediated, but lifecycle and remaining production security controls still need
completion.

## Findings

| ID | Finding | Evidence | Risk and severity | Recommended solution and acceptance criteria | Dependencies | Beta blocker | Effort |
|---|---|---|---|---|---|---|---|
| [PROFILE-01](https://github.com/jobseekercopilot/user-profile-service/issues/1) | Prevent forged and cross-user identity | **Remediated:** the profile API is an RS256 resource server using authentication-service JWKS; issuer, audience, expiry, type and signature are validated and ownership comes only from `sub`. | Direct callers, forged headers/signatures and cross-user body IDs cannot select another owner. | Retain negative security tests and coordinate JWKS rotation/cache windows; complete the UMG/client bearer path before beta readiness. | AUTH-13 complete; UMG/client adoption remains. | Yes | L |
| [PROFILE-02](https://github.com/jobseekercopilot/user-profile-service/issues/2) | Introduce production database configuration and migrations | **Remediated:** PostgreSQL 17, Flyway-owned schema/constraints, fail-closed production configuration and automated backup/restore evidence replace runtime H2 and `ddl-auto=update`. | The evidenced High durability/schema risk is resolved; operational provisioning and PROFILE-04 retention decisions remain external. | Keep migrations append-only; retain empty/previous-schema, constraint and restore tests; exercise the documented restore procedure before beta. | PostgreSQL platform pattern established. | No | L |
| [PROFILE-03](https://github.com/jobseekercopilot/user-profile-service/issues/3) | Bound and normalise all profile input | **Remediated:** nested Bean Validation, request/list/string/numeric/postcode/date bounds, NFC/canonical normalization, server-owned IDs and stable redacted field errors are enforced and integration tested. | The evidenced High input-abuse and inconsistent-normalisation risk is resolved; PROFILE-01 still owns caller identity trust. | Keep gateway and profile bounds coordinated; retain negative integration and body-limit tests. | Shared limits implemented from the gateway contract. | No | L |
| [PROFILE-04](https://github.com/jobseekercopilot/user-profile-service/issues/4) | Define deletion, retention, export and audit behaviour | API exposes only GET/PUT; no account deletion, export, retention or auditable change event exists. | **High / P1 privacy:** beta support cannot fulfil lifecycle decisions or investigate changes. | Make explicit non-legal privacy decisions; implement or document deletion/export/retention, audit events and operational procedures; test cascade deletion. | AUTH-08. | Yes | L |
| [PROFILE-05](https://github.com/jobseekercopilot/user-profile-service/issues/5) | Make upsert concurrency-safe | `findByUserId` followed by save can race; a unique constraint exists but conflicts are not mapped/idempotent. | **Medium / P1 reliability:** concurrent registration/profile saves can fail unpredictably. | Use transactional, database-safe upsert/versioning; map conflicts; test concurrent duplicate requests and retry behaviour. | PROFILE-02. | Yes | M |
| [PROFILE-06](https://github.com/jobseekercopilot/user-profile-service/issues/6) | Prove environment-data controls | **Remediated:** the controller and credential filter exist only with the explicit `environment-data` profile; startup additionally requires one approved non-production environment, an enable switch and an independent minimum-strength credential. | The evidenced destructive-endpoint exposure is resolved; access still depends on secure runtime secret distribution and network isolation. | Keep the credential independent, never enable the profile in production-like environments, and retain profile-combination and negative authentication tests. | AUTH-04 service identity contract implemented. | No | M |
| [PROFILE-07](https://github.com/jobseekercopilot/user-profile-service/issues/7) | Add repository, security and integration coverage | Tests mock the repository; no real migration, ownership, concurrency, cascade or full-path test exists. | **High / P1 testing:** persistence and cross-user guarantees are unproven. | Add DB integration/contract/security tests and include the complete browser journey. | PROFILE-01–05. | Yes | L |
| [PROFILE-08](https://github.com/jobseekercopilot/user-profile-service/issues/8) | Add readiness/telemetry and harden container/docs | **Remediated:** redacted DB-aware readiness, bounded profile outcome/latency metrics, PII-free logs, dashboard/alert/runbook contracts, graceful shutdown and a test-enforcing digest-pinned non-root image with a blocking image scan are present. | The repository operational baseline is complete; private exporter and target-environment alert delivery remain platform readiness validation. | Retain the container/runtime and privacy tests; connect the registry and prove alert delivery in the controlled beta environment. | PROFILE-02 complete; monitoring platform owner for environment validation. | Yes | M |
| [PROFILE-09](https://github.com/jobseekercopilot/user-profile-service/issues/9) | Establish reliable dependency vulnerability scanning | CI emits `mvn dependency:tree` but performs no vulnerability analysis; no dependable advisory-feed cache or risk-acceptance workflow is configured. | **High / P1 dependency:** libraries handling personal profile data can carry unreviewed Critical/High vulnerabilities. | Select a proprietary-compatible Maven scanner, configure authenticated/cached advisory data, publish a machine-readable report, fail on unaccepted Critical/High findings and document the risk-acceptance process. | Platform CI and advisory-feed decision. | Yes | M |

## PROFILE-08 remediation evidence

- Aggregate health is redacted; readiness includes application and database
  state without URL, credential, query, product/version or exception details.
- Low-cardinality read/upsert outcome counters and latency histograms use only
  fixed operation, outcome and status-family labels. User/profile identifiers
  were removed from logs and tests enforce the label allowlist.
- The release workflow runs `mvn -B clean verify` before the image can copy the
  verified JAR. The image pins its runtime base by digest, applies fixed runtime
  security packages, runs as `10001:10001`, declares a
  database-aware health check and is tested read-only through graceful stop.
- CI scans the rebuilt image for Critical/High OS and library vulnerabilities.
  Operations, dashboards, alerts, ownership and residual risks are documented
  without claiming a deployed production monitoring stack.

## PROFILE-09 remediation evidence

PROFILE-09 remediates the dependency-scanning finding; the service remains not
beta-ready because the other findings above are unresolved.

- The verified pre-remediation runtime set contained 83 Java packages and 31
  Critical/High findings (4 Critical and 27 High).
- Spring Boot was upgraded from 3.2.0 to 4.1.0, springdoc-openapi to 3.0.3,
  Lombok to 1.18.46, and the Spring Boot 4 REST client module/imports were
  adopted. The post-remediation scan covered 102 packages with zero Critical
  or High findings.
- CI uses pinned Trivy and action revisions, caches advisory data, scans only
  Maven's resolved runtime dependency directory, uploads the JSON report, and
  applies a fail-closed policy after report generation.
- Policy tests prove rejection of Critical findings, malformed or uncovered
  reports, and missing, invalid, or expired risk-exception metadata.
- The decision, evidence, local commands, exception rules, and residual risk
  are recorded in `docs/DEPENDENCY_SECURITY.md`.

## PROFILE-03 remediation evidence

- Validation is recursive across skills, aspirations, work preferences,
  qualifications and roles, including non-null entries, list counts, required
  fields, dates, coordinates, commute range and UK postcode/outcode syntax.
- Accepted strings are stripped and Unicode-normalized to NFC. Postcodes use
  uppercase canonical spacing; blank optional values and null lists have stable
  storage representations.
- Request-body IDs and user IDs are read-only, and the service derives ownership
  only from the validated access-token subject.
- JSON bodies are bounded independently of declared content length. Stable
  validation responses contain only field/code pairs and correlation metadata,
  never rejected profile content or parser details.
- Random-port persistence tests cover valid Unicode normalization, null/empty,
  oversize, nested numeric/postcode boundaries, conditional qualification
  rules, malformed JSON, media type and the request-size limit.

## PROFILE-02 remediation evidence

- PostgreSQL 17 is the only runtime database dependency; H2 is test-scoped and
  the production profile rejects non-PostgreSQL URLs or blank credentials.
- Flyway V1 creates the complete profile/nested schema with unique-owner,
  foreign-key, cascade and domain constraints; V2 adds relationship indexes.
  Hibernate is limited to schema validation in every environment.
- Disposable PostgreSQL tests prove empty and V1-to-V2 migrations, retention
  of every nested relation, rejection of duplicate/orphan/out-of-domain data,
  cascade cleanup, and custom-format backup/restore to a separate database.
- The production-profile Compose stack applies both migrations, reports healthy,
  and persists/read-backs a synthetic nested profile. Operations, rollback,
  restore ownership, secret handling and legacy H2 treatment are documented.

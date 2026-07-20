# Beta-readiness audit: user-profile service

Audit date: 18 July 2026

Status: **Not beta-ready.** Current-tree `mvn -B verify` passed 22 tests, but
the service trusts a caller-supplied identity header and lacks production data
management.

## Findings

| ID | Finding | Evidence | Risk and severity | Recommended solution and acceptance criteria | Dependencies | Beta blocker | Effort |
|---|---|---|---|---|---|---|---|
| [PROFILE-01](https://github.com/jobseekercopilot/user-profile-service/issues/1) | Prevent forged and cross-user identity | `UserProfileController` treats `X-User-Id` as authenticated identity; there is no security filter or trusted-proxy enforcement. | **Critical / P0 security/privacy:** any direct caller can read or overwrite another user's profile. | Authenticate the caller or cryptographically trust a service identity; derive owner server-side; deny external direct access; test forged headers and cross-user attempts. | Gateway/service identity design. | Yes | L |
| [PROFILE-02](https://github.com/jobseekercopilot/user-profile-service/issues/2) | Introduce production database configuration and migrations | Defaults are file H2, blank password, console exposed remotely, `ddl-auto=update`; no migrations or restore evidence. | **High / P1 data/reliability:** weak durability and unreproducible schema. | Add separate production profile, supported DB, versioned migrations/constraints, backup/restore and clean-clone migration tests. | Platform database decision. | Yes | L |
| [PROFILE-03](https://github.com/jobseekercopilot/user-profile-service/issues/3) | Bound and normalise all profile input | Only qualification/role validators exist; skills/list counts/string lengths/postcodes/commute ranges and request size are not comprehensively constrained. | **High / P1 API/security:** oversized or malformed PII can cause storage abuse and inconsistent search behaviour. | Add Bean Validation and normalisation across nested models; stable field errors; test null/empty/Unicode/oversize/boundaries. | Shared contract and location rules. | Yes | L |
| [PROFILE-04](https://github.com/jobseekercopilot/user-profile-service/issues/4) | Define deletion, retention, export and audit behaviour | API exposes only GET/PUT; no account deletion, export, retention or auditable change event exists. | **High / P1 privacy:** beta support cannot fulfil lifecycle decisions or investigate changes. | Make explicit non-legal privacy decisions; implement or document deletion/export/retention, audit events and operational procedures; test cascade deletion. | AUTH-08. | Yes | L |
| [PROFILE-05](https://github.com/jobseekercopilot/user-profile-service/issues/5) | Make upsert concurrency-safe | `findByUserId` followed by save can race; a unique constraint exists but conflicts are not mapped/idempotent. | **Medium / P1 reliability:** concurrent registration/profile saves can fail unpredictably. | Use transactional, database-safe upsert/versioning; map conflicts; test concurrent duplicate requests and retry behaviour. | PROFILE-02. | Yes | M |
| [PROFILE-06](https://github.com/jobseekercopilot/user-profile-service/issues/6) | Prove environment-data controls | `/internal/system-data` is enabled by properties/profile lists and has no independent authentication; production safety depends on profile naming. | **High / P1 security:** misnamed production environments may expose destructive seed/delete operations. | Require explicit test-only bean/profile plus service authentication; fail closed on ambiguous environment; test every production-like profile. | Service identity design. | Yes | M |
| [PROFILE-07](https://github.com/jobseekercopilot/user-profile-service/issues/7) | Add repository, security and integration coverage | Tests mock the repository; no real migration, ownership, concurrency, cascade or full-path test exists. | **High / P1 testing:** persistence and cross-user guarantees are unproven. | Add DB integration/contract/security tests and include the complete browser journey. | PROFILE-01–05. | Yes | L |
| [PROFILE-08](https://github.com/jobseekercopilot/user-profile-service/issues/8) | Add readiness/telemetry and harden container/docs | Health always shows details; no DB readiness/metrics/alerts; Docker skips tests, runs root, has mutable tags; README lists nonexistent CRUD routes and MIT. | **Medium / P1 operational/docs:** failures are hard to diagnose and deployment instructions are wrong. | Add redacted metrics/readiness/runbook, pin/non-root/scan image, run verify, and document actual API/config/proprietary licence. | PROFILE-02. | Yes | M |
| [PROFILE-09](https://github.com/jobseekercopilot/user-profile-service/issues/9) | Establish reliable dependency vulnerability scanning | CI emits `mvn dependency:tree` but performs no vulnerability analysis; no dependable advisory-feed cache or risk-acceptance workflow is configured. | **High / P1 dependency:** libraries handling personal profile data can carry unreviewed Critical/High vulnerabilities. | Select a proprietary-compatible Maven scanner, configure authenticated/cached advisory data, publish a machine-readable report, fail on unaccepted Critical/High findings and document the risk-acceptance process. | Platform CI and advisory-feed decision. | Yes | M |

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

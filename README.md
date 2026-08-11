# User Profile Service

## Role in Job Seeker Copilot

| Role | Called by | Calls | Data | Local port |
|---|---|---|---|---:|
| Claimant profile, preferences, Evidence Library and immutable snapshot owner | User Management, Job Finder, Document Generation and Reporting | Authentication JWKS | Own PostgreSQL database | 8085 |

See the central [account journey](https://docs.jobseekercopilot.com/journeys/account-authentication/), [document journey](https://docs.jobseekercopilot.com/journeys/documents/), and [data ownership](https://docs.jobseekercopilot.com/data/ownership/).

Spring Boot OAuth2 resource service that creates, retrieves and replaces the
current authenticated user's job seeker profile. Ownership is derived only from
the validated access-token `sub` claim.

> Delivery status: implemented, persisted and exercised for controlled
> private-beta profile/evidence use; production deployment assurance remains a
> separate gate. See
> [the audit](docs/BETA_READINESS_AUDIT.md).

User Profile's role as the owner of subject-scoped search defaults, rather than
the search orchestrator, is defined in the Infrastructure
[Job Search architecture ADR](https://github.com/jobseekercopilot/infrastructure/blob/develop/docs/adr/0001-job-search-architecture-and-ownership.md).

## Requirements and configuration

- Java 17 and Maven 3.9
- PostgreSQL 17
- PostgreSQL JDBC 42.7.12 (explicitly pinned to the reviewed fixed release)

| Variable | Local default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8085` | HTTP port |
| `PROFILE_DB_URL` | `jdbc:postgresql://localhost:5432/user_profile` locally | PostgreSQL JDBC URL |
| `PROFILE_DB_USERNAME` | `user_profile` locally | Database user |
| `PROFILE_DB_PASSWORD` | none | Required database password; inject as a secret |
| `APP_LOG_LEVEL` | `INFO` | Application log level |
| `PROFILE_REQUEST_MAXIMUM_BODY_BYTES` | `65536` | Maximum JSON request body size; must be positive |
| `AUTH_JWKS_URI` | local authentication service | Authentication-service public JWKS endpoint |
| `PROFILE_JWT_ISSUER` | `job-seeker-copilot-authentication` | Required access-token issuer |
| `PROFILE_JWT_AUDIENCE` | `job-seeker-copilot-services` | Required access-token audience |
| `ENVIRONMENT_DATA_ENABLED` | `false` | Additional opt-in for non-production fixture management |
| `ENVIRONMENT_DATA_TOKEN` | none | Independent secret of at least 32 characters for fixture-management requests |
| `ENVIRONMENT_DATA_ALLOWED_ENVIRONMENTS` | `local,test,demo` | Approved subset of the fixed non-production profile allowlist |

## API, health and build

- `GET /api/profiles/me` with a Bearer access token
- `PUT /api/profiles/me` with a Bearer access token and optional revision `If-Match`
- `PATCH /api/profiles/me` to update job-search preferences and canonical reusable skills
- `GET /api/profiles/me/export` for a synchronous, no-store personal-data export
- `GET /api/evidence` for the authenticated claimant's versioned Evidence Library
- `POST /api/evidence` to create a claimant-owned draft
- `GET /api/evidence/{entryId}` for one owner-scoped evidence entry
- `PUT /api/evidence/{entryId}` to create an edited draft revision
- `POST /api/evidence/{entryId}/{confirm|hide|show|archive|restore|supersede}` for explicit lifecycle actions
- `POST /api/evidence/snapshots` to issue an owner-scoped, purpose-bound immutable selection
- `GET /api/evidence/snapshots/{snapshotId}` to resolve an owned snapshot and its stable facts
- `/actuator/health`
- `/actuator/health/readiness` (application and redacted database status)

The producer-owned, versioned OpenAPI source contract is
[`api/openapi.json`](api/openapi.json). Consumers pin both the repository
revision and the checksum recorded in [`api/SHA256SUMS`](api/SHA256SUMS);
runtime API documentation remains disabled in production. See
[`api/README.md`](api/README.md) for the compatibility and update workflow.

The destructive `/internal/system-data/**` fixture API is absent unless the
explicit `environment-data` profile is active. It also requires exactly one of
`local`, `test`, or `demo`, the additional enabled switch, and a valid
`X-Environment-Data-Token`. Production-like or ambiguous profile combinations
stop startup. See [environment-data controls](docs/ENVIRONMENT_DATA_CONTROLS.md).

`PUT /api/profiles/me` accepts at most 100 skills, 50 qualifications, 50
roles, and 50 target roles. Text and nested numeric limits match the gateway
contract; role and qualification dates use `YYYY-MM` or `YYYY-MM-DD`; commute range is 0–500;
coordinates use normal latitude/longitude ranges; and postcode values must be a
UK full postcode or outcode. Request-body `id`/`userId` and `X-User-Id` cannot
select ownership. Accepted text is stripped and Unicode-normalized
to NFC, while postcodes are stored uppercase with canonical spacing. `id` and
`userId` in JSON are ignored because both are server-owned.

Invalid input returns a versioned `PROFILE_VALIDATION_FAILED` response with
stable field/code pairs, a correlation ID and no rejected PII. Malformed JSON,
unsupported content types and bodies above the configured limit have separate
stable error codes. See [the profile input contract](docs/PROFILE_INPUT_CONTRACT.md).

Profile replacement is concurrency-safe per user. PostgreSQL transactions take
a database-wide advisory lock derived from the trusted user ID before reading or
writing, so concurrent creates and updates serialize across service instances.
Repeated identical PUT requests are safe and retain a single profile row. The
database uniqueness constraint remains a defense-in-depth check; an unexpected
integrity conflict returns `409 PROFILE_WRITE_CONFLICT` without database details
and the caller may retry the complete PUT request.

Every profile response now carries a monotonic `revision`, opaque `revisionId`,
SHA-256 `contentDigest`, and matching `ETag`. Revision-aware consumers send that
ETag in `If-Match`; stale writes fail with `409 PROFILE_REVISION_CONFLICT`.
Omitting `If-Match` remains supported during the approved legacy-client
migration window. An identical normalized update does not create a new
revision, and omitted preferences remain unset rather than becoming claimant
declarations.

`PATCH /api/profiles/me` preserves historical roles, qualifications, and
Evidence Library records while updating job-search preferences and canonical
reusable skills. Omitting `skills` or sending `null` preserves the existing
canonical skills; sending an explicit empty array clears them.

Flyway V3 adds the bounded Evidence Library schema without removing legacy
profile roles or qualifications. Startup migrates those legacy rows into
owner-scoped `DRAFT`, `ACTIVE`, `VISIBLE`, review-required entries. A
source-content hash and unique migration key make reruns idempotent. Imported
rows are never silently marked user-confirmed, and the original profile fields
remain readable by old clients. Evidence categories, confirmation, visibility,
lifecycle, partial dates, immutable revision metadata and atomic fact IDs are
all represented separately. Permanent deletion of evidence referenced by
future documents or applications is intentionally not enabled pending the
required retention-policy approval.

Flyway V4 adds claimant-declared employment types, working patterns, workplace
arrangements and explicit availability without inventing defaults. It also adds
an optimistic entry version used by Evidence Library `ETag`/`If-Match`.
Evidence writes are bounded and category-aware, supporting links must be HTTPS,
and HTML-shaped content is rejected. Editing or confirming creates a new
immutable revision; visibility and lifecycle remain independent, and archived
entries are excluded from the default list while remaining recoverable.

Flyway V5 adds immutable CV and cover-letter evidence snapshots. The browser
submits only a purpose, ordered server-issued entry IDs and safe category
ordering. The service derives the owner from the access token, locks and
authorizes each entry, resolves only the latest visible active user-confirmed
revision, copies its stable fact IDs, and records profile/revision digests.
Draft, review-required, hidden, archived, superseded, stale, duplicate and
cross-owner selections fail closed. Evidence is never selected automatically.

```bash
./scripts/test-api-contract-policy.sh
./scripts/verify-api-contract.sh
mvn -B verify
./scripts/test-dependency-report-policy.sh
./scripts/verify-container.sh
PROFILE_DB_PASSWORD='<private local value>' mvn spring-boot:run
```

The release-shaped container workflow runs the complete verification before
building from the verified JAR, uses a digest-pinned runtime base, runs as fixed UID/GID `10001:10001`, has a
database-aware readiness check, supports graceful shutdown and is scanned in
CI. See [service operations](docs/OPERATIONS.md) and the
[observability contract](docs/OBSERVABILITY.md).

For an isolated local stack, copy `.env.example` to the ignored `.env`, set a
private database password, run `mvn -B clean verify`, then run
`docker compose up --build --wait`. The
stack starts PostgreSQL and the production profile; `docker compose down`
retains its named data volume. See [database operations](docs/DATABASE_OPERATIONS.md)
for migration, backup, restore, rollback, and legacy-H2 handling.

CI scans the resolved runtime dependency set with pinned Trivy releases,
publishes the JSON report, and rejects unaccepted Critical or High findings.
The resolved PostgreSQL JDBC version is regression-tested so parent dependency
management cannot silently reintroduce the remediated SCRAM downgrade flaw.
See [dependency security](docs/DEPENDENCY_SECURITY.md) for local reproduction,
scanner scope, and the time-bounded exception process.

Flyway owns the checked-in schema and Hibernate validates it. H2 is test-only;
production fails closed unless PostgreSQL and explicit credentials are supplied.

## Branch workflow and troubleshooting

Use `feature/* → develop`; `main` will be introduced as a release branch later.
For 401, verify the authentication-service JWKS is reachable and the token has
the configured RS256 key ID, issuer, audience, lifetime, access type and subject.
For 404, confirm a profile exists for the authenticated account. Never place
profile PII, bearer tokens or key material in logs.

## Licence

Copyright © 2026 Bernard McGeever. All rights reserved.

This repository contains proprietary software belonging to Bernard McGeever.
It may not be used, copied, modified or distributed without express written
permission. See [LICENSE](./LICENSE).

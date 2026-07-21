# User Profile Service

Spring Boot service that creates, retrieves and replaces the current user's job
seeker profile. The current gateway integration passes identity in
`X-User-Id`; that trust boundary is a P0 issue.

> Beta status: not beta-ready. See
> [the audit](docs/BETA_READINESS_AUDIT.md).

## Requirements and configuration

- Java 17 and Maven 3.9
- PostgreSQL 17

| Variable | Local default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8085` | HTTP port |
| `PROFILE_DB_URL` | `jdbc:postgresql://localhost:5432/user_profile` locally | PostgreSQL JDBC URL |
| `PROFILE_DB_USERNAME` | `user_profile` locally | Database user |
| `PROFILE_DB_PASSWORD` | none | Required database password; inject as a secret |
| `APP_LOG_LEVEL` | `INFO` | Application log level |
| `PROFILE_REQUEST_MAXIMUM_BODY_BYTES` | `65536` | Maximum JSON request body size; must be positive |

## API, health and build

- `GET /api/profiles/me` with trusted user identity
- `PUT /api/profiles/me` with trusted user identity
- `/actuator/health`

`PUT /api/profiles/me` accepts at most 100 skills, 50 qualifications, 50
roles, and 50 target roles. Text and nested numeric limits match the gateway
contract; role and qualification dates use `YYYY-MM` or `YYYY-MM-DD`; commute range is 0–500;
coordinates use normal latitude/longitude ranges; and postcode values must be a
UK full postcode or outcode. Accepted text is stripped and Unicode-normalized
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

```bash
mvn -B verify
./scripts/test-dependency-report-policy.sh
PROFILE_DB_PASSWORD='<private local value>' mvn spring-boot:run
docker build -t user-profile-service .
```

For an isolated local stack, copy `.env.example` to the ignored `.env`, set a
private database password, then run `docker compose up --build --wait`. The
stack starts PostgreSQL and the production profile; `docker compose down`
retains its named data volume. See [database operations](docs/DATABASE_OPERATIONS.md)
for migration, backup, restore, rollback, and legacy-H2 handling.

CI scans the resolved runtime dependency set with pinned Trivy releases,
publishes the JSON report, and rejects unaccepted Critical or High findings.
See [dependency security](docs/DEPENDENCY_SECURITY.md) for local reproduction,
scanner scope, and the time-bounded exception process.

Flyway owns the checked-in schema and Hibernate validates it. H2 is test-only;
production fails closed unless PostgreSQL and explicit credentials are supplied.

## Branch workflow and troubleshooting

Use `feature/* → develop`; `main` will be introduced as a release branch later.
For 404, confirm a profile exists for the authenticated account. Do not call
this service directly with a user-selected ID or place profile PII in logs.

## Licence

Copyright © 2026 Bernard McGeever. All rights reserved.

This repository contains proprietary software belonging to Bernard McGeever.
It may not be used, copied, modified or distributed without express written
permission. See [LICENSE](./LICENSE).

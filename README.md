# User Profile Service

Spring Boot service that creates, retrieves and replaces the current user's job
seeker profile. The current gateway integration passes identity in
`X-User-Id`; that trust boundary is a P0 issue.

> Beta status: not beta-ready. See
> [the audit](docs/BETA_READINESS_AUDIT.md).

## Requirements and configuration

- Java 17 and Maven 3.9
- local or approved production database

| Variable | Local default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8085` | HTTP port |
| `PROFILE_DB_URL` | local file H2 | Local-only profile database |
| `APP_LOG_LEVEL` | `INFO` | Application log level |

## API, health and build

- `GET /api/profiles/me` with trusted user identity
- `PUT /api/profiles/me` with trusted user identity
- `/actuator/health`

```bash
mvn -B verify
./scripts/test-dependency-report-policy.sh
mvn spring-boot:run
docker build -t user-profile-service .
```

CI scans the resolved runtime dependency set with pinned Trivy releases,
publishes the JSON report, and rejects unaccepted Critical or High findings.
See [dependency security](docs/DEPENDENCY_SECURITY.md) for local reproduction,
scanner scope, and the time-bounded exception process.

H2 console and Hibernate automatic schema update are local-only until
PROFILE-02 supplies a production profile and migrations.

## Branch workflow and troubleshooting

Use `feature/* → develop`; `main` will be introduced as a release branch later.
For 404, confirm a profile exists for the authenticated account. Do not call
this service directly with a user-selected ID or place profile PII in logs.

## Licence

Copyright © 2026 Bernard McGeever. All rights reserved.

This repository contains proprietary software belonging to Bernard McGeever.
It may not be used, copied, modified or distributed without express written
permission. See [LICENSE](./LICENSE).

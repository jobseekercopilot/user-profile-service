# Service operations

## Verification

From a clean clone with Java 17, Maven 3.9 and Docker:

```bash
mvn -B clean verify
./scripts/test-dependency-report-policy.sh
./scripts/verify-container.sh
```

The container script runs the complete test suite and builds only from the
resulting verified JAR, starts a disposable PostgreSQL database, runs the application with the
production profile and a read-only filesystem, waits for database-aware
readiness, confirms UID/GID `10001:10001`, and proves graceful exit. CI also
stops PostgreSQL to prove readiness fails closed, scans the rebuilt image for
unaccepted Critical/High OS and library findings, and proves graceful exit.

## Startup and shutdown

Production requires `PROFILE_DB_URL`, `PROFILE_DB_USERNAME` and
`PROFILE_DB_PASSWORD`; database migrations and Hibernate validation complete
before readiness becomes `UP`. The process handles termination gracefully and
allows up to 20 seconds for in-flight lifecycle work. Container orchestrators
must allow at least 25 seconds before forcible termination.

Check the process and database readiness without sending profile traffic:

```bash
curl --fail http://localhost:8085/actuator/health
curl --fail http://localhost:8085/actuator/health/readiness
```

Health output is deliberately redacted. See
[the observability contract](OBSERVABILITY.md) for signal privacy, dashboards
and alerts, and [database operations](DATABASE_OPERATIONS.md) for migration,
backup and restore procedures.

## Troubleshooting

- Readiness `DOWN`: verify PostgreSQL health, network reachability, runtime
  secret injection, migrations and datasource-pool saturation. Do not log or
  paste the JDBC password.
- Startup exits before readiness: inspect the stable configuration or migration
  error class; production deliberately fails closed for missing credentials,
  non-PostgreSQL URLs and schema incompatibility.
- `409 PROFILE_WRITE_CONFLICT`: the complete PUT is safe to retry with bounded
  backoff. Sustained conflicts require transaction/contention investigation.
- `400`/`413`/`415`: use the stable error code and field/code pairs. Rejected
  profile values are intentionally absent.
- Missing metrics/alerts: check the environment-owned private exporter and
  alert routing. `/actuator/metrics` is intentionally not exposed.

Use correlation IDs to join metadata-only logs. Do not add profile bodies,
user IDs, bearer/service/environment-data tokens, database values or raw
exceptions to logs or metric labels.

## Ownership and residual risks

Service owners maintain application health, metrics and this runbook. Platform
owners maintain PostgreSQL, runtime secrets, network policy, resource limits,
the private metric exporter, dashboards and alert routing. PROFILE-01 remains
the separate P0 identity-boundary blocker; this operational hardening does not
claim that direct caller identity is secure or that the complete path is
beta-ready.

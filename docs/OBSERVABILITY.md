# Observability and readiness contract

This service exposes vendor-neutral Spring health and Micrometer signals. It
does not select or configure a paid monitoring platform, public metrics route,
production alert destination or trace backend.

## Health and readiness

`GET /actuator/health` returns only aggregate process status. Component and
database details are hidden. `GET /actuator/health/readiness` includes the
application readiness state and Spring's database health contributor in its
aggregate `UP`/`DOWN` result. Component names and details remain hidden, so the
JDBC URL, database product/version, query, username, password and exception
details are never returned.

Database readiness opens a connection through the configured pool. A `DOWN`
database therefore makes the service unready without changing the liveness
signal or generating profile reads. Operators should investigate the database,
credentials, pool saturation and network path before restarting the service.

## Metrics and privacy

Spring provides standard JVM, process, datasource-pool and
`http.server.requests` meters. Profile requests additionally record:

| Meter | Type | Meaning |
|---|---|---|
| `jobseeker.user.profile.operation.outcomes` | Counter | Completed profile read/upsert outcomes |
| `jobseeker.user.profile.operation.duration` | Timer/histogram | Operation latency with 100 ms, 500 ms and 2 s boundaries |

The custom label allowlist is exactly `operation`, `outcome` and HTTP status
family. Values are fixed low-cardinality categories. User/profile IDs, names,
skills, qualifications, roles, postcodes, emails, request/response content,
query strings, tokens, correlation IDs, database values and exception text are
forbidden as metric labels. Tests enforce this contract. Service logs likewise
contain only operation metadata, bounded collection counts, outcomes and
duration—not user or profile identifiers.

`/actuator/metrics` is intentionally not exposed. Platform owners must connect
the `MeterRegistry` to an approved authenticated private exporter. Do not add a
public endpoint to make a dashboard work.

## Correlation IDs

Inbound `X-Correlation-Id` values are accepted only when they match
`[A-Za-z0-9][A-Za-z0-9._:-]{0,127}` after trimming. Missing or unsafe values
are replaced by a UUID before they enter logs or a response. Correlation IDs
are diagnostic metadata, not secrets or authorization credentials, and must
not be used as metric labels.

## Dashboard specification

The beta dashboard must show:

1. read and upsert volume, success and bounded failure outcomes;
2. p50, p95 and p99 latency and the 100 ms, 500 ms and 2 s buckets;
3. aggregate process health and database-aware readiness;
4. HTTP status/error rates and datasource active, idle, pending and maximum
   connections;
5. JVM heap, process CPU, threads and container restarts; and
6. release annotation plus links to the operations and database runbooks.

No panel may display profile content, user identity, credentials, raw request
bodies or exception text.

## Initial alert specification

| Signal | Initial trigger | Response |
|---|---|---|
| Readiness | `DOWN` continuously for 2 minutes | Page the profile-service owner; check database reachability and pool state |
| Profile availability | Internal-error outcomes exceed 5% for 5 minutes with at least 20 operations | Page and correlate HTTP/database signals |
| Conflict rate | Conflict outcomes exceed 2% for 10 minutes with at least 20 upserts | Notify owner; investigate retry storms and transaction contention |
| Latency | p95 exceeds 2 seconds for 10 minutes with at least 20 operations | Notify owner; inspect pool saturation and database latency |
| Telemetry silence | No samples for 5 minutes while the environment should serve traffic | Notify operations; check the process and exporter |

Platform owners must route these rules through the approved on-call system and
test firing, delivery, acknowledgement and recovery in the target environment.
This repository specifies the required signals but does not claim deployed
production monitoring.

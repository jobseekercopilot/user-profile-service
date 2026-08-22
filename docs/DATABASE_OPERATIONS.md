# Profile database operations

PostgreSQL 17 is the supported runtime database. Flyway is the sole schema
owner: application startup validates and applies checked-in migrations, while
Hibernate is restricted to `validate`. H2 exists only on the test classpath and
is never packaged as a runtime database.

## Configuration and ownership

Set `SPRING_PROFILES_ACTIVE=production` and supply `PROFILE_DB_URL`,
`PROFILE_DB_USERNAME`, and `PROFILE_DB_PASSWORD` through the environment's
secret manager. Production startup rejects non-PostgreSQL URLs and blank
credentials without including their values in the error. SQL value logging,
the H2 console, API docs, and detailed health output are disabled.

The service owner owns migrations and restore drills. The platform owner owns
database provisioning, encryption, network access, automated backups,
retention, monitoring, and credential rotation. Never commit a dump or put a
database password on a command line.

## Migration procedure

1. Review new `V*__*.sql` files as append-only changes. Never edit a migration
   already applied to a shared environment.
2. Run `mvn -B verify`; this starts disposable PostgreSQL, migrates an empty
   database and the preceding schema version, verifies constraints/data
   retention, then completes a backup/restore into a separate database.
3. Take and verify a backup before an environment change.
4. Apply the release to one non-serving instance first. Flyway validates
   checksums and migrates before the application accepts traffic.
5. Confirm `/actuator/health`, profile create/read/update, optimistic revision
   conflicts, Evidence Library reads, nested collection persistence, and the
   current schema version before increasing traffic.

V3 is additive. It adds profile revision metadata and the Evidence Library
tables while retaining legacy roles and qualifications. After Flyway completes,
the application runner backfills missing profile revision identities/digests
and imports legacy rows as review-required drafts. The import key is unique per
profile and derived from a SHA-256 source hash, so restarting or rerunning the
scan does not duplicate evidence. Observe the redacted completion counts and
verify that no entry is promoted to `USER_CONFIRMED`.

V4 is additive. It adds normalized profile preference sets, explicit
availability fields and the Evidence Library optimistic entry version. Existing
rows remain valid: no preference or availability value is inferred, and
existing evidence begins at entry version zero. After deployment, verify a
section-only preference update, a stale `If-Match` conflict and the
create/edit/confirm/archive/restore evidence journey before increasing traffic.

There are no automated down migrations. If an application rollback is needed,
restore the prior application only when its schema is forward compatible.
Otherwise stop writes and restore the pre-change backup into a newly provisioned
database, then repoint the service. An irreversible migration requires
Bernard's explicit approval before it is run.

## Backup and restore drill

Use secret-injected environment variables and write dumps only to an encrypted,
access-controlled location. A representative custom-format backup is:

```bash
pg_dump --format=custom --no-owner --no-acl \
  --dbname="$PROFILE_DB_URL" --file=/secure/user-profile.dump
```

Restore into a newly created, empty validation database—not over the source:

```bash
pg_restore --exit-on-error --no-owner --no-acl \
  --dbname="$PROFILE_RESTORE_DB_URL" /secure/user-profile.dump
```

After restore, verify Flyway history and counts for profiles, skills, target
roles, qualifications, roles, evidence entries, revisions and facts; then
exercise create/read/update before any cutover. Record the dump identifier,
checksum, timestamps, migration version, test evidence, and deletion date.
Destroy the validation database and dump under the approved retention policy
only after the drill is signed off.

The automated PostgreSQL test performs this drill with synthetic data in a
throwaway container and restores into a distinct database. It never reads or
modifies developer, staging, production, or user data.

## Legacy local H2 data

Old file-backed H2 databases are not upgraded in place and are not mounted by
the PostgreSQL Compose stack. They were development-only but may still contain
personal profile data. Do not copy them into PostgreSQL automatically. If any
data must be retained, stop and perform an owner-reviewed export, validation,
and import; otherwise preserve or securely dispose of it under PROFILE-04's
retention decision. `docker compose down` retains the named PostgreSQL volume;
`docker compose down --volumes` deletes local profile data and must be used only
when that deletion is intentional.

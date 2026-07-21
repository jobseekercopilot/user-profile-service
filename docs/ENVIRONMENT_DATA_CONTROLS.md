# Environment-data controls

`/internal/system-data/**` creates, verifies, and deletes synthetic profile
fixtures. It is for isolated local, automated-test, or demo environments only.
It is not an administrative or production API.

## Fail-closed activation

All of these conditions are required before the application starts with the
fixture controller:

1. `environment-data` is an active Spring profile.
2. Exactly one runtime profile is also active: `local`, `test`, or `demo`.
3. `ENVIRONMENT_DATA_ENABLED=true`.
4. `ENVIRONMENT_DATA_TOKEN` contains at least 32 characters.
5. `ENVIRONMENT_DATA_ALLOWED_ENVIRONMENTS`, when overridden, is a non-empty
   subset of `local,test,demo` and contains the active runtime profile.

Without the explicit profile, the controller and authentication filter are not
beans and the routes do not exist. With the profile but unsafe or ambiguous
configuration, startup fails. `prod`, `production`, `stage`, `staging`, `uat`,
`preprod`, and `live` are always rejected and cannot be added to the allowlist.

Example for an isolated local process (use a private generated value, not this
placeholder):

```bash
SPRING_PROFILES_ACTIVE=local,environment-data \
ENVIRONMENT_DATA_ENABLED=true \
ENVIRONMENT_DATA_TOKEN='<private-random-value-of-at-least-32-characters>' \
mvn spring-boot:run
```

## Request authentication

Every `/internal/system-data/**` request must contain exactly one
`X-Environment-Data-Token` header whose value matches the runtime secret.
Missing, duplicate, and invalid headers return `401` with the stable
`ENVIRONMENT_DATA_UNAUTHORIZED` code. The value is never included in responses
or logs.

Use a distinct credential for each environment. Do not reuse database,
authentication-service, gateway, user, or production credentials. Supply it
through the approved runtime secret mechanism; never commit it to source or a
populated `.env` file. Rotate the credential if its confidentiality is in doubt.

Network policy should additionally restrict this service to approved internal
callers. Credential enforcement is an application control, not a substitute for
network isolation.

## Verification

`EnvironmentDataGuardTest` covers every reserved production-like name,
ambiguous/unknown profiles, unsafe allowlists, disabled operation, and weak
credentials. `EnvironmentDataProfileTest` proves controller presence and
absence. `EnvironmentDataAuthenticationFilterTest` covers valid, missing,
invalid, and duplicate credentials and confirms unrelated routes are unchanged.
`EnvironmentDataIntegrationTest` exercises authenticated seed, verify, and
reset requests over a real HTTP listener and confirms negative credentials are
rejected before the controller.

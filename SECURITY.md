# Security policy

This repository is private. Report suspected vulnerabilities privately to the
repository owner. Do not open a public issue or include credentials, tokens,
personal data, exploit details, or production logs in an issue.

Do not commit secrets. Use runtime environment variables or the approved
secret-management mechanism. If a credential may have been exposed, stop its
use, report the type and affected location without reproducing its value, and
arrange rotation with the owner.

The current code is a beta-readiness baseline, not a security certification.
Known risks and beta blockers are tracked in `docs/BETA_READINESS_AUDIT.md`.

Every `/api/profiles/**` request requires an RS256 Bearer access token issued by
authentication-service. The service obtains public verification keys from the
configured JWKS URI, requires the configured issuer and audience plus
`token_type=access`, and derives ownership only from `sub`. `X-User-Id` and body
identity fields have no authority. Missing or invalid tokens receive one stable
redacted response; token contents, parser diagnostics and key material must not
be logged. Retain previous public keys at the issuer through the access-token
lifetime, clock skew and JWKS cache window during rotation; never distribute an
authentication private key to this service.

The fixture-management API is not a user API. It is absent by default and must
never be enabled in a production-like environment. Its independent credential
must be injected at runtime, sent only in `X-Environment-Data-Token`, and kept
separate from user, gateway, database, and other service credentials. See
`docs/ENVIRONMENT_DATA_CONTROLS.md` for the fail-closed profile contract.

CI scans resolved runtime dependencies and fails when its machine-readable
report contains an unaccepted Critical or High vulnerability. Exceptions must
be linked to a private repository issue, expire within 30 days, and be removed
when the finding is remediated. See `docs/DEPENDENCY_SECURITY.md` for the full
policy and safe local reproduction steps.

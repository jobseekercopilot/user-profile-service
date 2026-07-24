# User Profile API contract

`openapi.json` is the producer-owned source contract for the externally
supported User Profile API. The User Profile repository is authoritative;
consumers must not reconstruct this contract from controller source or depend
on runtime `/v3/api-docs` output. Production keeps runtime API documentation
disabled.

The contract follows additive semantic versioning. Removing or renaming a path,
operation, field or enum value, making an optional value required, or narrowing
an accepted constraint requires a coordinated major-version migration.
Compatible additions increment the minor version. Documentation-only
corrections increment the patch version.

To update the contract:

1. Change the controller/model validation and `openapi.json` together.
2. Update `info.version`.
3. Run `sha256sum openapi.json` from this directory and replace the entry in
   `SHA256SUMS`.
4. Run `../scripts/test-api-contract-policy.sh`,
   `../scripts/verify-api-contract.sh` and `mvn -B verify`.
5. Update `client-release.json` and the client POM together. The package version
   is `<contract-version>-rev.<first-12-source-revision-characters>`.
6. For any release after the initial package, record the prior published
   contract revision as `compatibilityBaseRevision`.
7. Merge the producer change before publishing or updating consumers.

The Java client is built from `client/pom.xml`. Generated source remains under
`client/target/`; it is not producer source and must not be committed.
Publication runs only from the protected `develop` workflow, rejects an
existing immutable coordinate, and proves that a fresh authenticated Maven
consumer can resolve and compile the package.

The policy requires both current-user operations, their stable operation IDs,
Bearer JWT security and the bounded `UserProfile` schema. The checksum makes a
consumer update an explicit review event rather than silent drift.

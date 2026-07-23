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
5. Merge the producer change before updating consumers. Each consumer records
   the exact producer commit and contract checksum it vendors.

The policy requires both current-user operations, their stable operation IDs,
Bearer JWT security and the bounded `UserProfile` schema. The checksum makes a
consumer update an explicit review event rather than silent drift.

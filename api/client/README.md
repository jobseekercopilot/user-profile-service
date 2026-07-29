# User Profile Java client package

This module generates and packages the producer-owned User Profile Java client.
Generated source is written only under `target/` and must never be committed.

The immutable Maven coordinate is:

```text
com.jobseekercopilot.clients:user-profile-service-client:1.2.0-rev.045100e4b6fc
```

The contract version and first 12 characters of the reviewed contract source
revision form the package version. Full provenance and the contract digest are
embedded in the JAR manifest and recorded in `../client-release.json`.

Build and verify locally:

```bash
../../scripts/verify-client-release.py
../../scripts/test-client-generation.sh
mvn -B -f pom.xml clean verify
```

Publication is only allowed through the protected `develop` workflow. GitHub
Packages credentials must never be added to this repository or Maven POM.

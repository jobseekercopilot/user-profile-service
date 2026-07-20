# Dependency security

## Decision and scope

The service uses Trivy's open-source vulnerability scanner for Maven runtime
dependencies. This is compatible with the repository's proprietary licence:
the scanner runs as a CI tool and does not change the application licence or
become part of the distributed service.

CI first verifies the application, then asks Maven to materialise the resolved
runtime dependency set in `target/dependency-scan`. A pinned Trivy version scans
only that directory for library vulnerabilities. Its advisory database is
cached by the pinned Trivy action, and the complete JSON result is retained as
a CI artifact for 30 days. The policy validates the report schema and Java
package coverage before rejecting any unaccepted Critical or High finding.

This deliberately covers resolved Java runtime libraries, including transitive
dependencies. It does not claim to scan the base container image, operating
system packages, build-only plugins, source code, infrastructure, or deployed
environments; those require their own controls.

## Remediation evidence

The initial scan of the Spring Boot 3.2.0 runtime set covered 83 Java packages
and reported 4 Critical and 27 High findings. The service was upgraded to
Spring Boot 4.1.0, springdoc-openapi 3.0.3, and Lombok 1.18.46, including the
Spring Boot 4 REST client module and package migration.

After the upgrade, the same scanner and scope covered 102 Java packages and
reported zero Critical and zero High findings. One Medium finding remained; it
does not breach the approved Critical/High gate and should continue to be
reviewed through routine dependency maintenance. There are no active risk
exceptions.

## Local verification

Run the application and policy tests first:

```bash
mvn -B clean verify
./scripts/test-dependency-report-policy.sh
```

For a current advisory scan, materialise dependencies and run the same pinned
Trivy release without mounting the repository or Docker socket. The example
uses a temporary cache and report outside tracked source:

```bash
mvn -B dependency:copy-dependencies \
  -DincludeScope=runtime \
  -DoutputDirectory=target/dependency-scan

scan_dir="$(mktemp -d)"
docker run --rm \
  -v "$PWD/target/dependency-scan:/scan:ro" \
  -v "$PWD/config/trivy/.trivyignore:/.trivyignore:ro" \
  -v "$scan_dir:/output" \
  aquasec/trivy:0.72.0 rootfs \
  --scanners vuln --vuln-type library --list-all-pkgs \
  --severity UNKNOWN,LOW,MEDIUM,HIGH,CRITICAL \
  --format json --output /output/dependencies.json \
  --ignorefile /.trivyignore /scan

./scripts/verify-dependency-report.sh \
  "$scan_dir/dependencies.json" config/trivy/.trivyignore
```

Do not commit reports or advisory caches. Reports reveal component versions and
should remain in the private CI artifact store or a local temporary directory.

## Time-bounded risk exceptions

The default is remediation. If a Critical or High finding cannot be fixed
immediately, the repository owner must assess it in a private
`jobseekercopilot/user-profile-service` issue. An exception in
`config/trivy/.trivyignore` must place the issue URL immediately before one
finding ID and an expiry no more than 30 days away:

```text
# Tracking: https://github.com/jobseekercopilot/user-profile-service/issues/123
CVE-2099-12345 exp:2099-01-30
```

The issue must record affected versions, exploitability in this service,
compensating controls, owner, remediation plan, and review date. CI rejects
untracked, malformed, expired, or overlong exceptions. Renewal requires a new
assessment; remove the ignore entry as soon as the dependency is remediated.

## Ownership and residual risk

The repository owner owns scanner/action upgrades, advisory review, exception
approval, and remediation. CI is the enforcement point, but a clean report is
not proof of absence: advisory feeds can lag disclosure and the scan scope is
limited to resolved Java runtime dependencies. Dependabot or equivalent update
review, container scanning, secret scanning, code review, and deployment
controls remain necessary complementary safeguards.

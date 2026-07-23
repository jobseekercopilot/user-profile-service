#!/usr/bin/env bash
set -euo pipefail

contract_dir="${1:-api}"
contract="$contract_dir/openapi.json"
manifest="$contract_dir/SHA256SUMS"

for required_file in "$contract" "$manifest"; do
    if [[ ! -f "$required_file" || -L "$required_file" ]]; then
        echo "API contract policy: required regular file is missing or is a symlink: $required_file" >&2
        exit 1
    fi
done

(
    cd "$contract_dir"
    sha256sum --check --strict SHA256SUMS
)

jq -e '
    (.openapi | type == "string" and startswith("3.")) and
    (.components.securitySchemes.bearerAuth.type == "http") and
    (.components.securitySchemes.bearerAuth.scheme == "bearer") and
    (.components.securitySchemes.bearerAuth.bearerFormat == "JWT") and
    (.paths["/api/profiles/me"].get.operationId == "getMyProfile") and
    (.paths["/api/profiles/me"].put.operationId == "createOrUpdateMyProfile") and
    (.paths["/api/profiles/me"].get.security | any(has("bearerAuth"))) and
    (.paths["/api/profiles/me"].put.security | any(has("bearerAuth"))) and
    (.paths["/api/profiles/me"].put.requestBody.required == true) and
    (.paths["/api/profiles/me"].put.requestBody.content["application/json"].schema["$ref"] == "#/components/schemas/UserProfile") and
    (.components.schemas.UserProfile.properties.skills.maxItems == 100) and
    (.components.schemas.UserProfile.properties.qualifications.maxItems == 50) and
    (.components.schemas.UserProfile.properties.roles.maxItems == 50) and
    (.components.schemas.Aspirations.properties.targetRoles.maxItems == 50) and
    (.components.schemas.WorkPreferences.properties.commuteRange.minimum == 0) and
    (.components.schemas.WorkPreferences.properties.commuteRange.maximum == 500) and
    (.components.schemas.UserProfile.properties.id.readOnly == true) and
    (.components.schemas.UserProfile.properties.userId.readOnly == true)
' "$contract" >/dev/null

echo "API contract policy: User Profile OpenAPI source is present, intact and compatible"

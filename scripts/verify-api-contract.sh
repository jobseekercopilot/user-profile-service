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
    (.paths["/api/profiles/me"].patch.operationId == "updateMyPreferences") and
    (.paths["/api/profiles/me/professional-contact"].patch.operationId
        == "updateMyProfessionalContact") and
    (.paths["/api/evidence"].get.operationId == "listEvidence") and
    (.paths["/api/evidence"].post.operationId == "createEvidence") and
    (.paths["/api/evidence/{entryId}"].get.operationId == "getEvidence") and
    (.paths["/api/evidence/{entryId}"].put.operationId == "updateEvidence") and
    (.paths["/api/evidence/{entryId}/confirm"].post.operationId == "confirmEvidence") and
    (.paths["/api/evidence/{entryId}/hide"].post.operationId == "hideEvidence") and
    (.paths["/api/evidence/{entryId}/show"].post.operationId == "showEvidence") and
    (.paths["/api/evidence/{entryId}/archive"].post.operationId == "archiveEvidence") and
    (.paths["/api/evidence/{entryId}/restore"].post.operationId == "restoreEvidence") and
    (.paths["/api/evidence/{entryId}/supersede"].post.operationId == "supersedeEvidence") and
    (.paths["/api/evidence/snapshots"].post.operationId == "createEvidenceSnapshot") and
    (.paths["/api/evidence/snapshots/{snapshotId}"].get.operationId == "getEvidenceSnapshot") and
    (.paths["/api/profiles/me"].get.security | any(has("bearerAuth"))) and
    (.paths["/api/profiles/me"].put.security | any(has("bearerAuth"))) and
    (.paths["/api/profiles/me"].patch.security | any(has("bearerAuth"))) and
    (.paths["/api/profiles/me/professional-contact"].patch.security
        | any(has("bearerAuth"))) and
    (.paths["/api/profiles/me"].put.requestBody.required == true) and
    (.paths["/api/profiles/me"].put.requestBody.content["application/json"].schema["$ref"] == "#/components/schemas/UserProfile") and
    (.components.schemas.UserProfile.properties.skills.maxItems == 100) and
    (.components.schemas.UserProfile.properties.qualifications.maxItems == 50) and
    (.components.schemas.UserProfile.properties.roles.maxItems == 50) and
    (.components.schemas.Aspirations.properties.targetRoles.maxItems == 50) and
    (.components.schemas.UserProfile.properties.professionalContact["$ref"]
        == "#/components/schemas/ProfessionalContact") and
    (.components.schemas.ProfessionalContact.properties.phone.maxLength == 40) and
    (.components.schemas.ProfessionalContact.properties.links.maxItems == 8) and
    (.components.schemas.ProfessionalLink.properties.label.minLength == 1) and
    (.components.schemas.ProfessionalLink.properties.label.maxLength == 40) and
    (.components.schemas.ProfessionalLink.properties.url.minLength == 9) and
    (.components.schemas.ProfessionalLink.properties.url.maxLength == 512) and
    (.components.schemas.ProfessionalLink.properties.url.pattern == "^https://") and
    (.components.schemas.WorkPreferences.properties.commuteRange.minimum == 0) and
    (.components.schemas.WorkPreferences.properties.commuteRange.maximum == 500) and
    (.components.schemas.WorkPreferences.properties.employmentTypes.maxItems == 5) and
    (.components.schemas.WorkPreferences.properties.workingPatterns.maxItems == 8) and
    (.components.schemas.WorkPreferences.properties.workplaceArrangements.maxItems == 3) and
    (.components.schemas.WorkPreferences.properties.noticePeriodDays.maximum == 3650) and
    (.components.schemas.EvidenceWriteRequest.properties.supportingLinks.maxItems == 10) and
    (.components.schemas.EvidenceWriteRequest.properties.supportingLinks.items.pattern == "^https://") and
    (.components.schemas.EvidenceWriteRequest.properties.description.maxLength == 2000) and
    (.components.schemas.EvidenceWriteRequest.properties.responsibilities.maxLength == 2000) and
    (.components.schemas.EvidenceWriteRequest.properties.achievements.maxLength == 2000) and
    (.components.schemas.EvidenceEntry.properties.version.readOnly == true) and
    (.components.schemas.UserProfile.properties.id.readOnly == true) and
    (.components.schemas.UserProfile.properties.userId.readOnly == true) and
    (.components.schemas.UserProfile.properties.revision.readOnly == true) and
    (.components.schemas.UserProfile.properties.revisionId.readOnly == true) and
    (.components.schemas.UserProfile.properties.contentDigest.readOnly == true) and
    (.components.schemas.EvidenceCategory.enum | length == 9) and
    (.components.schemas.EvidenceConfirmationState.enum == ["DRAFT", "USER_CONFIRMED"]) and
    (.components.schemas.EvidenceVisibility.enum == ["VISIBLE", "HIDDEN"]) and
    (.components.schemas.EvidenceLifecycle.enum == ["ACTIVE", "ARCHIVED", "SUPERSEDED"]) and
    (.components.schemas.EvidenceRevision.properties.contentDigest.readOnly == true) and
    (.components.schemas.EvidenceFact.properties.factId.readOnly == true)
    and (.components.schemas.EvidenceSnapshotRequest.properties.entryIds.maxItems == 50)
    and (.components.schemas.EvidenceSnapshotRequest.properties.sectionOrder.maxItems == 9)
    and (.components.schemas.EvidenceSnapshotPurpose.enum == ["CV", "COVER_LETTER"])
    and (.components.schemas.EvidenceSnapshot.properties.snapshotId.readOnly == true)
    and (.components.schemas.EvidenceSnapshot.properties.createdAt.readOnly == true)
    and (.components.schemas.EvidenceSnapshotSelection.properties.facts.maxItems == 50)
' "$contract" >/dev/null

echo "API contract policy: User Profile OpenAPI source is present, intact and compatible"

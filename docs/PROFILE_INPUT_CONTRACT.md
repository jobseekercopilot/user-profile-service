# Profile input contract

PROFILE-03 defines the profile service as the final validation and
normalisation boundary. Gateway validation may reject the same invalid input
earlier, but callers must not rely on the gateway as the only control.

## Bounds

| Field | Rule |
|---|---|
| request body | at most 65,536 bytes by default |
| authenticated `sub` | non-blank access-token subject, validated before controller invocation |
| skills | at most 100 entries; each non-blank and at most 100 characters |
| target roles | at most 50 entries; each non-blank and at most 100 characters |
| qualifications | at most 50 non-null entries |
| roles | at most 50 non-null entries |
| qualification name / issuer | required, at most 200 characters |
| qualification grade | at most 100 characters |
| role title / employer | required, at most 200 characters |
| key responsibilities | at most 2,000 characters |
| role and qualification dates | strict `YYYY-MM` or `YYYY-MM-DD` between years 1900 and 2099 |
| commute range | 0–500 |
| latitude / longitude | -90–90 / -180–180 |
| postcode | valid UK full postcode or outcode |
| region / administrative district | at most 100 characters |

The existing status rules remain enforced: completed qualifications require a
grade and achieved month, in-progress qualifications require an expected
completion month, previous roles require an end month, and current roles must
not have one.

## Normalisation and ownership

Accepted text is stripped and normalized to Unicode NFC before persistence.
Blank optional values become null, null lists become empty lists, and UK
postcodes are stored uppercase with canonical inward-code spacing. The service
ignores request-body `id` and `userId` values and derives ownership from the
validated access-token `sub` claim. `X-User-Id` is not part of the profile API
contract and cannot override the token owner.

## Error contract

Validation failures return HTTP 400 with schema version `1`, code
`PROFILE_VALIDATION_FAILED`, a generic message, correlation ID, timestamp and
sorted `{field, code}` violations. Rejected values and personal profile content
are never returned in an error. Malformed JSON, unsupported media types and
oversized requests use `MALFORMED_JSON`, `UNSUPPORTED_MEDIA_TYPE` and
`PAYLOAD_TOO_LARGE` respectively.

Changing a bound or accepted representation requires coordinated review of the
user-management gateway model and its generated downstream contract.

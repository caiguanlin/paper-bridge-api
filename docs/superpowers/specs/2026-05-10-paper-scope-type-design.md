# Paper Generate Scope Type Design

## Context

`PaperGenerateRequest` used to model paper scope as one `unit` plus multiple `chapters`. That works for chapter quizzes inside a single unit, but it cannot express common exam ranges:

- midterm papers that cover several units
- final papers that cover a whole volume
- precise chapter selections that may cross unit boundaries

## Decision

Use an explicit `scopeType` on `/api/papers/preview-plan` and `/api/papers/generate`.

```json
{
  "grade": "三年级",
  "publisher": "PEP",
  "subject": "MATH",
  "volume": "上册",
  "scopeType": "UNITS",
  "units": ["第一单元", "第二单元"]
}
```

Supported scope types:

- `CHAPTERS`: request `chapters`, each item containing `unit` and `chapter`.
- `UNITS`: request `units`; the backend uses all chapters under those units.
- `VOLUME`: request only the base grade/publisher/subject/volume fields; the backend uses the whole volume.

## Persistence

Existing `paper.unit_name` and `paper.chapter_name` stay as display snapshots for list/detail/export screens.

Two fields are added to preserve exact regeneration behavior:

- `scope_type`
- `scope_payload_json`

This avoids trying to parse display strings back into machine-readable scope.

## Generation Behavior

Question-bank lookup always filters by owner, grade, publisher, subject, volume, question type, and optional difficulty.

Scope-specific filters:

- `CHAPTERS`: match `(unit_name, chapter_name)` pairs.
- `UNITS`: match `unit_name in units`.
- `VOLUME`: do not add unit/chapter filters.

AI supplement generation receives a human-readable scope description built from the same request scope.

## Compatibility

This is a breaking request-contract change for paper generation. Existing saved papers without `scope_payload_json` can still regenerate as legacy single-unit chapter scopes by splitting the stored chapter display string.

## Tests

Coverage includes conditional DTO validation, AI prompt scope text, scope snapshot persistence, and regenerate from a stored scope payload.

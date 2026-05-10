# Paper Generate Chapter Multi-Select Design

## Context

`PaperController#generate` currently accepts a single `chapter` value through `PaperGenerateRequest`. This does not match the normal paper generation workflow: grade, publisher, subject, volume, and unit are single selections, but chapters under the selected unit should be multi-selectable so the paper can draw questions from a broader chapter scope.

The same request shape is used by `/api/papers/preview-plan`, `/api/papers/generate`, and regeneration logic in `PaperGenerationService`.

## Decision

Use a single-unit, multi-chapter request model:

```json
{
  "title": "Grade 3 Math Quiz",
  "grade": "Grade 3",
  "publisher": "PEP",
  "subject": "MATH",
  "volume": "Volume 1",
  "unit": "Unit 3",
  "chapters": ["Measurement", "Kilometer"],
  "totalScore": 100,
  "strategy": "BANK_WITH_AI",
  "difficulty": "MEDIUM",
  "sections": []
}
```

`chapter: string` is replaced by `chapters: List<String>` in `PaperGenerateRequest`.

## API Contract

- `chapters` is required.
- `chapters` must contain at least one chapter.
- Each chapter must be non-blank.
- `grade`, `publisher`, `subject`, `volume`, and `unit` remain single-value fields.
- `sections` and score validation behavior remain unchanged.

This is a breaking API cleanup. The project is still early enough that a compatibility layer for the old `chapter` field is not worth the extra ambiguity.

## Generation Behavior

Question bank lookup changes from exact single-chapter matching to multi-chapter matching:

- Before: `chapter_name = request.chapter`
- After: `chapter_name in request.chapters`

This affects both preview availability counts and actual bank question selection.

AI supplement generation receives the selected chapter scope. The preferred implementation is to change `AiQuestionGenerationRequest` from `chapter: String` to `chapters: List<String>`, then render those chapters in the Deepseek prompt as a joined display string. Mock AI can use the same joined text for deterministic titles.

## Persistence

The existing `paper.chapter_name` column remains a single string for this iteration. Generated papers store the selected chapters as a comma-separated display value, for example:

```text
Measurement, Kilometer
```

This keeps the database migration-free and preserves existing summary/detail/export response fields. The stored value is a display snapshot of the generation scope, not a normalized curriculum relation.

`regenerate` reconstructs the multi-chapter request by splitting the saved `paper.chapter_name` display value on commas and trimming blanks.

## Responses

`PaperResponse` and `PaperSummaryResponse` keep the existing `chapter: String` field for now. When multiple chapters are selected, the response returns the comma-separated display value saved on `paper.chapter_name`.

A later frontend precision pass can add `chapters: List<String>` to responses if exact round-trip display becomes necessary.

## Error Handling

- Empty `chapters` is rejected by bean validation.
- Blank chapter entries are rejected by element validation.
- Existing score mismatch errors remain unchanged.
- Bank-only shortage errors continue to report the missing count per section, now based on the combined chapter scope.

## Tests

Add or update tests to cover:

- `PaperGenerateRequest` construction with `chapters`.
- Preview/generation query scope using multiple chapters.
- AI generation request carrying multiple chapters.
- Generated `Paper` storing a comma-separated chapter display value.
- `regenerate` splitting saved chapter display value back into `chapters`.
- Empty chapter list validation shape where applicable.

## Non-Goals

- Do not support cross-unit chapter selection in this change.
- Do not introduce a `paper_chapter_scope` table yet.
- Do not change question import or question CRUD; individual questions still belong to one chapter.
- Do not change export behavior beyond showing the stored chapter display value.

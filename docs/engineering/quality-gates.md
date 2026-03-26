# Engineering Quality Gates

This document defines the minimum gates for every commit in this repository.

## Commit conventions

- Use Conventional Commits in English.
- Keep commits focused on a single concern.
- Never mix refactors with feature behavior changes unless strictly necessary.

## Backend gates

- Validate all external inputs at resource/service boundaries.
- Keep error payloads consistent: `{ "error": "<message>" }`.
- Preserve backward compatibility for public endpoints unless explicitly planned.
- Add or update unit tests for changed business logic.

## Frontend gates

- Use a single API client layer (`src/utils/httpClient.js` + `src/services/*`).
- Avoid hardcoded domain data in UI components when data can be loaded from API.
- Keep loading/error states explicit for async flows.
- Prefer stable memoized selectors for derived render data.

## Documentation gates

- Update LaTeX documentation incrementally with each functional milestone.
- Keep requirement traceability updated for mandatory requirements:
  `R1, R2, R3, R7, R11, R16`.
- Keep design notes aligned with the actual code contracts.

## Pre-merge checklist

- [ ] Changed behavior has test coverage or documented rationale.
- [ ] No obvious regressions in existing flows (auth, issues, comments, attachments).
- [ ] API contract changes are reflected in frontend usage and docs.
- [ ] Documentation updates are committed with related code when possible.

<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# STYLE GUIDE

## Project Conventions
- Prioritize UX and operational stability over abstract elegance.
- Keep implementations simple, reversible, and testable.
- Use fail-fast boundaries for I/O and async operations.

## Trace Tag Convention
- Format: `// [TRACE: <doc-file>.md]`
- Purpose: tie code to design intent and implementation notes.
- Use on non-obvious logic paths only.

## Comment Rules
- Comments explain "why", not "what".
- Use prefixes consistently: `TODO:`, `FIXME:`, `NOTE:`.
- Avoid noisy comments for self-evident code.

## Naming and Structure
- Java classes: `PascalCase`
- Java methods/fields: `camelCase`
- Keep methods focused and compact; split complex branches early.
- Keep docs and config names explicit and discoverable.

## Error Handling
- Never swallow exceptions silently.
- Log actionable context for operator-visible failures.
- Preserve state integrity first; reject invalid operations early.

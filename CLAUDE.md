# Instructions for AI agents working in this repo

This is a public learning journal. The owner reads every entry to learn from it, so accuracy and clarity matter more than volume.

## Writing an entry
- Pick the topic from `ROADMAP.md` (the first `planned` row), unless the task names a different topic.
- Follow `docs/ENTRY_TEMPLATE.md`. Adapt the sections to the topic: prefer diagrams for flows and code for patterns.
- Explain from first principles, directly and technically. Don't use everyday analogies.
- Keep each entry to a 15–20 minute read.
- Code: Java 21, no frameworks unless the topic is about one, JUnit 5 tests, and inline comments that explain why.
- **Code budget:** code is optional. Include it only when it makes the concept clearer than prose or a diagram. At most **~60 lines of main code in 1–2 files**, plus **one** focused test class. Short inline snippets in the README are preferred over full programs. Concept-heavy entries, which is most of Phase 1, may have no code folder at all.
- Package naming: `foundations.e<NNN>_<slug>`, for example `foundations.e003_http_methods`.
- Mermaid must be valid. Check it mentally against GitHub's renderer: no unsupported syntax, and quote labels that contain special characters.
- Cite primary sources (RFCs, official docs). Never invent references, numbers or benchmarks. If unsure, say so or leave it out.
- Update `ROADMAP.md` (set the status) and the index table in `README.md` in the same PR.

## Verifying before opening a PR
- `cd code && ./gradlew test` must pass.
- Re-read the entry once for correctness. An entry that teaches something wrong is worse than no entry.

## Pull requests
- One entry per PR. Branch name: `entry/<NNN>-<slug>`.
- Use the PR template. Keep the description professional, because this repo is public.
- Use conventional commit messages, for example `docs(entry): 003 HTTP methods and semantics`.

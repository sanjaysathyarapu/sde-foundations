# SDE Foundations

A daily journal for refreshing software engineering fundamentals from first principles. The focus is the building blocks themselves, not any one framework or language. Each entry explains one concept: why it exists, how it works, where it breaks, and how it shows up in real systems and interviews.

- **Concepts first.** The notes don't depend on any language. Runnable examples are in Java 21 and live under [`code/`](code/).
- **Visual where it helps.** Flows and architectures are drawn in Mermaid, which renders directly on GitHub.
- **Tied to real work.** Where a concept appears in my project [Callwise](https://github.com/sanjaysathyarapu/callwise), the entry links to it.

## How this journal is built

Entries are drafted with AI assistance (Claude), then reviewed, edited and merged by me through pull requests. Commits drafted by the assistant carry a `Co-authored-by: Claude` trailer. Every entry has to pass CI before it merges.

## Structure

| Folder | Track | Reference structure |
|---|---|---|
| [`01-backend-first-principles/`](01-backend-first-principles/) | How a backend works, from the request lifecycle up | Sriniously, *Backend from First Principles* |
| [`02-design-principles-and-lld/`](02-design-principles-and-lld/) | SOLID, OOP, design patterns, LLD problems | Hello Interview, *Low Level Design* |
| [`03-system-design/`](03-system-design/) | Delivery framework, core concepts, key technologies, patterns, problems | Hello Interview, *System Design* |
| [`04-agent-engineering/`](04-agent-engineering/) | Building and running AI agents as software systems | The journal's own automation, Spring AI |

The ordered topic list and progress live in [ROADMAP.md](ROADMAP.md). The entry format is in [docs/ENTRY_TEMPLATE.md](docs/ENTRY_TEMPLATE.md).

## Entries

<!-- The index below is updated with every entry. Newest first. -->

| # | Date | Entry | Track |
|---|---|---|---|
| 001 | 2026-11-18 | [What a backend is: the request → response lifecycle end to end](01-backend-first-principles/001-request-response-lifecycle/) | BE |

## Running the code

```bash
cd code
./gradlew test
```

Requires JDK 21.

## License

Code under [`code/`](code/) is MIT-licensed ([LICENSE](LICENSE)). Written content is licensed under [CC BY 4.0](LICENSE-CONTENT).

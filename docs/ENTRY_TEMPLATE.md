# Entry template

Every entry is a folder: `<track-folder>/<NNN>-<slug>/README.md`. Its code goes under `code/src/main/java/foundations/e<NNN>_<slug>/`, with tests under `code/src/test/java/foundations/e<NNN>_<slug>/`.

The sections below are the default. **Adapt them to the topic.** A protocol topic leans on sequence diagrams. A pattern topic leans on code. A pure-design topic may have no code at all. Leave out a section only when it adds nothing, never to save effort.

**Length:** a 15–20 minute read. Explain directly and technically. Don't use everyday analogies.

---

```markdown
# NNN · <Title>

> **One-line summary:** what this is, in one sentence.
> **Track:** BE | LLD | SD | AG · **Prerequisites:** links to earlier entries, if any

## 1. The problem
What breaks, or is impossible, without this concept? Start from a concrete failure.

## 2. From first principles
The mechanism, built up step by step from the most basic parts.

### Self-check
- 2–3 questions on this section. Answers are in <details> blocks.

## 3. Diagram
Mermaid sequence, flow, state or architecture diagram, wherever there's a flow or a structure to show.

## 4. Code
A small runnable Java example. Inline comments explain the *why*, not only the *what*.
Link to the source file and its test.

### Self-check
- 2–3 questions about the code (for example, "what happens if line X is removed?").

## 5. Trade-offs and failure modes
When to use it, when not to use it, what it costs, and how it fails in production.

## 6. In the real world
Where this shows up: Callwise (link the file or PR), a typical Spring Boot service, well-known systems.

## 7. Interview Q&A
**Q:** A question an interviewer would actually ask.
**A:** A model answer of 3–6 sentences, precise enough to say out loud.
(Write 3–5 pairs. Each question is followed straight away by its answer. No quizzing.)

## 8. References
Primary sources first: RFCs, official docs, the original papers or talks.
```

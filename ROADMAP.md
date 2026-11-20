# Roadmap

The order runs from easy to deep. Each row is one entry, about a 15–20 minute read. The agent works through this list top to bottom, one entry per day, unless a different topic is requested.

**Status:** `planned` → `in-review` (PR open) → `done` (merged). `skipped` means intentionally dropped.

**Tracks:** BE = backend first principles · LLD = design principles and low-level design · SD = system design · AG = agent engineering. Agent-engineering entries are interleaved at roughly 1 for every 3–4 fundamentals entries, starting in Phase 1 week 3.

## Phase 1: Backend from first principles

| # | Topic | Track | Status |
|---|---|---|---|
| 001 | What a backend is: the request → response lifecycle end to end | BE | done |
| 002 | HTTP fundamentals: messages, statelessness, HTTP/1.1 vs HTTP/2 | BE | done |
| 003 | HTTP methods and semantics: safe, idempotent, cacheable | BE | done |
| 004 | HTTP status codes: choosing the right one | BE | in-review |
| 005 | HTTP headers and CORS: simple vs preflight requests | BE | planned |
| 006 | Routing: matching paths, params, versioning | BE | planned |
| 007 | Serialization and deserialization: JSON, schemas, pitfalls | BE | planned |
| 008 | Validation and transformation: where and why | BE | planned |
| 009 | Authentication: sessions vs tokens (JWT), cookies, storage | BE | planned |
| 010 | Password storage: hashing, salting, slow hashes | BE | planned |
| 011 | Authorization models: RBAC, ABAC, ReBAC | BE | planned |
| 012 | AG: The streak agent as a system: stateless runs and external state | AG | planned |
| 013 | Middleware and the request pipeline | BE | planned |
| 014 | Request context: correlation IDs, per-request state | BE | planned |
| 015 | Layered architecture: controller → service → repository | BE | planned |
| 016 | AG: Idempotent jobs: why a rerun must not double-merge | AG | planned |
| 017 | REST API design: resources, naming, pagination | BE | planned |
| 018 | Idempotency keys for unsafe operations | BE | planned |
| 019 | Error handling: error models, propagation, client contracts | BE | planned |
| 020 | AG: Scheduling and time: cron, time zones, the UTC day boundary | AG | planned |
| 021 | Configuration management: environments, secrets, 12-factor | BE | planned |
| 022 | Logging, metrics, tracing: the three pillars of observability | BE | planned |
| 023 | HTTP caching: Cache-Control, ETag, conditional requests | BE | planned |
| 024 | AG: Human-in-the-loop: approval gates and least-privilege credentials | AG | planned |
| 025 | Background jobs and task queues | BE | planned |
| 026 | Concurrency vs parallelism: I/O-bound vs CPU-bound work | BE | planned |
| 027 | Graceful shutdown and health checks | BE | planned |
| 028 | Web security basics: CSRF, XSS, injection, TLS/MITM | BE | planned |
| 029 | Webhooks: event-driven integration between systems | BE | planned |
| 030 | API contracts with OpenAPI | BE | planned |
| 031 | The testing pyramid: unit, integration, end-to-end | BE | planned |

## Phase 2: Design principles and LLD

| # | Topic | Track | Status |
|---|---|---|---|
| 032 | SOLID, part 1: single responsibility, open/closed | LLD | planned |
| 033 | SOLID, part 2: Liskov, interface segregation, dependency inversion | LLD | planned |
| 034 | DRY, KISS, YAGNI, and when to break them | LLD | planned |
| 035 | AG: What an LLM agent is: the plan → act → observe loop | AG | planned |
| 036 | OOP concepts: encapsulation, abstraction, polymorphism, composition over inheritance | LLD | planned |
| 037 | Creational patterns: factory, builder, singleton | LLD | planned |
| 038 | Structural patterns: adapter, decorator, facade, proxy | LLD | planned |
| 039 | AG: Tool calling: how an LLM invokes code (Spring AI) | AG | planned |
| 040 | Behavioral patterns: strategy, observer, command | LLD | planned |
| 041 | Behavioral patterns: state, template method, chain of responsibility | LLD | planned |
| 042 | The LLD delivery framework: requirements → entities → classes → code | LLD | planned |
| 043 | AG: RAG from first principles: chunking, embeddings, retrieval (Java) | AG | planned |
| 044 | LLD problem: LRU cache | LLD | planned |
| 045 | LLD problem: parking lot | LLD | planned |
| 046 | LLD problem: in-memory rate limiter (token bucket) | LLD | planned |

## Phase 3: System design

| # | Topic | Track | Status |
|---|---|---|---|
| 047 | The system design delivery framework | SD | planned |
| 048 | Core concept: networking essentials | SD | planned |
| 049 | Core concept: API design at scale (REST, gRPC, GraphQL) | SD | planned |
| 050 | AG: Agent evaluation: measuring answers instead of eyeballing | AG | planned |
| 051 | Core concept: data modeling (system-design depth) | SD | planned |
| 052 | Core concept: database indexing (system-design depth) | SD | planned |
| 053 | Core concept: caching strategies and invalidation | SD | planned |
| 054 | AG: Multi-agent systems: planner, worker, reviewer | AG | planned |
| 055 | Core concept: sharding and partitioning | SD | planned |
| 056 | Core concept: consistent hashing | SD | planned |
| 057 | Core concept: CAP and PACELC | SD | planned |
| 058 | Core concept: numbers to know (latency, throughput, storage) | SD | planned |
| 059 | AG: Deploying an agent: scheduling, observability, cost control | AG | planned |
| 060 | Key technologies: relational and NoSQL databases | SD | planned |
| 061 | Key technologies: queues and streams (Kafka) | SD | planned |
| 062 | Key technologies: caches (Redis), blob storage, CDNs | SD | planned |
| 063 | Pattern: async processing with workers | SD | planned |
| 064 | Pattern: transactional outbox | SD | planned |
| 065 | Pattern: sagas (orchestration vs choreography) | SD | planned |
| 066 | Pattern: circuit breaker, retries, backoff | SD | planned |
| 067 | Pattern: real-time updates (polling, SSE, WebSockets) | SD | planned |
| 068 | Problem: URL shortener (Bitly) | SD | planned |
| 069 | Problem: distributed rate limiter | SD | planned |
| 070 | Problem: file storage and sync (Dropbox) | SD | planned |
| 071 | Problem: news feed | SD | planned |
| 072 | Problem: chat system (WhatsApp) | SD | planned |

## Changing the roadmap

Reorder or add rows in a PR. Requests for a specific next topic go through the ops channel.

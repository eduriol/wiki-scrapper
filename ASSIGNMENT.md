# Wiki Scrapper — Assignment

## Overview

A web scrapper that, given a root wiki page link, recursively traverses all linked pages and persists them. Built using **Hexagonal Architecture** (Ports & Adapters) so that the domain logic is decoupled from infrastructure concerns.

## Design Decisions

### Ports (Domain Interfaces)

The domain module defines two interfaces (ports) that external modules implement:

- **`WikiPageReader`** — fetches a wiki page by its link. Returns `Optional<WikiPage>` to represent the possibility of a missing page without resorting to exceptions for flow control.
- **`WikiPageStore`** — persists a wiki page. Keeps the domain agnostic of the storage mechanism.

These names are intentionally domain-oriented rather than technology-oriented (e.g., not `WikiPageHttpClient` or `WikiPageJpaRepository`), following the **Dependency Inversion Principle**: the domain defines contracts, and infrastructure modules provide implementations.

### BFS Traversal with Cycle Detection

`WikiScrapper.read(String link)` uses **Breadth-First Search** with a `Set<String> visited` to:

1. Avoid infinite loops when the page graph contains cycles (e.g., page A links to B, B links back to A).
2. Ensure each page is fetched and saved exactly once, even in diamond-shaped graphs (A → B, A → C, B → D, C → D).

BFS was chosen over DFS for predictable level-order traversal. Both would work correctly for this use case.

#### How DFS would differ

A Depth-First Search alternative would replace the `Queue<String>` (FIFO) with a `Stack<String>` (LIFO) — or use recursion with the call stack. The `Set<String> visited` would remain identical. The only behavioral difference is traversal order: DFS would go deep into one branch before backtracking, while BFS explores all neighbors at each level first. For this assignment, traversal order does not affect correctness since every reachable page is saved regardless.

### Error Handling Strategy

- **Root page not found**: throws `WikiPageNotFound` (unchecked exception). This is a hard failure — if the starting point doesn't exist, the operation cannot proceed.
- **Child page not found**: logged as a WARNING and skipped. The scrapper continues with remaining links. This is a soft failure — broken links in a wiki are expected and should not abort the entire operation.

## How to Run Tests

```bash
# Domain module only
mvn -pl domain test
```

## Assumptions

1. The persistence layer (`WikiPageRepository.save()`) is provided as a no-op stub, as stated in the assignment. We call it normally and trust it works.
2. Links in wiki pages are absolute URLs (e.g., `http://wikiscrapper.test/site1`).

## Future Work

- **Concurrency**: Parallel BFS using `ExecutorService` + `ConcurrentHashMap` for visited set, fetching multiple pages simultaneously.
- **Real HTTP client adapter**: An implementation of `WikiPageReader` that fetches pages from actual URLs over HTTP.
- **Malformed resource handling**: Graceful error handling when JSON/HTML resources are syntactically invalid.
- **Rate limiting**: Throttle requests when scraping real websites to avoid being blocked.


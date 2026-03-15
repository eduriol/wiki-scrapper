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

### JsonWikiClient — JSON Adapter

`JsonWikiClient` implements `WikiPageReader` by reading JSON files from the classpath. The URL-to-resource mapping extracts the last path segment from the link (e.g., `http://wikiscrapper.test/site1` → `wikiscrapper/site1.json`) and loads it via `ClassLoader.getResourceAsStream()`.

Parsing uses Jackson's `ObjectMapper.readTree()` for lightweight tree-model access without needing a DTO class — the JSON structure maps directly to `WikiPage` fields. If a resource is not found or parsing fails, `Optional.empty()` is returned and an appropriate log message is emitted.

#### Finding: `site5.json` was malformed

The original `site5.json` was missing the closing `]` for the `links` array, making it invalid JSON. This was fixed as part of this commit. If malformed resources should be handled gracefully at runtime instead of being fixed at source, the current implementation already returns `Optional.empty()` and logs a SEVERE error on parse failures.

### HtmlWikiClient — HTML Adapter

`HtmlWikiClient` implements `WikiPageReader` by reading HTML files from the classpath using the same URL-to-resource mapping as `JsonWikiClient` (last path segment, e.g., `site1` → `wikiscrapper/site1.html`).

Parsing uses **Jsoup**, a well-established Java HTML parser. The HTML structure is queried with CSS selectors:

| Field      | Selector                  | Extraction                |
|------------|---------------------------|---------------------------|
| `selfLink` | `meta[selfLink]`          | `attr("selfLink")`        |
| `title`    | `h1.title`                | `.text()`                 |
| `content`  | `p.content`               | `.text()`                 |
| `links`    | `ul.links a[href]`        | `.attr("href")` per `<a>` |

Jsoup was chosen over regex or manual parsing because it handles malformed HTML gracefully, provides a familiar CSS selector API, and has no transitive dependencies. The same error handling strategy applies: resource not found or parse failure returns `Optional.empty()` with an appropriate log message.

### Spring Profile-Based Client Selection

The application uses **Spring Profiles** to select which `WikiPageReader` implementation is active at startup. `WikiScrapperConfiguration` declares two beans:

- `JsonWikiClient` → `@Profile("json")`
- `HtmlWikiClient` → `@Profile("html")`

Only one is active at a time. The `WikiScrapper` bean receives a `WikiPageReader` parameter, and Spring injects whichever bean matches the active profile.

The default profile is set in `application.properties`:

```properties
spring.profiles.active=json
```

This can be overridden at startup via command-line argument (see "How to Build and Run" section). This is a **compile-time / startup-time** decision, not a per-request one — the entire application runs with one client throughout its lifecycle.

## How to Build and Run

```bash
# Build the entire project (compile + test + package)
mvn clean install

# Run the application (Spring Boot)
mvn -pl app spring-boot:run

# Run with a specific profile (json or html)
# On Bash / Linux / macOS
mvn -pl app spring-boot:run -Dspring-boot.run.profiles=json

# On PowerShell (Windows) — quotes are required around -D flags
mvn -pl app spring-boot:run "-Dspring-boot.run.profiles=json"
```

## How to Run Tests

```bash
# All modules
mvn test

# Domain module only
mvn -pl domain test

# JSON client module only
mvn -pl jsonwikiclient test

# HTML client module only
mvn -pl htmlwikiclient test
```

## Assumptions

1. The persistence layer (`WikiPageRepository.save()`) is provided as a no-op stub, as stated in the assignment. We call it normally and trust it works.
2. Links in wiki pages are absolute URLs (e.g., `http://wikiscrapper.test/site1`).

## Future Work

- **Concurrency**: Parallel BFS using `ExecutorService` + `ConcurrentHashMap` for visited set, fetching multiple pages simultaneously.
- **Real HTTP client adapter**: An implementation of `WikiPageReader` that fetches pages from actual URLs over HTTP.
- **Malformed resource handling**: Graceful error handling when JSON/HTML resources are syntactically invalid.
- **Rate limiting**: Throttle requests when scraping real websites to avoid being blocked.


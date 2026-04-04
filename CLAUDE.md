# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with
code in this repository.

## Commands

### Running Tests

Server-side (Clojure/JVM):

```bash
lein test
```

Run a single test namespace:

```bash
lein test dgknght.app-lib.core-test
```

Run tests with coverage (CI mode, requires 90% threshold):

```bash
lein cloverage
```

Client-side (ClojureScript):

```bash
lein doo once
```

### Building

```bash
lein jar
```

### Linting

```bash
clj-kondo --lint src:test
```

## Architecture

This is a Clojure/ClojureScript utility library (`com.github.dgknght/app-lib`)
for web application development. It targets both JVM (server) and browser
(client) environments using a three-directory source layout:

- `src/clj/` — Server-only Clojure code
- `src/cljs/` — Client-only ClojureScript code
- `src/cljc/` — Shared code using `#?(:clj ... :cljs ...)` reader conditionals

### Key Modules

#### Shared (CLJC)

- `core` — Parsing utilities (bool/int/float/decimal), deep map operations
  (`deep-get`, `deep-dissoc`, `deep-update-in-if`), UUID generation
- `web` — Email validation, path generation, date/number formatting
  (currency, percent, decimal)
- `dates` — Date range parsing ("2015-03"), relative date strings
  ("start-of-this-year"), periodic sequences
- `calendar` — Calendar data structure generation for UI widgets
  (month/week views, navigation)
- `models` — Collection indexing (`map-index`), hierarchical nesting/unnesting,
  ID extraction
- `validation` (shared part via `forms_validation`) — Form validation state
  management: rules, messages per field, `validated?` flag
- `inflection` — String transforms: humanize, title-case, ordinals,
  pluralization
- `math` — Simple math expression parser/evaluator

#### Server-only (CLJ)

- `api` — Ring middleware helpers: response builders for REST
  (201/200/401/403/404/422), token extraction from Authorization header,
  `wrap-api-exception` middleware
- `authorization` — Stowaway-based RBAC: `allowed?` multimethod, opaque/visible
  error modes, scope filtering
- `validation` — Clojure spec-based validation with human-readable error
  messages; email, date, length predicates

#### Client-only (CLJS)

- `api` / `api_async` / `api_3` — HTTP client layers built on cljs-http;
  `api_async` adds core.async channel-based composition
- `forms` — Reagent form components with validation integration; decorators
  select Bootstrap 4 or 5 field rendering
- `forms/typeahead` — Autocomplete input component
- `bootstrap_4` / `bootstrap_5` — Bootstrap decorator implementations for forms
- `notifications` — Toast/notification component
- `busy` — Loading state management

### Testing Infrastructure

Test files mirror source structure under `test/dgknght/app_lib/`. CLJ tests
use `.clj` extension; CLJS tests use `.cljs`. Shared fixtures live in
`*_fixtures.clj` files. `test_assertions.cljc` provides custom assertion
macros used throughout.

### Dependencies of Note

- **Dates:** `clj-time` (server) / `cljs-time` (client) — both wrap Joda-Time/goog.date
- **HTTP:** `clj-http` (server) / `cljs-http` (client)
- **UI:** Reagent 0.8.0 (React wrapper for ClojureScript)
- **JSON:** Cheshire + custom encoding in `json_encoding.clj`
- **Case conversion:** `camel-snake-kebab` — used heavily in API layers

### JVM Settings

Tests run with UTC timezone and US locale (set in `project.clj` JVM options).
This matters for any date/time or number formatting tests.

## Guidelines

- Write tests first
- Fix any failing tests
- Fix any linter errors

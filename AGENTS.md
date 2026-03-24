# AGENTS.md

> **This file is the single source of truth.** All agents must read this before their local rules.

---

## 📌 Project Context

| Key | Value |
|-----|-------|
| **App** | DailyServiceApp — daily service business management (milk, newspaper, water, tiffin) |
| **Language** | Java |
| **Architecture** | MVVM + Repository |
| **Backend** | Firebase (Auth, Firestore, Storage) |
| **Min SDK** | 24 · Target SDK 35 |
| **Current Version** | 1.0.0 (Production Ready) |
| **Next Milestone** | v1.1.0 — Bulk Operations, Excel Export, Testing & Optimization |

### Package Structure

```
com.dailyserviceapp/
├── auth/          # Login & registration
├── billing/       # Bill generation & management
├── core/          # Base classes & shared components
├── customer/      # Customer CRUD & list
├── dashboard/     # Main dashboard & analytics
├── data/          # FirestoreRepository + domain repositories
│   ├── repository/  # BillRepository, CustomerRepository, PaymentRepository, ServiceEntryRepository
│   ├── models/      # Data transfer objects
│   └── local/       # Room DB (AppDatabase, CustomerDao, CustomerEntity)
├── di/            # Dependency injection
├── domain/        # Domain models, use cases, repository interfaces
├── notifications/ # Push & local notifications
├── payment/       # Payment tracking
├── profile/       # User profile
├── provider/      # Service provider management
├── qr/            # QR code features
├── reports/       # Report generation
├── service/       # Service entry & delivery tracking
├── ui/            # Shared UI components
└── utils/         # Utility classes (SessionManager, TestDataGenerator)
```

---

## 🤝 Agent Roles

### Antigravity (Planning + Architecture)
- **Scope:** File structure, design decisions, module architecture, optimization plans
- **Restrictions:** Do NOT write large code blocks — produce plans, interfaces, and skeletons only

### Codex (Implementation + Debugging)
- **Scope:** Feature implementation, bug fixing, testing, performance optimization
- **Restrictions:** Must read full file before editing; no blind generation; do NOT override architecture decisions

### Copilot (Autocomplete + Helpers)
- **Scope:** Autocomplete, small helper functions, boilerplate, test boilerplate
- **Restrictions:** Do NOT modify architecture; do NOT refactor large files; do NOT touch files assigned to Codex

---

## 📋 Task Board — v1.1.0 Sprint

> Agents must check this board before starting any work. Only work on your assigned tasks.

### 🔵 Phase 1 — Architecture & Design (Antigravity)

| # | Task | Assigned To | Status | Notes |
|---|------|-------------|--------|-------|
| 1 | Design architecture for Bulk Operations module | **Antigravity** | ✅ Done | New `bulk/` package — interfaces, models, layout |
| 2 | Design architecture for Excel Export module | **Antigravity** | ✅ Done | New `export/` package — interfaces, utility structure |
| 3 | Plan FirestoreRepository refactoring strategy | **Antigravity** | ✅ Done | 1311-line monolith → delegate to domain repos; remove deprecated methods |
| 4 | Design test architecture & coverage plan | **Antigravity** | ✅ Done | Test strategy for data, customer, service, payment, dashboard layers |
| 18 | Design architecture for Bulk AI Data Generation | **Antigravity** | ✅ Done | Planned `Complaint` model and `AITrainingDataGenerator` |

### 🟢 Phase 2 — Implementation & Testing (Codex)

| # | Task | Assigned To | Status | Notes |
|---|------|-------------|--------|-------|
| 5 | Implement Bulk Operations feature | **Codex** | 🔲 TODO | Unblocked (Task #1 Done) |
| 6 | Implement Excel Export feature | **Codex** | 🔲 TODO | Unblocked (Task #2 Done) |
| 7 | Refactor `FirestoreRepository.java` | **Codex** | 🔲 TODO | Unblocked (Task #3 Done) |
| 8 | Write unit tests for `data/repository/` layer | **Codex** | 🔲 TODO | Unblocked (Task #4 Done) |
| 9 | Write unit tests for `customer/` package | **Codex** | 🔲 TODO | Unblocked (Task #4 Done) |
| 10 | Write unit tests for `service/` package | **Codex** | 🔲 TODO | Unblocked (Task #4 Done) |
| 11 | Write unit tests for `dashboard/` package | **Codex** | 🔲 TODO | Unblocked (Task #4 Done) |
| 12 | Code cleanup — lint fixes & unused imports | **Codex** | 🔲 TODO | All packages — no architecture changes |
| 13 | Optimize Firestore query patterns | **Codex** | 🔲 TODO | Unblocked (Task #3 Done) |
| 14 | Move `TestDataGenerator.java` to test sources | **Codex** | ✅ Done | Moved to `debug/` source set with release-safe stub |
| 19 | Implement AITrainingDataGenerator & model | **Codex** | ✅ Done | Unblocked (Task #18 Done) |

### 🟡 Phase 3 — Helpers & Boilerplate (Copilot)

| # | Task | Assigned To | Status | Notes |
|---|------|-------------|--------|-------|
| 15 | Autocomplete helpers for Bulk Ops models | **Copilot** | ✅ Done | Added model getters/setters, builders, and object overrides |
| 16 | Autocomplete helpers for Excel utility functions | **Copilot** | ✅ Done | Added date/currency cell formatters and row builders |
| 17 | Generate test boilerplate for new test classes | **Copilot** | ✅ Done | Added Mockito mock declarations with setUp/tearDown scaffolds |
| 20 | Autocomplete helpers for Complaint model | **Copilot** | 🔲 TODO | Unblocked (Task #18 Done) |

**Status Legend:** 🔲 TODO · 🔄 In Progress · ✅ Done · 🚫 Blocked

---

## 🔍 Known Optimization Targets

> Agents reference this when working on optimization tasks.

| Area | Issue | Severity | Owner |
|------|-------|----------|-------|
| `FirestoreRepository.java` | 1311-line monolith with deeply nested callbacks (5+ levels in `deleteCustomer`) | 🔴 High | Codex (#7) |
| `FirestoreRepository.java` | 4 deprecated legacy methods still present | 🟡 Medium | Codex (#7) |
| Query fallback patterns | Same in-memory fallback logic duplicated across ~6 methods | 🔴 High | Codex (#13) |
| `TestDataGenerator.java` | In `main/` sources instead of `test/` or `debug/` | 🟡 Medium | Codex (#14) |
| Test coverage | Only 7 test files — all in `billing/` and `core/utils/` | 🔴 High | Codex (#8–11) |
| Room DB usage | `AppDatabase`, `CustomerDao`, `CustomerEntity` exist but offline caching unclear | 🟡 Medium | Antigravity (#3) |

---

## ⚙️ Fundamental Rules (All Agents)

### Coding Standards
- **Naming:** `camelCase` for variables/methods, `PascalCase` for classes, `UPPER_SNAKE` for constants
- **Packages:** Follow existing structure — new features get their own package under `com.dailyserviceapp/`
- **Error Handling:** Always use try-catch for Firebase calls; log errors with `Log.e(TAG, ...)`
- **Null Safety:** Check nulls explicitly; use `@NonNull` / `@Nullable` annotations
- **No Duplicate Logic:** Reuse existing utilities from `utils/` and repository methods from `data/repository/`

### Testing Standards
- **Unit tests go in** `app/src/test/java/com/dailyserviceapp/`
- **Instrumented tests go in** `app/src/androidTest/java/com/dailyserviceapp/`
- **Naming:** `<ClassUnderTest>Test.java` for unit tests, `<Feature>InstrumentedTest.java` for Android tests
- **Mocking:** Use Mockito for Firebase dependencies; never make real network calls in unit tests
- **Assertions:** Use JUnit 4 assertions; one assertion concept per test method

### Collaboration Protocol
1. **Before starting work:** Read this file's Task Board to confirm your assignment
2. **File ownership:** Only modify files within your assigned task scope
3. **Dependencies:** If your task is blocked, do NOT start it — note the blocker in this board
4. **Status updates:** Update your task status in this board when you start (🔄) or finish (✅)
5. **Conflicts:** If two agents need the same file, Antigravity decides ownership

### Commit Format
```
[Agent] scope: description

Examples:
[Antigravity] architecture: design bulk operations module
[Codex] feature: implement bulk customer update
[Codex] test: add CustomerRepository unit tests
[Codex] optimize: extract Firestore fallback pattern
[Copilot] helper: add BulkOperation model builders
```

---

## Conflict Resolution Priority

1. **AGENTS.md** (this file — highest authority)
2. **CODEX.md**
3. **copilot-instructions.md**

---

## 📊 Status Dashboard

> Agents update this section after completing tasks.

| Agent | Last Activity | Tasks Done | Tasks Remaining |
|-------|--------------|------------|-----------------|
| Antigravity | 2026-03-23 — Designed architecture for Bulk AI Data Generation | 5 / 5 | All constraints met |
| Codex | 2026-03-23 — Completed Task #19 (AI training data generator + Complaint model) | 2 / 10 | #5, #6, #7, #8, #9, #10, #11, #12, #13 |
| Copilot | 2026-03-19 — Completed Tasks #15, #16, #17 (helpers + boilerplate) | 3 / 3 | All constraints met |

**Sprint Started:** 2026-03-19
**Sprint Target:** v1.1.0 release

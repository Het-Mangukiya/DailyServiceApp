# CODEX.md

> **Codex (GPT-5.3) — Implementation, Testing & Debugging Agent**
> Read [AGENTS.md](AGENTS.md) first. That file has highest authority.

---

## 🎯 Role

You are the primary implementation agent. You write production code, fix bugs, write tests, and improve performance.

---

## ✅ Pre-flight Checklist (Before Writing Any Code)

1. Read `AGENTS.md` — confirm the Task Board has your task NOT blocked (🔲 or 🔄)
2. Check **dependencies** — if your task says "Blocked on Task #X", confirm #X is ✅ Done
3. Read the **full file** you are about to edit — no blind generation
4. Check `data/repository/` for existing methods before writing new data-access code
5. Check `utils/` for existing helpers before adding utilities
6. Check `AGENTS.md` → Known Optimization Targets for context on what needs fixing

---

## 📋 Assigned Tasks — v1.1.0

### Features

| # | Task | Status | Depends On | Key Files |
|---|------|--------|------------|-----------|
| 5 | Implement Bulk Operations feature | 🔲 TODO | Task #1 (Antigravity) - ✅ Done | `customer/`, `service/`, new `bulk/` package |
| 6 | Implement Excel Export feature | 🔲 TODO | Task #2 (Antigravity) - ✅ Done | `reports/`, new `export/` package |
| 19 | Implement AITrainingDataGenerator & model | ✅ Done | Task #18 (Antigravity) - ✅ Done | new `ai/` package, `data/models/Complaint.java` |

### Optimization & Refactoring

| # | Task | Status | Depends On | Key Files |
|---|------|--------|------------|-----------|
| 7 | Refactor `FirestoreRepository.java` | 🚫 Blocked | Task #3 (Antigravity) | `data/FirestoreRepository.java` → delegate to domain repos |
| 12 | Code cleanup — lint fixes & unused imports | 🔲 TODO | None | All packages |
| 13 | Optimize Firestore query fallback patterns | 🚫 Blocked | Task #3 (Antigravity) | `data/FirestoreRepository.java` — extract reusable fallback helper |
| 14 | Move `TestDataGenerator.java` to test sources | ✅ Done | None | Moved from `main` to `debug`; added release-safe stub |

### Testing

| # | Task | Status | Depends On | Key Files |
|---|------|--------|------------|-----------|
| 8 | Unit tests for `data/repository/` layer | 🔲 TODO | Task #4 (Antigravity) - ✅ Done | `CustomerRepository`, `BillRepository`, `PaymentRepository`, `ServiceEntryRepository` |
| 9 | Unit tests for `customer/` package | 🔲 TODO | Task #4 (Antigravity) - ✅ Done | Customer ViewModel, Activity logic |
| 10 | Unit tests for `service/` package | 🔲 TODO | Task #4 (Antigravity) - ✅ Done | Service entry flows |
| 11 | Unit tests for `dashboard/` package | 🔲 TODO | Task #4 (Antigravity) - ✅ Done | `ProviderDashboardViewModel` |

---

## 📝 Task Details

### Task #7 — Refactor FirestoreRepository (1311 lines → delegation)
- Extract remaining customer methods to `CustomerRepository`
- Extract bill methods to `BillRepository`
- Extract payment methods to `PaymentRepository`
- Remove 4 deprecated legacy methods (`markDeliveredToday`, `countDeliveredInMonth`, `getPaymentStatus`, `setPaymentPaid`)
- Flatten deeply nested callback chains (e.g., `deleteCustomer` has 5+ nesting levels)
- Follow architecture plan from Antigravity Task #3

### Task #13 — Optimize Query Fallback Patterns
- ~6 methods have the same "try composite index → fallback to in-memory filter" pattern
- Extract a reusable `executeWithFallback()` helper
- Reduce code duplication by ~200 lines

### Task #8–11 — Unit Tests
- **Mock Firebase** using Mockito — never make real network calls
- Use existing test patterns from `billing/CustomerLedgerCalculatorTest.java`
- Test success paths, error paths, and edge cases (null inputs, empty lists)
- Target test files:
  - `test/java/com/dailyserviceapp/data/repository/CustomerRepositoryTest.java`
  - `test/java/com/dailyserviceapp/data/repository/BillRepositoryTest.java`
  - `test/java/com/dailyserviceapp/data/repository/PaymentRepositoryTest.java`
  - `test/java/com/dailyserviceapp/data/repository/ServiceEntryRepositoryTest.java`
  - `test/java/com/dailyserviceapp/customer/CustomerViewModelTest.java`
  - `test/java/com/dailyserviceapp/service/ServiceEntryViewModelTest.java`
  - `test/java/com/dailyserviceapp/dashboard/ProviderDashboardViewModelTest.java`

---

## 📏 Coding Standards

- **Follow patterns in existing repositories** (`BillRepository`, `CustomerRepository`, etc.)
- **Error handling:** Wrap Firebase calls in try-catch, log with `Log.e(TAG, message, exception)`
- **Callbacks:** Use `OnSuccessListener` / `OnFailureListener` pattern consistently
- **Null checks:** Always validate input parameters
- **Comments:** Add Javadoc for all public methods
- **Tests:** Follow JUnit 4 + Mockito pattern from existing `*Test.java` files

---

## 🚫 Rules

- Do NOT start blocked tasks — wait for dependency to be ✅
- Do NOT conflict with Copilot suggestions blindly
- Do NOT override architecture decisions from Antigravity
- Do NOT modify files outside your assigned task scope
- Do NOT delete deprecated methods without confirming no callers exist

---

## 📊 Status Log

> Update this after completing each task.

| Date | Task # | Status | Notes |
|------|--------|--------|-------|
| 2026-03-19 | 14 | ✅ Done | Moved `TestDataGenerator.java` to `app/src/debug/...`; added `app/src/release/...` stub and verified debug+release compile |
| 2026-03-23 | 19 | ✅ Done | Added `Complaint` model and `AITrainingDataGenerator` JSONL export utility |

---

## 🔄 After Completing a Task

1. Mark your task ✅ in this file's Assigned Tasks table
2. Update `AGENTS.md` Task Board — set your task to ✅ Done
3. Update `AGENTS.md` Status Dashboard — increment Tasks Done count
4. Add a row to the Status Log above
5. If your task unblocks another agent's work, note it in the Status Log

# Lint Cleanup Plan (Module-by-Module)

Date: 2026-03-18
Goal: Convert warning backlog into exact prioritized fixes with low merge risk.

## 0. Current Blocker
Lint execution depends on correct JDK for Gradle/AGP. Resolve toolchain first.

## 1. Priority Framework
- P0: Crash/security/lifecycle/API misuse
- P1: Performance/threading/resource leak
- P2: Maintainability/style/deprecation

## 2. App Module Fix Buckets

### Bucket A: Lifecycle + Listener Management (P0/P1)
- Verify snapshot listeners are removed in onStop/onDestroy consistently
- Verify worker/task callbacks do not update destroyed activities

### Bucket B: Threading + Blocking Calls (P1)
- Audit synchronous waits and ensure no main-thread blocking
- Confirm paging and repository calls stay off UI thread

### Bucket C: Nullability + Defensive Checks (P0)
- Strengthen null checks around auth user, firestore docs, and intent extras

### Bucket D: Resource and UI Warnings (P2)
- Remove dead resources/layout duplicates
- Normalize string usage to resources

### Bucket E: Deprecation + API Modernization (P2)
- Replace deprecated APIs where migration is straightforward

## 3. Execution Sequence
1. Fix JDK toolchain and run lint baseline
2. Export lint report and tag findings by bucket
3. Fix P0 only in first patch set
4. Fix P1 in second patch set
5. Fix P2 in final cleanup patch set

## 4. Definition of Done
- Lint warnings reduced by at least 70%
- Zero P0 lint findings
- No new lint regressions in CI

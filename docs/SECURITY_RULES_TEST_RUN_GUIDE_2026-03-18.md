# Security Rules Test Run Guide

Date: 2026-03-18
Status: Verified locally (43/43 tests passed)

## Prerequisites
- Node.js installed
- Java installed (required by Firestore emulator)

## Install dependencies

Run from repository root:

npm --prefix tests/security install

## Execute rules tests

Preferred command (project-aligned):

./tests/security/node_modules/.bin/firebase emulators:exec --project sgp-1-53142 --only firestore "npm --prefix tests/security run test:rules"

Alternative command (via npx):

npx --prefix tests/security firebase emulators:exec --project sgp-1-53142 --only firestore "npm --prefix tests/security run test:rules"

## Expected Output
- Jest summary should report:
  - Test Suites: 1 passed
  - Tests: 43 passed
- Exit code should be 0

## Notes on Console Warnings
You may see Firestore gRPC PERMISSION_DENIED warnings in output.
These are expected for deny-path test cases that intentionally assert failures.

## Troubleshooting
1. If emulator hub port conflicts appear:
- Ensure no stale firebase/emulator processes are running.

2. If dependency resolution fails:
- Use versions pinned in tests/security/package.json.

3. If multiple project ID warning appears:
- Use --project sgp-1-53142 when running emulators:exec.

## Related Files
- tests/security/firestore.rules.spec.js
- tests/security/testData.seed.js
- tests/security/helpers/authContexts.js
- docs/FIRESTORE_RULES_TEST_MATRIX_2026-03-18.md

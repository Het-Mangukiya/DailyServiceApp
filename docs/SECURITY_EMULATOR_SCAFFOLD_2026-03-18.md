# Security Emulator Scaffold

Date: 2026-03-18
Purpose: Provide a ready-to-implement scaffold for Firestore rules testing in Firebase Emulator Suite.

## 1) Recommended Folder Structure

Create these files/folders:

- tests/security/package.json
- tests/security/firestore.rules.spec.js
- tests/security/testData.seed.js
- tests/security/helpers/authContexts.js
- tests/security/helpers/assertions.js

Optional CI integration:
- .github/workflows/firestore-rules-tests.yml

## 2) package.json (tests/security)

Use Node test dependencies:
- @firebase/rules-unit-testing
- firebase
- firebase-tools
- jest

Suggested scripts:
- test:rules
- test:rules:watch

Example commands:
- npm --prefix tests/security install
- npm --prefix tests/security run test:rules

## 3) Emulator Command Patterns

Local run (single command):
- firebase emulators:exec --only firestore "npm --prefix tests/security run test:rules"

Start emulator manually (if needed):
- firebase emulators:start --only firestore

CI-friendly run:
- firebase emulators:exec --project demo-test --only firestore "npm --prefix tests/security run test:rules"

## 4) Test File Skeleton Plan

### firestore.rules.spec.js
Sections:
1. Setup test environment with firestore.rules
2. Seed helper data with admin context
3. users rules tests
4. providers rules tests
5. customerLinks rules tests
6. customers rules tests
7. serviceEntries rules tests
8. supportTickets rules tests
9. payments rules tests
10. bills rules tests
11. legacy deliveries rules tests

Each section should include both:
- assertSucceeds for ALLOW case
- assertFails for DENY case

## 5) Auth Context Helper Design

helpers/authContexts.js should expose factories:
- asProviderA(env)
- asProviderB(env)
- asCustomerA(env)
- asCustomerB(env)
- asAnon(env)

Each should return a Firestore test client bound to auth state.

## 6) Seed Data Contract

testData.seed.js should provide one function:
- seedBaseData(env)

Seed these docs before tests:
- users/providerA
- users/providerB
- providers/providerA
- customers/custA (providerId=providerA)
- customers/custB (providerId=providerB)
- customerLinks/customerA (customerId=customerA, providerId=providerA)
- serviceEntries/entryA (providerId=providerA, customerId=custA)
- bills/billA
- payments/payA

## 7) Mapping to Existing Matrix

All test cases should be directly mapped to:
- docs/FIRESTORE_RULES_TEST_MATRIX_2026-03-18.md

Rule:
- No matrix row without a test.
- No test without matrix row reference in comments.

## 8) Definition of Done

- 100% matrix rows covered by tests
- All allow/deny expectations passing
- Command works on clean checkout
- Added to release gate evidence under rules validation

## 9) Parallel Safety Notes

This scaffold is docs-only and does not alter app runtime code.
Safe to run in parallel with DI/repository split without merge conflicts.

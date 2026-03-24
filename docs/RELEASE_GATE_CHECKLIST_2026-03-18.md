# Release Gate Checklist

Date: 2026-03-18
Purpose: Enforce objective release readiness checks aligned to current project tooling and risk profile.

## Gate Categories
- G1 Build and Toolchain
- G2 Quality and Test Evidence
- G3 Security and Access Control
- G4 Product and Demo Validation
- G5 Release Packaging

## G1 Build and Toolchain (Blocker)
- [ ] Java runtime aligned with AGP/Gradle requirements (JDK 17/21 preferred)
- [ ] Gradle sync succeeds on clean machine
- [ ] scripts/quality_gate.sh executes without failure
- [ ] app debug and release APK build artifacts generated

Evidence:
- Terminal logs and artifact paths from quality gate run

## G2 Quality and Test Evidence (Blocker)
- [ ] Unit tests pass in CI (:app:testDebugUnitTest)
- [ ] Lint run passes or has approved waiver list
- [ ] No new critical crashes from smoke run
- [ ] Added/updated tests for changed modules

Evidence:
- Test report output and lint report snapshots

## G3 Security and Access Control (Blocker)
- [ ] Firestore rule emulator matrix passes (ALLOW/DENY)
- [ ] Role routing validated (provider/customer)
- [ ] Provider profile completion enforcement validated
- [ ] Logout/session invalidation validated

Evidence:
- Rules test report + QA notes

## G4 Product and Demo Validation (Required)
- [ ] Happy path demo passes: login -> service entry -> bill -> payment
- [ ] Offline fallback demo passes: queue -> reconnect -> sync reconciliation
- [ ] No blocking UI crashes in provider and customer dashboards

Evidence:
- Demo script checklist and recording references

## G5 Release Packaging (Required)
- [ ] Release notes updated and accurate
- [ ] Version code/version name updated
- [ ] Signing config validated for release
- [ ] Firebase rules/indexes deployed if changed

Evidence:
- Version diff, signed artifact checksums, deploy logs

## Pass/Fail Rule
- Release is BLOCKED if any G1/G2/G3 blocker item is unchecked.
- Release may proceed with minor G4/G5 exceptions only with owner + due date.

## Ownership Template
- Release Manager:
- QA Owner:
- Security Owner:
- Build Owner:
- Decision Timestamp:
- Final Result: PASS / BLOCKED

## Fast Preflight Commands
- ./gradlew :app:lintDebug
- ./gradlew :app:testDebugUnitTest
- ./gradlew :app:assembleDebug
- ./gradlew :app:assembleRelease
- firebase deploy --only firestore:rules,firestore:indexes

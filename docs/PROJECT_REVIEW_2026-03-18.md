# DailyServiceApp Technical Review (March 18, 2026)

## 1) Executive Score (0-100)

- Overall project score: **68 / 100**
- Functional completeness: **75 / 100**
- Reliability and performance: **66 / 100**
- Architecture and maintainability: **65 / 100**
- Security and production hardening: **63 / 100**
- Testing and quality gates: **40 / 100**

Why this score:
- Strong module coverage (provider + customer flows, billing/ledger, reports, QR linking, profile setup, offline sync).
- Build passes for debug and release.
- But production confidence is reduced by thin automated testing, high lint warning volume, and a few session/routing/placeholder gaps.

---

## 2) Verification Snapshot (What Was Run)

### Build and quality checks
- `./gradlew :app:compileDebugJavaWithJavac` -> **PASS**
- `./gradlew :app:assembleDebug` -> **PASS**
- `./gradlew :app:lintDebug` -> **PASS** (`0 errors, 335 warnings`)
- `./gradlew :app:assembleRelease` -> **PASS**
- `./gradlew :app:testDebugUnitTest` -> **FAIL** (`Type T not present` during task creation)

### Binary outputs
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (~22 MB)
- Release unsigned APK: `app/build/outputs/apk/release/app-release-unsigned.apk` (~7.7 MB)

---

## 3) Current Tech Stack (What is used where and why)

### Platform and language
- Android (minSdk 24, targetSdk 35, compileSdk 35), Java 17.
- Why: broad device support, modern Java language features.

### UI and UX
- Material Components + ViewBinding.
- RecyclerView, DrawerLayout, Coordinator/AppBar patterns.
- MPAndroidChart for bar/pie analytics in Reports.
- Why: consistent Material UI, fast development for data-heavy screens.

### Backend and cloud
- Firebase Auth for login/signup and Google sign-in.
- Cloud Firestore as primary real-time data store.
- Firebase Crashlytics + Analytics integrated.
- Why: fast backend iteration, real-time sync, lower ops overhead.

### Offline and sync
- SharedPreferences + Gson (`OfflineCache`) for customer cache and pending delivery queue.
- WorkManager (`PendingEntriesSyncWorker`) for retryable background sync.
- Why: resilience in low-connectivity service-delivery scenarios.

### Billing and documents
- Ledger model computed from `serviceEntries` + `payments`.
- PDF generation via Android `PdfDocument` helper (`MonthlyBillPdfGenerator`).
- Why: avoids hard dependency on bill lifecycle documents; can recompute truth from transactional history.

### QR and linking
- ZXing (`zxing-android-embedded`) for scan and provider code sharing.
- Why: quick onboarding of customer-to-provider mapping.

### Security
- Firestore rules present with role/ownership checks.
- `usesCleartextTraffic=false` and network security config.
- Why: baseline production safety.

---

## 4) Activity-wise Summary (What each screen does)

## Auth and routing
- `SplashActivity`: session check and role-based routing; enforces provider profile completion.
- `LoginActivity`: email/password + Google login; creates/fixes missing user doc; routes by role.
- `SignupActivity`: creates auth user + Firestore profile with provider/customer role.
- `ForgotPasswordActivity`: password reset email flow.

## Provider side
- `DashboardActivity`: main customer list, search/filter/sort, paging mode for large lists, analytics cards, drawer nav.
- `ProviderDashboardActivity`: KPI dashboard (today, monthly, payment overview) + quick actions.
- `JoinRequestsActivity`: approves/rejects customer link requests and creates customer records.
- `ProviderComplaintsActivity`: provider support inbox with status updates and email response.
- `ProfileActivity`: provider profile setup/edit with services, business data, and validation.
- `QRCodeActivity`: generates and shares provider QR/code for customer linking.
- `ServiceEntryActivity`: daily delivery marking (batch), duplicate protection, offline queue + background sync.
- `BillListActivity`: customer ledger list from service/payment history.
- `BillDetailActivity`: customer ledger detail + payment history + service history.
- `PaymentActivity`: records payments and updates legacy bill status when needed.
- `ReportsActivity`: range-based analytics, top customers, service split, bar/pie views.
- `CustomerEditActivity`: add/edit/delete customer records.
- `CustomerDetailActivity`: modern customer profile view with monthly summary and ledger status.

## Customer side
- `CustomerHomeActivity`: join provider via QR/manual code, view link state, unlink, open dashboard.
- `CustomerServiceDashboardActivity`: approved-link customer dashboard with service/payment snapshot and PDF share.
- `ComplaintSupportActivity`: customer ticket creation, ticket history, email provider.

## Notifications
- `NotificationListActivity`: placeholder screen only (not fully implemented).
- `FCMService`: scaffold only (message/token handling TODO).

---

## 5) Key Findings (Production-focused)

### [High] Automated test gate is not currently runnable
- `./gradlew :app:testDebugUnitTest` fails during task creation (`Type T not present`).
- Impact: CI confidence and regression protection are weak.
- Evidence: command output from current review run.

### [High] Session identity source is inconsistent across modules
- Some screens rely on SharedPreferences userId instead of current FirebaseAuth UID.
- Example reads of `providerId = getCurrentUserId()` without auth reconciliation:
  - `app/src/main/java/com/dailyserviceapp/billing/BillListActivity.java`
  - `app/src/main/java/com/dailyserviceapp/reports/ReportsActivity.java`
  - `app/src/main/java/com/dailyserviceapp/dashboard/DashboardActivity.java`
- Impact: can lead to intermittent `PERMISSION_DENIED` after stale local session data.

### [High] Release obfuscation hardening is incomplete
- Release minification enabled, but project-specific ProGuard/R8 rules file is effectively empty.
- Files:
  - `app/build.gradle`
  - `app/proguard-rules.pro`
- Impact: Firestore POJO mapping and reflection-heavy paths are at risk in obfuscated release builds.

### [Medium] Notifications feature is unfinished but scaffolded as if enabled
- Placeholder-only implementation:
  - `app/src/main/java/com/dailyserviceapp/notifications/NotificationListActivity.java`
  - `app/src/main/java/com/dailyserviceapp/notifications/FCMService.java`
- Impact: product expectation mismatch and incomplete customer-provider communication loop.

### [Medium] Dashboard top-bar has notification icon path but no functional handler
- Menu includes `action_notifications` in:
  - `app/src/main/res/menu/home_menu.xml`
- No handling branch in:
  - `app/src/main/java/com/dailyserviceapp/dashboard/DashboardActivity.java`
- Impact: user taps icon and nothing happens (broken UX path).

### [Medium] Quality debt is high in lint surface
- Lint reports `0 errors, 335 warnings` (`app/build/reports/lint-results-debug.txt`).
- Major buckets: hardcoded text, compatibility attributes, dependency drift.
- Impact: localization, maintainability, and long-term velocity suffer.

### [Medium] Legacy/duplicate resources increase maintenance risk
- Side-by-side old/new layouts exist (`activity_home.xml` vs `activity_dashboard.xml`, `activity_payment_new.xml` vs `activity_payment.xml`, etc.).
- Impact: accidental edits in inactive files, onboarding confusion, regressions during UI changes.

---

## 6) What is already strong

- End-to-end provider flow is substantial and mostly coherent.
- Customer-provider QR linking + approval flow exists.
- Ledger-first billing model is a solid design choice.
- Offline queue + WorkManager retry model is good for real-world delivery operations.
- Firestore rules are significantly stricter than typical prototypes.
- Both debug and release APKs currently build successfully.

---

## 7) Priority Improvement Roadmap

### Phase 1 (Immediate: 1-3 days)
- Fix unit-test task configuration so `testDebugUnitTest` runs in CI/local reliably.
- Introduce single source of truth for `currentUserId` (prefer FirebaseAuth UID, sync prefs).
- Wire `action_notifications` click to `NotificationListActivity` (or hide icon until ready).
- Add baseline R8 keep rules for Firestore model classes and reflection-sensitive paths.

### Phase 2 (Short term: 3-7 days)
- Reduce lint warnings by at least 60%:
  - hardcoded text extraction
  - appcompat drawable attribute replacements
  - stale dependency upgrades with compatibility checks
- Remove or archive unused duplicate layouts/classes.
- Add integration-style tests for critical flows:
  - signup/login role routing
  - mark delivery transaction
  - ledger calculations + payment effects
  - join request approve/reject

### Phase 3 (Review-ready polish: 1-2 weeks)
- Finish notifications (FCM token save, local notif display, notification list backed by Firestore).
- Add structured analytics events for key business actions.
- Add crash-safe global error handling and better retry UX states.
- Add release checklist automation (lint, unit tests, assembleRelease, smoke script).

---

## 8) Suggested Next Sprint Backlog (Concrete)

1. Stabilize test execution and add 8-12 critical unit/integration tests.
2. Centralize auth identity resolution and remove stale session edge cases.
3. Clean resource/layout duplication and unused classes/dependencies.
4. Complete notifications and communication loop.
5. Lint cleanup and dependency update pass.

---

## 9) Short panel-ready pitch

- "DailyServiceApp is a Firebase-backed Android app for service providers and customers, with delivery tracking, customer management, ledger-based billing, reports, QR onboarding, and offline sync."
- "Current maturity is 68/100: feature coverage is strong, but testing depth and production hardening are the main upgrade areas."
- "Our next focus is reliability and production polish: test gate stability, session consistency, notification completion, and lint/dependency cleanup."

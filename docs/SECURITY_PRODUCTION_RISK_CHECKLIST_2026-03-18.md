# Security Audit: Production Risk Checklist

Date: 2026-03-18
Scope: Auth, role routing, Firestore rules, client-side data flow

## 1. Current Strengths
- Firestore rules use signed-in checks and owner/provider constraints
- Customer/service/payment/bill paths include provider ownership checks
- Role-based routing exists in splash/login logic

## 2. Production Risks (Prioritized)

### P1 High
- Rules and app behavior may drift over time without automated rule tests
- Token lifecycle and push-notification handling is incomplete in FCM service
- Build/runtime inconsistency can hide security regressions during validation

### P2 Medium
- Mixed hardcoded collection names increase typo/misrouting risk
- Duplicate session abstractions can create inconsistent logout/session invalidation
- Offline cache in SharedPreferences lacks encryption-at-rest hardening for sensitive data

### P3 Low
- Legacy collection paths remain and should be sunset with migration controls

## 3. Mandatory Go-Live Checks
- Confirm all Firestore collections used by app are covered by explicit rules
- Verify no write path accepts providerId/customerId mismatch
- Verify role escalation is impossible through client-side profile updates
- Validate logout clears local session and refreshes route correctly
- Validate profile setup enforcement for provider before dashboard access

## 4. Recommended Security Work Items
1. Add Firebase emulator rule tests for allow/deny matrix by role
2. Implement FCM token registration + rotation + invalidation workflow
3. Remove hardcoded collection literals and centralize to constants
4. Deprecate unused session manager and keep single source of truth
5. Add threat checks for offline cache data handling and consider encrypted storage

## 5. Risk Decision Template
- Block release if any P1 item is unresolved
- Permit release with P2 only if mitigation owner and date are assigned
- Track P3 in routine technical debt cycle

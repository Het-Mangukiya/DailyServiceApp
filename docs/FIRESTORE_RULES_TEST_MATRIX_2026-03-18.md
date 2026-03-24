# Firestore Rules Test Matrix

Date: 2026-03-18
Purpose: Emulator-ready allow/deny matrix to validate production authorization behavior before release.

## Test Actors
- providerA: authenticated provider user A
- providerB: authenticated provider user B
- customerA: authenticated customer user A
- customerB: authenticated customer user B
- anon: unauthenticated user

## Seed Data Assumptions
- customers/custA has providerId = providerA
- customers/custB has providerId = providerB
- serviceEntries/entryA has providerId = providerA, customerId = custA
- bills/billA has providerId = providerA, customerId = custA
- payments/payA has providerId = providerA, customerId = custA
- customerLinks/customerA has customerId = customerA, providerId = providerA
- users/providerA exists
- providers/providerA exists with userId = providerA

## Matrix by Collection

### /users/{userId}
- providerA read users/providerA: ALLOW
- providerA update users/providerA: ALLOW
- providerA read users/providerB: DENY
- anon read users/providerA: DENY

### /providers/{providerId}
- providerA create providers/providerA with userId=providerA: ALLOW
- providerA update providers/providerA preserving userId=providerA: ALLOW
- providerA update providers/providerA with userId=providerB: DENY
- providerB delete providers/providerA: DENY
- customerA read providers/providerA: ALLOW
- anon read providers/providerA: DENY

### /customerLinks/{customerId}
- customerA create customerLinks/customerA with customerId=customerA: ALLOW
- providerA create customerLinks/customerA with providerId=providerA, customerId=customerA: ALLOW
- providerB create customerLinks/customerA with providerId=providerB but customerId mismatch path: DENY
- customerB read customerLinks/customerA: DENY
- providerA read customerLinks/customerA where doc.providerId=providerA: ALLOW

### /customers/{customerId}
- providerA create customers/custA with providerId=providerA: ALLOW
- providerA update customers/custA keeping providerId=providerA: ALLOW
- providerA update customers/custA changing providerId to providerB: DENY
- providerB read customers/custA existing doc: DENY
- providerA get non-existing customers/newCust: ALLOW (pre-check behavior)
- customerA get customers/customerA: ALLOW only if path id equals auth uid OR provider ownership condition satisfied

### /customers/{customerId}/deliveries/{deliveryId} (legacy subcollection)
- providerA write under customers/custA/deliveries/x where custA owned by providerA: ALLOW
- providerB write under customers/custA/deliveries/x: DENY

### /serviceEntries/{entryId}
- providerA create with providerId=providerA and customerId=custA (owned): ALLOW
- providerA create with customerId=custB (not owned): DENY
- providerB delete serviceEntries/entryA: DENY
- customerA get serviceEntries/entryA where customerId is linked to customerA identity doc: ALLOW only when rule condition matches doc.customerId == auth uid
- anon list serviceEntries: DENY

### /supportTickets/{ticketId}
- customerA create with customerId=customerA and providerId=providerA: ALLOW
- providerA update support ticket for customerA/providerA preserving both IDs: ALLOW
- providerA update support ticket while changing customerId: DENY
- any actor delete support ticket: DENY

### /payments/{paymentId}
- providerA create with providerId=providerA and customerId=custA (owned): ALLOW
- providerA create with customerId=custB (not owned): DENY
- providerB read existing payA: DENY
- customer mapped as doc.customerId read own payment doc: ALLOW
- anon get payA: DENY

### /bills/{billId}
- providerA create with providerId=providerA and customerId=custA (owned): ALLOW
- providerA update existing billA preserving providerId: ALLOW
- providerA update existing billA changing providerId: DENY
- providerB read existing billA: DENY
- anon list bills: DENY

### /deliveries/{deliveryId} (legacy root)
- providerA create with providerId=providerA: ALLOW
- providerB delete delivery owned by providerA: DENY
- providerA update while changing providerId: DENY

## Emulator Test Skeleton (Recommended)
- Framework: @firebase/rules-unit-testing (Node)
- Strategy:
  1. Initialize test env with firestore.rules
  2. Seed docs with security disabled context
  3. Assert ALLOW cases with assertSucceeds
  4. Assert DENY cases with assertFails

## Release Gate for Rules
- 0 failing DENY assertions
- 0 failing ALLOW assertions
- Rules tests run in CI on pull requests that modify:
  - firestore.rules
  - firestore.indexes.json
  - auth/role flow classes

## Notes
- Some customer read semantics depend on identity model alignment between customer auth UID and customer document IDs. Keep this explicitly validated in onboarding/login tests.

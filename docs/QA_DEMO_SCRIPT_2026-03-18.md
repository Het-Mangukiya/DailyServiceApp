# QA + Demo Script (Provider + Customer + Offline)

Date: 2026-03-18
Audience: Product demo, QA, stakeholder review

## 1. Demo Objective
Show that DailyServiceApp handles:
- Role-based login and routing
- Provider operations (customer, service entry, billing)
- Customer visibility (linked provider and dashboard)
- Offline queue and recovery sync

## 2. Pre-Demo Setup
- Use two accounts:
  - Provider account with complete profile
  - Customer account linked to provider
- Ensure at least 5 customers exist for provider
- Ensure at least 2 pending and 2 paid records for current month
- Keep one device on emulator and one real network toggle control

## 3. Happy Path Script (10-15 min)
1. Launch app as provider
- Verify splash routes to provider dashboard
- Show top KPIs and customer list

2. Open Service Entry
- Select today
- Mark delivery for 2-3 customers
- Confirm success toast and dashboard metric update

3. Open Bills
- Generate/open current month bill for one customer
- Show amount, status, and ledger summary

4. Open Payments
- Record partial payment
- Return to dashboard and show pending reduction

5. Switch to customer account
- Show customer home and linked provider details
- Open customer service dashboard and verify entries/payments visibility

## 4. Offline Fallback Script (Critical)
1. Disable internet (airplane mode or network cut)
2. Provider -> Service Entry -> mark deliveries
3. Verify pending sync indicator appears
4. Re-enable internet
5. Wait for worker/manual refresh
6. Verify pending count drops and records appear in dashboard/history

## 5. Failure Handling During Live Demo
- If provider dashboard fails to load:
  - Re-login and retry role resolution path
- If Firestore index error appears:
  - Use pre-created index set from firestore.indexes.json
- If sync does not flush immediately:
  - Trigger refresh and wait for periodic/immediate worker window

## 6. QA Acceptance Checklist
- Login route is correct for each role
- Provider can add/edit customers
- Service entries are idempotent per customer/day
- Bill and payment values update ledger correctly
- Offline queue persists and eventually syncs
- No crash in navigation transitions

## 7. Demo Artifacts to Capture
- Screen recording of happy path + offline path
- Screenshot of pending sync before/after recovery
- Notes of latency and any retry behavior

# DailyServiceApp

Daily Service Delivery Tracking and Automated Billing app (Android, Java).

**Features (minimal UI)**
- Add customers
- Mark daily delivery per customer
- Auto-calculate current month bill (delivered days × rate)
- Show payment status (paid/unpaid)
- Data storage: Firebase Firestore

## Project setup

1. Open the project in Android Studio.
2. Create a Firebase project and enable **Cloud Firestore**.
3. Download `google-services.json` and place it at:
	 - `app/google-services.json`
4. Sync Gradle.

## Data model (Firestore)

- `customers/{customerId}`
	- fields: `name`, `phone`, `address`, `serviceType`, `ratePerUnit`, `createdAt`
- `customers/{customerId}/deliveries/{yyyyMMdd}`
	- fields: `dateKey`, `delivered`, `updatedAt`
- `customers/{customerId}/payments/{yyyyMM}`
	- fields: `monthKey`, `paid`, `paidAmount`, `paidOn`

## Screens

- Customers list
- Add customer
- Customer detail (mark delivered today + month summary + payment status)


# Firestore Composite Indexes

This project uses multiple filtered Firestore queries that are faster and more reliable with explicit composite indexes.

## File

- `firestore.indexes.json`

## Deploy using Firebase CLI

Prerequisites:

- Firebase CLI installed (`npm i -g firebase-tools`)
- Logged in (`firebase login`)
- Linked project (`firebase use <project-id>`)

Command:

```bash
firebase deploy --only firestore:indexes
```

## Create manually in Firebase Console

If you prefer console setup, create these composite indexes:

1. Collection: `customers`  
   Fields: `providerId` (ASC), `status` (ASC)
2. Collection: `customers`  
   Fields: `providerId` (ASC), `status` (ASC), `name` (ASC)
3. Collection: `serviceEntries`  
   Fields: `providerId` (ASC), `delivered` (ASC), `date` (ASC)
4. Collection: `payments`  
   Fields: `providerId` (ASC), `paymentDate` (ASC)
5. Collection: `payments`  
   Fields: `billId` (ASC), `paymentDate` (DESC)
6. Collection: `bills`  
   Fields: `providerId` (ASC), `month` (ASC), `year` (ASC)
7. Collection: `deliveries` (legacy subcollection)  
   Fields: `delivered` (ASC), `dateKey` (ASC)

## Notes

- Building an index can take a few minutes in Firebase Console.
- Keep the legacy `deliveries` index until old subcollection paths are fully removed from app code.

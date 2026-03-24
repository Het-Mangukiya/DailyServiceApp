#!/usr/bin/env node
/*
 * One-time maintenance script to normalize provider ownership in Firestore.
 * - Sets `providerId` for all customers to the target provider UID.
 * - Optionally cleans serviceEntries/bills/payments with mismatched providerId.
 *
 * Usage (dry run by default):
 *   FIREBASE_PROJECT_ID=your-project \
 *   GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json \
 *   PROVIDER_UID=providerAuthUid \
 *   node scripts/fix_provider_ownership.js
 *
 * To apply changes (disable dry run): add DRY_RUN=false
 *   DRY_RUN=false node scripts/fix_provider_ownership.js
 *
 * Optional cleanup (dangerous: deletes mismatched docs):
 *   CLEAN_SERVICE_ENTRIES=true CLEAN_BILLS=true CLEAN_PAYMENTS=true ...
 */

const admin = require('firebase-admin');

// Env config
const projectId = process.env.FIREBASE_PROJECT_ID;
const credentialsPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;
const targetProvider = process.env.PROVIDER_UID;
const dryRun = (process.env.DRY_RUN || 'true').toLowerCase() !== 'false';
const cleanServiceEntries = (process.env.CLEAN_SERVICE_ENTRIES || 'false').toLowerCase() === 'true';
const cleanBills = (process.env.CLEAN_BILLS || 'false').toLowerCase() === 'true';
const cleanPayments = (process.env.CLEAN_PAYMENTS || 'false').toLowerCase() === 'true';

if (!projectId || !credentialsPath || !targetProvider) {
  console.error('Missing required env. Set FIREBASE_PROJECT_ID, GOOGLE_APPLICATION_CREDENTIALS, PROVIDER_UID');
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  projectId,
});

const db = admin.firestore();
const BATCH_LIMIT = 400; // Firestore batch write cap is 500; leave headroom.

async function run() {
  console.log(`Project: ${projectId}`);
  console.log(`Target provider UID: ${targetProvider}`);
  console.log(`Dry run: ${dryRun}`);
  console.log(`Clean serviceEntries: ${cleanServiceEntries}, bills: ${cleanBills}, payments: ${cleanPayments}`);

  await fixCollection('customers', fixCustomerProvider);

  if (cleanServiceEntries) {
    await cleanMismatched('serviceEntries');
  }
  if (cleanBills) {
    await cleanMismatched('bills');
  }
  if (cleanPayments) {
    await cleanMismatched('payments');
  }

  console.log('Done.');
}

async function fixCollection(collection, handler) {
  console.log(`\nScanning ${collection}...`);
  const snapshot = await db.collection(collection).get();
  if (snapshot.empty) {
    console.log('No documents found.');
    return;
  }

  let batch = db.batch();
  let pending = 0;
  let updates = 0;

  for (const doc of snapshot.docs) {
    const update = handler(doc);
    if (!update) continue;
    batch.update(doc.ref, update);
    pending++;
    updates++;

    if (pending >= BATCH_LIMIT) {
      await commit(batch);
      batch = db.batch();
      pending = 0;
    }
  }

  if (pending > 0) {
    await commit(batch);
  }

  console.log(`Updated ${updates} docs in ${collection}.`);
}

function fixCustomerProvider(doc) {
  const data = doc.data() || {};
  const current = (data.providerId || '').trim();
  if (current === targetProvider) return null;
  return { providerId: targetProvider };
}

async function cleanMismatched(collection) {
  console.log(`\nCleaning mismatched ${collection}...`);
  const snapshot = await db.collection(collection).get();
  if (snapshot.empty) {
    console.log('No documents found.');
    return;
  }

  let batch = db.batch();
  let pending = 0;
  let deletions = 0;

  for (const doc of snapshot.docs) {
    const data = doc.data() || {};
    const current = (data.providerId || '').trim();
    if (current === targetProvider) continue;
    batch.delete(doc.ref);
    pending++;
    deletions++;
    if (pending >= BATCH_LIMIT) {
      await commit(batch);
      batch = db.batch();
      pending = 0;
    }
  }

  if (pending > 0) {
    await commit(batch);
  }

  console.log(`Deleted ${deletions} docs in ${collection} (providerId mismatch).`);
}

async function commit(batch) {
  if (dryRun) {
    console.log('Dry run: skipping commit');
    return;
  }
  await batch.commit();
}

run().catch((err) => {
  console.error('Error:', err);
  process.exit(1);
});

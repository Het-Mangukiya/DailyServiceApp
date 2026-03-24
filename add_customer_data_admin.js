/**
 * One-shot script: adds 3 months (Jan–Mar 2026) of service entries
 * for a specific customer using Firebase Admin SDK (bypasses security rules).
 *
 * Setup:
 *   1. Go to Firebase Console → Project Settings → Service Accounts
 *   2. Click "Generate new private key" → save as serviceAccountKey.json
 *      in the same folder as this script.
 *   3. Run:  npm install firebase-admin && node add_customer_data_admin.js
 */

const admin = require("firebase-admin");
const serviceAccount = require("./serviceAccountKey.json");

// ── Init ─────────────────────────────────────────────────────────────────────
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId:  "sgp-1-53142",
});
const db = admin.firestore();

// ── Target customer ──────────────────────────────────────────────────────────
const CUSTOMER_ID = "1S7YslxSigQPnFgMQ3xA";
const PROVIDER_ID  = "HbmGaeBoQ9NDcBvSYOPAUmN5AxA2";
const RATE         = 60;

// ── Indian public holidays Jan–Mar 2026 + all Sundays treated as holidays ─────
const HOLIDAYS = new Set([
  "2026-01-01",  // New Year's Day
  "2026-01-14",  // Makar Sankranti / Pongal
  "2026-01-26",  // Republic Day
  "2026-03-20",  // Holi
]);

function pad(n) { return String(n).padStart(2, "0"); }

function isHoliday(date) {
  const key = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  return HOLIDAYS.has(key) || date.getDay() === 0; // 0 = Sunday
}

function alternate(day, a, b) {
  return day % 2 === 0 ? a : b;
}

function buildEntries() {
  const entries = [];
  const months  = [0, 1, 2]; // Jan=0, Feb=1, Mar=2

  for (const month of months) {
    const daysInMonth = new Date(2026, month + 1, 0).getDate();

    for (let day = 1; day <= daysInMonth; day++) {
      const date     = new Date(2026, month, day, 8, 0, 0, 0);
      const holiday  = isHoliday(date);
      const quantity = holiday ? alternate(day, 4, 5) : alternate(day, 2, 3);

      entries.push({
        customerId: CUSTOMER_ID,
        providerId: PROVIDER_ID,
        date:       admin.firestore.Timestamp.fromDate(date),
        quantity,
        rate:       RATE,
        delivered:  true,
        notes:      `${holiday ? "Holiday" : "Regular"} delivery – ${date.toDateString()}`,
        createdAt:  admin.firestore.FieldValue.serverTimestamp(),
        updatedAt:  admin.firestore.FieldValue.serverTimestamp(),
      });
    }
  }
  return entries;
}

// ── Batch write in chunks of 400 ─────────────────────────────────────────────
async function main() {
  const entries = buildEntries();
  console.log(`\n📦 Inserting ${entries.length} service entries for customer: ${CUSTOMER_ID}`);

  const CHUNK = 400;
  let done    = 0;

  for (let i = 0; i < entries.length; i += CHUNK) {
    const batch = db.batch();
    const slice = entries.slice(i, i + CHUNK);

    for (const entry of slice) {
      const ref = db.collection("serviceEntries").doc();
      batch.set(ref, entry);
    }

    await batch.commit();
    done += slice.length;
    console.log(`   ✅ ${done}/${entries.length} committed`);
  }

  console.log(`\n🎉 Done! All ${entries.length} entries added.`);
  process.exit(0);
}

main().catch((err) => {
  console.error("\n❌ Error:", err.message);
  process.exit(1);
});

/**
 * One-shot script: adds 3 months (Jan–Mar 2026) of service entries
 * for a specific customer directly into Firestore.
 *
 * Run: node add_customer_data.js
 */

const { initializeApp } = require("firebase/app");
const { getFirestore, collection, addDoc, Timestamp } = require("firebase/firestore");

// ── Firebase config ─────────────────────────────────────────────────────────
const firebaseConfig = {
  apiKey:    "AIzaSyAoklRbDNcplP_VNqGU0MNfcpVj3WrZadQ",
  projectId: "sgp-1-53142",
  storageBucket: "sgp-1-53142.firebasestorage.app",
};

// ── Target customer ──────────────────────────────────────────────────────────
const CUSTOMER_ID = "1S7YslxSigQPnFgMQ3xA";
const PROVIDER_ID  = "HbmGaeBoQ9NDcBvSYOPAUmN5AxA2";
const RATE         = 60;

// ── Indian public holidays Jan–Mar 2026 ─────────────────────────────────────
// Format: "YYYY-MM-DD"
const HOLIDAYS = new Set([
  "2026-01-01",  // New Year's Day
  "2026-01-14",  // Makar Sankranti / Pongal
  "2026-01-26",  // Republic Day
  "2026-03-20",  // Holi
]);

// ── Helpers ──────────────────────────────────────────────────────────────────
function isHoliday(date) {
  const key = date.toISOString().slice(0, 10); // "YYYY-MM-DD"
  return HOLIDAYS.has(key) || date.getDay() === 0; // Sunday = holiday
}

/** Alternate between two values using day-of-year index */
function alternate(day, a, b) {
  return day % 2 === 0 ? a : b;
}

function buildEntries() {
  const entries = [];
  const months  = [0, 1, 2]; // Jan=0, Feb=1, Mar=2

  months.forEach((month) => {
    const year    = 2026;
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    for (let day = 1; day <= daysInMonth; day++) {
      // 08:00 local → stored as UTC timestamp
      const date = new Date(year, month, day, 8, 0, 0, 0);

      const holiday  = isHoliday(date);
      const quantity = holiday
        ? alternate(day, 4, 5)   // holiday: 4 or 5
        : alternate(day, 2, 3);  // regular: 2 or 3

      entries.push({
        customerId: CUSTOMER_ID,
        providerId: PROVIDER_ID,
        date:       Timestamp.fromDate(date),
        quantity,
        rate:       RATE,
        delivered:  true,
        notes:      `${holiday ? "Holiday" : "Regular"} delivery – ${date.toDateString()}`,
        createdAt:  Timestamp.now(),
        updatedAt:  Timestamp.now(),
      });
    }
  });

  return entries;
}

// ── Main ─────────────────────────────────────────────────────────────────────
async function main() {
  const app = initializeApp(firebaseConfig);
  const db  = getFirestore(app);

  const entries = buildEntries();
  console.log(`📦 Preparing ${entries.length} service entries …`);

  let done = 0;
  for (const entry of entries) {
    await addDoc(collection(db, "serviceEntries"), entry);
    done++;
    if (done % 10 === 0) process.stdout.write(`   ✅ ${done}/${entries.length}\r`);
  }

  console.log(`\n🎉 Done! ${entries.length} entries added for customer ${CUSTOMER_ID}`);
  process.exit(0);
}

main().catch((err) => {
  console.error("❌ Error:", err.message);
  process.exit(1);
});

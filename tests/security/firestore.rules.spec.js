const fs = require("fs");
const path = require("path");
const {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment
} = require("@firebase/rules-unit-testing");
const { collection, doc, deleteDoc, getDoc, getDocs, setDoc } = require("firebase/firestore");

const { asProviderA, asProviderB, asCustomerA, asCustomerB, asAnon } = require("./helpers/authContexts");
const { seedBaseData } = require("./testData.seed");

let testEnv;

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: "demo-dailyserviceapp-security",
    firestore: {
      rules: fs.readFileSync(path.resolve(__dirname, "../../firestore.rules"), "utf8")
    }
  });
});

afterAll(async () => {
  await testEnv.cleanup();
});

beforeEach(async () => {
  await seedBaseData(testEnv);
});

afterEach(async () => {
  await testEnv.clearFirestore();
});

test("users: owner can read own profile", async () => {
  const db = asProviderA(testEnv);
  await assertSucceeds(getDoc(doc(db, "users/providerA")));
});

test("users: other user cannot read another profile", async () => {
  const db = asProviderB(testEnv);
  await assertFails(getDoc(doc(db, "users/providerA")));
});

test("users: owner can update own profile", async () => {
  const db = asProviderA(testEnv);
  await assertSucceeds(
    setDoc(doc(db, "users/providerA"), { name: "Provider A Updated" }, { merge: true })
  );
});

test("users: anonymous cannot read profile", async () => {
  const db = asAnon(testEnv);
  await assertFails(getDoc(doc(db, "users/providerA")));
});

test("providers: owner can create own provider doc", async () => {
  const db = asProviderB(testEnv);
  await assertSucceeds(
    setDoc(doc(db, "providers/providerB"), {
      userId: "providerB",
      name: "Provider B",
      businessName: "Provider B Services"
    })
  );
});

test("providers: owner can update own provider preserving userId", async () => {
  const db = asProviderA(testEnv);
  await assertSucceeds(
    setDoc(doc(db, "providers/providerA"), {
      userId: "providerA",
      businessName: "Updated Business"
    }, { merge: true })
  );
});

test("providers: owner cannot update own provider with mismatched userId", async () => {
  const db = asProviderA(testEnv);
  await assertFails(
    setDoc(doc(db, "providers/providerA"), {
      userId: "providerB"
    }, { merge: true })
  );
});

test("providers: non-owner cannot delete another provider", async () => {
  const db = asProviderB(testEnv);
  await assertFails(deleteDoc(doc(db, "providers/providerA")));
});

test("customers: owner provider can update own customer", async () => {
  const db = asProviderA(testEnv);
  await assertSucceeds(
    setDoc(doc(db, "customers/custA"), { providerId: "providerA", name: "Updated" }, { merge: true })
  );
});

test("customers: non-owner provider cannot update other provider customer", async () => {
  const db = asProviderB(testEnv);
  await assertFails(
    setDoc(doc(db, "customers/custA"), { providerId: "providerB", name: "Hijack" }, { merge: true })
  );
});

test("providers: signed-in customer can read provider profile", async () => {
  const db = asCustomerA(testEnv);
  await assertSucceeds(getDoc(doc(db, "providers/providerA")));
});

test("providers: anonymous cannot read provider profile", async () => {
  const db = asAnon(testEnv);
  await assertFails(getDoc(doc(db, "providers/providerA")));
});

test("customerLinks: customer can create own link document", async () => {
  const db = asCustomerA(testEnv);
  await assertSucceeds(
    setDoc(doc(db, "customerLinks/customerA"), {
      customerId: "customerA",
      providerId: "providerA"
    })
  );
});

test("customerLinks: unrelated customer cannot read another link", async () => {
  const db = asCustomerB(testEnv);
  await assertFails(getDoc(doc(db, "customerLinks/customerA")));
});

test("customerLinks: linked provider can read customer link", async () => {
  const db = asProviderA(testEnv);
  await assertSucceeds(getDoc(doc(db, "customerLinks/customerA")));
});

test("customerLinks: provider can create link with matching path customerId", async () => {
  const db = asProviderA(testEnv);
  await assertSucceeds(
    setDoc(doc(db, "customerLinks/customerX"), {
      customerId: "customerX",
      providerId: "providerA"
    })
  );
});

test("customerLinks: provider cannot create link with mismatched path customerId", async () => {
  const db = asProviderB(testEnv);
  await assertFails(
    setDoc(doc(db, "customerLinks/customerA"), {
      customerId: "anotherCustomer",
      providerId: "providerB"
    })
  );
});

test("customers: provider can create customer with own providerId", async () => {
  const db = asProviderA(testEnv);
  await assertSucceeds(
    setDoc(doc(db, "customers/custNewA"), {
      providerId: "providerA",
      name: "New Customer"
    })
  );
});

test("customers: non-owner provider cannot read existing customer", async () => {
  const db = asProviderB(testEnv);
  await assertFails(getDoc(doc(db, "customers/custA")));
});

test("customers: provider can get non-existing customer for pre-check", async () => {
  const db = asProviderA(testEnv);
  await assertSucceeds(getDoc(doc(db, "customers/custMissing")));
});

test("customers deliveries subcollection: owner provider can write", async () => {
  const db = asProviderA(testEnv);
  await assertSucceeds(
    setDoc(doc(db, "customers/custA/deliveries/20260318"), {
      delivered: true,
      dateKey: "20260318"
    })
  );
});

test("customers deliveries subcollection: non-owner provider cannot write", async () => {
  const db = asProviderB(testEnv);
  await assertFails(
    setDoc(doc(db, "customers/custA/deliveries/20260318"), {
      delivered: true,
      dateKey: "20260318"
    })
  );
});

test("serviceEntries: owner provider can create entry for owned customer", async () => {
  const db = asProviderA(testEnv);
  await assertSucceeds(
    setDoc(doc(db, "serviceEntries/entryOwned"), {
      providerId: "providerA",
      customerId: "custA",
      delivered: true
    })
  );
});

test("serviceEntries: provider cannot create entry for unowned customer", async () => {
  const db = asProviderA(testEnv);
  await assertFails(
    setDoc(doc(db, "serviceEntries/entryUnowned"), {
      providerId: "providerA",
      customerId: "custB",
      delivered: true
    })
  );
});

test("serviceEntries: anonymous list is denied", async () => {
  const db = asAnon(testEnv);
  await assertFails(getDocs(collection(db, "serviceEntries")));
});

test("serviceEntries: non-owner provider cannot delete entry", async () => {
  const db = asProviderB(testEnv);
  await assertFails(deleteDoc(doc(db, "serviceEntries/entryA")));
});

test("payments: provider can create payment for owned customer", async () => {
  const db = asProviderA(testEnv);
  await assertSucceeds(
    setDoc(doc(db, "payments/payOwned"), {
      providerId: "providerA",
      customerId: "custA",
      amount: 250
    })
  );
});

test("payments: provider cannot create payment for unowned customer", async () => {
  const db = asProviderA(testEnv);
  await assertFails(
    setDoc(doc(db, "payments/payUnowned"), {
      providerId: "providerA",
      customerId: "custB",
      amount: 250
    })
  );
});

test("payments: non-owner provider cannot read existing payment", async () => {
  const db = asProviderB(testEnv);
  await assertFails(getDoc(doc(db, "payments/payA")));
});

test("payments: anonymous cannot read payment", async () => {
  const db = asAnon(testEnv);
  await assertFails(getDoc(doc(db, "payments/payA")));
});

test("bills: provider can create bill for owned customer", async () => {
  const db = asProviderA(testEnv);
  await assertSucceeds(
    setDoc(doc(db, "bills/billOwned"), {
      providerId: "providerA",
      customerId: "custA",
      month: 2,
      year: 2026
    })
  );
});

test("bills: provider cannot create bill for unowned customer", async () => {
  const db = asProviderA(testEnv);
  await assertFails(
    setDoc(doc(db, "bills/billUnowned"), {
      providerId: "providerA",
      customerId: "custB",
      month: 2,
      year: 2026
    })
  );
});

test("bills: provider can update own bill preserving providerId", async () => {
  const db = asProviderA(testEnv);
  await assertSucceeds(
    setDoc(doc(db, "bills/billA"), {
      providerId: "providerA",
      month: 3
    }, { merge: true })
  );
});

test("bills: provider cannot update own bill changing providerId", async () => {
  const db = asProviderA(testEnv);
  await assertFails(
    setDoc(doc(db, "bills/billA"), {
      providerId: "providerB"
    }, { merge: true })
  );
});

test("bills: non-owner provider cannot read existing bill", async () => {
  const db = asProviderB(testEnv);
  await assertFails(getDoc(doc(db, "bills/billA")));
});

test("bills: anonymous list is denied", async () => {
  const db = asAnon(testEnv);
  await assertFails(getDocs(collection(db, "bills")));
});

test("supportTickets: customer can create ticket", async () => {
  const db = asCustomerA(testEnv);
  await assertSucceeds(
    setDoc(doc(db, "supportTickets/ticketA"), {
      customerId: "customerA",
      providerId: "providerA",
      message: "Need support"
    })
  );
});

test("supportTickets: provider can update ticket without changing identities", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await setDoc(doc(adminDb, "supportTickets/ticketSeed"), {
      customerId: "customerA",
      providerId: "providerA",
      status: "OPEN"
    });
  });

  const db = asProviderA(testEnv);
  await assertSucceeds(
    setDoc(doc(db, "supportTickets/ticketSeed"), {
      customerId: "customerA",
      providerId: "providerA",
      status: "CLOSED"
    }, { merge: true })
  );
});

test("supportTickets: delete is denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await setDoc(doc(adminDb, "supportTickets/ticketNoDelete"), {
      customerId: "customerA",
      providerId: "providerA",
      status: "OPEN"
    });
  });

  const db = asProviderA(testEnv);
  await assertFails(deleteDoc(doc(db, "supportTickets/ticketNoDelete")));
});

test("supportTickets: provider cannot update ticket and change customerId", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await setDoc(doc(adminDb, "supportTickets/ticketImmutable"), {
      customerId: "customerA",
      providerId: "providerA",
      status: "OPEN"
    });
  });

  const db = asProviderA(testEnv);
  await assertFails(
    setDoc(doc(db, "supportTickets/ticketImmutable"), {
      customerId: "customerB",
      providerId: "providerA",
      status: "CLOSED"
    }, { merge: true })
  );
});

test("legacy deliveries: provider can create own delivery", async () => {
  const db = asProviderA(testEnv);
  await assertSucceeds(
    setDoc(doc(db, "deliveries/delA"), {
      providerId: "providerA",
      delivered: true
    })
  );
});

test("legacy deliveries: provider cannot modify providerId on update", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await setDoc(doc(adminDb, "deliveries/delSeed"), {
      providerId: "providerA",
      delivered: true
    });
  });

  const db = asProviderA(testEnv);
  await assertFails(
    setDoc(doc(db, "deliveries/delSeed"), {
      providerId: "providerB",
      delivered: true
    }, { merge: true })
  );
});

test("legacy deliveries: non-owner provider cannot delete delivery", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await setDoc(doc(adminDb, "deliveries/delOwner"), {
      providerId: "providerA",
      delivered: true
    });
  });

  const db = asProviderB(testEnv);
  await assertFails(deleteDoc(doc(db, "deliveries/delOwner")));
});

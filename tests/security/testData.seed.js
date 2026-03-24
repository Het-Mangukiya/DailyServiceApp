const { doc, setDoc } = require("firebase/firestore");

async function seedBaseData(testEnv) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();

    await setDoc(doc(db, "users/providerA"), { role: "PROVIDER", email: "providerA@test.com" });
    await setDoc(doc(db, "users/providerB"), { role: "PROVIDER", email: "providerB@test.com" });

    await setDoc(doc(db, "providers/providerA"), {
      userId: "providerA",
      name: "Provider A",
      businessName: "Provider A Services"
    });

    await setDoc(doc(db, "customers/custA"), { providerId: "providerA", name: "Cust A" });
    await setDoc(doc(db, "customers/custB"), { providerId: "providerB", name: "Cust B" });

    await setDoc(doc(db, "customerLinks/customerA"), {
      customerId: "customerA",
      providerId: "providerA"
    });

    await setDoc(doc(db, "serviceEntries/entryA"), {
      providerId: "providerA",
      customerId: "custA",
      delivered: true
    });

    await setDoc(doc(db, "bills/billA"), {
      providerId: "providerA",
      customerId: "custA",
      month: 2,
      year: 2026
    });

    await setDoc(doc(db, "payments/payA"), {
      providerId: "providerA",
      customerId: "custA",
      amount: 100
    });
  });
}

module.exports = {
  seedBaseData
};

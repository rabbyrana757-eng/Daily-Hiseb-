const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 1000);
}

function validTxPayload(uid) {
  return {
    userId: uid,
    accountType: "personal",
    type: "expense",
    amount: 450.5,
    category: "খাবার",
    paymentMethod: "bKash",
    dateMillis: 1794614132000,
    dateKey: "13-11-2026",
    year: 2026,
    month: 11,
    day: 13,
    note: "দুপুরের খাবার",
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated user: cannot read or write transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx1").set(validTxPayload(ALICE_UID)));
});

test("Authenticated user: can create, list, update, delete own transaction", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txRef = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_01");
  await assertSucceeds(txRef.set(validTxPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertSucceeds(txRef.update({ amount: 500, updatedAt: pastTimestamp() }));
  await assertSucceeds(txRef.delete());
});

test("Cross-user isolation: Bob cannot read or write Alice's transactions, budgets, or savings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_01").set(validTxPayload(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_01").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_02").set(validTxPayload(BOB_UID)));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Shadow update & schema validation: reject ghost fields, negative amount, invalid paymentMethod", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const col = aliceDb.collection("users").doc(ALICE_UID).collection("transactions");

  // Ghost field
  await assertFails(col.doc("tx_ghost").set({ ...validTxPayload(ALICE_UID), isAdmin: true }));
  // Negative amount
  await assertFails(col.doc("tx_neg").set({ ...validTxPayload(ALICE_UID), amount: -50 }));
  // Invalid payment method
  await assertFails(col.doc("tx_bad_pm").set({ ...validTxPayload(ALICE_UID), paymentMethod: "Crypto" }));
  // Spoofed userId
  await assertFails(col.doc("tx_spoof").set({ ...validTxPayload(BOB_UID) }));
});

test("Budgets and SavingsGoals: owner CRUD succeeds, cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const budgetRef = aliceDb.collection("users").doc(ALICE_UID).collection("budgets").doc("b_1");
  await assertSucceeds(budgetRef.set({
    userId: ALICE_UID,
    accountType: "personal",
    year: 2026,
    month: 11,
    category: "খাবার",
    limitAmount: 10000,
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
  }));

  const goalRef = aliceDb.collection("users").doc(ALICE_UID).collection("savingsGoals").doc("g_1");
  await assertSucceeds(goalRef.set({
    userId: ALICE_UID,
    accountType: "business",
    name: "নতুন ল্যাপটপ",
    targetAmount: 80000,
    savedAmount: 25000,
    targetDateMillis: 1798675200000,
    targetDateKey: "31-12-2026",
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
  }));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("budgets").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("savingsGoals").get());
});

const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_staff_123";
const BOB_UID = "bob_staff_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: can create their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@warehouse.com",
      displayName: "Alice Staff",
      currentTrip: 1,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's staging batches", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("staging_batches").doc("batch_1").set({
      userId: ALICE_UID,
      tripNumber: 1,
      operationalDate: "2026-11-11",
      stagingDestination: "TANB",
      targetQty: 10,
      scannedCount: 10,
      bagseals: ["SEAL001", "SEAL002"],
      status: "bound",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("staging_batches").doc("batch_1").get()
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("staging_batches").doc("batch_1").delete()
  );
});

test("Alice can read and create her staging batch", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("staging_batches").doc("batch_alice").set({
      userId: ALICE_UID,
      tripNumber: 1,
      operationalDate: "2026-11-11",
      stagingDestination: "TBET",
      targetQty: 5,
      scannedCount: 0,
      status: "active",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("staging_batches").doc("batch_alice").get()
  );
});

test("Alice can create and read her bagseal record", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bagseals").doc("seal_1").set({
      userId: ALICE_UID,
      code: "SEAL-998822",
      stagingDestination: "KBYB",
      tripNumber: 1,
      operationalDate: "2026-11-11",
      tag: "REG",
      isBound: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can read app_config for version check", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("app_config").doc("version").set({
      latestVersionCode: 1,
      latestVersionName: "1.0.0",
      minSupportedVersionCode: 1,
      releaseNotes: "Initial release for Outbound Manual Reguler",
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("app_config").doc("version").get());
  // but cannot write
  await assertFails(aliceDb.collection("app_config").doc("version").set({
    latestVersionCode: 2,
    latestVersionName: "2.0.0",
    releaseNotes: "Hacked",
  }));
});

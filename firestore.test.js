const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";

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

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("messages").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("directives").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("directives").doc("dir_1").set({
      userId: ALICE_UID,
      title: "Rogue directive",
      priority: "HIGH",
      completed: false,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner: valid CloudMessage create, list, and reject extra id/ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("msg_1").set({
      userId: ALICE_UID,
      role: "user",
      text: "Halo J.A.R.V.I.S.",
      errorFlag: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").orderBy("createdAt", "asc").get()
  );

  // Reject @DocumentId "id" field or ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("messages").doc("msg_2").set({
      id: "msg_2",
      userId: ALICE_UID,
      role: "model",
      text: "Invalid id field",
      errorFlag: false,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Owner: valid CloudDirective create, partial update, immutable guard, and delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("directives").doc("dir_1").set({
      userId: ALICE_UID,
      title: "Periksa sistem tenaga",
      priority: "HIGH",
      completed: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("directives").doc("dir_1").update({
      completed: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject mutating immutable createdAt
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("directives").doc("dir_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("directives").doc("dir_2").set({
      userId: ALICE_UID,
      title: "Future timestamp attack",
      priority: "NORMAL",
      completed: false,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("directives").doc("dir_1").delete()
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's subcollections", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const now = new Date(Date.now() - 5000);
    await context.firestore().collection("users").doc(ALICE_UID).collection("directives").doc("alice_dir").set({
      userId: ALICE_UID,
      title: "Rahasia Alice",
      priority: "HIGH",
      completed: false,
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("directives").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("directives").doc("alice_dir").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("directives").doc("alice_dir").update({
      completed: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_survivor_01";
const BOB_UID = "bob_survivor_02";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, ts) {
  return {
    userId: uid,
    callsign: "AlphaStrike",
    goldCoins: 2500,
    diamonds: 300,
    level: 1,
    xp: 0,
    kills: 0,
    booyahs: 0,
    matchesPlayed: 0,
    luckPoints: 0,
    equippedHatId: "hat_red_bandana",
    equippedMaskId: "mask_none",
    equippedTopId: "top_classic_tank",
    equippedBottomId: "bottom_cargo_khaki",
    equippedShoesId: "shoes_combat_boots",
    equippedWeaponSkinId: "skin_default",
    createdAt: ts,
    updatedAt: ts,
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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any FireZone collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, ts)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("ownedItems").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("matchHistory").get());
});

test("Owner can create profile, update currencies/equipment, and cannot mutate immutable fields or inject ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created))
  );

  // Partial update mimicking custom diamond grant & equipment change
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      diamonds: 10300,
      equippedTopId: "top_criminal_red",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject negative diamonds
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      diamonds: -50,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject @DocumentId "id" injection on fresh create
  await assertFails(
    aliceDb.collection("users").doc("extra_profile").set({
      ...validProfilePayload("extra_profile", created),
      id: "extra_profile",
    })
  );
});

test("Cross-user access to profile, vault, matchHistory, and transactions is denied", async () => {
  const ts = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(BOB_UID).set(validProfilePayload(BOB_UID, ts));
    await db.collection("users").doc(BOB_UID).collection("ownedItems").doc("top_sakura").set({
      userId: BOB_UID,
      itemId: "top_sakura",
      category: "TOP",
      name: "Sakura Kimono",
      rarity: "LEGENDARY",
      acquiredVia: "DIAMOND_ROYALE",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).update({ diamonds: 999999, updatedAt: new Date(Date.now() - 1000) }));
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("ownedItems").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("matchHistory").get());
});

test("Owner can list and create ownedItems, matchHistory, and transactions matching Kotlin queries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("ownedItems").get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("ownedItems").doc("top_criminal_red").set({
      userId: ALICE_UID,
      itemId: "top_criminal_red",
      category: "TOP",
      name: "Red Criminal Suit",
      rarity: "LEGENDARY",
      acquiredVia: "DIAMOND_ROYALE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("matchHistory").doc("match_001").set({
      userId: ALICE_UID,
      mode: "BERMUDA_SOLO",
      placement: 1,
      kills: 8,
      damageDealt: 1840,
      survivalSeconds: 312,
      goldEarned: 450,
      xpEarned: 600,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_001").set({
      userId: ALICE_UID,
      txType: "CUSTOM_GRANT",
      currency: "DIAMONDS",
      amountDelta: 50000,
      note: "3s Hold Custom Diamond Vault Grant",
      createdAt: ts,
    })
  );

  // Match history and transactions are immutable
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("matchHistory").doc("match_001").update({
      kills: 20,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_001").delete()
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_future").set({
      userId: ALICE_UID,
      txType: "TOPUP_PACK",
      currency: "DIAMONDS",
      amountDelta: 100,
      note: "Tampered timestamp",
      createdAt: new Date(Date.now() + 60000),
    })
  );
});

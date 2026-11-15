const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const MANAGER_UID = "manager_baobab";
const MANAGER_EMAIL = "baobabstack@gmail.com";
const ALICE_UID = "resident_alice";
const ALICE_EMAIL = "alice.resident@example.com";
const BOB_UID = "resident_bob";
const BOB_EMAIL = "bob.resident@example.com";
const RANDOM_UID = "random_user";
const RANDOM_EMAIL = "stranger@example.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 2000);
}

function managerContext(verified = true) {
  return testEnv.authenticatedContext(MANAGER_UID, {
    email: MANAGER_EMAIL,
    email_verified: verified,
    name: "Estate Manager",
  });
}

function userContext(uid, email, verified = true) {
  return testEnv.authenticatedContext(uid, {
    email,
    email_verified: verified,
    name: email.split("@")[0],
  });
}

async function seedAdminDoc(uid = MANAGER_UID, email = MANAGER_EMAIL) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(uid).set({
      uid,
      email,
      displayName: "Estate Manager",
      role: "ADMIN",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    });
  });
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

test("Unauthenticated users cannot read or write any estate collection", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  const collections = [
    "admins",
    "cases",
    "properties",
    "tenants",
    "facilities",
    "bookings",
    "slot_locks",
    "maintenances",
    "contracts",
    "reminders",
    "vendors",
  ];
  for (const col of collections) {
    await assertFails(db.collection(col).get());
    await assertFails(db.collection(col).doc("doc_1").get());
  }
});

test("Admin bootstrap enforces verified baobabstack@gmail.com and blocks spoofing", async () => {
  const unverifiedMgrDb = managerContext(false).firestore();
  await assertFails(
    unverifiedMgrDb.collection("admins").doc(MANAGER_UID).set({
      uid: MANAGER_UID,
      email: MANAGER_EMAIL,
      displayName: "Unverified",
      role: "ADMIN",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  const strangerDb = userContext(RANDOM_UID, RANDOM_EMAIL, true).firestore();
  await assertFails(
    strangerDb.collection("admins").doc(RANDOM_UID).set({
      uid: RANDOM_UID,
      email: RANDOM_EMAIL,
      displayName: "Stranger",
      role: "ADMIN",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  const verifiedMgrDb = managerContext(true).firestore();
  await assertSucceeds(
    verifiedMgrDb.collection("admins").doc(MANAGER_UID).set({
      uid: MANAGER_UID,
      email: MANAGER_EMAIL,
      displayName: "Estate Manager",
      role: "ADMIN",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
});

test("Manager can CRUD estate records while verified Resident is strictly isolated", async () => {
  await seedAdminDoc();
  const mgrDb = managerContext(true).firestore();

  await assertSucceeds(
    mgrDb.collection("properties").doc("PROP_701").set({
      unitNumber: "Unit 701",
      pavilionWing: "East Pavilion",
      bedrooms: 3,
      bathrooms: 2.5,
      sqFt: 1850,
      floorLevel: "Level 7",
      viewType: "Park View",
      parkingSpots: ["B2-14"],
      amenities: ["Private Balcony"],
      monthlyRental: "$5,200 / mo",
      status: "OCCUPIED",
      tenantEmail: ALICE_EMAIL,
      assignedTenant: {
        name: "Alice Resident",
        email: ALICE_EMAIL,
        leaseStartDate: "Nov 01, 2026",
        leaseEndDate: "Nov 01, 2027",
        inviteCode: "TUTS-701",
        inviteLink: "tutsville://onboard?propertyId=PROP_701",
        isClaimed: false,
        assignedTimestamp: "Nov 01, 2026",
      },
      listedDate: "Oct 15, 2026",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    mgrDb.collection("tenants").doc("TEN_ALICE").set({
      firstName: "Alice",
      lastName: "Resident",
      fullName: "Alice Resident",
      nationalId: "ID-9981",
      address: "Unit 701",
      email: ALICE_EMAIL,
      phone: "+1 555 111 2222",
      emergencyContactName: "Bob",
      emergencyContactPhone: "+1 555 333 4444",
      assignedPropertyId: "PROP_701",
      assignedPropertyUnit: "Unit 701",
      leaseStartDate: "Nov 01, 2026",
      leaseEndDate: "Nov 01, 2027",
      inviteCode: "TUTS-701",
      isAppRegistered: false,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  // Unfiltered PII list queries must fail for non-admin residents
  const bobDb = userContext(BOB_UID, BOB_EMAIL, true).firestore();
  await assertFails(bobDb.collection("properties").get());
  await assertFails(bobDb.collection("tenants").get());
  await assertFails(bobDb.collection("contracts").get());
  await assertFails(bobDb.collection("cases").get());
  await assertFails(bobDb.collection("bookings").get());

  // Bob cannot get Alice's tenant or property record directly
  await assertFails(bobDb.collection("properties").doc("PROP_701").get());
  await assertFails(bobDb.collection("tenants").doc("TEN_ALICE").get());

  // Alice CAN query her own records with matching email filter and claim invite
  const aliceDb = userContext(ALICE_UID, ALICE_EMAIL, true).firestore();
  await assertSucceeds(
    aliceDb.collection("properties").where("tenantEmail", "==", ALICE_EMAIL).get()
  );
  await assertSucceeds(
    aliceDb.collection("tenants").where("email", "==", ALICE_EMAIL).get()
  );
  await assertSucceeds(
    aliceDb.collection("tenants").doc("TEN_ALICE").update({
      isAppRegistered: true,
      updatedAt: pastTimestamp(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("properties").doc("PROP_701").update({
      "assignedTenant.isClaimed": true,
      updatedAt: pastTimestamp(),
    })
  );

  // Alice cannot alter unit rent or assign herself another email
  await assertFails(
    aliceDb.collection("properties").doc("PROP_701").update({
      monthlyRental: "$1 / mo",
      updatedAt: pastTimestamp(),
    })
  );
});

test("Resident can sign PENDING_SIGNATURE TENANT_LEASE once and cannot tamper with terms", async () => {
  await seedAdminDoc();
  const mgrDb = managerContext(true).firestore();

  await assertSucceeds(
    mgrDb.collection("contracts").doc("CTR_LEASE_1").set({
      title: "Residential Lease — Unit 701",
      counterparty: "Alice Resident",
      vendorName: "Alice Resident",
      unit: "Unit 701",
      serviceCategory: "TENANT_LEASE",
      type: "TENANT_LEASE",
      annualValue: "$5,200 / mo",
      startDate: "Nov 01, 2026",
      expiryDate: "Nov 01, 2027",
      renewalDate: "Nov 01, 2027",
      daysRemaining: 365,
      renewalTerms: "12-month standard lease",
      contactPerson: "Estate Manager",
      contactEmail: MANAGER_EMAIL,
      isNotificationSent: false,
      tenantId: "TEN_ALICE",
      tenantEmail: ALICE_EMAIL,
      propertyId: "PROP_701",
      managerName: "Estate Manager",
      managerEmail: MANAGER_EMAIL,
      leaseStatus: "PENDING_SIGNATURE",
      isSignedByTenant: false,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  const bobDb = userContext(BOB_UID, BOB_EMAIL, true).firestore();
  await assertFails(bobDb.collection("contracts").doc("CTR_LEASE_1").get());
  await assertFails(
    bobDb.collection("contracts").doc("CTR_LEASE_1").update({
      leaseStatus: "ACTIVE",
      isSignedByTenant: true,
      signedByName: "Bob",
      signedByEmail: BOB_EMAIL,
      signedAt: "Nov 15, 2026",
      updatedAt: pastTimestamp(),
    })
  );

  const aliceDb = userContext(ALICE_UID, ALICE_EMAIL, true).firestore();
  await assertSucceeds(
    aliceDb.collection("contracts").where("tenantEmail", "==", ALICE_EMAIL).get()
  );

  // Alice cannot lower rent while signing
  await assertFails(
    aliceDb.collection("contracts").doc("CTR_LEASE_1").update({
      annualValue: "$100 / mo",
      leaseStatus: "ACTIVE",
      isSignedByTenant: true,
      signedByName: "Alice Resident",
      signedByEmail: ALICE_EMAIL,
      signedAt: "Nov 15, 2026",
      updatedAt: pastTimestamp(),
    })
  );

  // Alice signs cleanly
  await assertSucceeds(
    aliceDb.collection("contracts").doc("CTR_LEASE_1").update({
      leaseStatus: "ACTIVE",
      isSignedByTenant: true,
      signedByName: "Alice Resident",
      signedByEmail: ALICE_EMAIL,
      signedAt: "Nov 15, 2026 • 10:30 AM",
      updatedAt: pastTimestamp(),
    })
  );

  // Cannot re-update once ACTIVE
  await assertFails(
    aliceDb.collection("contracts").doc("CTR_LEASE_1").update({
      leaseStatus: "ACTIVE",
      isSignedByTenant: true,
      signedByName: "Alice Modified",
      signedByEmail: ALICE_EMAIL,
      signedAt: "Nov 16, 2026",
      updatedAt: pastTimestamp(),
    })
  );
});

test("Case lifecycle: Tenant creates & edits own OPEN case, Manager advances, Tenant reviews RESOLVED case + vendor", async () => {
  await seedAdminDoc();
  const mgrDb = managerContext(true).firestore();
  await assertSucceeds(
    mgrDb.collection("vendors").doc("VEND_1").set({
      name: "Apex HVAC",
      category: "HVAC",
      specialty: "HVAC",
      phone: "+1 555 888 9999",
      email: "dispatch@apex.example",
      leadTech: "Sam",
      rating: 4.0,
      reviewCount: 2,
      activeJobsCount: 0,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  const aliceDb = userContext(ALICE_UID, ALICE_EMAIL, true).firestore();
  await assertSucceeds(
    aliceDb.collection("cases").doc("CASE_1").set({
      userId: ALICE_UID,
      tenantEmail: ALICE_EMAIL,
      title: "AC Leak in Bedroom",
      description: "Water dripping from ceiling vent",
      category: "HVAC",
      priority: "HIGH",
      location: "Unit 701",
      reportedBy: "Alice Resident",
      reportedDate: "Nov 15, 2026",
      status: "OPEN",
      reviewTags: [],
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  // Alice can edit own OPEN case details
  await assertSucceeds(
    aliceDb.collection("cases").doc("CASE_1").update({
      title: "AC Leak in Master Bedroom",
      description: "Water dripping steadily from ceiling vent",
      location: "Unit 701",
      category: "HVAC",
      priority: "EMERGENCY",
      updatedAt: pastTimestamp(),
    })
  );

  // Alice CANNOT escalate status to RESOLVED herself
  await assertFails(
    aliceDb.collection("cases").doc("CASE_1").update({
      status: "RESOLVED",
      updatedAt: pastTimestamp(),
    })
  );

  // Bob cannot edit or delete Alice's case
  const bobDb = userContext(BOB_UID, BOB_EMAIL, true).firestore();
  await assertFails(bobDb.collection("cases").doc("CASE_1").delete());

  // Manager dispatches vendor & resolves
  await assertSucceeds(
    mgrDb.collection("cases").doc("CASE_1").update({
      assignedVendorId: "VEND_1",
      assignedVendorName: "Apex HVAC",
      status: "ASSIGNED",
      updatedAt: pastTimestamp(),
    })
  );

  // Once ASSIGNED, Alice cannot edit ticket fields
  await assertFails(
    aliceDb.collection("cases").doc("CASE_1").update({
      title: "Edited After Dispatch",
      description: "Should fail",
      location: "Unit 701",
      category: "HVAC",
      priority: "LOW",
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    mgrDb.collection("cases").doc("CASE_1").update({
      status: "RESOLVED",
      updatedAt: pastTimestamp(),
    })
  );

  // Alice reviews resolved case and increments vendor rating
  await assertSucceeds(
    aliceDb.collection("cases").doc("CASE_1").update({
      rating: 5,
      reviewTags: ["Fast", "Clean"],
      reviewComment: "Fixed quickly",
      status: "RESOLVED",
      updatedAt: pastTimestamp(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("vendors").doc("VEND_1").update({
      rating: 4.3,
      reviewCount: 3,
      updatedAt: pastTimestamp(),
    })
  );

  // Alice cannot decrease vendor reviewCount or overwrite vendor phone
  await assertFails(
    aliceDb.collection("vendors").doc("VEND_1").update({
      rating: 5.0,
      reviewCount: 1,
      updatedAt: pastTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection("vendors").doc("VEND_1").update({
      phone: "000",
      updatedAt: pastTimestamp(),
    })
  );
});

test("Atomic slot_locks prevent overlapping amenity bookings for the same facility/date/slot", async () => {
  await seedAdminDoc();
  const mgrDb = managerContext(true).firestore();
  await assertSucceeds(
    mgrDb.collection("facilities").doc("FAC_POOL").set({
      name: "Infinity Pool",
      tag: "Wellness",
      location: "Sky Deck",
      capacity: 15,
      description: "Heated lap pool",
      amenities: ["Towels"],
      allowedSlots: ["09:00 AM - 11:00 AM"],
      rules: "No glass",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  const aliceDb = userContext(ALICE_UID, ALICE_EMAIL, true).firestore();
  const lockId = "FAC_POOL__2026-11-20__09_00_AM_11_00_AM";

  // Orphan lock without booking must fail
  await assertFails(
    aliceDb.collection("slot_locks").doc("ORPHAN_LOCK").set({
      facilityId: "FAC_POOL",
      dateKey: "2026-11-20",
      timeSlot: "09:00 AM - 11:00 AM",
      bookingId: "NON_EXISTENT",
      bookedByUid: ALICE_UID,
      createdAt: pastTimestamp(),
    })
  );

  // Alice books atomically
  const aliceBatch = aliceDb.batch();
  aliceBatch.set(aliceDb.collection("bookings").doc("BK_ALICE"), {
    userId: ALICE_UID,
    facilityId: "FAC_POOL",
    facilityName: "Infinity Pool",
    residentName: "Alice Resident",
    unit: "Unit 701",
    date: "Nov 20, 2026",
    timeSlot: "09:00 AM - 11:00 AM",
    guestCount: 2,
    notes: "",
    confirmedTimestamp: "Nov 15, 2026 09:00 AM",
    createdAt: pastTimestamp(),
  });
  aliceBatch.set(aliceDb.collection("slot_locks").doc(lockId), {
    facilityId: "FAC_POOL",
    dateKey: "2026-11-20",
    timeSlot: "09:00 AM - 11:00 AM",
    bookingId: "BK_ALICE",
    bookedByUid: ALICE_UID,
    createdAt: pastTimestamp(),
  });
  await assertSucceeds(aliceBatch.commit());

  // Bob attempts double-booking the exact same slot lock -> MUST FAIL
  const bobDb = userContext(BOB_UID, BOB_EMAIL, true).firestore();
  const bobBatch = bobDb.batch();
  bobBatch.set(bobDb.collection("bookings").doc("BK_BOB"), {
    userId: BOB_UID,
    facilityId: "FAC_POOL",
    facilityName: "Infinity Pool",
    residentName: "Bob Resident",
    unit: "Unit 702",
    date: "Nov 20, 2026",
    timeSlot: "09:00 AM - 11:00 AM",
    guestCount: 2,
    notes: "",
    confirmedTimestamp: "Nov 15, 2026 09:05 AM",
    createdAt: pastTimestamp(),
  });
  bobBatch.set(bobDb.collection("slot_locks").doc(lockId), {
    facilityId: "FAC_POOL",
    dateKey: "2026-11-20",
    timeSlot: "09:00 AM - 11:00 AM",
    bookingId: "BK_BOB",
    bookedByUid: BOB_UID,
    createdAt: pastTimestamp(),
  });
  await assertFails(bobBatch.commit());

  // Bob cannot delete Alice's lock or booking
  await assertFails(bobDb.collection("slot_locks").doc(lockId).delete());
  await assertFails(bobDb.collection("bookings").doc("BK_ALICE").delete());

  // Alice can cancel her own booking & lock
  const cancelBatch = aliceDb.batch();
  cancelBatch.delete(aliceDb.collection("bookings").doc("BK_ALICE"));
  cancelBatch.delete(aliceDb.collection("slot_locks").doc(lockId));
  await assertSucceeds(cancelBatch.commit());
});

test("Notifications require provenance, isolate resident reads, restrict announcements/deletes to Manager, and isolate user_notice_states", async () => {
  await seedAdminDoc();
  const mgrDb = managerContext(true).firestore();
  const aliceDb = userContext(ALICE_UID, ALICE_EMAIL, true).firestore();
  const bobDb = userContext(BOB_UID, BOB_EMAIL, true).firestore();

  // Unauthenticated read of user_notice_states fails
  await assertFails(testEnv.unauthenticatedContext().firestore().collection("user_notice_states").doc(ALICE_UID).get());

  // Seed a legacy unattributed reminder via rules-disabled admin fixture
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("reminders").doc("LEGACY_SEED").set({
      userId: "legacy_seed",
      title: "Old Hardcoded Notice",
      message: "Legacy seeded message",
      targetAudience: "All Residents",
      urgency: "Notice",
      createdAt: pastTimestamp(),
    });
  });

  // Resident CANNOT create unattributed reminder or spoof MANUAL_ADVISORY / MANAGER role
  await assertFails(
    aliceDb.collection("reminders").doc("REM_UNATTR").set({
      userId: ALICE_UID,
      title: "Spoof",
      message: "No provenance",
      targetAudience: "All Residents",
      urgency: "Notice",
      createdAt: pastTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection("reminders").doc("REM_ADV_SPOOF").set({
      userId: ALICE_UID,
      title: "Fake Announcement",
      message: "Building closed",
      targetAudience: "All Residents & Tenants",
      urgency: "High Priority",
      recordType: "ANNOUNCEMENT",
      triggerEvent: "MANUAL_ADVISORY",
      createdByUid: ALICE_UID,
      createdByName: "Alice",
      createdByEmail: ALICE_EMAIL,
      createdByRole: "MANAGER",
      relatedEntityType: "ADVISORY",
      createdAt: pastTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection("reminders").doc("REM_VENDOR_SPOOF").set({
      userId: ALICE_UID,
      title: "Vendor Dispatched",
      message: "Unauthorized trigger",
      targetAudience: "Targeted Resident",
      urgency: "Notice",
      recordType: "ACTIVITY",
      triggerEvent: "CASE_VENDOR_ASSIGNED",
      createdByUid: ALICE_UID,
      createdByName: "Alice",
      createdByEmail: ALICE_EMAIL,
      createdByRole: "TENANT",
      targetEmail: ALICE_EMAIL,
      relatedEntityType: "CASE",
      createdAt: pastTimestamp(),
    })
  );

  // Alice CAN create an attributed CASE_CREATED activity notification
  await assertSucceeds(
    aliceDb.collection("reminders").doc("REM_ALICE_CASE").set({
      userId: ALICE_UID,
      title: "New Case: Sink Leak",
      message: "HIGH Plumbing ticket in Unit 701",
      targetAudience: "Estate Management",
      urgency: "High Priority",
      recordType: "ACTIVITY",
      triggerEvent: "CASE_CREATED",
      createdByUid: ALICE_UID,
      createdByName: "Alice Resident",
      createdByEmail: ALICE_EMAIL,
      createdByRole: "TENANT",
      targetEmail: ALICE_EMAIL,
      targetUnit: "Unit 701",
      relatedEntityType: "CASE",
      relatedEntityId: "CASE_1",
      createdAtEpochMs: Date.now(),
      createdAt: pastTimestamp(),
    })
  );

  // Bob CANNOT read Alice's personal case notification or list unfiltered reminders
  await assertFails(bobDb.collection("reminders").doc("REM_ALICE_CASE").get());
  await assertFails(bobDb.collection("reminders").get());

  // Manager CAN publish an estate-wide ANNOUNCEMENT, read all, and wipe the legacy doc
  await assertSucceeds(
    mgrDb.collection("reminders").doc("REM_MGR_ANN").set({
      userId: MANAGER_UID,
      title: "Pool Deck Resurfacing",
      message: "Pool closed Friday 8 AM - 4 PM.",
      targetAudience: "All Residents & Tenants",
      urgency: "Advisory",
      isBroadcast: true,
      recordType: "ANNOUNCEMENT",
      triggerEvent: "MANUAL_ADVISORY",
      createdByUid: MANAGER_UID,
      createdByName: "Estate Manager",
      createdByEmail: MANAGER_EMAIL,
      createdByRole: "MANAGER",
      relatedEntityType: "ADVISORY",
      createdAtEpochMs: Date.now(),
      createdAt: pastTimestamp(),
    })
  );
  await assertSucceeds(mgrDb.collection("reminders").get());
  await assertSucceeds(bobDb.collection("reminders").doc("REM_MGR_ANN").get());

  // Resident CANNOT delete reminders; Manager CAN wipe legacy and active reminders
  await assertFails(aliceDb.collection("reminders").doc("REM_MGR_ANN").delete());
  await assertSucceeds(mgrDb.collection("reminders").doc("LEGACY_SEED").delete());

  // Per-user user_notice_states is strictly owner-only
  await assertSucceeds(
    aliceDb.collection("user_notice_states").doc(ALICE_UID).set({
      uid: ALICE_UID,
      readNoticeIds: ["REM_MGR_ANN"],
      updatedAt: pastTimestamp(),
    })
  );
  await assertFails(bobDb.collection("user_notice_states").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("user_notice_states").doc(ALICE_UID).set({
      uid: ALICE_UID,
      readNoticeIds: [],
      updatedAt: pastTimestamp(),
    })
  );
});


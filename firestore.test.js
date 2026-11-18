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
const COMGR_UID = "comgr_elena";
const COMGR_EMAIL = "elena.manager@example.com";
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
    "manager_invites",
    "estate_config",
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

test("Admin bootstrap enforces verified baobabstack@gmail.com, protects bootstrap admin, and enables co-manager invite claim", async () => {
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

  // Bootstrap manager invites a co-manager by lowercase email ID
  await assertSucceeds(
    verifiedMgrDb.collection("manager_invites").doc(COMGR_EMAIL).set({
      email: COMGR_EMAIL,
      displayName: "Elena Co-Manager",
      invitedByEmail: MANAGER_EMAIL,
      invitedDate: "Nov 18, 2026",
      status: "PENDING",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  // Stranger cannot read or claim Elena's invite
  await assertFails(strangerDb.collection("manager_invites").doc(COMGR_EMAIL).get());
  await assertFails(
    strangerDb.collection("manager_invites").doc(COMGR_EMAIL).update({
      status: "CLAIMED",
      updatedAt: pastTimestamp(),
    })
  );

  // Elena (verified email matching invite ID) reads invite, creates her admin doc, and claims invite
  const elenaDb = userContext(COMGR_UID, COMGR_EMAIL, true).firestore();
  await assertSucceeds(elenaDb.collection("manager_invites").doc(COMGR_EMAIL).get());
  await assertSucceeds(
    elenaDb.collection("admins").doc(COMGR_UID).set({
      uid: COMGR_UID,
      email: COMGR_EMAIL,
      displayName: "Elena Co-Manager",
      role: "ADMIN",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
  await assertSucceeds(
    elenaDb.collection("manager_invites").doc(COMGR_EMAIL).update({
      status: "CLAIMED",
      updatedAt: pastTimestamp(),
    })
  );

  // Co-manager cannot delete the bootstrap manager's admin record
  await assertFails(elenaDb.collection("admins").doc(MANAGER_UID).delete());
});

test("estate_config/options allows signed-in reads and admin-only writes", async () => {
  await seedAdminDoc();
  const mgrDb = managerContext(true).firestore();
  const aliceDb = userContext(ALICE_UID, ALICE_EMAIL, true).firestore();

  // Resident cannot create/update config options
  await assertFails(
    aliceDb.collection("estate_config").doc("options").set({
      floorElevations: ["Penthouse"],
      contractorDisciplines: ["Plumbing"],
      updatedAt: pastTimestamp(),
    })
  );

  // Manager creates config options
  await assertSucceeds(
    mgrDb.collection("estate_config").doc("options").set({
      floorElevations: ["1 Story", "2 Story", "3 Story"],
      contractorDisciplines: ["Plumbing & Drains", "Electrical & Power"],
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  // Partial/merge update preserving createdAt succeeds
  await assertSucceeds(
    mgrDb.collection("estate_config").doc("options").update({
      floorElevations: ["1 Story", "2 Story", "3 Story", "4 Story"],
      contractorDisciplines: ["Plumbing & Drains", "Electrical & Power", "Roofing"],
      updatedAt: pastTimestamp(),
    })
  );

  // Signed-in resident can read dropdown options
  await assertSucceeds(aliceDb.collection("estate_config").doc("options").get());
  // Resident still cannot update config
  await assertFails(
    aliceDb.collection("estate_config").doc("options").update({
      floorElevations: ["Hack"],
      updatedAt: pastTimestamp(),
    })
  );
});

test("Manager can CRUD estate records (sqMeters, discipline, calendar event scope) while Resident is isolated", async () => {
  await seedAdminDoc();
  const mgrDb = managerContext(true).firestore();

  await assertSucceeds(
    mgrDb.collection("properties").doc("PROP_701").set({
      unitNumber: "Unit 701",
      pavilionWing: "",
      bedrooms: 3,
      bathrooms: 2.5,
      sqFt: 175,
      sqMeters: 175,
      floorLevel: "2 Story",
      viewType: "Park View",
      parkingSpots: [],
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

  await assertSucceeds(
    mgrDb.collection("maintenances").doc("MAINT_1").set({
      userId: MANAGER_UID,
      title: "Estate Fire Drill",
      discipline: "FIRE_LIFE",
      eventCategory: "INSPECTION",
      scheduledDate: "Nov 25, 2026",
      scheduledTime: "10:00 AM",
      contractorName: "",
      targetArea: "Unit 701",
      targetScope: "SPECIFIC_UNITS",
      targetUnits: ["Unit 701"],
      intervalMonths: 0,
      status: "SCHEDULED",
      notes: "Mandatory annual inspection",
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

  // Alice cannot alter unit rent or schedule estate events
  await assertFails(
    aliceDb.collection("properties").doc("PROP_701").update({
      monthlyRental: "$1 / mo",
      updatedAt: pastTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection("maintenances").doc("MAINT_ROGUE").set({
      userId: ALICE_UID,
      title: "Rogue Event",
      discipline: "HVAC",
      scheduledDate: "Nov 25, 2026",
      contractorName: "",
      status: "SCHEDULED",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
});

test("Contracts support active leases, SAF attachments, archive toggles, and block tenant modifications", async () => {
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
      leaseStatus: "ACTIVE",
      isSignedByTenant: true,
      isArchived: false,
      signedContractDoc: {
        fileName: "lease_unit_701_signed.pdf",
        contentUri: "content://com.android.providers.downloads.documents/document/42",
        mimeType: "application/pdf",
        attachedAt: "Nov 18, 2026",
      },
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  // Manager can attach Code of Conduct and archive contract
  await assertSucceeds(
    mgrDb.collection("contracts").doc("CTR_LEASE_1").update({
      codeOfConductDoc: {
        fileName: "estate_code_of_conduct.pdf",
        contentUri: "content://com.android.providers.downloads.documents/document/43",
        mimeType: "application/pdf",
        attachedAt: "Nov 18, 2026",
      },
      isArchived: true,
      updatedAt: pastTimestamp(),
    })
  );

  // Bob cannot read or mutate Alice's lease
  const bobDb = userContext(BOB_UID, BOB_EMAIL, true).firestore();
  await assertFails(bobDb.collection("contracts").doc("CTR_LEASE_1").get());

  // Alice CAN read her own lease, CANNOT alter lease or archive status
  const aliceDb = userContext(ALICE_UID, ALICE_EMAIL, true).firestore();
  await assertSucceeds(
    aliceDb.collection("contracts").where("tenantEmail", "==", ALICE_EMAIL).get()
  );
  await assertFails(
    aliceDb.collection("contracts").doc("CTR_LEASE_1").update({
      annualValue: "$100 / mo",
      updatedAt: pastTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection("contracts").doc("CTR_LEASE_1").update({
      isArchived: false,
      updatedAt: pastTimestamp(),
    })
  );
});

test("Case lifecycle: preferredDate, resident OPEN edit, manager immutable description + chronological notes, contractor discipline", async () => {
  await seedAdminDoc();
  const mgrDb = managerContext(true).firestore();
  await assertSucceeds(
    mgrDb.collection("vendors").doc("VEND_1").set({
      name: "Apex HVAC",
      category: "HVAC",
      specialty: "HVAC",
      discipline: "HVAC & Ventilation",
      phone: "+1 555 888 9999",
      email: "dispatch@apex.example",
      leadTech: "Sam",
      rating: 0,
      reviewCount: 0,
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
      preferredDate: "Nov 20, 2026",
      status: "OPEN",
      notes: [],
      statusHistory: [
        {
          status: "OPEN",
          note: "Case reported",
          updatedBy: "Alice Resident",
          timestamp: "Nov 15, 2026 • 09:00 AM",
          epochMs: Date.now(),
        },
      ],
      createdAtEpochMs: Date.now(),
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  // Alice can edit own OPEN case details including preferredDate
  await assertSucceeds(
    aliceDb.collection("cases").doc("CASE_1").update({
      title: "AC Leak in Master Bedroom",
      description: "Water dripping steadily from ceiling vent",
      location: "Unit 701",
      category: "HVAC",
      priority: "EMERGENCY",
      preferredDate: "Nov 22, 2026",
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

  // Bob cannot delete Alice's case
  const bobDb = userContext(BOB_UID, BOB_EMAIL, true).firestore();
  await assertFails(bobDb.collection("cases").doc("CASE_1").delete());

  // Manager CANNOT overwrite resident's original complaint description
  await assertFails(
    mgrDb.collection("cases").doc("CASE_1").update({
      description: "Manager altered resident description",
      updatedAt: pastTimestamp(),
    })
  );

  // Manager CAN append chronological notes and dispatch contractor
  await assertSucceeds(
    mgrDb.collection("cases").doc("CASE_1").update({
      notes: [
        {
          id: "NOTE-1",
          authorName: "Estate Manager",
          authorEmail: MANAGER_EMAIL,
          authorRole: "MANAGER",
          text: "Spoke with HVAC lead, arriving tomorrow morning.",
          createdAt: "Nov 16, 2026 • 10:00 AM",
          createdAtEpochMs: Date.now(),
        },
      ],
      statusHistory: [
        {
          status: "OPEN",
          note: "Case reported",
          updatedBy: "Alice Resident",
          timestamp: "Nov 15, 2026 • 09:00 AM",
          epochMs: Date.now() - 1000,
        },
        {
          status: "OPEN",
          note: "Note added: Spoke with HVAC lead, arriving tomorrow morning.",
          updatedBy: "Estate Manager",
          timestamp: "Nov 16, 2026 • 10:00 AM",
          epochMs: Date.now(),
        },
      ],
      updatedAt: pastTimestamp(),
    })
  );

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
});

test("Amenity approval flow & hourly slot_locks prevent overlapping approved bookings", async () => {
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
      allowedSlots: ["09:00 AM - 11:00 AM", "10:00 AM - 12:00 PM"],
      rules: "No glass",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  const aliceDb = userContext(ALICE_UID, ALICE_EMAIL, true).firestore();
  const bobDb = userContext(BOB_UID, BOB_EMAIL, true).firestore();

  // Orphan lock without booking must fail
  await assertFails(
    aliceDb.collection("slot_locks").doc("ORPHAN_LOCK").set({
      facilityId: "FAC_POOL",
      dateKey: "2026-11-20",
      timeSlot: "09:00 AM - 10:00 AM",
      bookingId: "NON_EXISTENT",
      bookedByUid: ALICE_UID,
      createdAt: pastTimestamp(),
    })
  );

  // Resident CANNOT self-approve a booking on create
  await assertFails(
    aliceDb.collection("bookings").doc("BK_ALICE_BYPASS").set({
      userId: ALICE_UID,
      facilityId: "FAC_POOL",
      facilityName: "Infinity Pool",
      residentName: "Alice Resident",
      unit: "Unit 701",
      date: "Nov 20, 2026",
      timeSlot: "09:00 AM - 11:00 AM",
      startHour: 9,
      durationHours: 2,
      guestCount: 2,
      approvalStatus: "APPROVED",
      createdAt: pastTimestamp(),
    })
  );

  // Alice submits PENDING booking request (no locks yet until Manager approves)
  await assertSucceeds(
    aliceDb.collection("bookings").doc("BK_ALICE").set({
      userId: ALICE_UID,
      facilityId: "FAC_POOL",
      facilityName: "Infinity Pool",
      residentName: "Alice Resident",
      unit: "Unit 701",
      date: "Nov 20, 2026",
      timeSlot: "09:00 AM - 11:00 AM",
      startTime: "09:00 AM",
      endTime: "11:00 AM",
      startHour: 9,
      durationHours: 2,
      guestCount: 2,
      notes: "Family swim",
      confirmedTimestamp: "Nov 15, 2026 09:00 AM",
      approvalStatus: "PENDING",
      createdAt: pastTimestamp(),
    })
  );

  // Alice cannot approve her own booking
  await assertFails(
    aliceDb.collection("bookings").doc("BK_ALICE").update({
      approvalStatus: "APPROVED",
    })
  );

  // Manager approves Alice's booking and locks H09 + H10 atomically
  const lockH09 = "FAC_POOL__2026-11-20__H09";
  const lockH10 = "FAC_POOL__2026-11-20__H10";
  const lockH11 = "FAC_POOL__2026-11-20__H11";

  const approveAliceBatch = mgrDb.batch();
  approveAliceBatch.update(mgrDb.collection("bookings").doc("BK_ALICE"), {
    approvalStatus: "APPROVED",
  });
  approveAliceBatch.set(mgrDb.collection("slot_locks").doc(lockH09), {
    facilityId: "FAC_POOL",
    dateKey: "2026-11-20",
    timeSlot: "09:00 AM - 10:00 AM",
    bookingId: "BK_ALICE",
    bookedByUid: ALICE_UID,
    createdAt: pastTimestamp(),
  });
  approveAliceBatch.set(mgrDb.collection("slot_locks").doc(lockH10), {
    facilityId: "FAC_POOL",
    dateKey: "2026-11-20",
    timeSlot: "10:00 AM - 11:00 AM",
    bookingId: "BK_ALICE",
    bookedByUid: ALICE_UID,
    createdAt: pastTimestamp(),
  });
  await assertSucceeds(approveAliceBatch.commit());

  // Bob submits an overlapping request (10:00 AM - 12:00 PM) and Manager cannot approve lockH10
  await assertSucceeds(
    bobDb.collection("bookings").doc("BK_BOB").set({
      userId: BOB_UID,
      facilityId: "FAC_POOL",
      facilityName: "Infinity Pool",
      residentName: "Bob Resident",
      unit: "Unit 702",
      date: "Nov 20, 2026",
      timeSlot: "10:00 AM - 12:00 PM",
      startHour: 10,
      durationHours: 2,
      guestCount: 2,
      notes: "",
      confirmedTimestamp: "Nov 15, 2026 09:05 AM",
      approvalStatus: "PENDING",
      createdAt: pastTimestamp(),
    })
  );

  const approveBobBatch = mgrDb.batch();
  approveBobBatch.update(mgrDb.collection("bookings").doc("BK_BOB"), {
    approvalStatus: "APPROVED",
  });
  approveBobBatch.set(mgrDb.collection("slot_locks").doc(lockH10), {
    facilityId: "FAC_POOL",
    dateKey: "2026-11-20",
    timeSlot: "10:00 AM - 11:00 AM",
    bookingId: "BK_BOB",
    bookedByUid: BOB_UID,
    createdAt: pastTimestamp(),
  });
  approveBobBatch.set(mgrDb.collection("slot_locks").doc(lockH11), {
    facilityId: "FAC_POOL",
    dateKey: "2026-11-20",
    timeSlot: "11:00 AM - 12:00 PM",
    bookingId: "BK_BOB",
    bookedByUid: BOB_UID,
    createdAt: pastTimestamp(),
  });
  await assertFails(approveBobBatch.commit());

  // Bob cannot delete Alice's locks or booking
  await assertFails(bobDb.collection("slot_locks").doc(lockH09).delete());
  await assertFails(bobDb.collection("bookings").doc("BK_ALICE").delete());

  // Manager declines Bob's overlapping request cleanly
  await assertSucceeds(
    mgrDb.collection("bookings").doc("BK_BOB").update({
      approvalStatus: "DECLINED",
      notes: "Overlaps approved reservation",
    })
  );

  // Alice cancels her approved booking & hourly locks
  const cancelBatch = aliceDb.batch();
  cancelBatch.delete(aliceDb.collection("bookings").doc("BK_ALICE"));
  cancelBatch.delete(aliceDb.collection("slot_locks").doc(lockH09));
  cancelBatch.delete(aliceDb.collection("slot_locks").doc(lockH10));
  await assertSucceeds(cancelBatch.commit());
});

test("Notifications require provenance, support new triggers (CASE_NOTE_ADDED, AMENITY_APPROVED, EVENT_SCHEDULED), and isolate user_notice_states", async () => {
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

  // Resident CANNOT spoof MANAGER announcement or CASE_NOTE_ADDED
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
    aliceDb.collection("reminders").doc("REM_NOTE_SPOOF").set({
      userId: ALICE_UID,
      title: "Case Note Added",
      message: "Unauthorized trigger",
      targetAudience: "Targeted Resident",
      urgency: "Notice",
      recordType: "ACTIVITY",
      triggerEvent: "CASE_NOTE_ADDED",
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

  // Manager CAN emit CASE_NOTE_ADDED, AMENITY_APPROVED, EVENT_SCHEDULED (multi-target), and ANNOUNCEMENT
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
  await assertSucceeds(
    mgrDb.collection("reminders").doc("REM_MGR_EVT").set({
      userId: MANAGER_UID,
      title: "Inspection / Audit: Boiler Check",
      message: "Scheduled for Nov 25, 2026 (10:00 AM)",
      targetAudience: "Unit 701",
      urgency: "Advisory",
      isBroadcast: false,
      recordType: "ACTIVITY",
      triggerEvent: "EVENT_SCHEDULED",
      createdByUid: MANAGER_UID,
      createdByName: "Estate Manager",
      createdByEmail: MANAGER_EMAIL,
      createdByRole: "MANAGER",
      targetEmail: ALICE_EMAIL,
      targetEmails: [ALICE_EMAIL],
      targetUnit: "Unit 701",
      targetUnits: ["Unit 701"],
      relatedEntityType: "EVENT",
      relatedEntityId: "MAINT_1",
      createdAtEpochMs: Date.now(),
      createdAt: pastTimestamp(),
    })
  );

  await assertSucceeds(mgrDb.collection("reminders").get());
  await assertSucceeds(bobDb.collection("reminders").doc("REM_MGR_ANN").get());
  await assertSucceeds(aliceDb.collection("reminders").doc("REM_MGR_EVT").get());
  await assertFails(bobDb.collection("reminders").doc("REM_MGR_EVT").get());

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
});

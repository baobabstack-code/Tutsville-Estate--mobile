const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085
    }
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("unauthenticated user cannot read cases", async () => {
  const unauthed = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthed.collection("cases").get());
});

test("authenticated user can create a valid case", async () => {
  const authed = testEnv.authenticatedContext("user_123").firestore();
  await assertSucceeds(
    authed.collection("cases").doc("case_1").set({
      userId: "user_123",
      title: "HVAC Seepage in Plant",
      description: "Pressure dropped below safe threshold",
      category: "HVAC",
      priority: "HIGH",
      location: "Plant Room A",
      status: "OPEN",
      createdAt: new Date()
    })
  );
});

test("user cannot create case for another user", async () => {
  const authed = testEnv.authenticatedContext("user_123").firestore();
  await assertFails(
    authed.collection("cases").doc("case_2").set({
      userId: "user_attacker",
      title: "Malicious Case",
      description: "Trying to spoof userId",
      category: "PLUMBING",
      priority: "LOW",
      location: "Wing B",
      status: "OPEN",
      createdAt: new Date()
    })
  );
});

test("authenticated user can book a facility", async () => {
  const authed = testEnv.authenticatedContext("user_123").firestore();
  await assertSucceeds(
    authed.collection("bookings").doc("booking_1").set({
      userId: "user_123",
      facilityId: "FAC-01",
      facilityName: "The Orangery Clubhouse",
      date: "Nov 15, 2026",
      timeSlot: "10:00 AM - 02:00 PM",
      guestCount: 8,
      createdAt: new Date()
    })
  );
});

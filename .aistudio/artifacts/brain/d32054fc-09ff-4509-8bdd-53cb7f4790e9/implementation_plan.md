# Cloud Firestore Full-Module Persistence & Real-Time Sync Plan

A comprehensive architecture to link every estate management module—Properties, Tenants, Amenity Bookings, Maintenances, Vendor Contracts, Reminders & Notices, Contractors, and Work Order Cases—to Google Cloud Firestore with real-time live synchronization, zero-trust security rules, and initial bootstrap seeding.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed with the user during Phase 1:
> - **Module Scope**: All modules (`properties`, `tenants`, `facilities`, `bookings`, `maintenances`, `contracts`, `reminders`, `vendors`, `cases`) will be fully stored and persisted in Cloud Firestore.
> - **Initial Data Bootstrapping**: If Firestore collections are empty on first launch, the app will automatically seed default estate residences, tenant profiles, amenities, vendor SLAs, and scheduled tasks to Firestore so the console immediately displays full data tables.
> - **Synchronization Strategy**: Real-time snapshot listeners with local offline persistence backing Jetpack Compose `StateFlow` reactive pipelines.

---

## 1. Overview & Core Concept

Currently, the database console (`ai-studio-android-tutsvill-d32054fc-09ff-4509-8bdd-53cb7f4790e9`) only displays the `cases` collection because other entities were kept purely in in-memory state. 

This update links **every module** in Tutsville Estate directly to the designated Cloud Firestore database:
1. **Properties (`/properties`)**: Villa and penthouse units, architectural specifications, parking bays, amenities, and rental valuations.
2. **Tenants (`/tenants`)**: Registered resident profiles, mobile numbers, emergency contacts, invite codes, and unit bindings.
3. **Amenities & Venues (`/facilities`)**: Estate pavilions, swimming pools, tennis courts, and private cinema specifications.
4. **Passes & Reservations (`/bookings`)**: Resident amenity passes and reservations.
5. **Maintenances (`/maintenances`)**: Mechanical, electrical, life safety, and structural servicing calendar.
6. **Vendor Contracts & SLAs (`/contracts`)**: Contractor service agreements, annual values, and renewal countdowns.
7. **Notices & Advisories (`/reminders`)**: Management broadcast alerts and scheduled servicing notices.
8. **Approved Vendors (`/vendors`)**: Vetted contractors, specialties, response times, and active dispatch counts.
9. **Work Orders (`/cases`)**: Incident tracking, contractor dispatch, and 5-star resolution reviews.

---

## 2. Architecture & Data Flow

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
│   Dashboard  │  Facilities & Tenants  │  Calendar  │  Cases  │  Notices  │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ Observe StateFlows
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                    EstateViewModel (State Management)                  │
│       properties, tenants, facilities, bookings, maintenances, ...     │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ Delegate actions & collect flows
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│               EstateRepository (Firestore Data Layer)                  │
│   • Named Database: ai-studio-android-tutsvill-d32054fc-09ff-4509-8bdd  │
│   • Snapshot Listeners: Live real-time bidirectional updates          │
│   • Auto-Seeder: Batched seed on launch if collections are empty      │
│   • Fail-safe Writes: Authenticated writes with ServerTimestamps       │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ Read/Write with Security Rules
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                         Cloud Firestore Backend                        │
│  /properties  /tenants  /facilities  /bookings  /maintenances         │
│  /contracts   /reminders  /vendors   /cases                           │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Data Schema & Security Rules

### 3.1 `firebase-blueprint.json` Expansion
Define explicit schemas for all 9 entities:
- `Property`: `unitNumber`, `pavilionWing`, `bedrooms`, `bathrooms`, `sqFt`, `floorLevel`, `viewType`, `parkingSpots`, `amenities`, `monthlyRental`, `status`, `assignedTenant`, `listedDate`, `createdAt`, `updatedAt`
- `Tenant`: `fullName`, `email`, `phone`, `emergencyContactName`, `emergencyContactPhone`, `assignedPropertyId`, `assignedPropertyUnit`, `leaseStartDate`, `leaseEndDate`, `inviteCode`, `isAppRegistered`, `createdAt`, `updatedAt`
- `Facility`: `name`, `location`, `capacity`, `allowedSlots`, `amenities`, `createdAt`
- `Booking`: `facilityId`, `facilityName`, `residentName`, `unit`, `date`, `timeSlot`, `guestCount`, `notes`, `userId`, `createdAt`
- `Maintenance`: `title`, `discipline`, `scheduledDate`, `scheduledTime`, `contractorName`, `targetArea`, `intervalMonths`, `status`, `notes`, `userId`, `createdAt`
- `Contract`: `vendorName`, `serviceCategory`, `annualValue`, `renewalDate`, `status`, `daysRemaining`, `autoRenewal`, `isNotificationSent`, `createdAt`
- `Reminder`: `title`, `message`, `targetAudience`, `urgency`, `timestamp`, `isBroadcast`, `userId`, `createdAt`
- `Vendor`: `name`, `category`, `phone`, `rating`, `activeJobsCount`, `responseTimeMinutes`, `createdAt`
- `Case`: `title`, `description`, `category`, `priority`, `location`, `reportedBy`, `reportedDate`, `assignedVendorId`, `assignedVendorName`, `status`, `rating`, `reviewComment`, `userId`, `createdAt`, `updatedAt`

### 3.2 Hardened `firestore.rules`
- Require `request.auth != null` for all read and write operations.
- Enforce schema validation functions (`isValidProperty`, `isValidTenant`, `isValidFacility`, `isValidBooking`, `isValidMaintenance`, `isValidContract`, `isValidReminder`, `isValidVendor`, `isValidCase`).
- Allow managers and authenticated residents appropriate read access while protecting sensitive data.

---

## 4. Implementation Steps

1. **Update `firebase-blueprint.json`**: Add full schema definitions for all 9 collections.
2. **Update `firestore.rules`**: Author comprehensive security rules covering all collections.
3. **Author Rule Validation Tests**:
   - `firestore.test.js`: Rapid unit tests validating read/write constraints across all 9 collections.
   - Run tests against the local Firestore emulator to ensure 100% compliance.
4. **Deploy Security Rules**:
   - Call `deploy_firebase` to deploy the hardened rules directly to the user's project backend.
5. **Update `EstateRepository.kt`**:
   - Resolve `FirebaseFirestore.getInstance(databaseId)`.
   - Implement live `addSnapshotListener` streams for all collections so any change made via the app or Firebase Console reflects immediately in the UI.
   - Implement `checkAndSeedInitialData(db)`: If collections are empty on first run, populate initial rich sample records directly into Firestore via `db.runBatch()`.
   - Update all mutation methods (`createTenant`, `assignTenantToPropertyById`, `unassignTenant`, `listProperty`, `bookFacility`, `cancelBooking`, `scheduleMaintenance`, `toggleMaintenanceStatus`, `sendReminder`, `renewContract`, `reportCase`, `assignVendorToCase`, `submitCaseReview`) to write directly to Cloud Firestore.
6. **Verify Build & Run Tests**:
   - Run `compile_applet` to confirm zero compilation errors.
   - Run `gradle :app:testDebugUnitTest` to verify all unit and repository integration tests pass.

<div align="center">

# Tutsville Estate

**Residential Estate Operations, Lease Management & Resident Concierge Platform for Android**

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.09.00-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material%203-6750A4?style=flat-square&logo=materialdesign&logoColor=white)](https://m3.material.io)
[![Firebase](https://img.shields.io/badge/Backend-Firebase%20Auth%20%26%20Firestore-FFCA28?style=flat-square&logo=firebase&logoColor=black)](https://firebase.google.com)
[![SDK](https://img.shields.io/badge/API-24%2B%20(Target%2036)-0F9D58?style=flat-square)](https://developer.android.com/tools/releases/platforms)

</div>

**Tutsville Estate** is a native Kotlin and Jetpack Compose Android application that unifies residential property operations and tenant concierge services behind a single role-gated codebase. Estate Managers oversee units, tenants, leases, contractors, work orders, amenity approvals, estate events, and audit trails in real time, while verified Residents report maintenance cases, request hourly amenity passes, view scoped calendar notices, and access their lease documents.

---

## Table of Contents

- [1. Product Overview & Role Portals](#1-product-overview--role-portals)
- [2. Complete Feature Catalog](#2-complete-feature-catalog)
- [3. System Architecture & Workflow Diagrams](#3-system-architecture--workflow-diagrams)
- [4. Role-Based Access & Data Model (14 Collections)](#4-role-based-access--data-model-14-collections)
- [5. Firebase Setup & Security Rules Guide](#5-firebase-setup--security-rules-guide)
- [6. Local Build, Emulators & Unit Tests](#6-local-build-emulators--unit-tests)
- [7. Repository Layout & Tech Stack](#7-repository-layout--tech-stack)

---

## 1. Product Overview & Role Portals

Tutsville Estate authenticates users exclusively with verified **Google Sign-In** (via Android Jetpack `CredentialManager` and Firebase Authentication) and routes each signed-in identity into one of three states:

| Role State | Resolution Criteria | Experience & Access |
| :--- | :--- | :--- |
| **Estate Manager (`MANAGER`)** | UID in `/admins`, verified bootstrap email, or verified email claiming a `/manager_invites/{email}` | Full operational command center across 4 bottom tabs, 8 navigation drawer modules, and Co-Manager / Landlord invite management |
| **Verified Resident (`TENANT`)** | Verified email matched to an onboarded record in `/tenants` | Concierge workspace across 4 bottom tabs (`Dashboard`, `Cases`, `Amenities`, `Calendar`) and 5 drawer modules scoped strictly to the resident's unit and notices |
| **Pending Verification (`UNREGISTERED`)** | Authenticated Google account not yet invited as Manager or onboarded as Resident | Locked `PendingAccessScreen` displaying active email, **Check Access Again** re-verification, and **Sign Out** (zero operational data fetched) |

### Navigation Structure by Portal

| Navigation Surface | Estate Manager Portal | Resident Concierge Portal |
| :--- | :--- | :--- |
| **Bottom Navigation Bar** | `Dashboard` · `Cases` · `Units` · `Calendar` | `Dashboard` · `Cases` · `Amenities` · `Calendar` |
| **Top Header Bar** | Drawer toggle · Estate title & portal badge · Notification bell with live unread badge | Drawer toggle · Estate title & concierge badge · Notification bell with live unread badge |
| **Navigation Drawer** | `1. Tenants` · `2. Units` · `3. Amenities` · `4. Cases` · `5. Contracts` · `6. Contractors` · `7. Calendar` · `8. Reports & Audits` · `Manage Managers / Landlords` | `1. Amenities` · `2. Cases` · `3. Advisories` · `4. Calendar` · `5. Contracts` |

---

## 2. Complete Feature Catalog

| Module | Primary Capabilities | Available To |
| :--- | :--- | :--- |
| **Authentication & Onboarding** | Credential Manager Google Sign-In, silent returning-user sign-in, `tutsville://onboard` & `tutsville://invite` deep-link email prefill, `PendingAccessScreen` gate, auto-claim of `/manager_invites` and tenant `isAppRegistered` | Manager · Resident · Unregistered |
| **Estate Dashboard** | Live KPI summary cards, maintenance & lease expiry rundown alerts, broadcast advisory composer (Manager), personalized residence card & quick actions (Resident) | Manager · Resident |
| **Cases & Work Orders** | **Report Case** FAB (`Summary`, `Description`, `Category`, `Priority SLA`, `Location`, optional `Preferred Date`), resident edit/delete while `OPEN`, contractor dispatch, immutable resident description, timestamped manager notes, status progression | Manager · Resident |
| **Units & Floor Elevations** | Residential unit inventory (`sqMeters`, bedrooms, bathrooms, floor elevation, amenities, monthly rent, status), inline **Manage Options** editor for `floorElevations`, one-tap tenant assignment & vacate lifecycle | Manager Only |
| **Tenants & Invite Links** | Standalone resident directory CRUD with emergency contacts, unit assignment (auto-generates `ACTIVE` `TENANT_LEASE` contract), shareable `tutsville://onboard` deep links and personal invite codes | Manager Only |
| **Contracts & Document Archive** | `TENANT_LEASE`, `VENDOR_SLA`, and `EQUIPMENT_WARRANTY` tracking, dynamic expiry urgency badges (`<=14d`, `<=30d`, `<=60d`), one-tap renewal, **Archive / Unarchive** filters, SAF local PDF/document attachment & viewing (`Signed Contract` & `Code of Conduct`) | Manager (All) · Resident (Own Lease) |
| **Amenities & Atomic Slot Locks** | Facility catalog with capacity, location, amenities, rules, and Manager-configured hourly `allowedSlots`; Resident `PENDING` booking requests; Manager `APPROVED` / `DECLINED` workflow backed by atomic hourly `/slot_locks` | Manager · Resident |
| **Unified Estate Calendar** | Monthly grid & agenda aggregating scheduled events (`MAINTENANCE`, `GENERAL`, `INSPECTION`, `COMMUNITY`), amenity bookings, contract expirations, and cases with `preferredDate`; supports `ALL_RESIDENTS` vs `SPECIFIC_UNITS` targeting | Manager · Resident (Scoped) |
| **Approved Contractors** | Contractor directory with service trade, inline **Manage Options** editor for `contractorDisciplines`, lead technician, phone/email, and live `activeJobsCount` incremented on dispatch | Manager Only |
| **Advisories & Alert Notifications** | Broadcast announcements, automated lifecycle activity alerts across 12 `NoticeTriggerEvent` types, per-user synced read state (`/user_notice_states/{uid}`), and Android system notifications (`POST_NOTIFICATIONS`) | Manager · Resident (Scoped) |
| **Reports & Audits** | Month/year selector & **Rolling 30 Days** filter, status KPI breakdown, and unified chronological audit timeline combining case status movements (`statusHistory`) and manager notes | Manager Only |
| **Manager & Landlord Governance** | Invite co-managers or landlords by email, view `PENDING` vs `CLAIMED` status, revoke manager access while protecting the bootstrap admin account | Manager Only |

<details>
<summary><strong>Deep Dive: Detailed Walkthrough of Every Feature Module</strong></summary>

#### 1. Authentication, Deep Links & Pending Access Gate
- Uses **Android Credential Manager** (`GetGoogleIdOption` for silent auto-sign-in and `GetSignInWithGoogleOption` for explicit chooser) bridged to Firebase Authentication (`GoogleAuthProvider`).
- Supports `tutsville://onboard?propertyId=...&email=...` and `tutsville://invite?...` intents declared in `AndroidManifest.xml`, pre-filling the invited email banner on `AuthScreen`.
- Accounts not found in `/admins`, `/manager_invites`, or `/tenants` are held on `PendingAccessScreen` without attaching operational collection listeners.

#### 2. Cases & Maintenance Incident Lifecycle
- **Case Categories**: `Plumbing & Drains`, `Electrical & Power`, `Heating & Air Conditioning`, `Doors, Windows & Locks`, `Kitchen & Laundry`, `Common Areas & Grounds`, `Access & Surveillance`.
- **Priority SLAs**: `Low (72h SLA)`, `Standard (24h SLA)`, `Urgent (8h SLA)`, `Critical (2h SLA)`.
- **Tenant Controls**: Residents submit cases via the **Report Case** FAB (`Summary` headline, detailed description, category, priority, unit/location, and optional `Preferred Date`). While a case remains in `OPEN` (`Reported`) status, the resident can edit or delete it.
- **Manager Triage & Audit Integrity**: Managers cannot overwrite a resident's original `description` after submission (enforced both in UI and `firestore.rules`). Instead, Managers append timestamped **Case Notes**, dispatch an approved contractor (`OPEN` → `ASSIGNED`), and advance status through `IN_PROGRESS` and `RESOLVED`, automatically recording `statusHistory` entries and firing attributed notifications.

#### 3. Residential Units, Tenants & Auto-Lease Provisioning
- Units store interior dimensions in **square meters (`sqMeters`)**, bed/bath counts, view type, monthly valuation, amenities, and occupancy status (`AVAILABLE`, `OCCUPIED`, `UNDER_MAINTENANCE`).
- **Inline Config Management**: Inside the unit creation form, Managers can open **Manage Options** to add/remove custom **Floor Elevations** (default: `1 Story`, `2 Story`, `3 Story`) persisted in `/estate_config/options`.
- **Assignment & Onboarding**: Assigning a tenant to an available unit atomically marks the unit `OCCUPIED`, links the tenant profile, issues an onboarding invite code + `tutsville://onboard` deep link, and auto-creates an `ACTIVE` `TENANT_LEASE` contract. Vacating a unit terminates active lease records and resets occupancy to `AVAILABLE`.

#### 4. Contracts, Expiry Rundowns & SAF Document Attachments
- Categorizes agreements into `TENANT_LEASE`, `VENDOR_SLA`, and `EQUIPMENT_WARRANTY`.
- Automatically computes `effectiveDaysRemaining` from `expiryDate` and assigns visual urgency levels: `URGENT_CRITICAL` (`<= 14 days`), `EXPIRING_30_DAYS` (`<= 30 days`), `EXPIRING_60_DAYS` (`<= 60 days`), or `HEALTHY`.
- Supports **One-Tap Renewal** (extends expiry by 1 year and clears alert flags), **Archive / Unarchive** toggle with a dedicated Archived filter pill, and **Storage Access Framework (`ActivityResultContracts.OpenDocument`)** document picking for `Signed Contract` and `Code of Conduct` files with persistable URI read permissions (`ContractDocumentMeta`).

#### 5. Shared Amenities & Overlapping Slot Prevention
- Managers create facilities with guest capacity, physical location, amenity tags, house rules, and **Allowed Time Slots**. Facilities with no configured slots cannot be booked by residents until a Manager adds slots.
- Residents pick a date, hourly start time, duration, and guest count inside `BookFacilitySheet`, which checks real-time slot availability before creating a `PENDING` booking request.
- When a Manager taps **Approve**, the repository executes a Firestore transaction that validates and writes non-PII hourly lock documents (`/slot_locks/{facilityId}__{dateKey}__H{HH}`) for every hour in the requested window. Declining or cancelling a booking deletes any associated slot locks so the hours immediately reopen for other residents.

#### 6. Unified Estate Calendar
- Aggregates four record streams onto a monthly calendar grid and selected-day agenda:
  1. **Maintenance & Estate Events** (`MAINTENANCE`, `GENERAL`, `INSPECTION`, `COMMUNITY`) with `ALL_RESIDENTS` or `SPECIFIC_UNITS` targeting.
  2. **Amenity Bookings** (resident-scoped to own bookings vs. estate-wide for Managers).
  3. **Contract Expirations** (showing active agreements nearing renewal/expiry dates).
  4. **Cases with Preferred Date** (surfaces target completion dates requested during case reporting).

#### 7. Contractors, Notifications & Reports
- **Contractors (`Contractors` Drawer Module)**: Supports inline **Manage Options** editing of estate-wide `contractorDisciplines` in `/estate_config/options`, lead technician details, and live open work-order counts (`activeJobsCount`).
- **Notifications & Android Alerts**: Combines Manager broadcast advisories (`ANNOUNCEMENT`) with automated lifecycle activity alerts (`ACTIVITY`). Unread notices authored by other users trigger local high-importance Android notifications on channel `tutsville_estate_alerts` (`POST_NOTIFICATIONS` requested on Android 13+). Read state persists per user in `/user_notice_states/{uid}`.
- **Reports & Audits**: Lets Managers inspect any calendar month or switch to a **Rolling 30 Days** audit window, displaying KPI counts and an interleaved chronological log of every `CaseMovementEntry` and `CaseNote`.

</details>

---

## 3. System Architecture & Workflow Diagrams

### Runtime Component Architecture

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│                     ANDROID UI (Jetpack Compose + Material 3)                │
│  ┌──────────────┐   ┌───────────────────────┐   ┌─────────────────────────┐  │
│  │  AuthScreen  │   │  PendingAccessScreen  │   │  Main Scaffold + Drawer │  │
│  │ (Cred. Mgr)  │   │  (Unregistered Gate)  │   │  Manager (8) / Tenant(5)│  │
│  └──────┬───────┘   └───────────┬───────────┘   └────────────┬────────────┘  │
└─────────┼───────────────────────┼────────────────────────────┼───────────────┘
          │                       │                            │ StateFlow & Events
          ▼                       ▼                            ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                  STATE & BUSINESS LOGIC (EstateViewModel)                    │
│  • Resolves UserRole (MANAGER / TENANT / UNREGISTERED)                       │
│  • Aggregates Unified Calendar, Audit Timelines, & Role-Scoped Contracts     │
│  • Coordinates SAF Contract Document URIs & Android Notification Alerts      │
└──────────────────────────────────────┬───────────────────────────────────────┘
                                       │ Coroutines & Snapshots
                                       ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│               DATA & CONCURRENCY LAYER (EstateRepository)                    │
│  • Real-Time Firestore Snapshot Listeners across 14 Collections              │
│  • Atomic Firestore Transactions for Unit/Tenant Sync & Hourly Slot Locks    │
│  • Local-only Fallback Mode for Unit & Robolectric Testing                   │
└──────────────┬───────────────────────────────────────────────┬───────────────┘
               │                                               │
               ▼                                               ▼
┌─────────────────────────────────────┐       ┌────────────────────────────────┐
│   Firebase Auth & Cloud Firestore   │       │     Local Android OS APIs      │
│  • Zero-Trust firestore.rules       │       │  • NotificationManagerCompat   │
│  • Named DB: ai-studio-android-...  │       │  • SAF OpenDocument URI Picker │
└─────────────────────────────────────┘       └────────────────────────────────┘
```

### Work Order (Case) & Atomic Amenity Booking Workflows

```text
CASE LIFECYCLE (Immutable Complaint Integrity)
────────────────────────────────────────────────────────────────────────────────
  [Resident: Report Case FAB]
         │ (title, description, category, priority, location, preferredDate)
         ▼
    ┌─────────┐  Resident may Edit / Delete
    │  OPEN   ├──────────────────────────────┐
    └────┬────┘                              ▼ (Optional Resident Edit/Delete)
         │ Manager dispatches Vendor
         ▼
    ┌──────────┐   Manager appends CaseNotes (original description locked!)
    │ ASSIGNED ├─────────────────────────────┐
    └────┬─────┘                             ▼
         │ Vendor begins work         ┌─────────────┐        ┌──────────────┐
         └───────────────────────────▶│ IN_PROGRESS ├───────▶│   RESOLVED   │
                                      └─────────────┘        └──────────────┘
         * Every transition writes statusHistory[] + Reminder + Local Alert *


ATOMIC AMENITY RESERVATION & HOURLY SLOT LOCKS
────────────────────────────────────────────────────────────────────────────────
  [Facility: allowedSlots configured] ──(empty)──▶ Booking Blocked (Manager must add slots)
         │
         │ Resident submits date + startHour + durationHours
         ▼
  ┌──────────────┐   No /slot_locks created yet (Request awaiting review)
  │   PENDING    │
  └──────┬───────┘
         │
         ├─── Manager APPROVES (Firestore Transaction)
         │         ├─ Checks all /slot_locks/{facilityId}__{dateKey}__H{HH}
         │         ├─ Writes Booking.approvalStatus = APPROVED
         │         └─ Creates non-PII /slot_locks docs for each booked hour
         │
         └─── Manager DECLINES or Resident CANCELS
                   ├─ Marks booking DECLINED or deletes booking
                   └─ Deletes any /slot_locks held for that bookingId
```

---

## 4. Role-Based Access & Data Model (14 Collections)

All estate operational records reside in Cloud Firestore under a named database instance and are strictly segmented by role and ownership:

| Collection Path | Purpose | Who Can Read | Who Can Write |
| :--- | :--- | :--- | :--- |
| `/admins/{adminUid}` | Authorized estate managers & landlords | Own doc or Admin (list: Admin) | Self-create if bootstrap/invited email; Admin update/delete (bootstrap email protected) |
| `/manager_invites/{inviteId}` | Pending & claimed manager email invites (keyed by lowercase email) | Admin or matching verified invitee email | Admin create/delete; Invitee may claim (`PENDING` → `CLAIMED`) |
| `/estate_config/{configId}` | Configurable `floorElevations` and `contractorDisciplines` lists | Any signed-in estate user | Admin create/update (delete disabled) |
| `/cases/{caseId}` | Maintenance cases, notes, & `statusHistory` audit trail | Admin or matching participant (`userId` / `tenantEmail`) | Resident creates `OPEN` & edits/deletes own `OPEN`; Admin updates preserving `description` |
| `/properties/{propertyId}` | Residential units, architecture specs, & occupancy state | Admin or assigned unit `tenantEmail` | Admin create/delete/update; Assigned resident can claim onboarding flag |
| `/tenants/{tenantId}` | Resident profiles, emergency contacts, & onboarding status | Admin or matching verified `email` | Admin create/delete/update; Resident can mark `isAppRegistered = true` |
| `/facilities/{facilityId}` | Shared estate amenities, rules, capacity, & `allowedSlots` | Any signed-in user (non-PII directory) | Admin only |
| `/bookings/{bookingId}` | Amenity reservation requests & approval status | Admin or booking owner (`userId`) | Signed-in user creates own `PENDING` booking; Admin approves/declines; Owner or Admin deletes |
| `/slot_locks/{lockId}` | Non-PII hourly reservation locks (`{facility}__{date}__H{HH}`) | Any signed-in user (collision checks) | Signed-in / Admin transaction create (`!exists` + booking `existsAfter`); Owner or Admin delete |
| `/maintenances/{maintenanceId}` | Scheduled estate calendar events (`ALL_RESIDENTS` / unit-targeted) | Any signed-in user | Admin only |
| `/contracts/{contractId}` | Tenant leases, vendor SLAs, warranties, archive flag, & doc URIs | Admin or matching `tenantEmail` | Admin only |
| `/reminders/{reminderId}` | Broadcast announcements & event-triggered activity alerts | Admin or resident matching audience/unit/email | Admin creates `MANAGER` notices; Residents create scoped `ACTIVITY` notices on own actions |
| `/user_notice_states/{userId}` | Synced array of `readNoticeIds` per Firebase Auth UID | Owner UID only (`list` disabled) | Owner UID only |
| `/vendors/{vendorId}` | Approved service contractors, disciplines, & `activeJobsCount` | Any signed-in user | Admin only |

<details>
<summary><strong>Deep Dive: Security Invariants & Anti-Tamper Rules</strong></summary>

1. **Default-Deny Catch-All**: `match /{document=**} { allow read, write: if false; }` locks down all unspecified paths.
2. **Verified Email Requirement**: Role elevation (`baobabstack@gmail.com` bootstrap, `/manager_invites` claims, `/tenants` self-registration, and email-scoped reads) strictly checks `request.auth.token.email_verified == true`.
3. **Immutable Resident Complaint Integrity**: On `/cases/{caseId}`, Admin updates must satisfy `incoming().description == existing().description`, guaranteeing original resident work-order descriptions cannot be altered during triage.
4. **Atomic Non-PII Slot Locks**: `/slot_locks/{lockId}` intentionally excludes resident PII (no names, emails, or unit numbers) so any authenticated resident client can inspect hourly availability while `/bookings/{bookingId}` stays private to the owner and Managers.
5. **Bootstrap Admin Protection**: `allow delete` on `/admins/{adminUid}` prevents accidental removal of the primary bootstrap manager document (`baobabstack@gmail.com`).

</details>

---

## 5. Firebase Setup & Security Rules Guide

### Configured Project Coordinates

| Configuration Artifact | Key / Value |
| :--- | :--- |
| **Application ID** | `com.aistudio.tutsville.kxpm` |
| **Android Namespace** | `com.example` |
| **Named Firestore Database ID** | `ai-studio-android-tutsvill-d32054fc-09ff-4509-8bdd-53cb7f4790e9` (in `firebase.json` and `firebase_applet_config.xml`) |
| **Bootstrap Manager Email** | `baobabstack@gmail.com` |
| **Local Emulator Ports** | Firestore: `127.0.0.1:8085` · Auth: `127.0.0.1:9099` (`firebase.json`) |

### Step-by-Step Setup for New Deployments

<details>
<summary><strong>Expand: Connecting Your Own Firebase Project, Google Sign-In & Security Rules</strong></summary>

1. **Firebase Project & Authentication**:
   - Create or select a Firebase project and enable **Google** under **Authentication → Sign-in method**.
   - Register the Android app with package name `com.aistudio.tutsville.kxpm` and your debug/release SHA-1 & SHA-256 keystore fingerprints.
   - Download `google-services.json` into `app/google-services.json` (ensure the Web Client ID in `oauth_client` matches Google Sign-In via Credential Manager).
2. **Named Cloud Firestore Database**:
   - Provision the Firestore database matching `firestore_database_id` in `app/src/main/res/values/firebase_applet_config.xml` and `firebase.json` (or update both files to match your target database ID).
3. **Deploy Firestore Security Rules**:
   ```bash
   firebase deploy --only firestore:rules
   ```
4. **Bootstrap Access**:
   - Sign in with the verified bootstrap Google account (`baobabstack@gmail.com`, or update `EstateRepository.kt` and `firestore.rules` before deploying for a custom organization).
   - Once signed in, open the navigation drawer → **Manage Managers / Landlords** to invite additional managers by email, and onboard residents via **Tenants** or **Units**.

</details>

> [!NOTE]
> **Platform Capabilities Scope**: Signed contract and code-of-conduct documents are attached using Android's zero-permission **Storage Access Framework (`OpenDocument`)** with persistable local URI metadata (`ContractDocumentMeta`) rather than Firebase Cloud Storage. Phone alerts are delivered directly in-app via Android's `TutsvilleNotificationHelper` (`POST_NOTIFICATIONS` channel `tutsville_estate_alerts`) rather than external Firebase Cloud Functions / FCM push triggers.

> [!IMPORTANT]
> **API Keys & Secrets**: `.env.example` documents `GEMINI_API_KEY` as commented-out because Tutsville Estate does not use Gemini/LLM APIs at runtime. Leave `GEMINI_API_KEY` commented out unless intentionally extending the app with AI features, and never commit `google-services.json`, `.env`, or release keystores to public remotes.

---

## 6. Local Build, Emulators & Unit Tests

> [!IMPORTANT]
> **Use System `gradle`**: This repository does not include a `./gradlew` wrapper script; run commands with `gradle` installed on your `PATH`.

### Quick Command Reference

| Task | Command | Notes |
| :--- | :--- | :--- |
| **Build Debug APK** | `gradle assembleDebug` | Compiles Kotlin/Compose sources and packages debug APK |
| **Run Android JVM & Robolectric Tests** | `gradle :app:testDebugUnitTest` | Runs unit tests + Robolectric suite (`ExampleUnitTest`, `ExampleRobolectricTest`, `EstateRepositoryRuleTest`) |
| **Start Firebase Emulators** | `firebase emulators:start --only firestore,auth` | Binds Firestore (`127.0.0.1:8085`) and Auth (`127.0.0.1:9099`) per `firebase.json` |
| **Run Firestore Security Rules Suite** | `node --test firestore.test.js` | Executes Node rule assertions using `@firebase/rules-unit-testing` against running emulators |

<details>
<summary><strong>Deep Dive: Prerequisites & Running Emulator-Backed Tests</strong></summary>

1. **Prerequisites**:
   - **JDK 11+** (configured in `app/build.gradle.kts` `compileOptions`)
   - **Android SDK** with `compileSdk` 36.1 (`minSdk = 24`, `targetSdk = 36`)
   - **Gradle CLI** (`gradle`)
   - **Node.js (v18+)** and **Firebase CLI** (`firebase`) with `@firebase/rules-unit-testing` available when running `firestore.test.js`
2. **All-in-One Emulator + Android Rule Test Workflow**:
   ```bash
   # Terminal 1: Launch local Firestore & Auth Emulators
   firebase emulators:start --only firestore,auth

   # Terminal 2: Run Node.js security rule tests + Android JVM/Robolectric tests
   node --test firestore.test.js
   gradle :app:testDebugUnitTest
   ```
   Or wrap Node rules testing in a single ephemeral emulator session:
   ```bash
   firebase emulators:exec --only firestore,auth "node --test firestore.test.js"
   ```
3. **Test Suites Included**:
   - **Kotlin Unit Tests (`ExampleUnitTest.kt`)**: Core JVM logic checks.
   - **Robolectric Behavioral Suite (`ExampleRobolectricTest.kt`)**: Validates empty repository initialization, case lifecycle + preferred dates + notes, inline configurable options, contract archiving & document metadata, and unified calendar aggregation.
   - **Emulator Integration Rule Suite (`EstateRepositoryRuleTest.kt`)**: Exercises live `EstateRepository` operations against the local Firestore & Auth emulators via `FirestoreEmulatorTestBase`.
   - **Security Rules Suite (`firestore.test.js`)**: Verifies default-deny behavior, bootstrap admin protection, co-manager invite claims, immutable resident case descriptions, atomic slot-lock collision guarantees, and resident read isolation across all 14 collections.

</details>

---

## 7. Repository Layout & Tech Stack

### Core Libraries (`gradle/libs.versions.toml`)

| Layer | Technology | Version |
| :--- | :--- | :--- |
| **Language & Build** | Kotlin · Android Gradle Plugin · Secrets Plugin · Google Services | Kotlin `2.2.10` · AGP `9.1.1` · `2.0.1` · `4.5.0` |
| **UI & Design System** | Jetpack Compose BOM · Material 3 · Material Icons Extended · Activity Compose | BOM `2024.09.00` · Activity `1.10.1` |
| **State & Concurrency** | AndroidX Lifecycle / ViewModel Compose · Kotlin Coroutines | Lifecycle `2.8.7` · Coroutines `1.10.2` |
| **Identity & Backend** | Firebase BoM (`firebase-auth`, `firebase-firestore`) · Credential Manager · Google ID | BoM `34.17.0` · Credentials `1.5.0` · `googleid 1.1.1` |
| **Testing** | JUnit 4 · Robolectric · `kotlinx-coroutines-test` · `@firebase/rules-unit-testing` | JUnit `4.13.2` · Robolectric `4.16.1` · Coroutines Test `1.10.2` |

<details>
<summary><strong>Deep Dive: Source Tree Overview</strong></summary>

```text
.
├── app/src/main/
│   ├── AndroidManifest.xml                   # POST_NOTIFICATIONS permission & tutsville:// deep links
│   ├── res/values/
│   │   ├── strings.xml                       # App display name ("Tutsville Estate")
│   │   └── firebase_applet_config.xml        # Named Firestore database ID
│   └── java/com/example/
│       ├── MainActivity.kt                   # Auth gate, role routing, bottom tabs, dialogs & alert observer
│       ├── ui/theme/                         # Tutsville Color, Typography, and Material 3 Theme tokens
│       └── tutsville/
│           ├── data/model/EstateModels.kt    # Domain entities, enums, SLA levels & date helpers
│           ├── data/repository/              # EstateRepository & Firestore mappers/transactions
│           ├── notifications/                # TutsvilleNotificationHelper (local Android alerts)
│           └── ui/
│               ├── auth/AuthScreen.kt        # Google Sign-In via Credential Manager
│               ├── components/               # SidebarDrawer, HeaderBar, Cards, Tables & Action Dialogs
│               ├── screens/                  # Dashboard, Cases, Units, Tenants, Amenities, Calendar, Contracts, Contractors, AuditReports
│               └── viewmodel/                # EstateViewModel StateFlows & UI actions
├── app/src/test/java/com/example/            # JUnit, Robolectric & FirestoreEmulatorTestBase suites
├── firebase-blueprint.json                   # JSON Schema specification for all 14 Firestore collections
├── firebase.json                             # Named Firestore rules target & local emulator ports
├── firestore.rules                           # Zero-trust schema-validated security rules
├── firestore.test.js                         # Node.js security rules test suite
└── gradle/libs.versions.toml                 # Centralized dependency & plugin catalog
```

</details>

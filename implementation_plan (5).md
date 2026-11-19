# Tutsville Estate Operations Update Plan

This plan incorporates every workflow improvement, terminology alignment, and feature request captured during the customer review call while preserving role-based security and offline-safe cloud synchronization.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions confirmed during Phase 1 govern this release and resolve platform boundaries transparently.

- **Confirmed Decision 1 — Full Scope Delivery**: All customer review changes will be implemented together in a cohesive update across Data Models, Security Rules, Repositories, and UI Modules.
- **Confirmed Decision 2 — Unified Terminology**: Tenant and Manager interfaces will standardize on **Cases**, rename the ticket headline input to **Summary** (keeping **Description** for details), and place a prominent **+ Report Case** Floating Action Button at the bottom-right of the Tenant Dashboard and Cases views.
- **Confirmed Decision 3 — Local Document Attachments & Android Alerts**: Contract attachments (**Signed Contract** and **Code of Conduct**) will use Android's zero-permission System Document Picker so no broad storage permissions are requested; estate advisories, case status transitions, and amenity approvals will trigger local Android system notifications (`NotificationCompat` with runtime `POST_NOTIFICATIONS` permission on Android 13+) while the app listener is active. *(Background push when the app is closed requires external Firebase Cloud Functions and an FCM push server outside this sandbox.)*
- **Confirmed Decision 4 — Inline Configurable Dropdowns**: Managers will add or remove **Floor Elevation** options (Units) and **Contractor Discipline / Trade** options (Contractors) via inline **Manage Options** controls inside those creation/editing dialogs, persisted to shared estate configuration.
- **Architectural Security Choice — Individual Co-Manager & Landlord Accounts**: Rather than sharing one generic complex login—which breaks audit accountability—existing Managers can invite additional Co-Managers or Landlords by email through an allowlisted Manager Invite workflow, granting full Manager portal access upon verified Google Sign-In while preserving per-user attribution.

---

## 1. Overview & Core Concept

- **What It Does**: Evolves the Tutsville Estate Manager & Resident platform to mirror real-world estate operations: structured case reporting with immutable tenant descriptions and appended manager notes, exclusive manager-approved amenity time blocks, scoped estate calendar events, auditable lease records with attached documents, monthly case movement audit reports, and multi-manager access.
- **Target Audience / Persona**:
  - **Estate Managers & Landlords**: Overseeing unit inventory, lease contracts, contractor dispatch, amenity approvals, estate-wide or unit-targeted announcements, and monthly case movement audits.
  - **Verified Residents (Tenants)**: Reporting unit cases effortlessly, tracking manager progress notes, requesting clubhouse/amenity time windows, checking scoped estate schedules, and viewing their signed lease and Code of Conduct.
- **Key Value**: Eliminates terminology mismatches (`Vendor` vs `Contractor`, `Title` vs `Summary`), prevents accidental data erasure via archiving and note-appending, enforces exclusive conflict-free clubhouse scheduling through Manager approval, and provides transparent 30-day audit reporting.

---

## 2. User Experience & Visual Design

### Visual Identity & Theme

- **Aesthetic Direction**: Warm architectural concierge command center—structured, high-contrast operational cards set against warm linen surfaces with deep evergreen headers and burnished gold priority accents.
- **Color Palette & Mood** *(from centralized theme tokens)*:
  - `background` (`#F6F3EB` Warm Linen) and `surface` (`#FCF9F1`) provide calm editorial readability.
  - `primary` (`#183B2B` Deep Evergreen Forest) anchors top bars, primary CTAs, FABs, and modal headers.
  - `secondary` (`#C29047` Burnished Gold) highlights active passes, pending approvals, and subtle card borders.
  - Status badges use dedicated high-contrast pairs: Green (`#EAF2EC` / `#1B4D2E`) for Approved/Resolved/Active, Amber (`#FDF6EB` / `#8C5E1B`) for Pending Approval/Open, and Crimson (`#FAF0F0` / `#8A2E2E`) for Urgent/Overdue/Critical.
- **Typography & Hierarchy**: Display and screen titles use bold Serif typography for editorial estate authority paired with crisp sans-serif labels scaled in `sp` for form inputs, filter chips, and timeline entries.
- **Component Styling & Layout**: Edge-to-edge layout (`enableEdgeToEdge()`) respecting system status and navigation bar insets; minimum `48.dp` touch targets on all buttons, chips, and icon controls; `RoundedCornerShape` cards (`12.dp`–`20.dp`) with `1.dp` hairline borders; standard M3 `NavigationBar` for top-level destinations paired with a full `ModalNavigationDrawer`.

### Key User Flows (Step-by-Step)

1. **Tenant Case Reporting & Tracking**:
   - From the Tenant Dashboard or Cases tab, the resident taps the bottom-right **+ Report Case** FAB (`Report Case`).
   - Their assigned unit is auto-filled and locked read-only so cases always map accurately to their residence.
   - Resident enters **Summary** (concise issue title), **Description** (detailed symptoms), picks Priority & Category, and optionally picks a **Preferred Completion Date** (`preferredDate`) that appears on the shared Calendar.
   - Submitting creates the `OPEN` case and emits an estate notification. Service ratings/reviews after resolution are removed completely from Tenant Dashboard, Cases, and Navigation.
2. **Manager Case Notes & Monthly Audit Reports**:
   - Managers triage cases, dispatch to an onboarded **Contractor**, and advance workflow status (`Reported` -> `Assigned` -> `In Progress` -> `Resolved`).
   - Selecting **Add Note** appends a timestamped, attributed note entry without altering or overwriting the original Tenant description; both Manager and Tenant see the chronological note history on the card.
   - Selecting **Reports & Audits** from the Manager drawer opens Monthly Audit & Case Movement Reports: selectable monthly periods plus rolling 30-day totals (Reported, Active/Outstanding, Resolved, Pending Approvals) and an audited chronological timeline showing every case's status progression, assignee, and appended notes.
3. **Contractors & Inline Trade Options**:
   - All `Vendor` labels across tabs, drawer entries, headers, cards, filters, and dispatch modals switch to **Contractor** / **Contractors**.
   - Inside Contractor creation/editing dialogs, Managers pick from configured disciplines or tap **Manage Options** inline to add/remove custom disciplines (e.g., *Glazing*, *Generator Servicing*, *Gate Automation*).
4. **Units Inventory (Square Meters & Floor Elevation Dropdown)**:
   - Unit cards, forms, and the Tenant Dashboard Hero Card remove **Pavilion Wing** and **Parking Bays**.
   - Unit area is labeled **Size (sq m)** (`sq m`) everywhere; Unit forms collect Unit Number, Description, Size (sq m), Bedrooms, Bathrooms, Floor Elevation dropdown (defaulting to `1 Story`, `2 Story`, `3 Story`), and Monthly Rental Valuation.
   - Tapping **Manage Options** inline inside the Unit dialog allows Managers to customize available Floor Elevation tiers.
5. **Amenities: Start Time, 1–3h Duration & Manager Approval**:
   - `Usage Rules` label becomes **Notes** on Amenity forms and cards.
   - Booking an amenity lets a resident pick a **Date**, a **Start Time** dropdown (e.g., 08:00 AM–08:00 PM hourly slots), and a **Duration** dropdown (`1 Hour`, `2 Hours`, `3 Hours`), dynamically disabling any combination whose hourly block overlaps an already-approved reservation.
   - Resident submits the request as **Pending Approval** (`PENDING`), which notifies Managers immediately without locking out calendar dates or other residents until approved.
   - Managers review pending booking cards (`Approve` / `Decline`): approving commits atomic hourly slot locks, updates the reservation to `APPROVED`, places it on the Calendar, and pushes an estate alert; declining marks it `DECLINED` without blocking slots and notifies the resident.
6. **Calendar: Generic Events & Target Unit Scoping**:
   - Managers tap **Schedule** -> **Schedule Event** (replacing "Schedule Maintenance"), choose an Event **Category** (`Maintenance`, `General`, `Inspection`, `Community`), select a **Date**, and choose a **Time Window** from a standardized dropdown.
   - **Assign Contractor** appears conditionally *only* when Event Category is `Maintenance`.
   - **Target Units / Scope** defaults to `All Residents` via a dropdown, with an expandable checklist to target specific Units (e.g., Unit 1–Unit 11) and/or `Clubhouse`. Events and their notifications only appear to Managers and targeted residents.
7. **Contracts: Lease Defaults, Live Days Left, Document Attachments & Archiving**:
   - **Add Contract** defaults Type to **Tenant Lease**; selecting a Tenant auto-fills their assigned Unit and defaults **Contract Title** (e.g., `<Tenant Name> — <Unit>`) while remaining editable independently of the occupant/tenant name.
   - Choosing Start Date or Expiry Date recalculates **Days Left** immediately inside the form.
   - **Signed Contract** and **Code of Conduct** document slots open Android's system document picker (`OpenDocument`), persisting document display name, MIME type, and access URI without storage permissions.
   - Tenant in-app e-signing actions (`Review & Sign Lease`, `Sign & Accept`) are completely removed; Tenants see their active lease terms and attached document badges cleanly.
   - Managers use **Archive** instead of **Delete**, toggling an `Active` / `Archived` filter to preserve complete lease history.
8. **Notifications & Co-Manager Invitations**:
   - The Notification panel includes **Mark All Read**, individual delete, and Manager **Clear All** with confirmation, plus optional Android system alert delivery whenever new database alerts arrive during an active session.
   - A **Manage Managers / Landlords** dialog in the Manager drawer lets existing Managers invite additional Co-Manager / Landlord emails so each stakeholder signs in with their own verified Google account.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Preserving Case Descriptions via Append-Only Notes**
  - *Chosen Approach*: Store `notes` as a structured chronological list on each case alongside immutable original `description`.
  - *Why*: Meets customer requirement to never overwrite what the tenant originally reported while keeping follow-up clarifications visible to both sides and included in audit reports.
  - *Alternatives Considered*: Editing `description` in place (rejected because it destroys audit history).

- **Decision 2 — Hourly Contiguous Locks on Manager Approval Only**
  - *Chosen Approach*: Resident booking requests write a `PENDING` booking document only; upon Manager **Approve**, a Firestore batch marks the booking `APPROVED` and writes per-hour lock records (`facilityId + dateKey + hour`) covering the 1–3 hour duration.
  - *Why*: Prevents unapproved requests from blocking other residents, prevents partial-window overlaps (e.g., 10:00–12:00 vs. 11:00–13:00), and eliminates tenant cross-write rule violations.
  - *Alternatives Considered*: Single string slot locks written by tenants at submission (rejected because `from`–`to` 1–3h durations overlap across arbitrary boundaries and unapproved bookings shouldn't block calendar availability).

- **Decision 3 — Zero-Permission SAF Document Picker for Contract PDFs/Docs**
  - *Chosen Approach*: Use Android Storage Access Framework (`OpenDocument` with persistable URI permission + document metadata `{ name, mimeType, uri }` saved on the contract).
  - *Why*: Satisfies strict Google Play permissions policy (no `READ_EXTERNAL_STORAGE` / media permissions) and works reliably without provisioning an external Firebase Storage bucket.
  - *Alternatives Considered*: Embedding Base64 files in Firestore documents (rejected due to Firestore's 1 MB document limit) or adding a Firebase Storage dependency outside project config.

- **Decision 4 — Co-Manager Email Invite Allowlist instead of Shared Complex Credentials**
  - *Chosen Approach*: Add an allowlisted `manager_invites` collection managed by existing Managers and auto-claimed into `admins/{uid}` on verified Google Sign-In.
  - *Why*: Gives landlords and property managers identical portal visibility while maintaining strict per-user audit trails and zero shared passwords.
  - *Alternatives Considered*: Sharing one generic Gmail account across multiple staff (discouraged for security and audit accountability).

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       PRESENTATION & UI LAYER                           │
│                                                                         │
│  ┌──────────────┐   ┌──────────────────┐   ┌─────────────────────────┐  │
│  │  Navigation  │   │  Primary Screens │   │   Interactive Modals    │  │
│  │  & Chrome    │   │                  │   │                         │  │
│  │ • Top Bar +  │   │ • Dashboard      │   │ • Report Case (Summary, │  │
│  │   Bell Badge │   │   (+ Report Case │   │   Unit lock, Due Date)  │  │
│  │ • Role-Aware │   │    FAB)          │   │ • Append Case Note      │  │
│  │   Drawer     │   │ • Cases          │   │ • Amenity Start + 1-3h  │  │
│  │   (incl.     │   │ • Units (sq m)   │   │ • Schedule Event +      │  │
│  │   Contractors│   │ • Contractors    │   │   Unit Multi-Select     │  │
│  │   & Audits)  │   │ • Amenities      │   │ • Contract + SAF Picker │  │
│  └──────┬───────┘   │ • Calendar       │   │ • Inline Manage Options │  │
│         │           │ • Contracts      │   │ • Invite Co-Manager     │  │
│         │           │ • Audit Reports  │   └────────────┬────────────┘  │
│         │           └────────┬─────────┘                │               │
└─────────┼────────────────────┼──────────────────────────┼───────────────┘
          │                    │                          │
          ▼                    ▼                          ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      STATE & NOTIFICATION ENGINE                        │
│                                                                         │
│  ┌───────────────────────────────────────┐   ┌───────────────────────┐  │
│  │           Estate ViewModel            │   │ Android Alert Service │  │
│  │ • Role & Co-Manager Resolution        │   │ • POST_NOTIFICATIONS  │  │
│  │ • Overlap Window Availability Engine  │──▶│   Runtime Check       │  │
│  │ • Live Days Left & Calendar Merger    │   │ • NotificationCompat  │  │
│  │ • Monthly Audit Report Aggregator     │   │   Alert Dispatcher    │  │
│  └───────────────────┬───────────────────┘   └───────────────────────┘  │
└──────────────────────┼──────────────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    DATA REPOSITORY & CLOUD FIRESTORE                    │
│                                                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                        Estate Repository                          │  │
│  │ • Firestore Snapshot Listeners (Role-Scoped & Query-Matched)      │  │
│  │ • Batch Writes (Booking Approval + Hourly Locks, Invites, Clear)  │  │
│  └───────────────────────────────┬───────────────────────────────────┘  │
│                                  │                                      │
│  ┌───────────────────────────────▼───────────────────────────────────┐  │
│  │              Named Cloud Firestore Database & Rules               │  │
│  │ • /admins & /manager_invites  • /bookings (PENDING/APPROVED/DECL.)│  │
│  │ • /cases (+ notes list &      • /slot_locks (hourly exclusivity)  │  │
│  │   preferredDate)              • /maintenances (category & target) │  │
│  │ • /properties (sqMeters,      • /contracts (isArchived, docs)     │  │
│  │   floorElevation, no wing/bay)• /estate_config/options            │  │
│  │ • /vendors (contractors)      • /reminders & /user_notice_states  │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State Changes

- **Cases (`CaseItem`)**: Add `preferredDate: String?` and `notes: List<CaseNote>` (where each note stores `id`, `authorName`, `authorRole`, `text`, `timestamp`, `epochMs`) plus `statusHistory: List<CaseMovementEntry>` for audit trails; remove Tenant review UI triggers.
- **Units (`PropertyItem`)**: Remove Pavilion Wing and Parking Bays from UI cards/dialogs (keep safe read compatibility for legacy documents), represent size in **sq m**, and bind Floor Elevation to configurable dropdown options (`1 Story`, `2 Story`, `3 Story` default).
- **Contractors (`VendorItem`)**: Retain backend collection parity while supporting dynamic String-based discipline/trade labels managed in `/estate_config/options` alongside UI renaming (`Contractor`).
- **Amenities & Bookings (`Facility`, `FacilityBooking`, `BookingSlotLock`)**: Repurpose `rules` label as **Notes**; add `startHour` / `startTime`, `durationHours` (`1..3`), `endTime`, and `approvalStatus` (`PENDING`, `APPROVED`, `DECLINED`) to bookings; store per-hour `BookingSlotLock` IDs (`facilityId__dateKey__HH`) created atomically by Managers on approval.
- **Calendar Events (`MaintenanceEvent`)**: Add `eventCategory` (`MAINTENANCE`, `GENERAL`, `INSPECTION`, `COMMUNITY`), `targetScope` (`ALL_RESIDENTS` vs `SPECIFIC_UNITS`), and `targetUnits: List<String>` (supporting Units + Clubhouse).
- **Contracts (`ContractItem`)**: Add `isArchived: Boolean = false`, `signedContractDoc: ContractDocumentMeta?`, and `codeOfConductDoc: ContractDocumentMeta?` (`name`, `mimeType`, `uri`); default new records to `TENANT_LEASE` with `ACTIVE` status (hard-copy signed offline), removing digital signature gates.
- **Configuration & Invites**: Add `EstateConfigOptions` (`/estate_config/options`) holding `floorElevations: List<String>` and `contractorDisciplines: List<String>`, plus `/manager_invites/{inviteId}` storing invited co-manager/landlord emails claimed on verified sign-in.

### Interactive Component & State Mapping

- **Dashboard & Cases**: `+ Report Case` FAB -> opens Report Case Dialog -> auto-populates read-only Tenant Unit -> validates Summary (`>= 3` chars) -> submits `OPEN` case & syncs Calendar due date -> triggers Management alert.
- **Case Card Actions**: Manager clicks `Add Note` -> appends immutable timestamped `CaseNote` + `CaseMovementEntry` -> updates both Tenant and Manager feeds cleanly. Manager clicks `Assign Contractor` -> opens vetted Contractors picker -> dispatches & notifies Tenant.
- **Amenity Booking & Approval**: Resident selects Date + Start Time + Duration (`1–3h`) -> live overlap check disables colliding hours -> submits `PENDING` request -> Manager sees Approval Queue -> tapping **Approve** writes atomic hourly locks + flips booking to `APPROVED` + adds Calendar block + notifies residents.
- **Schedule Event Modal**: Manager picks Category -> if `Maintenance`, shows Contractor dropdown; otherwise hides Contractor field -> picks Time Window dropdown + Target Units (`All Residents` or multi-select checklist) -> publishes to scoped residents' Calendars.
- **Contracts Screen & Form**: Picks `Tenant Lease` default -> selecting Tenant auto-populates Unit & default `Title` while keeping Tenant Name independent -> Date pickers recalculate `Days Left` live -> Document slots launch system `OpenDocument` picker -> Card actions replace `Delete` with `Archive` + `Active / Archived` filter pills.
- **Security Rules & Parity Verification**: Every updated model field, write keyset, batch approval, co-manager invite claim, and scoped query will be mirrored in Firestore Security Rules, validated with unit tests against the local Firestore Emulator, verified with Robolectric unit tests, compiled cleanly, and deployed to Firestore rules.

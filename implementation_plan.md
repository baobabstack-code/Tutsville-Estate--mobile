# Standalone Entity Navigation (Properties, Tenants, Amenities) & Resident Experience Architecture

Restructure the app's core navigation bar to elevate **Properties**, **Tenants**, and **Amenities** as standalone top-level tabs on the main bottom menu, eliminating nested sub-tabs as requested by the client in the screenshots. Establish role-tailored 4-tab navigation for both Estate Managers and Resident Tenants, backed by 100% Cloud Firestore data flows, robust error-free compilation, and zero-overlap layouts.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions have been confirmed and will guide execution upon approval:

- **Confirmed Decision 1 (Manager Bottom Navigation: 4 Standalone Tabs)**:
  - **Dashboard**: High-level estate KPIs, quick dispatch action ribbon, urgent preventive servicing timeline, and open work order triage.
  - **Properties**: Dedicated top-level tab for estate residences (*"Properties are what tenants rent"*). Features occupancy telemetry, architectural specifications, available vs leased filters, and *"+ List Property"*.
  - **Tenants**: Dedicated top-level tab for resident tenant management (*"Do not put tenants under anything"*). Features resident directory, onboarding/invite links via Google Sign-In, unit assignment, and *"+ Add Tenant"*.
  - **Amenities**: Dedicated top-level tab for estate facilities (*"Amenities are what tenants book to use"*). Features amenity catalog (pool, gym, courts), active reservations/passes, and *"+ Add Amenity"*.
- **Confirmed Decision 2 (Resident Tenant Navigation: 4 Tailored Tabs)**:
  - **Dashboard**: Bespoke resident concierge welcome card, assigned unit & parking badge, quick repair & booking triggers, and estate advisories.
  - **My Requests**: Private work order tracking showing only tickets reported for the resident's unit, technician dispatch status, and service reviews.
  - **Amenities**: Estate amenity directory and reservation engine to book private sessions and view active estate passes.
  - **My Residence**: Private architectural showcase of the resident's assigned apartment, layout specs, assigned parking bays, and in-unit features.
- **Confirmed Decision 3 (No Nested Sub-Tabs)**:
  - Completely deprecate the segmented switcher that buried Tenants and Properties inside Facilities. Both Managers and Residents now have direct 1-tap access to primary entities from the bottom bar.
- **Confirmed Decision 4 (Zero Overlap & Touch Target Standards)**:
  - Enforce `spacedBy(8.dp)` with flex weights, minimum 48dp touch targets, and `WindowInsets.navigationBars` padding to prevent any element collision or clipping across device dimensions.
- **Confirmed Decision 5 (100% Firestore Wiring & Stability)**:
  - Real-time snapshot listeners for all 4 entities (Properties, Tenants, Amenities/Bookings, Work Orders/Maintenances) remain active and resilient with offline-first fallbacks.

---

## 1. Overview & Core Concept

- **What It Does**: Transforms the estate app navigation into an intuitive, standalone entity architecture where **Properties**, **Tenants**, and **Amenities** are primary citizens rather than hidden sub-tabs.
- **Target Personas**:
  - *Estate Manager*: Instant 1-tap switching between overall operations (Dashboard), residential units (Properties), resident directory (Tenants), and shared amenities (Amenities).
  - *Resident Tenant*: Clear personal visibility over their living space (My Residence), maintenance requests (My Requests), amenity booking (Amenities), and resident home (Dashboard).
- **Core Value**: Direct alignment with client expectations captured in the screenshots, zero confusion from nested menus, and reliable real-time cloud sync.

---

## 2. Navigation & Screen Breakdown

### A. Estate Manager Navigation (4 Tabs)
```
[ 1. Dashboard ]   [ 2. Properties ]   [ 3. Tenants ]   [ 4. Amenities ]
```
1. **Dashboard Tab**:
   - Operations command center with KPIs (Servicing, Cases, Units, Vendors).
   - Quick Action Ribbon: One-tap dispatch to List Residence, Log Work Order, Schedule Servicing, Onboard Tenant, and Onboard Contractor.
   - Preventive Servicing Pipeline & Urgent Incident Work Orders.
2. **Properties Tab**:
   - Total Units, Leased vs Available occupancy telemetry cards.
   - Filter chips: *All Properties*, *Available*, *Occupied*.
   - Unit cards displaying unit number, pavilion wing, architectural grid (beds, baths, sq ft, elevation), view type, and parking bays.
   - Unit management actions: *Assign to Tenant*, *Share Invite Link*, and *Vacate Unit*.
   - Floating Action Button: *"+ List Property"* opening the property creation dialog.
3. **Tenants Tab**:
   - Resident directory telemetry: Total residents, allocated units, and app-verified users.
   - Filter chips: *All Tenants*, *Assigned*, *Unassigned*, *App Registered*.
   - Resident profile cards displaying tenant avatar, name, email, phone, emergency contacts, and assigned unit pill.
   - Actions: *Share Invite Link* (direct Google Sign-In guidance), *Assign Unit*, and *Unassign*.
   - Floating Action Button: *"+ Add Tenant"* opening the tenant creation dialog.
4. **Amenities Tab**:
   - Active estate passes and confirmed reservations with cancellation controls.
   - Estate amenities directory (Clubhouse, Heated Infinity Pool, Sports Courts, Screening Room) with capacity, location, and amenity tags.
   - Actions: *Book Facility* modal.
   - Floating Action Button: *"+ Add Amenity"* opening the amenity registration dialog.

---

### B. Resident Tenant Navigation (4 Tabs)
```
[ 1. Dashboard ]   [ 2. My Requests ]   [ 3. Amenities ]   [ 4. My Residence ]
```
1. **Dashboard Tab**:
   - Luxury resident hero card: *"Tower A • Penthouse 402"*, verified resident badge, assigned EV parking bay (*"Bay P-04 EV"*).
   - Resident quick actions: *"Reserve Amenity"*, *"Report Unit Repair"*, *"View My Passes"*.
   - Active personal estate passes and recent maintenance updates.
2. **My Requests Tab**:
   - Incident cases strictly scoped to `assignedPropertyUnit` or `fullName`.
   - Incident progress timeline (Open -> Assigned -> In Progress -> Completed).
   - Dispatched contractor details (company name, phone, SLA).
   - One-tap *"+ Report Repair"* pre-populating resident location.
3. **Amenities Tab**:
   - Browse estate amenities and reserve private sessions for family/guests.
   - Private estate pass ledger showing active QR/pass codes and timeslots.
4. **My Residence Tab**:
   - High-fidelity view of the resident's leased residence.
   - Architectural specs (bedroom suites, bathrooms, living area sq ft, view outlook).
   - Assigned parking bays and private in-unit amenities list.
   - Emergency contact button & *Report Maintenance for this Residence*.

---

## 3. Technical Implementation Steps

### Step 1: Update `MainActivity.kt` Navigation Items
- Update `navTabs` dynamically based on `currentRole`:
  - When `currentRole == UserRole.MANAGER`:
    - Tab 0: `Dashboard` (`Icons.Filled.Dashboard`)
    - Tab 1: `Properties` (`Icons.Filled.HomeWork`)
    - Tab 2: `Tenants` (`Icons.Filled.People`)
    - Tab 3: `Amenities` (`Icons.Filled.Apartment`)
  - When `currentRole == UserRole.TENANT`:
    - Tab 0: `Dashboard` (`Icons.Filled.Dashboard`)
    - Tab 1: `My Requests` (`Icons.AutoMirrored.Filled.Assignment`)
    - Tab 2: `Amenities` (`Icons.Filled.Apartment`)
    - Tab 3: `My Residence` (`Icons.Filled.VpnKey`)
- Update `Crossfade(targetState = safeTabIndex)` to map tabs to dedicated composables.

### Step 2: Modularize Screens
- **`PropertiesScreen.kt`**: Extract the full properties listing, telemetry, filters, and *"+ List Property"* FAB into a dedicated, clean composable.
- **`TenantsScreen.kt`**: Extract the full resident tenant directory, telemetry, invite sharing, and *"+ Add Tenant"* FAB into a dedicated composable.
- **`AmenitiesScreen.kt`** (in `FacilitiesBookingScreen.kt`): Streamline to focus purely on amenities catalog, pass reservations, and amenity booking.
- **`ResidentResidenceScreen.kt`**: Dedicated high-polish composable showing the resident's architectural details, parking bays, and maintenance triggers.
- **`CasesScreen.kt`**: Serves as *My Requests* for residents and work order management for managers.

### Step 3: Layout Audit & Collision Prevention
- Verify all action buttons and chip rows have minimum 40–48dp touch heights.
- Apply `windowInsetsPadding(WindowInsets.navigationBars)` and bottom content padding (`PaddingValues(bottom = 96.dp)`) to avoid navigation bar overlaps.
- Ensure all icon buttons have content descriptions and test tags for UI testing.

### Step 4: Verification & Automated Tests
- Run `compile_applet` to ensure zero compilation or syntax errors.
- Update `ExampleRobolectricTest.kt` to verify the 4-tab role-tailored navigation items and screen transitions.
- Run `gradle :app:testDebugUnitTest` to guarantee all tests pass.

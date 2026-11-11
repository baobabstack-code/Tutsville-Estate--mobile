# Navigation Streamlining, Vendor Creation & Direct Tenant Google Sign-In Architecture

Transition Tutsville Estate into a focused 3-tab operational layout (Dashboard, Work Orders with Vendor Pipeline & Onboarding, Facilities & Residences) and upgrade tenant onboarding to frictionless Direct Google Sign-In with automatic Firestore email matching.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions have been incorporated based on your answers and will guide execution upon approval:

- **Confirmed Decision 1 (In-App Vendor Creation & Onboarding)**:
  - **Work Orders Tab Trigger**: A prominent **"+ Add Vendor"** button on the Work Orders header/top bar allows the manager to register new contractors at any time.
  - **On-the-Fly Trigger in Assign Dialog**: Inside the **Assign Vendor** dialog, an intuitive **"+ Register New Contractor"** option lets managers add a new vendor immediately if they are not already in the directory, without abandoning the dispatch flow.
  - **Firestore Persistence**: Newly created vendors are written directly to the `vendors` Firestore collection with specialty category, phone number, email, and SLA response time, updating the contractor list instantly.
- **Confirmed Decision 2 (Tab Consolidation)**: The redundant standalone "Properties" tab is removed from the bottom navigation. Property units, tenant leases, floor plans, and parking bays are fully managed within the **Facilities & Residences** module.
- **Confirmed Decision 3 (3-Tab Core Navigation)**: Bottom navigation is streamlined into 3 primary tabs:
  1. `Dashboard` (Index 0): Key performance metrics, urgent announcements, security notices, and rapid actions.
  2. `Work Orders` (Index 1): Maintenance tracking pipeline (Open -> Assigned -> In Progress -> Resolved), case reporting, vendor dispatching, and vendor onboarding.
  3. `Facilities` (Index 2): Estate amenities & reservations, residential properties & units, and tenant directory management.
- **Confirmed Decision 4 (Vendor Role & Management Pipeline)**: Third-party contractors and vendors do not have consumer app accounts. Instead, they exist as an internal service directory so estate managers can assign work orders, enforce contract SLAs, and track repair lifecycles across a visual dispatch pipeline.
- **Confirmed Decision 5 (Frictionless Tenant Onboarding)**: The non-functioning pre-login invite code dialog on the Auth screen is removed. Instead, when an estate manager registers a tenant with their email (e.g. `resident@gmail.com`), the tenant simply downloads the app and taps **Sign In with Google**. The app instantly matches their email against Firestore, assigns the Resident role, and attaches their apartment unit and digital amenities pass.

---

## 1. Overview & Core Concept

- **What It Does**: Provides a cleaner, clutter-free mobile interface for estate managers and residents. Consolidates duplicated screens into clear operational pillars, allows managers to create and dispatch vendors on-the-fly, and eliminates login friction for residents by matching their Google identity to their estate unit.
- **Target Audience / Persona**:
  - *Estate Managers*: Monitor occupancy, register approved contractors, dispatch vendors to maintenance tickets, and onboard residents.
  - *Residents*: Book amenities (pool, gym, tennis court), view property details, and submit repair requests.
- **Key Value**: Eliminates redundant navigation items, speeds up task completion, provides on-the-fly contractor management, and provides a 100% reliable onboarding flow with zero broken pre-login states.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Vendor Onboarding & Dispatch Flows**:
   - *Via Work Orders Header*: The manager taps **"+ Add Vendor"** on the Work Orders screen. The `AddVendorDialog` opens, allowing input of Vendor Name, Specialty (Plumbing, Electrical, HVAC, Landscaping, Security, Elevators, Pool/Water), Contact Phone, Email, and SLA Response Time (e.g. 2 hours, 4 hours, 24 hours). Upon tapping Save, it persists to Firestore and is immediately selectable.
   - *Via Assign Vendor Dialog*: While reviewing an open case, the manager taps **"Assign Vendor"**. If the needed contractor is not yet in the list, the manager taps **"+ Register New Contractor"** directly at the top of the sheet. It opens the vendor creation dialog, saves to Firestore, and automatically selects the newly created vendor for the case.

2. **Resident Onboarding Flow**:
   - Manager adds a new resident in Facilities -> Tenant Manager with name, email, phone, and assigned apartment unit.
   - Manager taps **Share Pass** or **Copy Invite Link**, sending the invitation to the resident.
   - Resident opens the app and signs in with their Google account.
   - The app's `recomputeUserRole()` queries the live Firestore `tenants` collection, instantly links them as a **Resident**, and displays their unit badge and access pass on the luxury header bar.

3. **Work Orders & Contractor Dispatch Pipeline**:
   - Manager navigates to the **Work Orders** tab.
   - A Kanban/Pipeline filter bar displays tickets categorized by status: `ALL`, `OPEN`, `ASSIGNED`, `IN PROGRESS`, and `RESOLVED`.
   - Tapping **Assign Contractor** opens the vendor selector, assigning a certified contractor with SLA tracking and contact buttons (Call/Message).
   - Tapping "+ Report Case" allows instant ticket creation.

4. **Facilities & Residences Hub**:
   - Top segmented pills switch smoothly between:
     - `Amenities & Bookings`: Clubhouse, Heated Infinity Pool, Rooftop Lounge, Tennis Courts, and reservation slots.
     - `Properties & Units`: Living units, bedrooms, bathrooms, occupancy status, and parking bay assignments.
     - `Tenant Directory`: Active residents, lease dates, emergency contacts, and the "+ Onboard Tenant" action.

### Visual Identity & Theme
- **Color Palette**:
  - Background: Luxury Warm Ivory (`#FDFBF7`) / Dark Slate Green (`#101B14`).
  - Primary: Deep Emerald (`#1B3B2B`).
  - Accents: Burnished Gold (`#D4AF37`) for active badges, VIP indicators, and status borders.
  - Cards: Crisp layered surfaces (`#FFFFFF` and `#FAF8F5`) with `tonalElevation = 2.dp`.
- **Typography**: Clean serif display headlines paired with legible modern sans-serif body text.
- **Navigation Touch Standards**: 48dp minimum interactive component touch targets with ripple effects.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Dual Entry Points for Vendor Creation**:
  - *Chosen Approach*: Place "+ Add Vendor" on the Work Orders screen AND "+ Register New Contractor" inside the Assign Vendor dialog.
  - *Why*: Eliminates friction when dispatching urgent maintenance cases. If a new emergency plumber is needed, the manager doesn't have to exit the work order screen to register them.
- **Decision 2: 3-Tab Bottom Navigation vs. 5-Tab Layout**:
  - *Chosen Approach*: Streamlined to 3 tabs: Dashboard, Work Orders, and Facilities.
  - *Why*: Reduces cognitive load and eliminates duplicate property screens. 3 tabs provide spacious, thumb-friendly touch targets that look exceptional on Android devices.
- **Decision 3: Direct Google Sign-In vs. Unauthenticated Invite Code Lookups**:
  - *Chosen Approach*: Automatic Firestore email resolution upon sign-in.
  - *Why*: Firebase Firestore security rules enforce `isSignedIn()`, which prevents unauthenticated users from querying resident lists before login. Relying on Google Sign-In provides cryptographic identity proof and eliminates code-typing errors.

---

## 4. Technical Architecture & Data Strategy

### System Architecture & Navigation Model

```
┌────────────────────────────────────────────────────────────────────────┐
│                   Tutsville Estate Top Header Bar                      │
│   [Estate Crest]          [Role: Resident/Manager]     [User Avatar]   │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              ▼                    ▼                    ▼
     ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
     │  Tab 0:         │  │  Tab 1:         │  │  Tab 2:         │
     │  DASHBOARD      │  │  WORK ORDERS    │  │  FACILITIES     │
     ├─────────────────┤  ├─────────────────┤  ├─────────────────┤
     │ • KPI Overview  │  │ • Status        │  │ • Amenities &   │
     │ • Quick Actions │  │   Pipeline      │  │   Bookings      │
     │ • Notices &     │  │ • [+ Add Vendor]│  │ • Properties &  │
     │   Alerts        │  │ • Assign Vendor │  │   Units         │
     │ • Urgent Repairs│  │   [+ New Vendor]│  │ • Tenant Manager│
     └─────────────────┘  └─────────────────┘  └─────────────────┘
                                   ▲
                                   │ (Streamlined 3-Tab Bar)
┌──────────────────────────────────┴─────────────────────────────────────┐
│    [Dashboard]             [Work Orders]             [Facilities]      │
└────────────────────────────────────────────────────────────────────────┘
```

### Component & State Mapping

1. **`MainActivity.kt`**:
   - Update `navTabs` to 3 items:
     - `Dashboard` (`Icons.Filled.Dashboard`)
     - `Work Orders` (`Icons.AutoMirrored.Filled.Assignment`)
     - `Facilities` (`Icons.Filled.Apartment`)
   - Update `Crossfade(safeTabIndex)` to handle 3 screens:
     - `0 -> DashboardScreen(viewModel)`
     - `1 -> CasesScreen(viewModel)`
     - `2 -> FacilitiesBookingScreen(viewModel, defaultSection = 0)`
   - Bind `onNavigateToTab` so quick-action buttons on Dashboard route directly to Tab 1 (Work Orders) or Tab 2 (Facilities).
   - Wire `AddVendorDialog` when `viewModel.activeAddVendorDialog` is true.

2. **`CasesScreen.kt` & Vendor Pipeline**:
   - Add a prominent **"+ Add Vendor"** button to the top action row alongside "+ Report Case" (for Managers).
   - Display active vendor count and fast-filter chips for ticket states (`ALL`, `OPEN`, `ASSIGNED`, `IN PROGRESS`, `RESOLVED`).
   - In `AssignVendorDialog`, add an option card or header action: `"+ Register New Contractor"`. Clicking it opens the `AddVendorDialog`. Once saved, the new contractor is immediately available in the list.

3. **`AuthScreen.kt`**:
   - Remove the unauthenticated `showInviteCodeDialog` and the `"Have a Resident Invite Code?"` button.
   - Replace with a clear, welcoming onboarding note:
     *"Are you a registered resident? Sign in with your registered email to automatically activate your resident keys and concierge pass."*

4. **`PropertyDialogs.kt` (Tenant Invite Pass)**:
   - Update the invite text and share sheet copy:
     *"Welcome to Tutsville Private Estate! You have been registered as our resident for [Unit]. Download the app and sign in with [Email] via Google Sign-In to activate your access keys, valet, and facility booking privileges."*

5. **`EstateViewModel.kt` & `EstateRepository.kt`**:
   - Ensure `submitNewVendor(...)` creates the vendor in Firestore and triggers an immediate snapshot update.
   - Ensure `selectedTab` clamping works with indices `0..2`.

---

## 5. Verification Plan

1. **Compilation**:
   - Execute `compile_applet` to verify clean build with no missing references or index out of bounds.
2. **Automated Testing**:
   - Run `gradle :app:testDebugUnitTest` via `run_command` to ensure all Robolectric JVM tests pass.
   - Update any test cases that verify navigation tabs to assert the streamlined 3-tab configuration.
3. **Interactive Validation**:
   - Verify smooth transitions between Dashboard, Work Orders, and Facilities.
   - Verify "+ Add Vendor" from both the Work Orders screen and inside the Assign Vendor dialog.
   - Confirm vendor assignment works seamlessly on Work Orders.
   - Confirm tenant role auto-detection works when signing in with a registered resident email.

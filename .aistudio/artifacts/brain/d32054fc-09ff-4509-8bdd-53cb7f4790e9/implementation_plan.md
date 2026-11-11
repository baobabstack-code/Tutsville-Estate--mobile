# Production Launch: Pure Cloud Architecture & Clean Streamlined UI

Prepare Tutsville Estate for public live launch by removing all prototype, testing, and confusing UI controls, simplifying navigation, auto-detecting user roles via Firestore email matching, and making the entire data layer 100% dynamic from Cloud Firestore with in-app management.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed in Phase 1 and will govern this update:
> - **Remove Prototype Controls**: Remove the top role switcher pill, the manual "Sync Cloud DB" button, and hardcoded demo count banners.
> - **Simplified Bottom Navigation**: Streamline bottom tabs to focused core destinations (Dashboard, Facilities & Tenants, Work Orders, Contracts & Vendors, and Notices).
> - **Automatic Role Detection**: Determine permissions automatically by checking if the authenticated Google user's email exists in the `tenants` collection. If matched, the user receives the tailored Resident experience (their unit, bookings, work order reporting); otherwise, they operate as Estate Manager.
> - **Clean Luxury Header**: Elegant Tutsville header featuring the gold script logo, verified user avatar, and account menu/sign out, eliminating clutter.
> - **100% Dynamic Cloud Persistence**: Remove all static seed fallbacks; all data streams live from Cloud Firestore with smooth loading states and empty-state creation shortcuts.

---

## 1. Overview & Core Concept

Tutsville Estate is moving from an internal prototype to a production-grade live platform. This change polishes the end-user experience so that:
- Real residents log in and automatically see their personal residence, lease status, and amenity booking tools.
- Estate managers access full administrative command (managing residences, onboarding tenants, scheduling servicing, dispatching vendor work orders).
- No confusing developer toggles, mock banners, or manual sync buttons remain visible.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Authentication & Automatic Role Assignment**:
   - User signs in with Google.
   - The app matches `currentUser.email` against the `tenants` Firestore collection:
     - **Matched Tenant**: Welcomed as Resident (e.g., "Welcome, Eleanor • Villa 402"), showing resident-specific quick actions (Report Issue, Reserve Amenity, View Lease).
     - **Manager**: Displays Estate Command Hub with full dispatch, listing, and tenant onboarding controls.
2. **Clean Minimal Luxury Top Bar**:
   - Premium black-and-gold aesthetic with the gold script "Tutsville Estate" branding.
   - User profile circle with Google profile photo / initial, showing role badge ("Estate Manager" or "Resident") and a sign-out dropdown.
   - Prototype role-switching dropdown completely removed.
3. **Streamlined Bottom Navigation**:
   - Simplified from 7 cluttered tabs down to 5 distinct, high-value destinations:
     1. **Dashboard** (Command overview for managers / Resident Hub for tenants)
     2. **Properties & Tenants** (Units, resident directory, onboarding invite codes)
     3. **Work Orders** (Maintenance incidents, vendor dispatches, status tracking)
     4. **Facilities** (Amenity catalog, rules, reservations)
     5. **Vendors & SLAs** (Certified contractors, vendor ratings, SLA agreements)
4. **Production Quick Dispatch Row**:
   - "Sync Cloud DB" button removed.
   - Clean, production-ready action pills:
     - For Managers: **List Residence**, **Log Work Order**, **Schedule Servicing**, **Onboard Tenant**, **Broadcast Advisory**.
     - For Tenants: **Report Issue**, **Reserve Amenity**, **View Passes**.
5. **In-App Management Modals**:
   - Managers can create facilities, add approved vendors, and register new SLA contracts directly in the app, writing immediately to Cloud Firestore.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Removal of Manual Sync & Role Switcher**
  - *Chosen Approach*: Automatic sync on app startup and automatic role detection based on authenticated account email.
  - *Why*: Prevents user confusion in a live environment where end users should never see developer sync buttons or be able to switch into another role's view arbitrarily.
- **Decision 2: 5-Tab Canonical Navigation Bar**
  - *Chosen Approach*: Merge scattered tabs into 5 logical domains (Dashboard, Properties, Work Orders, Facilities, Vendors).
  - *Why*: Fits comfortably on any Android screen without label truncation or crowding, meeting Material Design guidelines.
- **Decision 3: Zero-Fallback StateFlows**
  - *Chosen Approach*: StateFlows initialize as empty lists with an `isLoading` boolean. When Firestore listeners emit data, `isLoading` flips to false.
  - *Why*: Eliminates mock data remnants and ensures that what is shown on screen is 100% verified cloud data.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Cloud Firestore Backend                         │
│                                                                        │
│  /tenants/{id}         -> Auto-detect role: email == user.email        │
│  /properties/{id}      -> Residences & layouts                         │
│  /cases/{id}           -> Incident tickets & work orders               │
│  /facilities/{id}      -> Amenities & reservations                     │
│  /contracts/{id}       -> SLAs & warranties                            │
│  /vendors/{id}         -> Approved contractors                         │
│  /maintenances/{id}    -> Servicing schedule                           │
│  /reminders/{id}       -> Advisories & notices                         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Real-Time Live Sync
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   EstateRepository (Production-Ready)                  │
│                                                                        │
│  • Empty default collections (no hardcoded seed lists)                 │
│  • isLoading: StateFlow<Boolean>                                       │
│  • Mutation methods for all 9 entities                                 │
│  • checkAndAssignUserRole(email: String): UserRole                     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                            EstateViewModel                             │
│                                                                        │
│  • currentRole: StateFlow<UserRole> (derived from Firestore email)     │
│  • Simplified 5-tab index navigation                                   │
│  • Clean production action handlers                                    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     Jetpack Compose UI (Clean & Live)                  │
│                                                                        │
│  • Luxury Top Bar (No role pill, clean profile avatar + sign out)      │
│  • 5-Tab Streamlined Bottom Navigation Bar                             │
│  • Dynamic Loading Skeletons & Empty States                            │
│  • Modal Forms: Add Facility, Onboard Vendor, Register Contract        │
└────────────────────────────────────────────────────────────────────────┘
```

### Steps to Implement

1. **`EstateRepository.kt`**:
   - Remove hardcoded seed fallback lists (`seedCases`, `seedProperties`, etc.) from being default values.
   - Initialize StateFlows with `emptyList()`.
   - Add `isLoading: StateFlow<Boolean>` set to false once initial snapshots arrive.
   - Add `createFacility`, `createVendor`, `createContract` mutations.
   - Add helper to resolve user role by querying `tenants` where email matches.
2. **`EstateViewModel.kt`**:
   - Update role resolution to listen to authenticated email and repository tenant records.
   - Remove manual `syncOrSeedFirestore` and temporary role switcher setter.
   - Add dialog state flows for `activeAddFacilityDialog`, `activeAddVendorDialog`, `activeAddContractDialog`.
3. **`MainActivity.kt` & Navigation**:
   - Update `TutsvilleApp` navigation tabs to the streamlined 5-tab layout:
     1. Dashboard (`Icons.Filled.Dashboard`)
     2. Properties & Tenants (`Icons.Filled.HomeWork`)
     3. Work Orders (`Icons.Filled.Assignment`)
     4. Facilities (`Icons.Filled.Apartment`)
     5. Contracts & Vendors (`Icons.Filled.Handshake` / `Icons.Filled.Description`)
   - Simplify TopBar: remove role switcher dropdown; display user email, role badge, and sign-out button cleanly.
4. **`DashboardScreen.kt`**:
   - Remove manual "Sync Cloud DB" quick action button.
   - Remove hardcoded demo banners or mock headline counts.
   - Show dynamic counters computed strictly from live Firestore StateFlows.
5. **Add Management Dialogs**:
   - Add `AddFacilityDialog` for amenities.
   - Add `AddVendorDialog` for contractor onboarding.
   - Add `AddContractDialog` for SLA contracts.
6. **Verification**:
   - Validate with `compile_applet` and Robolectric unit test suite (`gradle :app:testDebugUnitTest`).

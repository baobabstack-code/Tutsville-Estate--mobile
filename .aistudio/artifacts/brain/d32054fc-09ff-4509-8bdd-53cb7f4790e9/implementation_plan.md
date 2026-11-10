# Revised Implementation Plan: Property Listing, Tenant Assignment & Secure Invitation Link

Build a complete property management and tenant onboarding system for Tutsville Estate inside the Facilities section, featuring tenant invitation link generation, native Android share dispatch, and email-matched tenant sign-in.

---

## User Flow & Requirements Overview

1. **Manager Property & Facility Listing**:
   - Manager lists a property/residence with comprehensive architectural details (bedrooms, bathrooms, square footage, floor level, view), dedicated parking spots (with EV status), luxury amenities, and monthly valuation.
2. **Tenant Assignment**:
   - Manager assigns a vacant property to a resident by entering the tenant's full name, email address, and lease start date.
3. **Invitation Link Generation & Sharing**:
   - When assigned, the manager generates a personalized tenant onboarding link (e.g. `tutsville://onboard?propertyId=PROP-402&email=resident@domain.com` and web fallback).
   - Manager can **Copy Link** to clipboard or tap **Share Invitation** to send via Android's native share sheet (Email, WhatsApp, Messages).
4. **Email-Matched Tenant Onboarding & Sign-In**:
   - When the tenant logs in or creates an account matching the assigned email address (or opens the app via the invitation link), the app detects the pending property assignment, binds the resident to their unit, and displays their property on their dashboard.
   - An "Enter Invitation Code / Link" helper is provided on the sign-in/sign-up screen so tenants can also paste their link manually.

---

## Detailed Proposed Changes

### 1. Data Models (`EstateModels.kt`)
- `PropertyStatus`: `AVAILABLE`, `OCCUPIED`, `UNDER_MAINTENANCE`.
- `PropertyArchitecture`:
  - `bedrooms: Int`, `bathrooms: Float`, `sqFt: Int`, `floorLevel: String`, `viewType: String`.
- `AssignedTenantInfo`:
  - `name: String`, `email: String`, `leaseStartDate: String`, `leaseEndDate: String`, `inviteCode: String`, `inviteLink: String`, `isClaimed: Boolean`, `assignedTimestamp: String`.
- `PropertyItem`:
  - `id: String`, `unitNumber: String`, `pavilionWing: String`, `architecture: PropertyArchitecture`, `parkingSpots: List<String>`, `amenities: List<String>`, `monthlyRental: String`, `status: PropertyStatus`, `assignedTenant: AssignedTenantInfo?`, `listedDate: String`.

### 2. Repository Layer (`EstateRepository.kt`)
- `_properties`: Seeded with luxury Tutsville residences (*Penthouse 801*, *Villa 402*, *Garden Villa 104*, *Tower B Suite 602*).
- `listProperty(...)`: Adds new property to state and Firestore.
- `assignPropertyToTenant(propertyId, tenantName, tenantEmail, leaseStartDate, leaseEndDate)`:
  - Generates a unique invite code and onboarding link.
  - Updates property status to `OCCUPIED` with pending invite details.
- `claimPropertyInvite(email, inviteCode)`:
  - Validates and marks the assignment as claimed when the resident authenticates with that email.
- `vacateProperty(propertyId)`: Clears tenant and returns property to `AVAILABLE`.

### 3. ViewModel (`EstateViewModel.kt`)
- Expose `properties: StateFlow<List<PropertyItem>>`.
- Add dialog and modal state flows:
  - `activeListPropertyDialog: MutableStateFlow<Boolean>`
  - `activeAssignTenantProperty: MutableStateFlow<PropertyItem?>`
  - `activeShareInviteDialog: MutableStateFlow<PropertyItem?>`
- Implement `listNewProperty(...)`, `assignTenantToProperty(...)`, and `claimTenantPropertyForEmail(email)`.

### 4. Interactive Dialogs & Share Sheet (`Dialogs.kt`)
- **`ListPropertyDialog`**:
  - Full architectural specifications (Beds, Baths, Sq Ft, Floor Level, Wing).
  - Dedicated parking bays with EV toggle.
  - Amenity tags selector (Plunge Pool, Wine Cellar, Smart Home, Balcony).
  - Monthly lease rate.
- **`AssignTenantDialog`**:
  - Property preview.
  - Tenant Name, Email Address, Lease Start Date, and duration.
  - "Assign & Generate Invitation Link" primary action.
- **`ShareTenantInviteDialog`**:
  - Displays generated onboarding link and invite code.
  - One-tap "Copy Link" with clipboard feedback.
  - "Share via Apps" triggering Android `Intent(Intent.ACTION_SEND)` with pre-filled message welcoming the resident to Tutsville Estate.
- **Auth Screen Integration (`AuthScreen.kt`)**:
  - Recognizes invited email addresses during login or account creation to automatically display their assigned unit.

### 5. Facilities Screen Enhancement (`FacilitiesBookingScreen.kt`)
- Segmented header:
  - **Amenities & Passes**: Existing clubhouse, sports, cinema, and lounge reservations.
  - **Properties & Residences**:
    - Summary metrics: Total Units, Leased/Occupied %, Available Units.
    - Quick Action: `+ List New Property`.
    - Property cards showing architectural details, parking spots, luxury amenities.
    - Available properties show **"Assign to Tenant"**.
    - Occupied/Assigned properties show tenant details, **"Share Invite Link"**, and **"Manage Resident"**.

### 6. Deep Link Configuration (`AndroidManifest.xml`)
- Declare custom URL scheme `tutsville://invite` and `tutsville://onboard` so clicking the link on device opens the app directly with pre-populated email credentials.

---

## Verification Plan

### Automated Tests (`ExampleRobolectricTest.kt`)
1. `test listing property with full architecture parking and amenities`
2. `test assigning tenant generates valid onboarding link and matches tenant email`
3. `test claiming property invite binds resident to unit upon matching email`
4. `test vacating property restores status to AVAILABLE`

### Manual Verification
1. Open Facilities tab > Toggle to **Properties & Residences**.
2. Tap **+ List Property**, fill in architectural details, parking, and amenities, then save.
3. Tap **Assign to Tenant**, enter resident name & email (`resident@tutsville.com`).
4. In the generated dialog, tap **Copy Link** and **Share Invitation**.
5. Switch user or log in with that email to verify the property assignment automatically matches and displays for the resident.

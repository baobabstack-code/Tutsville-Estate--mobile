# Implementation Plan - Manager Dashboard & Data Models Revision

Align the application's data models, side menu, bottom menu bar, and feature screens with the exact specifications provided in the user's specification screenshots (`image_8b35f878.png` and `image_517852ba.png`), and remove all extraneous dashboard cards (maintenance schedule card and contract alert card).

---

## Dashboard Requirements & Layout Specification

### Manager Dashboard Overview Screen
The Manager Dashboard overview screen will feature **exclusively the 4 requested tiles/cards**:
1. **1-Tenants Tile**: Displays total residents count, registered vs pending app invites, and quick view shortcut.
2. **2-Units Tile**: Displays total inventory count, occupied vs available units, occupancy percentage, and quick view shortcut.
3. **3-Amenities Tile**: Displays active facilities count, capacity utilization, and quick booking shortcut.
4. **4-Cases Tile**: Displays active work orders count, open/assigned/in-progress breakdown, emergency alerts, and log case shortcut.

*Note: The Maintenance Schedule card, Calendar section, and Contract Alert banner are completely removed from the Manager Dashboard overview screen.*

---

## Full Feature Specifications

### 1. Navigation Architecture
- **Bottom Navigation Bar**:
  - `Dashboard` (Manager Overview with the 4 tiles above)
  - `Cases`
  - `Units`
- **Side Menu (Drawer) - Items 1 to 6**:
  1. `Tenants`
  2. `Units`
  3. `Amenities`
  4. `Cases`
  5. `Contracts`
  6. `Vendors`

---

### 2. Data Models Revision (`com.example.tutsville.data.model`)
Update Kotlin data classes and Cloud Firestore serialization/deserialization to match all requested fields:
- **1-Tenants**: `firstName`, `lastName`, `nationalId`, `address`, `phone`, `email`, `profilePhoto` (URL/base64 avatar string), `assignedPropertyUnit`.
- **2-Units**: `unitNumber`, `description`, `size` (sq ft / sq m), `monthlyRental`, `status`.
- **3-Amenities**: `number` (amenity code/id), `description`, `name`, `capacity`, `location`.
- **4-Cases**: `number` (case #), `unitNumber`, `tenantNumber` (or tenant name/phone), `date`, `status`, `vendor` (assigned vendor name/id), `title`.
- **5-Contracts**: `number` (contract #), `tenant`, `unit`, `startDate`, `expiryDate`, `contractAmount`.
- **6-Vendors**: `name`, `phoneNumber`, `email`, `specialty`.

---

## Proposed Code Modifications

### Data & State Layer
1. `app/src/main/java/com/example/tutsville/data/model/Models.kt`:
   - Update `TenantProfile`, `PropertyItem`, `Facility`, `CaseItem`, `ContractItem`, and `VendorItem` with exact specified fields.
2. `app/src/main/java/com/example/tutsville/data/repository/EstateRepository.kt`:
   - Update Firestore mappers (`mapTenantToFirestore`, `docToTenant`, `mapPropertyToFirestore`, `docToProperty`, `mapFacilityToFirestore`, `docToFacility`, `mapCaseToFirestore`, `docToCase`, `mapContractToFirestore`, `docToContract`, `mapVendorToFirestore`, `docToVendor`).
3. `app/src/main/java/com/example/tutsville/ui/viewmodel/EstateViewModel.kt`:
   - Expose CRUD actions operating on the updated model structures.

### UI & Navigation Layer
1. `app/src/main/java/com/example/tutsville/ui/components/SidebarDrawer.kt`:
   - Configure drawer menu items 1 through 6 in order.
2. `app/src/main/java/com/example/tutsville/ui/components/EstateHeaderBar.kt` & `MainActivity.kt`:
   - Configure bottom navigation bar with Dashboard, Cases, Units tabs, synced to side drawer.
3. `app/src/main/java/com/example/tutsville/ui/screens/DashboardOverviewScreen.kt`:
   - Build the Manager Dashboard containing strictly the 4 tiles (1-Tenants, 2-Units, 3-Amenities, 4-Cases), removing the Maintenance Schedule and Contract cards.
4. Screen Dialogs & Cards:
   - Update `TenantsScreen.kt`, `UnitsScreen.kt`, `AmenitiesScreen.kt`, `CasesTableScreen.kt`, `ContractsScreen.kt`, and `VendorsScreen.kt` to present all exact fields.

---

## Verification Plan

### Automated & Build Verification
1. Run `compile_applet` to confirm zero compilation errors.
2. Run `gradle :app:testDebugUnitTest` to verify data model serialization, repository CRUD operations, and Robolectric UI tests.

### User Visual & Functional Verification
1. Confirm Manager Dashboard displays exclusively the 4 tiles (1-Tenants, 2-Units, 3-Amenities, 4-Cases) and that Maintenance Schedule & Contract cards are removed.
2. Confirm Bottom Menu has Dashboard, Cases, Units.
3. Confirm Side Drawer has items 1 to 6 in exact order.
4. Confirm form dialogs and cards across all 6 sections display all specified fields.

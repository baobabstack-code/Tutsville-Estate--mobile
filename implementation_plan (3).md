# End-to-End Firestore Document Deletion & Centered Delete Pop-up Modal Architecture

Fix the record deletion issue by wiring real-time Cloud Firestore document deletion methods in `EstateRepository` and `EstateViewModel` for all **6 features** (**Tenants**, **Units**, **Cases**, **Contracts**, **Vendors**, **Amenities**). Redesign the delete confirmation pop-up into a centered, floating card modal with a max width of `340.dp`, warning icon emblem, clear item context, and touch-friendly action buttons.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions have been confirmed and will guide execution upon approval:

- **Confirmed Decision 1 (Real-time Cloud Firestore Document Deletion)**:
  - Add explicit repository & view model deletion methods for all 6 entities:
    1. `deleteTenant(tenantId)` -> Removes document from Firestore `/tenants/{tenantId}` and updates `_tenants` StateFlow.
    2. `deleteProperty(propertyId)` -> Removes document from Firestore `/properties/{propertyId}` and updates `_properties` StateFlow.
    3. `deleteCase(caseId)` -> Removes document from Firestore `/cases/{caseId}` and updates `_cases` StateFlow.
    4. `deleteContract(contractId)` -> Removes document from Firestore `/contracts/{contractId}` and updates `_contracts` StateFlow.
    5. `deleteVendor(vendorId)` -> Removes document from Firestore `/vendors/{vendorId}` and updates `_vendors` StateFlow.
    6. `deleteFacility(facilityId)` -> Removes document from Firestore `/facilities/{facilityId}` and updates `_facilities` StateFlow.

- **Confirmed Decision 2 (Centered Floating Card Modal for Delete Confirmations)**:
  - Deprecate bottom sheet / full-width dialog styling.
  - Implement a centered, floating card modal (`Dialog` + `Surface`) formatted as follows:
    - **Position**: Floating squarely in the screen center.
    - **Dimensions**: `Modifier.widthIn(max = 340.dp).fillMaxWidth(0.88f)`.
    - **Header Emblem**: Red circular badge containing `Icons.Default.DeleteForever` or `Icons.Default.Warning`.
    - **Title**: Bold title identifying the entity (*"Delete Unit Penthouse 402?"*, *"Delete Tenant Vivienne Sterling?"*).
    - **Body**: Explicit confirmation message explaining permanent Cloud Firestore removal.
    - **Buttons**: Equal-width *"Cancel"* (outlined) and *"Delete Permanently"* (red filled) buttons with 40dp+ touch targets.

- **Confirmed Decision 3 (Seamless Screen Integration)**:
  - Wire `DeleteConfirmDialog` in **`TenantsScreen.kt`**, **`UnitsScreen.kt`**, **`CasesTableScreen.kt`**, **`ContractsScreen.kt`**, **`VendorsScreen.kt`**, and **`AmenitiesScreen.kt`** to execute real deletion when confirmed.

---

## 1. Deletion Flow & Repository Specs

### Firestore & Repository Deletion Workflow
```
[ User Taps Delete Icon on Card ] ──> Opens Centered Pop-up Modal (340dp max width)
                                             │
                                   [ User Taps "Delete Permanently" ]
                                             │
                                             ▼
                                  EstateViewModel.deleteXYZ(id)
                                             │
                                             ▼
                                  EstateRepository.deleteXYZ(id)
                                             │
                 ┌───────────────────────────┴───────────────────────────┐
                 ▼                                                       ▼
Firestore Document Deleted                                   Local StateFlow Updated
db.collection("xyz").document(id).delete()                 _xyz.update { list - deleted }
                 │                                                       │
                 └───────────────────────────┬───────────────────────────┘
                                             ▼
                                 UI Snapshot Auto-Refreshes
```

---

## 2. Technical Implementation Steps

### Step 1: Add Deletion Methods in `EstateRepository.kt` & `EstateViewModel.kt`
- Implement:
  - `deleteTenant(tenantId: String)`
  - `deleteProperty(propertyId: String)`
  - `deleteCase(caseId: String)`
  - `deleteContract(contractId: String)`
  - `deleteVendor(vendorId: String)`
  - `deleteFacility(facilityId: String)`
- Ensure each function deletes the corresponding document from Cloud Firestore and updates the local `StateFlow` so the UI instantly updates.

### Step 2: Redesign `DeleteConfirmDialog` Component (`DataTableComponents.kt`)
- Build a centered, floating card pop-up:
  ```kotlin
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color.White,
      shadowElevation = 8.dp,
      border = BorderStroke(1.dp, TutsvilleOutline),
      modifier = Modifier
        .widthIn(max = 340.dp)
        .fillMaxWidth(0.88f)
        .wrapContentHeight()
    ) { ... }
  }
  ```
- Add warning emblem badge, title, descriptive message, and side-by-side action buttons.

### Step 3: Connect Screen Confirmations to ViewModel
- Update **`TenantsScreen.kt`**, **`UnitsScreen.kt`**, **`CasesTableScreen.kt`**, **`ContractsScreen.kt`**, **`VendorsScreen.kt`**, and **`AmenitiesScreen.kt`** so `onConfirm` calls `viewModel.delete...()`.

### Step 4: Verification & Automated Tests
- Run `compile_applet` to confirm clean compilation.
- Add unit test in `ExampleRobolectricTest.kt` verifying deletion of units, tenants, cases, and contracts from `EstateRepository`.
- Run `gradle :app:testDebugUnitTest` to guarantee all tests pass.

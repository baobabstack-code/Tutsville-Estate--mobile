# Role-Scoped Case Locations & Register Contract Dropdowns

This update streamlines case reporting and contract registration across the Manager and Resident portals by enforcing resident unit lock-in on cases, giving managers an optional unit dropdown defaulting to `Estate Wide`, adding smart dropdowns to contract registration, and updating project documentation.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> All ambiguous workflow choices have been confirmed and govern the design below:

- **Confirmed Decision 1 — Optional Manager Case Location**: When a manager creates (or edits) a case without choosing a specific unit, location automatically defaults to **`Estate Wide`**.
- **Confirmed Decision 2 — Tenant Name Dropdown in Register Contract**: When registering a Tenant Lease, **Tenant Name** presents a dropdown of registered tenants (with autofill of linked unit, email, contact, and valuation) plus support for manual entry of an unlisted tenant.
- **Confirmed Decision 3 — Unit / Scope Dropdown in Register Contract**: Alongside existing quick-pick chips, **Unit / Scope** becomes a dropdown offering all created units, **`Estate Wide`**, and a **custom scope** entry for Vendor SLAs, Warranties, or non-standard scopes.
- **Confirmed Decision 4 — Resident Case Unit Lock**: Residents never pick or edit a case location; both UI and ViewModel automatically bind resident cases to their assigned estate unit.

---

## 1. Overview & Core Concept

- **What It Does**:
  - **Report & Edit Case (Manager Portal)**: Replaces free-form location typing with a Material 3 dropdown listing all existing estate units plus a default **`Estate Wide`** option.
  - **Report & Edit Case (Resident Portal)**: Removes location selection completely and displays a locked read-only residence banner tied automatically to the tenant's assigned unit.
  - **Register Contract**: Upgrades **Tenant Name** (for leases) and **Unit / Scope** (for all contract types) to interactive dropdown selectors with autofill and manual/custom fallback, while retaining existing quick-pick chips for speed.
  - **Documentation Sync**: Updates the project README to reflect these updated case and contract workflows.
- **Target Personas**: Estate Managers triaging property work orders & registering agreements, and Verified Residents submitting unit maintenance cases.
- **Key Value**: Eliminates typos in unit names across cases and contracts, speeds up manager data entry via dropdown autofill, and guarantees every resident case traces cleanly back to the resident's assigned home.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Manager Reporting or Editing a Case**:
   - Opens **Report Case** (or **Edit Case** from the cases table).
   - Sees **Location / Unit (Optional)** rendered as a Material 3 `ExposedDropdownMenuBox` pre-selected to **`Estate Wide`**.
   - Taps the dropdown to select either **`Estate Wide`** or any existing estate unit (displaying unit number and pavilion/occupancy context).
   - Submitting without picking a unit succeeds seamlessly and stores **`Estate Wide`**.

2. **Resident Reporting or Editing a Case**:
   - Opens **Report Case** (or edits an `OPEN` case).
   - Sees a non-interactive **Assigned Unit** card/read-only indicator displaying their assigned unit (e.g., `Unit 701`) and an explanatory note confirming cases are automatically linked to their residence.
   - Cannot alter the unit or choose another location; business logic in the ViewModel also enforces this server-facing unit binding.

3. **Manager Registering a Contract**:
   - Opens **Register Contract** and selects Contract Type (`Tenant Lease`, `Vendor SLA`, or `Equipment Warranty`).
   - **Quick Pick + Tenant Name Dropdown (`Tenant Lease`)**: May either tap a quick-pick chip or open the **Tenant Name \*** dropdown listing registered tenants (name + unit/email hint). Choosing `"Enter Unlisted Tenant"` switches/enables direct manual typing. Selecting a registered tenant automatically fills `Tenant Name`, `Contact Person`, `Tenant Email`, `Unit / Scope`, rental value (if blank), and a starter lease title (if blank).
   - **Quick Pick + Unit / Scope Dropdown (All Contract Types)**: May tap a quick-pick chip or open the **Unit / Scope** dropdown containing **`Estate Wide`**, every created estate unit, and **`Custom Scope...`**. Choosing a unit links the property ID and optional rental defaults; choosing **`Custom Scope...`** reveals/enables a text field for custom areas (e.g., `Main Lobby`, `Backup Generator Bay`).

### Visual Identity & Layout Polish
- **Theme & Typography**: Preserves the existing Tutsville Estate Material 3 palette (`TutsvillePrimary`, `TutsvilleSecondary`, `TutsvilleCardBg`, `TutsvilleOutline`) with serif dialog headers, bold uppercase/small field labels, and rounded `10.dp` outlined inputs.
- **Touch & Accessibility**: All dropdown anchors, quick-pick chips, and action buttons honor the `48.dp` minimum interactive target height, explicit `contentDescription` labels, and `testTag` identifiers for automated verification.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Storing `"Estate Wide"` on Blank Manager Case Locations**
  - *Chosen Approach*: When a manager leaves the optional case location unselected, submit `"Estate Wide"` rather than an empty string.
  - *Why*: Satisfies the user preference (`Default to Estate Wide when blank`) while honoring existing zero-trust Firestore validation (`location.size() >= 1`), requiring zero disruptive database migrations or permissive rule relaxations.
  - *Alternatives Considered*: Allowing empty strings in Firestore rules; rejected because blank locations degrade table readability and audit exports.

- **Decision 2 — Non-Destructive Mode Switching Inside Contract Dropdowns**
  - *Chosen Approach*: Provide dropdown selection alongside a clear inline option (`Enter Unlisted Tenant` / `Custom Scope...`) rather than forcing rigid autocomplete typing or disabling quick-pick chips.
  - *Why*: Managers get one-tap unit/tenant linking and autofill without ever being blocked when recording an external vendor location or pre-onboarding lease.
  - *Alternatives Considered*: Pure read-only dropdowns without custom entry; rejected per confirmed requirement to keep manual/custom scope supported.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Component & State Flow Diagram

```text
┌────────────────────────────────────────────────────────────────────────────┐
│                            MainActivity / Screens                          │
│  Provides: currentRole, currentTenant, properties, tenants, vendors        │
└───────────────┬──────────────────────────────────────────┬─────────────────┘
                │                                          │
                ▼                                          ▼
┌───────────────────────────────┐        ┌───────────────────────────────────┐
│  ReportCaseDialog / EditCase  │        │        AddContractDialog          │
│                               │        │                                   │
│  If MANAGER:                  │        │  • Contract Type Chips            │
│   └─ Unit Dropdown (Optional) │        │  • Quick Pick Tenant/Vendor/Unit  │
│      [Estate Wide | Units...] │        │  • Tenant Name Dropdown (Lease)   │
│                               │        │    [Registered Tenants | Unlisted] │
│  If TENANT:                   │        │  • Unit / Scope Dropdown (All)    │
│   └─ Locked Assigned Unit     │        │    [Estate Wide | Units | Custom] │
│      (No unit selection)      │        │    └─ Autofills Property/Value    │
└───────────────┬───────────────┘        └─────────────────┬─────────────────┘
                │                                          │
                ▼                                          ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                              EstateViewModel                               │
│  • Resolves authoritative resident unit (tenant profile + assigned unit)   │
│  • Enforces assigned unit override on resident case submission & updates    │
│  • Persists linked propertyId, tenantId, tenantEmail & scope on contracts  │
└────────────────────────────────────┬───────────────────────────────────────┘
                                     │
                                     ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                     EstateRepository & Cloud Firestore                     │
│  • /cases/{caseId}       (valid location >= 1 char; manager or owner)      │
│  • /contracts/{id}       (optional propertyId / tenantId / tenantEmail)    │
└────────────────────────────────────────────────────────────────────────────┘
```

### Data & State Mapping Summary

- **Resident Assigned Unit Resolution**:
  - Evaluates assigned unit cleanly in order: assigned property unit on tenant profile -> matching assigned unit lookup across estate properties (by property ID or tenant email) -> fallback address on tenant profile.
  - Enforced both visually (read-only locked display, no selector) and in ViewModel submission/update handlers so tenant-submitted or tenant-edited `OPEN` cases cannot target another unit.
- **Manager Case Location Resolution**:
  - Dropdown maintains `selectedLocation` initialized to `"Estate Wide"` on create (or preserving the existing location on edit alongside `"Estate Wide"` and created units).
  - Unit selection writes the canonical `unitNumber` cleanly to `location` so property case counts and scoped resident visibility continue matching accurately.
- **Contract Form State & Autofill**:
  - `AddContractDialog` tracks dropdown expansion states (`tenantDropdownExpanded`, `unitDropdownExpanded`) and manual override flags (`useCustomTenantName`, `useCustomUnitScope`) synchronized seamlessly with the existing `Quick Pick` chips so tapping either a chip or a dropdown item stays in sync.
  - Clearing unit or picking `"Estate Wide"` clears stale `selectedPropertyId` while keeping user-entered terms and dates intact.
- **Documentation Update**:
  - Project documentation in the root README will be updated after implementation and compilation verification to describe the manager optional case location dropdown, resident unit lock, contract Tenant Name & Unit/Scope dropdowns, and quick-pick + manual fallback behavior.

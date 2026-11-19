# Tutsville Estate — Configurable Operations & Announcement Unification

Standardizes estate terminology around **Announcements** instead of Advisories and replaces hardcoded form fields across Tenants, Amenities, Cases, and Scheduled Events with live, manager-editable dropdowns and option managers backed by Firestore.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The decisions below reflect the preferences you confirmed in Phase 1 along with recommended defaults for backward compatibility. Please review and approve before implementation begins.

- **Confirmed Decision 1 — Optional-First Amenity Creation**: Only **Amenity Name** will be required to create or update an amenity. Category Tag, Capacity, Location, Description, Included Features, Time Slots, and Rules are all optional and will never block saving.
- **Confirmed Decision 2 — Add Tenant Unit Selection**: The **Address / Unit** field becomes a dropdown of existing units in the system with an inline **"+ Create New Unit"** action. Picking or creating a unit saves the unit reference on the tenant profile (and adds any newly created unit to the Units directory as Available), while formal lease assignment remains a separate workflow.
- **Confirmed Decision 3 — Separate Discipline Lists**: **Case Category Disciplines** (Report/Edit Case) and **Engineering Disciplines** (Schedule Event) are stored and managed as two independent, manager-customizable lists, separate from Contractor Disciplines.
- **Recommended Default — Full Create / Edit / Delete Option Management**: Upgrade the shared option-management dialog used across Units, Contractors, Amenities, Cases, and Events so managers can **add**, **rename/edit**, and **delete** options consistently across the entire app, while keeping legacy records readable if an option is renamed or removed.

---

## 1. Overview & Core Concept

- **What It Does**: Eliminates hardcoded dropdowns and inconsistent labels across Tutsville Estate's management workflows. Managers gain self-service CRUD controls (`Manage Options`, `Manage Times`, `Manage Discipline`) that persist in real time to Firestore and immediately populate dropdown menus across forms.
- **Target Audience / Persona**: Estate Managers configuring operations on the fly without needing app updates, and Residents/Tenants interacting with clear **Announcement** bulletins and up-to-date case categories.
- **Key Value**: Scales estate operations cleanly—new property types, custom amenity categories, flexible operating time slots, and specialized case or engineering disciplines can be added, edited, or retired directly inside the app.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Announcement Terminology Unification**
   - The Dashboard floating action button reads **Post Announcement** (replacing *Post Advisory*).
   - The publish dialog displays **Post Estate Announcement**, **Announcement Title \***, **Announcement Details \***, urgency pills (**Announcement**, **Notice**, **High Priority**), and **Post Announcement** submit action.
   - Notification panel filters (**Announcements**), empty states, clear-all confirmation copy, calendar maintenance banners (**Estate Maintenance Announcement**), and legacy notice badges normalize any previously stored `"Advisory"` label to display **Announcement**.

2. **Add / Edit Tenant — Existing Units Dropdown & Quick Unit Creation**
   - In the Add/Edit Tenant dialog, **Address / Unit** is presented as an interactive Material 3 dropdown listing all units currently in the estate directory (showing unit number and current status badge, e.g., `Available` or `Occupied`).
   - Selecting an existing unit binds that unit to the tenant's profile fields (address and unit reference) without forcing an inline lease upload.
   - A dedicated **"+ Create New Unit"** action sits inside the dropdown (and adjacent quick trigger). Tapping it opens a streamlined sub-dialog requiring only a **Unit Number** (plus optional Floor Elevation dropdown and Monthly Rental). Saving immediately writes the new vacant unit to Firestore, selects it in the Add Tenant dropdown, and returns the manager smoothly to finish creating the tenant.

3. **Add / Edit Amenity — Optional-First Form, Manage Category Tags & Manage Times**
   - **Only Amenity Name** displays a required marker and gates the Save button; all other fields (Category Tag, Capacity, Location, Description, Included Features, Time Slots, Rules) can be left blank.
   - **Category Tag**: Replaced with a dropdown backed by managed amenity tags plus a **Manage Options** button (manager-only) to add, rename, or delete category tags.
   - **Managed Time Slots**: Replaces the hardcoded time preset buttons with a **Manage Times** button (opening the shared Create/Edit/Delete sub-dialog) alongside selectable chips for all managed time slots so managers can toggle which slots apply to the amenity—or leave slots empty if configuring hours later.
   - If an amenity is saved without time slots, residents see a clear informational banner in the booking modal explaining that hours have not yet been configured rather than encountering a crash or invalid state.

4. **Report & Edit Case — Manage Case Category Disciplines**
   - Replaces the hardcoded 3-button category row (`Plumbing`, `Electrical`, `HVAC`) with a clean dropdown populated from the persisted **Case Disciplines** list.
   - Managers see a compact **"Manage Discipline"** action directly beside the field label, launching the Create / Edit / Delete sub-dialog to add new disciplines, edit existing ones, or remove unused ones.
   - Residents reporting a case see the exact up-to-date dropdown list configured by management (without the manager-only *Manage Discipline* button).
   - Case cards and edit dialogs display the stored discipline label cleanly across all views.

5. **Schedule Event — Manage Engineering Disciplines**
   - When scheduling a Maintenance event, the hardcoded 3-button row (`HVAC`, `Elevators`, `Fire Life`) is replaced with a dropdown populated from the persisted **Engineering Disciplines** list.
   - Managers see a **"Manage Discipline"** button right beside the label, opening the same Create / Edit / Delete sub-dialog pattern used across the rest of the app.
   - Scheduled events display the chosen custom engineering discipline on calendar cards and detail views.

### Visual Identity & Theme

- **Aesthetic Direction**: Warm, understated luxury estate management — editorial clarity paired with high-trust operational controls.
- **Color Palette & Mood**: Preserves Tutsville Estate's established Material 3 light palette: deep forest green primary (`TutsvillePrimary`), muted metallic gold accents (`GoldAccent`), warm ivory canvas (`TutsvilleBackground`), crisp card surfaces (`TutsvilleCardBg`), warm neutral outlines (`TutsvilleOutline`), and semantic status tints for Pending, Completed, and Urgent badges.
- **Typography & Hierarchy**: Serif display headers (`FontFamily.Serif`) for dialog titles and section headers paired with clean sans-serif body and uppercase micro-labels for status pills and field headers.
- **Component Styling & Layout**: Standardized Material 3 `ExposedDropdownMenuBox` paired with inline gold-accented `TextButton` triggers (`Manage Options`, `Manage Times`, `Manage Discipline`), 10–12dp rounded corners, minimum `48dp` interactive touch targets, and clear accessibility content descriptions.

### Interactive Feedback & Motion

- Saving, editing, or deleting an option in any management sub-dialog updates the live dropdown immediately and shows a concise confirmation snackbar.
- Inline editing within the option manager toggles a row smoothly into a focused text field with Confirm/Cancel icon buttons (minimum `48dp` touch targets) to prevent accidental edits.
- Quick-creating a unit from the Add Tenant modal displays inline validation (preventing duplicate unit numbers) and auto-selects the newly created unit upon return.
- Form submission buttons show clear disabled states only when truly required fields are missing (e.g., Amenity Name blank).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Unified Create / Update / Delete Option Manager**
  - *Chosen Approach*: Upgrade the existing option-management sub-dialog so every managed list supports **Add**, **Inline Rename/Edit**, and **Delete** in one reusable pattern.
  - *Why*: Delivers the exact consistency requested across Units, Contractors, Amenity Tags, Amenity Times, Case Disciplines, and Engineering Disciplines without duplicating UI logic or diverging behavior.
  - *Alternatives Considered*: Keeping Add/Delete-only and asking managers to delete and re-add items when fixing typos—rejected because the user explicitly requested create, update, and delete support.

- **Decision 2 — Backward-Compatible Custom Labels with Enum Fallbacks**
  - *Chosen Approach*: Keep existing enum keys intact for legacy Firestore records, analytics, and security rules while storing explicit custom label strings (e.g., `categoryLabel` on cases, `disciplineLabel` on maintenance events, and normalizing legacy `"Advisory"` urgency strings on read).
  - *Why*: Guarantees zero breakage for already-published Firestore documents, preserves existing R8 enum safety, and prevents older app versions or historical records from losing their labels.
  - *Alternatives Considered*: Replacing enums completely with raw strings across all collections—rejected due to migration risk with live production records.

- **Decision 3 — Centralized Estate Configuration Document in Firestore**
  - *Chosen Approach*: Extend the existing `estate_config/options` Firestore document with four new optional string arrays (`amenityCategoryTags`, `amenityTimeSlots`, `caseDisciplines`, `engineeringDisciplines`), paired with sensible defaults whenever a field is absent on older documents.
  - *Why*: Single real-time snapshot listener keeps all dropdowns synchronized across every manager and tenant device with minimal Firestore read cost.
  - *Alternatives Considered*: Creating separate Firestore collections per dropdown—rejected as unnecessary read overhead and extra listener complexity.

- **Decision 4 — Firestore Security Rules Alignment for Optional Amenities & New Config Lists**
  - *Chosen Approach*: Update Firestore security rules so `facilities` allows `capacity >= 0` and empty optional fields (`tag`, `location`, `allowedSlots`), and `estate_config` validates the four new string arrays when present.
  - *Why*: Without relaxing the existing `capacity >= 1` rule on `facilities`, saving an amenity with only a Name would be rejected by Firestore at runtime.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       Firestore Live Data Layer                         │
│                                                                         │
│  estate_config/options ──► floorElevations, contractorDisciplines,      │
│                            amenityCategoryTags, amenityTimeSlots,       │
│                            caseDisciplines, engineeringDisciplines      │
│  properties            ──► Unit Directory (Available / Occupied)        │
│  tenants               ──► Tenant Profiles (unit/address link)          │
│  facilities            ──► Amenities (only name required)               │
│  cases                 ──► Case Records (+ custom categoryLabel)        │
│  maintenances          ──► Scheduled Events (+ custom disciplineLabel)  │
│  reminders             ──► Announcements & Activity Notices             │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │ Real-time Snapshot Listeners
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        EstateViewModel (StateFlow)                      │
│  • estateConfig   • properties   • facilities   • cases   • events      │
│  • Option CRUD actions (add / edit / delete per managed list)           │
│  • Quick-create vacant unit action   • Optional-first amenity mutations │
└───────────┬──────────────────┬──────────────────┬───────────────────────┘
            │                  │                  │
            ▼                  ▼                  ▼
┌───────────────────┐ ┌──────────────────┐ ┌──────────────────────────────┐
│  Announcement UI  │ │  Tenant & Unit   │ │  Configurable Manager Forms  │
│  • Dashboard FAB  │ │  • Unit Dropdown │ │  • Amenity Form (Tag + Time) │
│  • Post Dialog    │ │  • Quick-Create  │ │  • Report / Edit Case Form   │
│  • Notice Panel   │ │    Unit Sub-Form │ │  • Schedule Event Form       │
│  • Calendar Banner│ └──────────────────┘ └──────────────┬───────────────┘
└───────────────────┘                                     │
                                                          ▼
                                           ┌──────────────────────────────┐
                                           │  Shared ManageOptionsDialog  │
                                           │  • Add new item              │
                                           │  • Inline Edit / Rename item │
                                           │  • Delete item (w/ guard)    │
                                           └──────────────────────────────┘
```

### Data Model & State

- **Estate Configuration (`EstateConfigOptions`)**: Expands with four persisted lists alongside existing `floorElevations` and `contractorDisciplines`:
  - `amenityCategoryTags` (defaults: e.g., `Wellness`, `Recreation`, `Dining & Lounge`, `Sports`, `Business`, `Estate Amenity`)
  - `amenityTimeSlots` (defaults: `08:00 AM - 10:00 AM`, `10:00 AM - 12:00 PM`, `12:00 PM - 02:00 PM`, `02:00 PM - 04:00 PM`, `04:00 PM - 06:00 PM`, `06:00 PM - 08:00 PM`, `08:00 PM - 10:00 PM`)
  - `caseDisciplines` (defaults seeded from standard `CaseCategory` labels)
  - `engineeringDisciplines` (defaults seeded from standard `MaintenanceDiscipline` labels)
- **Amenity (`Facility`)**: `name` is the sole mandatory field; `capacity` defaults safely to `0` when omitted (with booking dialog falling back gracefully if capacity is unspecified), and `tag`, `location`, `description`, `amenities`, `allowedSlots`, and `rules` accept empty values cleanly.
- **Case (`CaseItem`)**: Adds `categoryLabel` (defaulting to `category.label` for legacy records) and computed `effectiveCategoryLabel`, mapping any custom discipline to the closest matching enum value while displaying and storing the exact manager-defined label.
- **Scheduled Event (`MaintenanceEvent`)**: Adds `disciplineLabel` (defaulting to `discipline.label` for legacy records) and computed `effectiveDisciplineLabel`, preserving exact manager-defined engineering disciplines.
- **Announcements (`ReminderItem` & `NoticeTriggerEvent`)**: Keeps enum identifier `MANUAL_ADVISORY` intact for Firestore compatibility while updating its user-facing display label to `"Announcement"` and defaulting new manual posts to urgency `"Announcement"`.

### Interactive Component & State Mapping

- **Shared Option Manager (`ManageOptionsSubDialog`)**:
  - *Add*: Trims input, prevents case-insensitive duplicates, appends to list, calls `onUpdateList(updatedList)` -> writes to Firestore `estate_config/options`.
  - *Edit/Rename*: Tapping the Edit icon turns the option row into an inline text field; confirming validates non-blank & uniqueness, replaces the item in place, updates the parent dialog's currently selected value if it matched the renamed item, and persists to Firestore.
  - *Delete*: Removes item (enforcing optional minimum count, e.g., at least 1 item for required selectors like Disciplines, or 0 allowed for optional lists like Amenity Tags/Times), adjusts selection if the deleted item was active, and persists to Firestore.
- **Add/Edit Tenant Unit Picker**:
  - Reads live `properties` list; displays unit dropdown + `"+ Create New Unit"` menu item.
  - Submitting the inline Quick-Create Unit sub-dialog validates unique `unitNumber` (length >= 2), creates a vacant `PropertyItem` via ViewModel/Repository, awaits confirmation/ID, populates the tenant's `address` / `assignedPropertyUnit` & `assignedPropertyId`, and leaves lease contract assignment untouched.
- **Add/Edit Amenity Dialogs (Both Amenities Screen & Management Dialog)**:
  - Save enabled whenever `name.trim().length >= 2`.
  - Category Tag uses dropdown populated from `estateConfig.amenityCategoryTags` + `"Manage Options"` trigger.
  - Time Slots displays toggleable chips from `estateConfig.amenityTimeSlots` + `"Manage Times"` trigger (and keeps any custom slots already saved on an existing amenity visible so nothing is lost on edit).
- **Report Case & Edit Case Dialogs**:
  - Category Discipline uses dropdown populated from `estateConfig.caseDisciplines`.
  - When `currentRole == UserRole.MANAGER`, displays `"Manage Discipline"` trigger next to the field label opening the Create/Edit/Delete sub-dialog.
- **Schedule Event Dialog**:
  - Engineering Discipline uses dropdown populated from `estateConfig.engineeringDisciplines` + `"Manage Discipline"` trigger for managers.

### Firestore Security Rules & Verification Strategy

- Update Firestore security rules and schema blueprint to:
  1. Allow `facilities` documents with `capacity >= 0` (when present) and empty optional `tag`, `location`, and `allowedSlots`.
  2. Validate optional arrays (`amenityCategoryTags`, `amenityTimeSlots`, `caseDisciplines`, `engineeringDisciplines`) in `estate_config`.
  3. Permit optional `categoryLabel` on `cases` and `disciplineLabel` on `maintenances`.
- Verify all rule changes locally against the Firestore Rules test suite on the emulator, deploy updated rules, and validate compilation with a full Android build.

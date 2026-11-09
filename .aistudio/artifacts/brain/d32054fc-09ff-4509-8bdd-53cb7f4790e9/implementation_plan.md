# Tutsville Estate — Auth-Managed Role Profile & Hero Sign-In Screen (Revised)

This revised plan incorporates your feedback: preserving the 3-word subtitle **"Operations Stewardship Portal"** positioned directly above the Google sign-in button, while the golden **Tutsville Estate** logo card dominates the top half of the screen. In the main application, the manual role switcher is replaced by an authenticated user profile bar.

### User Review & Critical Decisions

> [!IMPORTANT]
> - **Sign-In Screen Layout**:
>   - **Top Half**: The official **Tutsville Estate** logo card occupies the majority of the top half in a luxury black card with gold border.
>   - **Bottom Half**: Keeps the refined one-line title **"Operations Stewardship Portal"** positioned close to the **Sign in with Google** button, with the long descriptive paragraph removed.
> - **Role Switcher Replaced**: The manual `(Manager | Resident | Vendor)` toggle is removed. Signed-in users are automatically treated as **Estate Managers** with full administrative privileges.
> - **User Profile Bar**: Directly below the header logo card in the main app, a sleek profile bar displays the authenticated account email, an **Estate Manager** gold badge, and a **Sign Out** button.

---

### 1. Overview & Core Concept

- **What It Does**: Unifies user access under authenticated Google credentials while providing a grand, editorial brand presence.
- **Target Persona**: Property managers and estate operations directors managing maintenance, cases, contracts, and facilities.
- **Key Value**: Replaces artificial role toggling with real authentication, while establishing an immediate luxury estate identity.

---

### 2. User Experience & Visual Design

#### A. Sign-In Screen (Unauthenticated View)

```
┌────────────────────────────────────────────────────────┐
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │                                                  │  │
│  │                                                  │  │
│  │              [ Luxury Black Card ]               │  │
│  │                Tutsville Estate                  │  │
│  │             (Dominates Top Half)                 │  │
│  │                                                  │  │
│  │                                                  │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│             Operations Stewardship Portal              │
│                                                        │
│         [ 🛡️  Sign in with Google ]                  │
│                                                        │
└────────────────────────────────────────────────────────┘
```

1. **Top Half — Hero Logo Card**:
   - Elevated black card with gold hairline accent (`BorderStroke(1.dp, GoldAccent.copy(alpha = 0.55f))`).
   - Occupies ~45–50% of the screen height, showcasing the high-resolution golden script calligraphy.
2. **Bottom Section — Action Area**:
   - One-line title: **"Operations Stewardship Portal"** in deep evergreen serif typography (`TutsvillePrimary`), positioned directly above the sign-in button.
   - Prominent **Sign in with Google** button with clear touch target and loading spinner during authorization.

#### B. Main Application Header (Authenticated View)

```
┌────────────────────────────────────────────────────────┐
│ ┌────────────────────────────────────────────────────┐ │
│ │  [ 🔔 ]            Tutsville Estate                │ │
│ │                  (Full-Width Luxury Card)          │ │
│ └────────────────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────────────────┐ │
│ │ 👤 user@email.com   [ Estate Manager ]  [ Sign Out]│ │
│ └────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

1. **Full-Width Logo Card**: Golden script emblem centered with unread notification bell.
2. **Authenticated Profile Bar**:
   - Compact luxury pill surface (`#F6F3EB` / `#FFFFFF` with warm gold border).
   - Displays user email, gold `Estate Manager` role badge, and `Sign Out` icon button.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Title Placement Close to Sign-In**:
  - *Chosen Approach*: Place "Operations Stewardship Portal" right above the sign-in button rather than grouped in the top logo card.
  - *Why*: Gives the logo card maximum breathing room in the top half, while anchoring the action button with its operational purpose.
- **Decision 2: Automated Manager Permissions**:
  - *Chosen Approach*: Default all signed-in users to `Role.MANAGER`.
  - *Why*: Removes unnecessary role switching so property directors have instant access to dispatching vendors, scheduling maintenance, approving bookings, and managing contracts.

---

### 4. Technical Architecture & Modifications

- **`AuthScreen.kt`**:
  - Adjust layout to dedicate the top portion (`fillMaxHeight(0.48f)` or weighted layout) to the black elevated card featuring `tutsville_estate_logo_trimmed`.
  - Place `Text("Operations Stewardship Portal", ...)` immediately above the Google Sign-In button with a tight 8dp spacer.
  - Ensure error messages and loading spinners are cleanly contained.
- **`MainActivity.kt`**:
  - Replace `RoleSwitcherBar` with `UserProfileHeaderBar`.
  - Pass `Role.MANAGER` as active role across all tab views.
  - Implement sign-out dialog / action handler that clears credentials and updates auth state.
- **Verification**:
  - Run `compile_applet` and test suite `gradle :app:testDebugUnitTest`.

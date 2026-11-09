# Tutsville Estate — Auth-Managed Role Profile & Hero Sign-In Screen

This plan details retiring the manual `(Manager | Resident | Vendor)` role switcher in favor of authenticating users as Estate Managers by default, adding a sleek authenticated user profile chip beneath the header logo, and expanding the brand logo to dominate the top half of the Sign-In screen.

### User Review & Critical Decisions

> [!IMPORTANT]
> - **Role Switcher Retired**: The manual 3-way toggle is removed. All authenticated users are automatically granted full **Estate Manager** privileges across all 8 workflows.
> - **Authenticated Profile Bar**: Directly below the full-width header logo card, a luxury user profile bar displays the signed-in Google account email, an **Estate Manager** badge, and a **Sign Out** button.
> - **Hero Sign-In Experience**: On the sign-in page, the title text and descriptive paragraph are removed. The golden script **Tutsville Estate** logo card expands to occupy the majority of the top half of the screen, creating an immediate editorial aesthetic before the Google Sign-In button.

---

### 1. Overview & Core Concept

- **What It Does**: Shifts role handling from a manual interactive segmented control to automated identity management via Google Authentication.
- **Target Persona**: Property directors, operations coordinators, and estate managers handling day-to-day stewardship.
- **Key Value**: Streamlines the UI, removes confusing manual role switching, and provides a polished, luxury brand identity on first load.

---

### 2. User Experience & Visual Design

#### A. Main Application Header (Authenticated View)

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

1. **Full-Width Logo Card**: Remains prominent at the top with the golden script emblem and notification bell.
2. **User Profile Bar**:
   - Displays user avatar / Google icon and email.
   - Shows a caramel gold styled `Estate Manager` badge.
   - Includes a discreet, accessible `Sign Out` button with confirmation.

#### B. Sign-In Screen (Unauthenticated View)

```
┌────────────────────────────────────────────────────────┐
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │                                                  │  │
│  │                                                  │  │
│  │                [ Luxury Black Card ]             │  │
│  │                  Tutsville Estate                │  │
│  │               (Dominates Top Half)               │  │
│  │                                                  │  │
│  │                                                  │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│           [ 🛡️  Sign in with Google ]                │
│                                                        │
└────────────────────────────────────────────────────────┘
```

1. **Hero Logo Card**:
   - Takes up ~50% of the screen height in a luxury black card with gold hairline border and subtle elevation.
   - Logo scaled to fit with optimal clarity and contrast.
2. **Authentication Action**:
   - Positioned cleanly directly beneath the hero card.
   - No cluttering text or paragraphs; clean, immediate, high-end login flow.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Direct Manager Capabilities**:
  - *Chosen Approach*: Authenticated users get instant access to all management features (work orders, vendor assignment, contracts, scheduled maintenance, facility reservations, broadcast alerts).
  - *Why*: In operations apps, authenticated staff need immediate full capability rather than toggling personas.
- **Decision 2: Dedicated Profile & Sign-Out Bar**:
  - *Chosen Approach*: Replace the role switcher container with an identity bar showing email and role.
  - *Why*: Clearly communicates the active authenticated state and provides an intuitive sign-out affordance.

---

### 4. Technical Architecture & Modifications

- **`MainActivity.kt`**:
  - Remove `RoleSwitcherBar` composable and references.
  - Implement `UserProfileHeaderBar(user = currentUser, onSignOut = { ... })`.
  - Fix default role to `Role.MANAGER`.
- **`AuthScreen.kt`**:
  - Remove the portal title text and description.
  - Adjust container layout: Box with top hero logo card occupying the top half (`Modifier.fillMaxWidth().weight(1f)` or `fillMaxHeight(0.48f)`), followed by the Google Sign-In button and loading states.
- **Verification**:
  - Run `compile_applet` and test suite `gradle :app:testDebugUnitTest`.

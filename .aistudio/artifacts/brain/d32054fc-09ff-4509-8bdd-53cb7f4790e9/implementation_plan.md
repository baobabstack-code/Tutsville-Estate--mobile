# Tutsville Estate — Modern Form Layout with Bottom Google Button

This plan details adopting the reference layout requested by the user: clean standalone labels above the input fields, a pill-shaped primary **Sign In [→]** action button, **"Forgot Password"** and **"Sign Up"** links, removing the duplicate top Google button, and featuring a dedicated Google authentication capsule button at the bottom without Facebook or X.

### User Review & Critical Decisions

> [!IMPORTANT]
> - **Reference Form Layout**:
>   - **Standalone Field Labels**: *"Email Address"* and *"Password"* displayed directly above their respective input containers in crisp typography.
>   - **Pill-Shaped Action Button**: Full-width rounded pill button labeled **"Sign In [→]"** (or **"Sign Up [→]"** in registration mode) using Tutsville's deep evergreen `#183B2B` with gold accents.
>   - **Helper Links**:
>     - *"Don't have an account? Sign Up"* / *"Already have an account? Sign In"*
>     - *"Forgot Password"* (triggers a password reset dialog / email dispatch via Firebase Auth).
> - **Remove Duplicate Google Button**: The previous top Google button is removed.
> - **Bottom Google Capsule Button**: A sleek, pill-shaped Google button is positioned in the bottom row (with no Facebook or X icons). Tapping it launches the verified Firebase Google Sign-In flow.
> - **Hero Branding & Framing Preserved**: The grand black Tutsville Estate golden script hero card and caramel gold perimeter frame remain intact at the top.

---

### 1. Overview & Core Concept

- **What It Delivers**: Streamlines the sign-in experience into a modern, industry-standard form layout matching the user's reference mockup while preserving Tutsville Estate's luxury visual identity.
- **Key Value**: Direct focus on credentials, clean visual labels, seamless Google sign-in without duplicates, and password recovery capability.

---

### 2. User Experience & Visual Design

```
┌────────────────────────────────────────────────────────┐  ◄── Screen Edge
│  ┌──────────────────────────────────────────────────┐  │  ◄── Gold Hairline Frame
│  │                                                  │  │
│  │  ┌────────────────────────────────────────────┐  │  │
│  │  │                                            │  │  │
│  │  │            [ Luxury Black Card ]           │  │  │
│  │  │              Tutsville Estate              │  │  │
│  │  │                                            │  │  │
│  │  └────────────────────────────────────────────┘  │  │
│  │                                                  │  │
│  │          ▲                                       │  │
│  │          │   Generous Luxury Gap                 │  │
│  │          ▼                                       │  │
│  │                                                  │  │
│  │     Email Address                                │  │
│  │     ┌──────────────────────────────────────────┐ │  │
│  │     │ ✉️ elementary221b@gmail.com              │ │  │
│  │     └──────────────────────────────────────────┘ │  │
│  │                                                  │  │
│  │     Password                                     │  │
│  │     ┌──────────────────────────────────────────┐ │  │
│  │     │ 🔒 ••••••••••••••••                   👁️ │ │  │
│  │     └──────────────────────────────────────────┘ │  │
│  │                                                  │  │
│  │     (                   Sign In [→]            ) │  │  ◄── Pill Button
│  │                                                  │  │
│  │            Don't have an account? Sign Up        │  │
│  │                   Forgot Password                │  │
│  │                                                  │  │
│  │                    (    [G]    )                 │  │  ◄── Google Capsule Button
│  │                                                  │  │
│  │     🔒 Secured with Firebase Firestore           │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Standalone Field Labels**:
  - *Chosen Approach*: Place bold, legible category labels ("Email Address", "Password", "Full Name") above the inputs rather than relying solely on floating placeholder hints.
  - *Why*: Directly matches the user's reference design and improves accessibility and visual scanning.
- **Decision 2: Single Bottom Google Button**:
  - *Chosen Approach*: Remove the redundant top Google button and place an interactive Google capsule button centered at the bottom.
  - *Why*: Avoids cluttering the interface with multiple Google buttons while strictly respecting the user's request to omit Facebook and X.
- **Decision 3: Password Recovery Integration**:
  - *Chosen Approach*: Connect "Forgot Password" to Firebase Auth `sendPasswordResetEmail(email)`.
  - *Why*: Provides complete self-service recovery for users signing in with email.

---

### 4. Technical Architecture & Modifications

- **`AuthScreen.kt`**:
  - Remove top Google Sign-In button and divider.
  - Redesign form inputs with external labels:
    - Text label: `Text("Email Address", fontWeight = FontWeight.Bold, ...)`
    - Input: `OutlinedTextField` with rounded corners (28dp for pill effect or 16dp rounded rectangle).
    - Text label: `Text("Password", fontWeight = FontWeight.Bold, ...)`
    - Input: Password `OutlinedTextField` with leading lock icon and trailing eye toggle icon.
  - Primary button:
    - `Button(shape = CircleShape / RoundedCornerShape(26.dp))` with text `"Sign In"` and trailing icon `Icons.AutoMirrored.Filled.Login`.
  - Text links:
    - `"Don't have an account? Sign Up"` / `"Already have an account? Sign In"`
    - `"Forgot Password"` opening a password reset prompt.
  - Bottom action row:
    - Centered capsule button with Google G icon (`Surface(shape = CircleShape / RoundedCornerShape(20.dp), border = BorderStroke(1.dp, TutsvilleOutline))`) triggering `signInWithGoogle`.
- **Verification**:
  - Run `compile_applet`.
  - Run `gradle :app:testDebugUnitTest`.

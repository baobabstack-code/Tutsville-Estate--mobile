# Tutsville Estate — Layout Redistribution & Bottom-Anchored Sign-In

This plan details redesigning the vertical composition of the gold-framed sign-in portal: preserving the official **Tutsville Estate** logo card at the top, restoring the generous luxury gap between the logo and authentication controls, and pushing all sign-in elements down to the bottom of the screen to eliminate the empty whitespace at the bottom.

### User Review & Critical Decisions

> [!IMPORTANT]
> - **Preserve Top Logo Design**: The elevated black hero card with golden script **Tutsville Estate** calligraphy and gold hairline border remains untouched at the top of the frame.
> - **Restore Generous Luxury Gap**: Replace the tight spacing with a flexible layout (`Arrangement.SpaceBetween` / weighted spacer) that pushes all sign-in inputs and action buttons down to the bottom of the gold frame, matching the refined spacing from earlier.
> - **Eliminate Awkward Bottom Whitespace**: The bottom section (Operations Stewardship Portal title, Google Sign In, email inputs, Sign In button, and security tag) will now anchor naturally at the base of the frame.
> - **Scroll & Keyboard Resilience**: Ensure `BoxWithConstraints` or minimum height constraints with `verticalScroll` so the layout remains beautifully anchored on large screens while smoothly scrolling on smaller screens or when the software keyboard appears.

---

### 1. Root Cause & Solution

- **Previous Issue**: Adding email/password input fields inside a simple `verticalScroll` column without bottom anchoring caused all controls to bunch up immediately beneath the logo card, leaving a large, unsightly empty expanse at the bottom of the frame.
- **Redesigned Solution**: Structure the framed container with two distinct regions separated by a flexible spacer:
  1. **Top Anchor**: Tutsville Estate luxury black logo card.
  2. **Flexible Luxury Gap**: Naturally expands to fill available viewport height.
  3. **Bottom Anchor**: The entire authentication section cleanly docked at the bottom of the gold perimeter frame.

---

### 2. User Experience & Visual Layout

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
│  │          │   Generous Luxury Gap (Restored)      │  │
│  │          ▼                                       │  │
│  │                                                  │  │
│  │            Operations Stewardship Portal          │  │
│  │        [ 🛡️  Sign in with Google ]                │  │
│  │             ─── or continue with email ───        │  │
│  │     [ Email Address                       ]      │  │
│  │     [ Password                            ]      │  │
│  │     [   Sign In with Email                ]      │  │
│  │     [ Don't have an account? Create one   ]      │  │
│  │     🔒 Secured with Firebase Firestore           │  │
│  └──────────────────────────────────────────────────┘  │  ◄── Pushed to Bottom
└────────────────────────────────────────────────────────┘
```

---

### 3. Technical Changes

- **`AuthScreen.kt`**:
  - Implement a `BoxWithConstraints` layout inside the gold frame.
  - Set the scrollable content with `Modifier.fillMaxSize().verticalScroll(scrollState).padding(18.dp)` and minimum height equal to `maxHeight - 36.dp`.
  - Use `Column(modifier = Modifier.fillMaxSize().defaultMinSize(minHeight = maxHeight - 36.dp), verticalArrangement = Arrangement.SpaceBetween)`:
    - **Top element**: `ElevatedCard` hero logo card (height 180dp–220dp with golden script).
    - **Bottom element**: Authentication container containing the title, diagnostic alerts, Google Sign In, email/password form, action button, toggle button, and security tag.
- **Verification**:
  - Run `compile_applet`.
  - Run `gradle :app:testDebugUnitTest`.

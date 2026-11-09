# Tutsville Estate — Inset Luxury Gold Border on Sign-In Screen

This plan details adding a refined caramel gold luxury hairline border around the perimeter of the sign-in screen, giving the entire authentication portal an elevated, editorial framed presentation.

### User Review & Critical Decisions

> [!IMPORTANT]
> - **Border Style & Hue**: Refined caramel gold hairline border (`GoldAccent.copy(alpha = 0.65f)`, 1.5dp stroke, rounded corners 24dp).
> - **Placement & Scope**: Inset frame framing the entire sign-in screen with an intentional 12dp outer margin from the device edges and status/navigation bars, creating a bespoke luxury passport/invitation aesthetic.
> - **Content Harmony**: The hero logo card and bottom Google sign-in actions remain elegantly arranged inside the gold-framed container.

---

### 1. Overview & Core Concept

- **What It Delivers**: An editorial inset border that encases the sign-in screen in warm linen `#F6F3EB` and caramel gold `#C29047`, making the portal look like an exclusive estate concierge pass.
- **Visual Distinction**: Eliminates plain edge bleed by giving the viewport a structured architectural perimeter.

---

### 2. User Experience & Visual Design

```
┌────────────────────────────────────────────────────────┐  ◄── Screen Edge
│  ┌──────────────────────────────────────────────────┐  │  ◄── Inset Gold Border (1.5dp)
│  │                                                  │  │
│  │  ┌────────────────────────────────────────────┐  │  │
│  │  │                                            │  │  │
│  │  │            [ Luxury Black Card ]           │  │  │
│  │  │              Tutsville Estate              │  │  │
│  │  │            (Golden Script Logo)            │  │  │
│  │  │                                            │  │  │
│  │  └────────────────────────────────────────────┘  │  │
│  │                                                  │  │
│  │            Operations Stewardship Portal          │  │
│  │                                                  │  │
│  │        [ 🛡️  Sign in with Google ]                │  │
│  │                                                  │  │
│  │     🔒 Secured with Firebase Firestore           │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

1. **Outer Viewport Inset**:
   - The screen root applies window insets padding for system bars, then draws a padded frame (12dp margin).
2. **Gold Hairline Border**:
   - `BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.65f))` on an elevated or bordered container with smooth 24dp rounded corners.
   - Retains the warm linen background (`TutsvilleBackground`).
3. **Internal Content**:
   - The top hero black card sits comfortably within the framed interior with 16dp inner padding.
   - The "Operations Stewardship Portal" title and "Sign in with Google" button are nestled in the lower section of the framed layout.

---

### 3. Technical Architecture & Modifications

- **`AuthScreen.kt`**:
  - Outer `Box(modifier = Modifier.fillMaxSize().background(TutsvilleBackground).windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp))`
  - Inside, an enclosing `Surface` or `Box` with `Modifier.fillMaxSize().border(1.5.dp, GoldAccent.copy(alpha = 0.65f), RoundedCornerShape(24.dp)).padding(16.dp)`
  - Encapsulates the top hero logo card and the bottom Google sign-in section.
- **Verification**:
  - Run `compile_applet` to ensure seamless compilation.
  - Run `gradle :app:testDebugUnitTest` to verify test suite health.

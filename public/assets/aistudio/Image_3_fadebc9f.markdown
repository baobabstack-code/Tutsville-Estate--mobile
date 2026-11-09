---
name: Tutsville Property Operations
colors:
  surface: '#fcf9f1'
  surface-dim: '#dcdad2'
  surface-bright: '#fcf9f1'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f6f3eb'
  surface-container: '#f1eee6'
  surface-container-high: '#ebe8e0'
  surface-container-highest: '#e5e2da'
  on-surface: '#1c1c17'
  on-surface-variant: '#414844'
  inverse-surface: '#31312b'
  inverse-on-surface: '#f3f1e9'
  outline: '#727973'
  outline-variant: '#c1c8c2'
  surface-tint: '#436653'
  primary: '#002517'
  on-primary: '#ffffff'
  primary-container: '#183b2b'
  on-primary-container: '#80a690'
  inverse-primary: '#a9cfb9'
  secondary: '#7f5610'
  on-secondary: '#ffffff'
  secondary-container: '#fdc576'
  on-secondary-container: '#785009'
  tertiary: '#002517'
  on-tertiary: '#ffffff'
  tertiary-container: '#0f3c2a'
  on-tertiary-container: '#7aa78f'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#c4ecd4'
  primary-fixed-dim: '#a9cfb9'
  on-primary-fixed: '#002113'
  on-primary-fixed-variant: '#2b4e3d'
  secondary-fixed: '#ffddb3'
  secondary-fixed-dim: '#f4bd6f'
  on-secondary-fixed: '#291800'
  on-secondary-fixed-variant: '#633f00'
  tertiary-fixed: '#beedd3'
  tertiary-fixed-dim: '#a3d1b8'
  on-tertiary-fixed: '#002114'
  on-tertiary-fixed-variant: '#244f3c'
  background: '#fcf9f1'
  on-background: '#1c1c17'
  surface-variant: '#e5e2da'
typography:
  headline-xl:
    fontFamily: Playfair Display
    fontSize: 44px
    fontWeight: '600'
    lineHeight: 52px
    letterSpacing: -0.02em
  headline-xl-mobile:
    fontFamily: Playfair Display
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.015em
  headline-lg:
    fontFamily: Playfair Display
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Playfair Display
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Playfair Display
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-overline:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.08em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-tablet: 1.25rem
  gutter-desktop: 1.5rem
  margin: 1rem
  margin-tablet: 1.5rem
  margin-desktop: 2.5rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style
The design system articulates an architectural, high-trust operational ecosystem for modern real estate stewardship. Merging the restrained sophistication of heritage editorial design with the agility of utility-focused mobile interfaces, it serves property managers, asset owners, tenants, and maintenance vendors.

The aesthetic philosophy draws on contemporary editorial minimalism:
- **Atmospheric Warmth:** Replacing harsh utilitarian gray canvases with warm linen and cream tones, creating an inviting yet authoritative atmosphere.
- **Architectural Gravity:** Grounded by deep evergreen tones that establish stability, contrasted with refined caramel gold for critical status markers and highlights.
- **Editorial Typographic Hierarchy:** High-contrast serif headlines evoke permanence and curated clarity, while modern humanist sans-serif body typography guarantees scannable legibility across operational views and dense work orders.

## Colors
The color palette creates a serene, grounded visual structure designed for clear daylight readability and reduced cognitive fatigue.

- **Primary (`#183B2B`):** Deep evergreen forest. Applied to high-emphasis interactive surfaces, active tab indicators, primary action triggers, and brand framing elements.
- **Secondary (`#C29047`):** Warm burnished caramel. Serves as a focused operational accent for priority highlights, pending verifications, badge accents, and overline categorizations.
- **Tertiary (`#234E3B`):** Mid-tone cedar green. Utilized for secondary action states, pressed variations, and subdued structural fills.
- **Neutral Surface Palette:**
  - Base canvas background: `#F6F3EB` (Warm Linen)
  - Secondary canvas / card elevated surfaces: `#FFFFFF` (Crisp Pure White)
  - Subtle borders and hairline divides: `#E6E1D5`
  - High-emphasis body text: `#1C1E1D`
  - Secondary metadata & captions: `#5F6360`
  - Operational states: Soft sage for completed (`#EAF2EC`, text `#1B4D2E`), amber tint for pending attention (`#FDF6EB`, text `#8C5E1B`), and muted brick for overdue incidents (`#FAF0F0`, text `#8A2E2E`).

## Typography
Typographic expression bridges traditional estate ledgers with mobile clarity:
- **Headlines:** Set in `Playfair Display`. Reserved for major module entry points, property headers, portfolio rollups, and sheet modal crowns. It projects architectural permanence and high craft.
- **Body & Controls:** Set in `Plus Jakarta Sans`. Provides high legibility for rapid on-site scanning, work ticket specifications, tenant communication, and structured data tables.
- **Overline Badges & Category Trackers:** Transformed to uppercase with `0.08em` tracking in `label-overline` to organize operational queues without visual weight.

## Layout & Spacing
A fluid 4-column layout governs mobile devices, shifting into 8 columns on tablets and 12 columns on desktop admin dashboards.

- **Mobile Viewport Rules:** Fixed `margin` of `1rem` (16px) establishes edge security, while components breathe with `gutter` spacing of `1rem`. Dense operational queues maintain `space-sm` (8px) between sibling items and `space-md` (16px) across cards.
- **Content Stacking:** Operational workflows prioritize finger-accessible vertical scrolling with sticky filter ribbons and bottom command anchors.
- **Tablet & Desktop Reflow:** Grid cards convert from full-width mobile single-column stacks into balanced multi-column cards, preventing excessive horizontal stretch while preserving standard aspect ratios.

## Elevation & Depth
Depth is established primarily through crisp surface boundaries and ambient, warm-tinted separation rather than heavy synthetic drop shadows.

- **Base Canvas:** `#F6F3EB` acts as the recessed environmental baseline.
- **Elevated Cards & Sheets:** `#FFFFFF` resting directly on the canvas, bounded by a continuous hairline border: `1px solid #E6E1D5`.
- **Card Ambient Hover & Touch Response:** Subtle elevation is introduced using an organic low-opacity shadow tinted with the primary forest hue: `0 4px 16px -2px rgba(24, 59, 43, 0.05)`.
- **Modals, Drawers & Action Sheets:** Elevated to top-tier depth with soft layered diffusion: `0 12px 32px -4px rgba(24, 59, 43, 0.12)`, underpinned by a semi-opaque modal scrim: `rgba(24, 59, 43, 0.35)`.

## Shapes
The design system employs a structured, rounded visual geometry:
- **Base Components (Inputs, Small Badges, Chips):** Use standard 8px (`0.5rem`) corner radiuses for crisp precision.
- **Cards, Surface Panels, & Notification Banners:** Utilize `rounded-xl` (16px to 24px) delivering welcoming, soft-edged containers reminiscent of architectural drafting cards.
- **Pills & Status Triggers:** Small operational metadata pills and RBAC badge containers make selective use of full pill rounding (`9999px`) to distinguish tags from interactive rect cards.

## Components

### Buttons
- **Primary Action Button:** Solid `#183B2B` fill with pure white typography, `rounded-xl` (12px to 16px radius), `48px` minimum touch target height, with `1rem` horizontal padding.
- **Secondary Action Button:** Pure white background with hairline border `1px solid #E6E1D5`, `#183B2B` text, and subtle hover transition to `#FAF8F5`.
- **Tertiary / Utility Link:** Transparent fill, underlined or accompanied by directional glyphs, set in `#183B2B` or `#C29047`.

### RBAC Role Chips & Status Badges
- **Role Chips:** Crisp pills or `rounded-lg` containers with subdued tint backgrounds (e.g., Owner: `#F6F3EB` with `#183B2B` text; Manager: `#FAF3E8` with `#8C5E1B` text; Tenant: `#EAF2EC` with `#1B4D2E` text; Vendor: `#F2F4F7` with `#344054` text).
- **Status Badges:** Compact labels paired with a `6px` circular status indicator dot, specifying states like "In Progress", "Overdue", or "Verified".

### Cards & Structured Lists
- **Operational Cards:** Constructed from `#FFFFFF` background with `1px solid #E6E1D5` border, `rounded-2xl` geometry, and inner padding of `1.25rem` (`space-lg`). Cards feature clear sectional separation: Playfair Display section title, concise metadata summary, and inline status badges.
- **Structured List Rows:** Embedded within white card containers; divided by `1px solid #F0ECE1` separators. Rows feature high touch-target heights (minimum `56px`) with clear chevron indicators for navigation.

### Input Fields & Controls
- **Form Inputs:** Pure white background framed in `1px solid #D9D3C7`, `rounded-lg` (8px), height `44px`, with inset placeholder text in `#7E827F`.
- **Focus State:** Transitions border to `#183B2B` accompanied by an ambient `2px` focus ring tinted at `rgba(24, 59, 43, 0.15)`.
- **Checkboxes & Radios:** Evergreen active fills (`#183B2B`) with crisp white selection checks; unselected states utilize hairline borders on pure white foundations.

### Tab Navigation
- **Segmented App Bar / Bottom Navigation:** Grounded over `#FAF8F5` with `1px` top hairline border. Active icons and labels are rendered in `#183B2B` with a discrete indicator dot; inactive elements rest in muted `#7E827F`.
# Tutsville Estate — Full-Width Top Header Card with Role Switcher Below

This plan details reorganizing the top bar layout so the official **Tutsville Estate** logo takes the entire top section as a prominent full-width card, with the role switcher bar placed directly underneath.

### User Review & Critical Decisions

> [!IMPORTANT]
> - **Full-Width Top Section**: The logo will expand across the entire width of the top screen within a grand, elevated luxury card container.
> - **Role Switcher Directly Underneath**: The 3-role segmented switcher (`Manager` | `Resident` | `Vendor`) will be positioned directly below the full-width logo card, providing a clear visual hierarchy.
> - **Notification Alert**: Integrated seamlessly as an action icon within the top header card so users can access real-time notices without crowding the logo.

---

### 1. Visual Layout & Component Hierarchy

```
┌────────────────────────────────────────────────────────┐
│               Tutsville Top Section                    │
│ ┌────────────────────────────────────────────────────┐ │
│ │  [ 🔔 ]           Tutsville Estate                 │ │
│ │                  (Full-Width Card)                 │ │
│ └────────────────────────────────────────────────────┘ │
│                          ▼                             │
│ ┌────────────────────────────────────────────────────┐ │
│ │ [ Manager (Admin) ] [ Resident ] [ Vendor (Apex) ] │ │
│ └────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

1. **Full-Width Logo Header Card (`app_brand_logo_card`)**:
   - Spans `fillMaxWidth()` with 16dp rounded corners and 4dp elevation.
   - Centered high-resolution golden script **Tutsville Estate** calligraphy.
   - Embedded notification bell icon with unread badge in the top right corner.
2. **Role Switcher Container**:
   - Sits directly below with 10dp vertical spacing.
   - Full-width pill segmented control allowing instant switching between Manager, Resident, and Vendor.

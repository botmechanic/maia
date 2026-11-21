---
name: Warm Architectural Precision
colors:
  surface: '#fff8f4'
  surface-dim: '#e6d8cb'
  surface-bright: '#fff8f4'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#fff1e5'
  surface-container: '#faebde'
  surface-container-high: '#f4e6d9'
  surface-container-highest: '#eee0d3'
  on-surface: '#211a12'
  on-surface-variant: '#56423d'
  inverse-surface: '#372f26'
  inverse-on-surface: '#fdeee1'
  outline: '#89726c'
  outline-variant: '#dcc0ba'
  surface-tint: '#9d4229'
  primary: '#903820'
  on-primary: '#ffffff'
  primary-container: '#af4f35'
  on-primary-container: '#ffeae5'
  inverse-primary: '#ffb5a1'
  secondary: '#556348'
  on-secondary: '#ffffff'
  secondary-container: '#d8e8c6'
  on-secondary-container: '#5a694e'
  tertiary: '#774a00'
  on-tertiary: '#ffffff'
  tertiary-container: '#93621b'
  on-tertiary-container: '#ffecd9'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdbd1'
  primary-fixed-dim: '#ffb5a1'
  on-primary-fixed: '#3c0800'
  on-primary-fixed-variant: '#7e2b14'
  secondary-fixed: '#d8e8c6'
  secondary-fixed-dim: '#bcccab'
  on-secondary-fixed: '#131f0a'
  on-secondary-fixed-variant: '#3d4b32'
  tertiary-fixed: '#ffddb8'
  tertiary-fixed-dim: '#fabb6c'
  on-tertiary-fixed: '#2a1700'
  on-tertiary-fixed-variant: '#653e00'
  background: '#fff8f4'
  on-background: '#211a12'
  surface-variant: '#eee0d3'
typography:
  headline-xl:
    fontFamily: Plus Jakarta Sans
    fontSize: 34px
    fontWeight: '700'
    lineHeight: 42px
    letterSpacing: -0.03em
  headline-xl-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 38px
    letterSpacing: -0.025em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 26px
    fontWeight: '600'
    lineHeight: 34px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.015em
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 17px
    fontWeight: '400'
    lineHeight: 26px
    letterSpacing: -0.01em
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 22px
    letterSpacing: -0.005em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0em
  caption:
    fontFamily: Plus Jakarta Sans
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 18px
    letterSpacing: 0.01em
  numeric-metric:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.02em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1.25rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system establishes an intimate, serene, and cryptographically reassuring atmosphere for an on-device personal companion. The aesthetic fuses the meticulous, high-fidelity tactile precision of top-tier Swiss-inspired neobanks with the restorative warmth of private, tactile bookbinding and archival stationery.

### Personality & Emotional Tenor
- **Impenetrable yet Gentle:** Security is conveyed not through austere militaristic tropes, but through clean, pristine mechanical precision and calm silence.
- **Uncluttered Introspection:** Generous warm whitespace invites candid journaling and deliberate contemplation.
- **Organic Precision:** Geometric geometry meets earthy hearth undertones, balancing computational speed with organic human presence.

### Visual Architecture Rules
- Strictly eliminate all human avatars, illustrative cartoon companions, or stock photography. The product’s persona is embodied exclusively by abstract mathematical geometry—primarily the radiant organic orb.
- Visual weight leans on pure spatial proportion, razor-thin hairlines, and deliberate editorial typographic scaling rather than decorative embellishments.

## Colors

The palette grounds high-technology privacy within warm, tactile physical materials—evoking heavyweight cold-pressed paper, linen book cloth, terracotta pottery, and natural dried sage.

### Functional Chromatic Discipline
- **Primary Terracotta (`#AF4F35`):** Reserved strictly for primary state anchors and singular calls-to-action. Enforce a dogmatic threshold: exactly **one** primary terracotta action element per viewport.
- **Atmospheric Background (`#F8F5F0`):** The persistent canvas across all root frames. Never pure white `#FFFFFF` at the viewport root.
- **Surface Elevation (`#FFFFFF`):** High-clarity white card backgrounds that sit quiet and clean over the morning hearth warmth.
- **User Bubble (`#EFEAE2`):** A subtle, grounded wash for user-originating conversation payloads.
- **Hairlines & Boundaries (`#E8E2D9`):** 1px structural boundaries maintaining immaculate delineation without visual clutter.
- **Amber (`#E0A458`):** Restricted solely to the interior radial core of the companion orb to denote thought synthesis, active inference, and computational life. Never used for general warnings or tags.
- **Sage (`#7D8C6F`):** Cryptographic attestations, completed device backups, verified state markers, and positive progress.
- **Brick Red (`#A8443A`):** Destructive actions, cache purges, and identity resets. Distinct and deeper than the terracotta primary.

## Typography

Typography relies on a crisp geometric sans-serif tuned for readability, high-density editorial scanning, and clinical clarity.

### Implementation Guidelines
- **Tabular Numerals (`font-variant-numeric: tabular-nums`):** Mandatory for all memory index counters, vector storage sizes, latency readouts, and timestamps.
- **Editorial Contrast:** Titles span bold weights (32–34px) set tightly against body prose fixed at 17px with generous 26px line spacing for comfortable prolonged reflection.
- **Micro-Copy Balance:** Captions at 13px utilize medium weights (`500`) to maintain crisp optical rendering on high-DPI displays without thinning out against the warm background.

## Layout & Spacing

Layout adheres to an uncompromising, centered column rhythm prioritizing handheld ergonomics and distraction-free single-column focus.

### Spatial Discipline
- **Viewport Canvas:** Single vertical container capped at a maximum width of 480px on desktop web viewports, centered with balanced gutter fallbacks. On native mobile devices, spans edge-to-edge with a permanent `1.25rem` (20px) horizontal safe margin.
- **Vertical Rhythm:** Strict multiples of 4px and 8px base units. Sections demarcate boundaries using 24px–32px vertical gaps to preserve an unhurried, calm pacing.
- **Bottom Clearance:** Screens must append an explicit `96px` bottom inset to prevent content collision with the persistent floating pill navigation bar.

## Elevation & Depth

This system intentionally rejects muddy, heavy drop shadows in favor of crisp architectural separation, surface contrast, and microscopic hairline boundaries.

### Depth Mechanics
- **Level 0 (Base Canvas):** Background `#F8F5F0`.
- **Level 1 (Cards & Modular Surfaces):** Pure `#FFFFFF` surface container, strictly circumscribed by a 1px solid hairline (`#E8E2D9`). Shadow: `none`.
- **Level 2 (Floating System Chrome):** Applied only to the bottom navigation pill and transient floating contextual actions. Uses a razor-sharp 1px border (`#E8E2D9`) paired with a micro ambient diffused lift: `box-shadow: 0 8px 24px rgba(36, 29, 21, 0.06), 0 2px 6px rgba(36, 29, 21, 0.03)`.
- **Backdrop Filters:** Chrome surfaces hovering over text layers use `backdrop-filter: blur(16px)` with an `rgba(255, 255, 255, 0.88)` fill to ensure seamless typographic legibility during scrolling.

## Shapes

Shapes reflect warm modernist furniture design: organic, welcoming, and rigorously structured.

### Geometry Hierarchy
- **Cards & Primary Modules:** 20px to 24px border radius. This radius creates a pebble-like softness that frames complex data and journal thoughts with warmth.
- **Icon Accent Tiles:** Fixed 40px × 40px dimensions with an exact 12px border radius.
- **Interactive Controls & System Pills:** 9999px (full pill radius) for search bars, bottom navigation frames, and segment toggles.
- **Conversation Bubbles:** 20px corner radius, with an asymmetric 4px tuck on the anchor corner closest to the sender origin.

## Components

### 1. Unified Identity Top Bar
A consolidated, non-duplicative global status header anchored at the top of the viewport.
- **Left Core:** The 28px radiant companion orb. Built using a smooth radial gradient shifting from interior `#E0A458` (Amber) to `#AF4F35` (Terracotta) with a subtle breath animation cycle.
- **Center Telemetry:** Fixed pill badge containing a 10px padlock glyph accompanied by uppercase tracking text: `ON DEVICE`. Background: `#EFEAE2`, text: `#5F5550`, height: 24px, padding: 0 8px.
- **Right Security Anchor:** Minimalist cryptographic shield glyph (`#5F5550`), opening the tamper-proof local storage audit sheet when tapped.
- No secondary or duplicate page titles are permitted within the top chrome.

### 2. Floating Bottom Navigation Bar
- **Form:** Centered floating pill suspended 16px above the device bottom home indicator.
- **Dimensions:** Height 56px, horizontal internal padding 6px.
- **Structure:** Encased in `#FFFFFF` with a 1px hairline border (`#E8E2D9`) and Level 2 ambient micro-shadow.
- **Destinations:** Exactly three icon-label pairs: **Talk**, **Remembers**, **Insights**.
- **Active State:** The active destination receives a terracotta tint pill highlight (`rgba(175, 79, 53, 0.08)`) with `#AF4F35` typography and iconography. Inactive states render in `#5F5550`.

### 3. Action Buttons
- **Primary Button:** Solid Terracotta `#AF4F35`, label `#FFFFFF`, 48px height, 9999px pill radius. Enforce the system ceiling: strictly one primary button per screen view.
- **Secondary Button:** Surface `#FFFFFF`, 1px border `#E8E2D9`, text `#241D15`, 48px height, 9999px pill radius.
- **Destructive Button:** Surface `#FFFFFF`, 1px border `rgba(168, 68, 58, 0.3)`, text `#A8443A`.

### 4. Cards & Information Containers
- **Visuals:** `#FFFFFF` background, 22px border radius, 1px solid border `#E8E2D9`, zero drop shadows.
- **Padding:** 16px internal padding on mobile; 20px on expanded views.
- **Header Structure:** Optional 40px icon tile top-left with contextual action top-right.

### 5. Pill Segmented Toggles
- **Track:** 36px height, rounded full (9999px), background `#EFEAE2`, internal inset padding 2px.
- **Segment Item:** 32px height, full pill radius. Selected segment receives `#FFFFFF` fill with an ultra-fine border hairline (`#E8E2D9`) and text `#241D15`. Unselected segment text `#5F5550`.

### 6. Icon Tiles
- Standardized 40px × 40px container with 12px corner radius.
- Fill uses low-opacity tonal tints corresponding to context:
  - Memory Index: `rgba(175, 79, 53, 0.08)` with `#AF4F35` icon.
  - Security Verification: `rgba(125, 140, 111, 0.12)` with `#7D8C6F` icon.
  - Hardware State: `rgba(95, 85, 80, 0.08)` with `#5F5550` icon.

### 7. Form Fields & Inputs
- **Chat Input Bar:** 48px height, background `#FFFFFF`, 1px solid `#E8E2D9`, 9999px pill shape. Left mic or attachment icon (`#5F5550`), right circular terracotta submit trigger (36px).
- **Text Area & Inputs:** Background `#FFFFFF`, 1px solid `#E8E2D9`, 16px border radius, text `#241D15`, placeholder `#5F5550`. Active focus rings shift the border to `#AF4F35` with zero glow spread.

### 8. Conversational Message Bubbles
- **User Message:** Aligned right. Fill `#EFEAE2`, text `#241D15`, border radius 20px with bottom-right corner at 4px.
- **Maia Response:** Aligned left. Transparent background with zero border, text `#241D15`, anchored directly on the `#F8F5F0` base canvas to convey an integrated, uncontained voice.
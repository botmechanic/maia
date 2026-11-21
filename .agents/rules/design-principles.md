# Maia Design Principles (Project Memory)

All UI work in Maia follows **Warm Architectural Precision** distilled through **Apple-like Minimalism**:

## 1. Apple-Like Minimalism & Copy Discipline
- **Ruthlessly edit down verbose copy:** Reference mockups in `docs/design/reference-screens/` (generated via Stitch) are visual inspiration, **not** literal copy specs. Cut multi-sentence technical paragraphs down to crisp, scannable, human micro-copy (1 title + ≤ 6–8 words of subtitle max).
- **Quiet luxury & generous whitespace:** Let content breathe. Prefer 20dp horizontal screen margins, 24–32dp section gaps, and calm single-column focus.
- **Zero visual clutter:** No stacked badges, redundant headers, or heavy drop shadows. Separate surfaces with subtle tonal contrast (`canvas` `#F8F5F0` vs `surface` `#FFFFFF`) and razor-thin `1dp` hairlines (`#E8E2D9`).

## 2. Chromatic & Shape Discipline (Use `MaiaTheme.colors` Only)
- **One primary terracotta (`maiaPresence` `#AF4F35`) CTA per viewport.**
- **Ambient canvas (`#F8F5F0` light / `#171310` dark)** at the root; pure white (`#FFFFFF`) / warm charcoal (`#221C16`) for cards with `20–24dp` corner radius and `1dp` hairline border.
- **Amber (`maiaGlow`)** is reserved strictly for the companion orb's breathing core.
- **Sage (`success`)** is reserved strictly for verified/completed attestations.
- **Brick red (`danger`)** is reserved strictly for destructive actions.

## 3. Signature Components
- **Unified Top Bar:** 28dp radiant orb + subtle `ON DEVICE` padlock pill + minimal shield/settings anchor. No duplicate page titles.
- **Floating Pill Bottom Navigation:** Suspended pill (`56dp` height, `9999dp` radius, `1dp` hairline, soft micro-shadow) instead of a heavy full-width Material bar.
- **Floating Pill Chat Input:** `48dp` pill (`#FFFFFF`, `1dp` hairline), left mic icon, right `36dp` circular terracotta send button.
- **Uncontained Maia Voice:** Maia's replies render as clean ink directly on the canvas (no card/bubble wrapper); user messages sit right-aligned in `userBubble` (`20dp` radius, `4dp` bottom-right tuck).
- **Tabular Numerals:** Use tabular figures (`tnum`) for memory counters, sizes, timestamps, and latency readouts.

## 4. Reference Location
- Stitch reference screens & token spec live in `docs/design/reference-screens/`.


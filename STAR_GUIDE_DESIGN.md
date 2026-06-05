# Star Guide — design (RELEASE_CHECKLIST §1)

The Star Guide is Nerospace's in-game tutorial and progression tracker: a block that holds the
**Star Guide Book** and opens an **interactive visual progression tree** — the player's roadmap
from first nerosium to a terraformed world, with live per-player completion.

## 1. UX flow

- Craft the **Star Guide Book** (cheap, day-one: book + nerosium dust). Right-clicking the book in
  hand shows a hint pointing at the pedestal (full GUI lives on the block — the book is the key).
- Craft the **Star Guide** block (pedestal; stone slab + planks — deliberately pre-machine).
- Right-click the empty pedestal with the book to **install** it (lectern-style; the book is
  consumed into the block and pops back out on break / shift-right-click). An empty pedestal shows
  a status message ("place a Star Guide Book").
- Right-click the loaded pedestal → the **Star Guide screen**: chapter rail on the left, the
  selected chapter's step nodes on the right with connecting trajectory lines (rocket-UI styling),
  completed steps lit, current frontier pulsing accent. Clicking a step shows its guide text.

## 2. Content model

Chapters → steps, defined in code (a static `StarGuideChapters` table — data-driven JSON is
overkill for one mod and would need its own sync). Seven chapters mirroring the real progression:

1. **Nerosium** — mine raw nerosium · smelt an ingot · nerosium pickaxe
2. **Machines** — Nerosium Grinder · Combustion Generator · grind dust
3. **Power Grid** — Universal Pipes · Battery · Passive Generator · Configurator
4. **Rocketry** — rocket fuel + canister · 3×3 Launch Pad · Tier 1 Rocket · first launch (Station)
5. **New Worlds** — Tier 2 → Greenxertz (nerosteel, xertz quartz) · Tier 3 + Station-Wall ring →
   Cindara (cindrite)
6. **Surviving Vacuum** — Oxygen Generator · gas storage/pipes · Oxygen Suit · airlock refill ·
   Tier 2 suit
7. **Terraforming** — Terraformer · feed the frontier · stand on terraformed ground

Each step: `id`, icon (ItemStack), title/description lang keys, guide text lang key, and the
advancement id that completes it.

> **Sign-off (2026-06-05):** tracking = advancements as completion truth PLUS a small per-player
> attachment for guide-local state (seen-flags so fresh completions pulse until viewed). Book flow
> = BOTH lectern-style pedestal and book-in-hand read-only view, plus a **hologram** above the
> loaded pedestal (rotating planet, upgraded to the viewer's next incomplete phase icon where the
> client advancement API allows). Scope = full ~20-advancement expansion in this batch.

## 3. Completion tracking — advancements as the source of truth (RECOMMENDED)

Steps complete when their **advancement** completes. Rationale:

- A progression advancement tree already exists in datagen (`ModAdvancements`: root → grinder →
  rocket → Greenxertz → Cindara → station) and §1 requires the full tree anyway. One progression
  definition instead of two ("mirroring" becomes literal: the guide *renders* the tree).
- Vanilla gives per-player persistence, criteria triggers (inventory/dimension/custom), and toasts
  for free — no custom attachment, no bespoke sync, no double bookkeeping.
- The checklist's "per-player attachment, synced" line predates this design; an attachment would
  duplicate what `PlayerAdvancements` already stores.

Sync to the GUI: the screen is menu-backed (the mod's established pattern). The server packs step
completion into a `ContainerData` bitmask (steps ≤ 32 per chapter → one int per chapter, read from
`ServerPlayer.getAdvancements()`), so the client renders live progress without touching client
advancement internals. Completion is server-authoritative and updates while the screen is open
(`dataAccess` re-reads each broadcast).

Alternative (NOT recommended): custom per-player attachment + explicit event triggers — more code,
a second progression source to keep aligned, and custom payload sync; only worth it if steps ever
need non-advancement semantics (e.g. repeatable quests).

Advancement expansion (slice 2): grow `ModAdvancements` from 6 to ~20 nodes so every step above
has one; add toasts for key milestones (first launch, first planet, terraformed) per §1.

## 4. Block & item

- `star_guide` block: pedestal, `HorizontalDirectionalBlock`, comparator output 0/15 (empty/loaded),
  loot drops block + installed book. Block entity `StarGuideBlockEntity` stores the installed book
  ItemStack (Value I/O, like the machines), exposes NO capabilities (not automation — a landmark).
- `star_guide_book` item: flat item, max stack 1. The pedestal's `useItemOn` installs it;
  shift-right-click an installed pedestal returns it.
- `StarGuideMenu`: no slots, `ContainerData` of 7 ints (chapter bitmasks) + `stillValid` range
  check. `StarGuideScreen extends TexturedContainerScreen` reusing `SpaceButton`; chapter rail =
  7 buttons, step nodes = small SpaceButtons with the dotted-arc connector from `RocketScreen`;
  bottom panel renders the selected step's guide text (multiline, `font.split`).

## 5. Assets & datagen

- Datagen: block model (custom parent later — trivial cube for slice 1 per repo convention), item
  models (book = `FLAT_ITEM`), loot, recipes (book: book + nerosium dust, shapeless; pedestal:
  slab/planks + book NOT consumed — actually: pedestal = stone slab + 2 planks + nerosium dust,
  book separate), lang (titles, step text), GUI texture `star_guide.png` (176×166, generated like
  the machine panels).
- `tools/gen_textures.py`: `gen_star_guide()` (block) + `gen_star_guide_book()` (item) +
  `gen_gui_star_guide()`; `tools/gen_bbmodels.py`: add to BLOCKS + ITEMS. Palette: nerosium
  red/purple accents on steel (it's the mod's signpost).

## 6. Slices

- **Slice 1 (this batch):** block + BE + book + install/return interaction + menu/screen with all
  7 chapters and step nodes wired to the EXISTING 6 advancements (other steps render as
  "not yet trackable" using a constant-false bit) + datagen + textures + gametests.
- **Slice 2:** expand advancements to ~20 (one per step) + milestone toasts; flip the remaining
  bits live.
- **Slice 3 (post-art-pass):** bespoke pedestal model, step-icon polish, possibly map-style
  background.

## 7. Testing

Gametests: (a) pedestal install/return round-trip (use book on block → BE holds it, block re-use
returns it); (b) loot — breaking a loaded pedestal drops both; (c) menu data — complete a step's
advancement for a mock player, open the menu server-side, assert the chapter bitmask flips.
Plus `ecjCheck` + `runData` + `build` + full suite via the gradle MCP.

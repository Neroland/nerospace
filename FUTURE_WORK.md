# Nerospace — Future Work / Roadmap

Phases 1–7 are implemented and compile-green (verified via the gradle MCP). This file tracks the
remaining work: bigger planned features, deferred polish, and the items that still need an in-game
(`runClient` / `runServer`) pass. Keep it updated as things land.

## Big planned features (need design + a dedicated slice)

### Multiblock launch pad (→ "massive launch pad")
- **DONE (Phase 8a) — auto-fuel Fuel Tank + multiblock-aware pad.** A new `fuel_tank` machine
  (`FuelTankBlock` + `FuelTankBlockEntity`, 32 000 mB) auto-fuels a rocket standing on an adjacent
  launch pad: each server tick it finds the connected pad cluster via `LaunchPadMultiblock`, locates
  the `RocketEntity` above the footprint, and pumps fuel through the rocket's own `addFuel`. A
  complete aligned **3×3 pad** pumps 4× faster (160 vs 40 mB/tick) — the first reward for building
  the full multiblock. Fill the tank with a fuel bucket/canister (empty-bucket draws one back out);
  empty-hand right-click prints the fuel level. Verified green via gradle MCP. **Verify in runClient.**
- **DONE (suit-and-station batch) — multiblock gating + pad item proxy.** Deploying a rocket now
  REQUIRES a complete aligned **3×3 pad** (`RocketItem` checks `LaunchPadMultiblock`, with a clear
  chat message on failure); a **Tier 3** rocket additionally needs the 16-block **5×5 Station Wall
  ring** at pad level (`hasStationWallRing`). The same checks re-run in `RocketEntity.canLaunch`, so
  breaking pads under a deployed rocket grounds it (message on the Launch attempt). No block entity
  was needed — pure geometry + a block capability: every pad block proxies `Capabilities.Item.BLOCK`
  to the rocket above its connected cluster, so hoppers/pipes feed the rocket's intake through the
  pad. Covered by gametests. **Verify in runClient.**
- **Still TODO on this feature:**
  - **Comparator output — DONE (Phase 9c):** the Fuel Tank emits a redstone comparator signal from
    its fuel level, and the Oxygen Generator from its energy charge. 26.1 gotcha:
    `getAnalogOutputSignal(BlockState, Level, BlockPos, Direction)` gained a 4th `Direction` param.
  - **Pumping particles / sound — DONE (client-visual batch):** while fuel is actually flowing,
    `FuelTankBlockEntity` strings sparse vapour puffs (`CLOUD`) between the tank top and the rocket's
    intake plus a `SMALL_FLAME` flicker at the intake (every 8 ticks), and plays a soft interval
    gurgle (every ~1.2 s) — `ModSounds.FUEL_TANK_PUMP`, a `block.brewing_stand.brew` alias in
    sounds.json until real audio ships (vanilla particles only; the 26.1 custom particle API is a
    known trap). All server-side via `sendParticles`/`playSound`, no extra sync. **Verify in runClient.**
  - Grow into the **much larger pad** later (bigger footprint, more modules).

### Oxygen / atmosphere system
- **DONE (Phase 8c) — oxygen as a resource + generator bubbles.** Spec in `OXYGEN_SPEC.md`. Oxygen is
  a persistent per-player `int` (NeoForge data attachment `ModAttachments.OXYGEN`, copy-on-death). In
  the airless dimensions (Greenxertz/Cindara/Station) it drains while exposed and refills inside a
  breathable zone — near a Rocket Launch Pad (landing site) or an **active Oxygen Generator** machine
  (`oxygen_generator`, energy-buffered like the grinder, projects a `oxygenBubbleRadius` bubble). At
  zero oxygen the player suffocates (the old damage path, now gated on the stat). Oxygen is mirrored
  onto the **vanilla air-supply bar** so the bubble HUD shows it for free. Config: `oxygenMax`,
  `oxygenDrainPerTick`, `oxygenBubbleRadius` (+ existing atmosphere keys). Verified green via gradle
  MCP. **Verify in runClient.**
- **Generator upkeep — DONE (Phase 8d).** The Oxygen Generator no longer self-charges: it burns a
  fuel item (coal/charcoal/blaze rod/rocket fuel canister) placed in its slot (right-click to insert)
  to charge its energy buffer; idle once fuel + buffer are spent. Still TODO: hopper/pipe feeding of
  the fuel slot via `Capabilities.Item.BLOCK`.
- **Sealed rooms — DONE (Phase 8e).** Besides the raw bubble radius, `GreenxertzAtmosphere` now
  flood-fills the air space the player stands in (cap `oxygenSealedRoomMax`); if it is fully enclosed
  and borders an active Oxygen Generator, the whole room is breathable — so a sealed base works
  without hugging the generator. (Passable = air-only for now; doors/trapdoors/glass as seals is a
  refinement.)
- **Bespoke oxygen HUD bar — DONE (client-visual batch):** `OxygenHudLayer` now draws a proper
  gauge above the hotbar on airless dimensions — a generated 16×16 bubble icon
  (`textures/gui/oxygen_hud_icon.png`, `gen_textures.py gen_oxygen_hud_icon`) + the cyan/red bar +
  a **SUIT T1/T2 badge** (steel/gold, from the client-synced armour slots via
  `GreenxertzAtmosphere.suitTier`). The vanilla air-bubble row is suppressed on those dimensions
  (`RenderGuiLayerEvent.Pre` + `VanillaGuiLayers.AIR_LEVEL`) so the gauge is THE readout.
  **Sync resolved — no custom payload needed:** the server mirrors `oxygen/max` onto the vanilla
  air supply (suit-tier capacity is the divisor), so the synced `air/maxAir` fraction is exactly
  what the gauge shows; only an absolute "mB remaining" readout would need a bespoke payload.
  **Verify in runClient.**

### Space suits — DONE (Phase 8d)
- Four-piece **Oxygen Suit** (`oxygen_suit_helmet/chestplate/leggings/boots`), diamond-class
  nerosteel armour (repairs with nerosteel). Wearing the **full set** acts as personal life support:
  `GreenxertzAtmosphere` treats a fully-suited player as in a breathable zone (no oxygen drain),
  letting them explore off a generator/pad. The suit now has a **finite air tank** (Phase 8e): while
  exposed it drains oxygen slowly at `oxygenSuitDrain` per ~0.5 s (vs the fast unprotected drain),
  refilling in any breathable zone — so it greatly extends your air rather than being infinite.
  Equipment asset + worn layer + inventory textures are placeholders. **Verify in runClient.**
- **Suit tiers + airlock refill — DONE (suit-and-station batch).** A **Tier 2 Oxygen Suit**
  (`oxygen_suit_t2_*`: each piece = T1 piece + 4 cindrite, repairs with cindrite — same progression
  depth as the Tier 3 rocket) doubles the air tank (`oxygenSuitT2Max`, 600) and the airlock refill
  rate; a mixed set counts as Tier 1 (`GreenxertzAtmosphere.suitTier`). **Airlock refill**: a worn
  suit within `oxygenAirlockRadius` (3) of a Gas Tank / Oxygen Generator holding Oxygen refills from
  that store at `oxygenAirlockRefillPerCheck` air per ~0.5 s, drawing `oxygenAirlockMbPerAir` (5) mB
  per air unit — a tank at the base door is an airlock. Derived T2 textures (ember recolour) via
  `gen_textures.py gen_oxygen_suit_t2`. Covered by gametests. **Verify in runClient.**
  Follow-ups: nicer worn-armour art.

### Mobs — bespoke alien models + animations
- **Cohesive grounded rebuild (Phase 10c):** the 10b "floating loose boxes" were scrapped. Each
  creature is now built from **overlapping, layered cubes** that read as one solid body, **planted on
  the ground** (lowest cubes at y=24), minimal purposeful rotation only on connected details:
  - **Xertz Stalker** → *Crystal Hunter* (hero): a tall **standing biped** — layered torso, broad
    shoulders, sleek browed head + jaw, a crest and a row of bladed back-fins, long arms ending in
    down-swept crystal blades, two powerful legs with feet on the floor.
  - **Quartz Crawler** → *Geode Skitterer*: a low domed carapace with an overhanging rim, a back
    crystal cluster, a sensor-head, and **six tapered legs that bend out and plant** on the ground.
  - **Greenling** → *Sprout*: a chubby grounded critter — rounded body, oversized cheeky head with
    two big eyes, little arms, a three-frond leaf crest, two stubby legs.
  - **Cinder Stalker** → *Magma Hulk*: a heavy grounded quadruped — layered body + shoulders, big
    low browed head + jaw, blunt horns, a ridge of angled obsidian back-plates, four planted legs.
  `GreenxertzCreatureRenderer` is model-agnostic. Textures repainted (standalone script) with vertical
  shading + placed glowing eyes + lava/crystal accents on the UV atlas (body 0,0 / head+face 0,28 /
  limbs 44,0). Player-confirmed in-game (looks great).
- **Walk animation done (Phase 10d):** limbs are now hip-pivoted single parts; a shared
  `GreenxertzMobModel` base overrides `Model.setupAnim` to swing each registered limb by
  `walkAnimationPos/Speed` — the Crystal Hunter strides, the Magma Hulk trots diagonally, the Geode
  Skitterer ripples its six legs, the Sprout toddles. Offline previewer (`tools/render_models.py`,
  see [[nerospace-model-previewer]]) renders neutral + walk frames so the geometry/animation can be
  reviewed without the game.
- **Idle/ambient animation done (Phase 10f):** `GreenxertzMobModel` now drives `ageInTicks`-based
  ambient motion that fades out as walk speed rises (never fights the stride, legs stay planted):
  a tunable **breathing bob** (`breathing(freq, amp)`) plus per-part **ambient oscillators**
  (`ambient(part, axis, freq, phase, amp)`). Every creature got a subtle head sway plus a signature:
  the **Crystal Hunter** flexes its blade-arms at the shoulder, the **Geode Skitterer** keeps a faint
  at-rest leg ripple, the **Sprout**'s leaf crest wiggles out of phase, and the **Magma Hulk**
  breathes slow and heavy (half rate, near-double depth). `tools/render_models.py` mirrors the
  ambient poses and now renders an `_idle` frame + `_idle_sheet` alongside neutral/walk.
  **Verify in runClient.**
- **Emissive glow done:** `GlowEyesLayer` (an `EyesLayer` with a per-creature glow texture via
  `RenderTypes.eyes`) is wired for all four creatures; `<name>_glow.png` overlays are derived from
  the base atlases by `gen_textures.py gen_entity_glow()` (brightest pixels only — additive, never
  overwrites committed art). The Greenling's overlay is deliberately just its eye glint (a friendly
  creature, not a glowing one).
- **Still TODO:** per-creature Blockbench sources / nicer textures. The legacy shared
  `GreenxertzCreatureModel` + `.bbmodel`s are unused by renderers now (kept for the
  `model_sync` pipeline); retire once Blockbench sources exist per creature.

## Deferred polish (smaller, mostly runtime-visual)

- **Rocket UI (Phase 8b — interactive):** the in-rocket screen is now built from widgets + a real
  fuel-intake slot — a live **fuel readout** (percent + mB), an interactive **trajectory** row (the
  pad followed by one selectable button per reachable destination, chosen one marked `>`), a
  **Launch** button that enables only when fuelled+crewed, and a **fuel-intake slot** that accepts a
  rocket-fuel bucket/canister (drained into the tank each tick, empty bucket returned — the hook for
  hopper automation). All refresh via `containerTick()`. 26.1 note: `GuiGraphics` is gone (replaced
  by the `GuiGraphicsExtractor` submission model) and `Screen`/`AbstractContainerScreen` expose no
  `render`/`fill`/`drawString`, so hand-drawn bars/arcs aren't practical — the UI is widget-only.
  Still TODO (needs a `GuiGraphicsExtractor` runClient pass): a painted **background panel**, a
  graphical **fuel gauge bar**, and a true graphical **trajectory arc**. Verify the whole UI in
  runClient.
- **Rocket fuel-intake automation — DONE (suit-and-station batch).** The intake slot is now an
  `ItemStacksResourceHandler` (the authoritative store) exposed via `Capabilities.Item.ENTITY` and
  proxied through the launch-pad blocks via `Capabilities.Item.BLOCK`; the menu uses a small
  `Container` VIEW over the handler. ⚠ 26.1 gotcha discovered here: `StacksResourceHandler` COPIES
  any `NonNullList` passed to its constructor (`mutableCopyOf`) — it never shares a Container's
  backing list.
- **Machine inventory audit — DONE (machine-audit batch).** Gametests confirmed ALL FIVE Phase 9
  machines were broken by the copy gotcha (capability inserts landed in the handler's private copy;
  the machine never saw them): Nerosium Grinder, Combustion Generator, Passive Generator,
  Terraformer, Item Store. Fixed with a shared `machine/MachineItemHandler` (extends
  `ItemStacksResourceHandler`; the handler IS the store, with `getStack`/`setStack`/`removeStack`/
  `takeStack`/`isStoreEmpty`/`clearStore` accessors) — each BE's `Container` methods, tick logic and
  ValueIO persistence now read/write through it (NBT layouts unchanged). The Creative Item Store has
  its own endless handler and was unaffected; the Oxygen Generator has no item slot since the gas
  rework. Each machine is covered by a `*_cap_feed` gametest. **Verify in runClient.**
- **Rocket renderer transform:** scale/vertical offset were tuned blind (`RocketRenderer` uses
  `scale(-1.6,-1.6,1.6)` + `translate(0,-2.4,0)`). Confirm it stands correctly on the pad and seats
  the rider; the hitbox is now `2.6 × 5.0`.
- **Launch-pad block visual:** the single pad still renders as a near-full cube; replaced by the
  multiblock above, but until then a flatter pad model would help.
- **Fluid visuals:** register `IClientFluidTypeExtensions` for `rocket_fuel` (still/flow textures via
  `RegisterClientExtensionsEvent.registerFluidType`) and add a particle texture for the
  `rocket_fuel` block (currently uses a missing-texture particle).
- **Capability automation — DONE (Phase 9 + suit-station batch).** Fluid:
  `Capabilities.Fluid.ENTITY`→rocket tank, `Capabilities.Fluid.BLOCK`→Fuel Tank. Item:
  `Capabilities.Item.BLOCK`→Nerosium Grinder (sided — insert input from top/sides, extract output
  from below, via `RangedResourceHandler.ofSingleIndex`), generators' fuel/core slots, and the
  rocket intake (`Capabilities.Item.ENTITY` + launch-pad proxy). STALE NOTE REMOVED: the Oxygen
  Generator has NO fuel slot since the gas rework — it is grid-powered electrolysis (the old
  "fuel slot cap" follow-up is N/A). Machine inventories use the shared `MachineItemHandler`
  (handler owns the store; the Container is a view — see the machine-audit batch).
- **Machine GUIs — DONE (Phase 9 / 9a / 9b "spacified").** Grinder, Oxygen Generator, Fuel Tank and
  Rocket open real container screens with a **sci-fi hull look**: dark gradient panels with a glowing
  accent frame, header starfield, recessed slots and corner bolts (themed per machine: cyan O₂ /
  orange fuel / magenta nerosium / red rocket), editable 256×256 PNGs at
  `assets/nerospace/textures/gui/<machine>.png`. Readouts are now **drawn gauges + themed text**
  (`GuiGraphicsExtractor.fill`/`text` in `extractForeground`): energy/power/burn/fuel bars; the rocket
  shows a fuel gauge + an interactive **trajectory** of custom `SpaceButton`s (the chosen one lit) +
  Launch. `SpaceButton` overrides `AbstractButton.extractContents` to draw a themed button (not
  vanilla). `TexturedContainerScreen` blits the panel in `extractContents` and themes the labels via
  `extractLabels`. Follow-ups: animated/■-segmented gauges, real fluid render, tooltips.
- **Spawn eggs — DONE (Phase 10e):** the four creatures have working spawn eggs. 26.1 has no
  `DeferredSpawnEggItem` and vanilla `SpawnEggItem` binds its type too early for a DeferredRegister,
  so `item/NerospaceSpawnEggItem` resolves the `EntityType` lazily via a `Supplier` and spawns on
  right-click (`type.spawn(level, stack, player, pos, EntitySpawnReason.SPAWN_ITEM_USE, …)`). Custom
  flat egg-icon textures (no procedural tint). In the Nerospace creative tab.
- ~~**Spawn eggs**~~ (the spawn-egg item-model datagen was uncertain; use `/summon`
  for now).
- **Custom sounds:** rocket launch + planet ambience need real `.ogg` assets (can't be generated);
  register `SoundEvent`s + `sounds.json` once audio exists. Currently uses vanilla launch sound.

## Done recently (this polish pass) — verify in-game
- Tier destinations remapped + **selectable**: T1→Station, T2→+Greenxertz, T3→+Cindara (cumulative);
  the in-rocket UI has a **Destination** button to cycle through a tier's unlocked targets.
- Tier-3 recipe regated on **Station Wall** (cindrite gate would have been circular).
- **Launch smoothed** (server-authoritative movement; client interpolates) and **rocket enlarged**
  to seat the rider.
- **Creative-only destination compasses** (Station / Greenxertz / Cindara) for one-click travel.
- Rocket fuel **bucket** now has a texture/model/bbmodel.

## Gametests (suit-and-station batch)
The mod now has a gametest suite (`gametest/NerospaceGameTests`, run via `gradlew runGameTestServer`):
pad deploy gating, Tier 3 Station Wall ring, pad→rocket intake proxy, suit airlock refill, suit tier
detection, and a **registry-sync round-trip** regression test. 26.1 framework notes: tests are
data-driven `GameTestInstance`s registered via NeoForge's `RegisterGameTestsEvent`
(`registerEnvironment` + `registerTest`); each needs a structure template
(`data/nerospace/structure/gametest/empty.nbt`, generated empty 7×12×7). ⚠ The `TEST_INSTANCE`
registry is **synced to clients on world join** — a custom instance type MUST register its own
`MapCodec` in `Registries.TEST_INSTANCE_TYPE` and round-trip through `codec()`, or every world join
crashes with a `ClassCastException` in `RegistrySynchronization`.

## Config refactor (2026-06-05, BREAKING — pre-1.0, no migration)
The flat absolute-key config was replaced by **base values in code (`Tuning.java`) + five exposed
multipliers** (`oxygenDrainMultiplier`, `oxygenCapacityMultiplier`, `energyRateMultiplier`,
`fuelCostMultiplier`, `machineSpeedMultiplier`, each clamped 0.1–10.0). Booleans, radii,
performance caps, the ADVANCED oxygen-field sim keys and client visual keys stay absolute; the
template keys (`logDirtBlock` etc.) and the dead `oxygenSealedRoomMax` were dropped, and
`terraformPlantChance`/`terraformResourceChance` were internalised (their enable booleans remain).
Old config files are NOT migrated — delete `nerospace-common.toml` and let it regenerate (done for
the dev `run/config`). Every key is documented in `wiki/Configuration.md`. Scaled values clamp to
≥1 so 0.1x/10x extremes can't divide-by-zero, stall progression, or make a launch cost exceed the
tank (launch cost is clamped to tank size).

## Workflow reminders
- Verify every change via the gradle MCP (build / runData), never on an unverified change.
- Run `gradlew runGameTestServer` (gametests) as part of every verification pass.
- javap-probe the compile classpath for unknown 26.1 signatures.
- `gradlew genAssets` (procedural textures — needs Pillow; pyenv here lacks it, so PNGs are made
  off-box and copied) and `gradlew syncModels` (Blockbench⇄Java entity models).
- Do NOT auto commit/push.

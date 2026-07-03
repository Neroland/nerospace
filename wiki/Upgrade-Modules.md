# Upgrade Modules & Frame Casing

The crafting parts and tuning cards for the [Quarry Controller](Quarry-Controller). The modules are a
**cross-machine** system — designed so other machines can use the same cards in the future.

## Frame Casing

The structural material the quarry spends to build its frame ring — **one casing per open-air
perimeter cell** (cells already backed by terrain are free). Casings are also **placeable** as
frame blocks, so you can outline a mining area by hand instead of using landmarks; a frame block
broken by a player drops its casing, and a **finished dig returns its standing casings** to the
controller's frame slots.

**Craft** (shaped, makes 4):

```text
I I I
I   I
I I I
```

`I` = [Nerosteel Ingot](Items)

## Upgrade Modules

Slot module cards into a machine's module slots to change how it works. A machine counts the modules
across its slots and sums their effects (you can stack several). The [Quarry Controller](Quarry-Controller)
has **1 module slot at Tier 1** (more at higher tiers).

| Module | Effect | Notes |
| --- | --- | --- |
| **Speed Module** | +50% to the work-cap per module | Lets the machine do more when fed more power; capped at ×8. |
| **Efficiency Module** | −15% energy cost per module | Floors at 25% of the base cost. |
| **Fortune Module** | Applies Fortune to mined blocks | Stacks up to Fortune III. |
| **Silk Touch Module** | Mines blocks with Silk Touch | **Overrides** Fortune when present. |

**Craft** — every module shares one frame with a signature centre item:

```text
 N
R S R
 N
```

`N` = [Nerosteel Ingot](Items) · `R` = Redstone · `S` = signature:

| Module | Signature `S` |
| --- | --- |
| Speed | Sugar |
| Efficiency | Lapis Lazuli |
| Fortune | Diamond |
| Silk Touch | Amethyst Shard |

## Details

- IDs: `nerospace:frame_casing`, `nerospace:speed_module`, `nerospace:efficiency_module`,

  `nerospace:fortune_module`, `nerospace:silk_touch_module`

- Used by: [Quarry Controller](Quarry-Controller) (more machines planned)

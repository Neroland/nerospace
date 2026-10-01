# Upgrade Modules & Frame Casing

The crafting parts and tuning cards for the [Quarry Controller](Quarry-Controller). The modules are a
**cross-machine** system — designed so other machines can use the same cards in the future.

## Frame Casing

The structural material the quarry spends to build its frame ring — **one casing per perimeter
cell** (terrain on the ring is replaced by frame; chests and machines on the ring are left in place). Casings are also **placeable** as
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
| **Speed Module** | +50% work speed per module | Capped at ×8. Power alone never speeds a machine up — only Speed modules do (and they don't change the energy cost per block). |
| **Efficiency Module** | −15% energy cost per module | Floors at 25% of the base cost. |
| **Fortune Module** | Applies Fortune to mined blocks | Stacks up to Fortune III. |
| **Silk Touch Module** | Mines blocks with Silk Touch | **Overrides** Fortune when present. |
| **Evaporator Module** | Destroys liquids instead of buffering them | Keeps the quarry's fluid buffer empty — nothing to pipe away. Texture is placeholder art for now. |

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
| Evaporator | Blaze Powder |

## Details

- IDs: `nerospace:frame_casing`, `nerospace:speed_module`, `nerospace:efficiency_module`,

  `nerospace:fortune_module`, `nerospace:silk_touch_module`, `nerospace:evaporator_module`

- Used by: [Quarry Controller](Quarry-Controller) (more machines planned)

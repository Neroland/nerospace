# Pipe Filters and Upgrades

Fine-tune what flows where, and how fast.

## Pipe Filter

Restricts a pipe face's **item layer** to a single item.

- **Craft** (shaped, yields 4): nerosteel corners around Iron Bars.
- **Set the filter:** hold the filter in one hand and the item to match in the other, then

  right-click in air. (Empty other hand = clear the filter item.)

- **Apply:** right-click a Universal Pipe **face** with the configured filter — only the matching

  item is pulled or pushed through that face. Apply an empty filter to remove it.

Filters affect extraction (pulling faces only grab the matching item), routing (packets won't head
toward a filtered face that rejects them) and delivery.

## Advanced Pipe Filter

The higher-tier filter: one pipe line, **many item types**, each face deciding what it accepts —
BuildCraft-diamond-pipe-style sorting without parallel lines.

- **Craft** (shaped, yields 1): a Pipe Filter core ringed by nerosteel ingots and xertz quartz
  (Greenxertz-tier materials).
- **Configure:** right-click in air to open the filter GUI —
  - **9 ghost slots** (3×3): click with an item on your cursor to set an entry, click with an
    empty cursor to clear it, shift-click items straight from your inventory.
  - **Right-click an entry** (empty cursor) to cycle its match mode: the exact item → each of the
    item's **tags** (e.g. `#c:ores` covers every ore with one entry, marked with a gold `#`) →
    back to the exact item.
  - **Whitelist / Blacklist** toggle: only these items — or everything **except** these items.
  - **Match Exact / Ignore Data** toggle: whether item data components (NBT) must match too.
- **Apply:** right-click a Universal Pipe **face** — the whole configuration lands on that face.
  An unconfigured filter clears the face.
- **Copy:** **sneak-right-click** a pipe face to read its current filter back into the item —
  replicate a sorting wall without rebuilding the config.

The configuration is stored on the item, so a tuned filter is a **template**: stack it in a chest,
share it, or apply it to a dozen faces in a row. A filter with no entries behaves like no filter
(everything passes) in both whitelist and blacklist mode.

**Example — the grinder split:** run one pipe from your grinder; on the furnace face apply a
whitelist with the smeltable-ore tag entries, and on the bypass-chest face apply a **blacklist**
of the same entries. Smeltables go to the furnace, everything else flows past.

Basic Pipe Filters keep working exactly as before — an advanced filter simply replaces a face's
basic filter when applied (and vice versa).

## Speed Upgrade

- **Craft** (shaped): redstone + gold core in a nerosteel frame.
- **Install:** right-click a pipe segment (consumed, up to **3** per pipe).
- Each one multiplies the segment's energy/fluid/gas **throughput** and makes items **travel faster**

  through it.

## Capacity Upgrade

- **Craft** (shaped): xertz quartz + chest core in a nerosteel frame.
- **Install:** right-click a pipe segment (up to **3**).
- Each one multiplies the segment's fluid/gas **buffers** and how many item stacks may be **in

  transit** at once.

## Removing upgrades

Sneak-right-click the pipe with an **empty hand** — all installed upgrades pop back out.

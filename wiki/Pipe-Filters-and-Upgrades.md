# Pipe Filters and Upgrades

Fine-tune what flows where, and how fast.

## Pipe Filter

Restricts a pipe face's **item layer** to a single item.

- **Craft** (shaped, yields 4): nerosteel corners around Iron Bars.
- **Set the filter:** hold the filter in one hand and the item to match in the other, then

  right-click in air. (Empty other hand = clear the filter item.)

- **Install:** right-click a Universal Pipe **face** with the configured filter — the item is
  **consumed into that face's filter slot** (like pipe upgrades), and only the matching item is
  pulled or pushed through the face. A filter already on the face pops back to you.
- **Remove / inspect:** open the pipe's [Configurator](Configurator) panel — each face's filter
  sits in a real slot; hover it to see what it matches, take it out to clear the face. Breaking
  the pipe drops its installed filters.

Filters affect extraction (pulling faces only grab the matching item), routing (packets won't head
toward a filtered face that rejects them) and delivery.

**Routing rules (how filters and destinations interact):**

- Items enter a pipe network **only through faces set to In** (with the
  [Configurator](Configurator)). An **Auto** face delivers but never extracts items — so the
  network can't drain its own destination chests back out again. Energy, fluid and gas Auto
  faces still pull as before.
- **Junctions are gated.** An item only travels across a pipe-to-pipe junction if the leaving
  face allows outgoing items (**Out**/Auto), the entering face allows incoming items
  (**In**/Auto), and **both faces' filters** pass it. A filter installed on a T-junction's
  branch face therefore controls exactly what may go up that branch — items that don't match
  route around it or stay behind.
- **Filtered faces claim their items first.** An item that matches any filtered face goes to a
  filtered face; unfiltered destinations only receive what no filter claimed. So "cobblestone →
  chest A (whitelist)" really means all the cobblestone lands in A, and the unfiltered overflow
  chest gets everything else.

> **Tip — watch which face you click.** The filter lands on the face you clicked, which is not
> always the face touching the target chest. Hold the Configurator to see the per-face colour
> shading, then check the panel: the filter should sit in the row whose colour matches the shaded
> face pointing at your target. Items with **no accepting face anywhere** wait inside the pipes
> (backpressure — nothing is ever dropped), so a fully filtered line needs at least one
> unfiltered destination for the leftovers.

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
- **Install:** right-click a Universal Pipe **face** — the item (with its whole configuration)
  is consumed into that face's filter slot; a previously installed filter pops back to you.
  Remove or inspect it via the [Configurator](Configurator) panel's face slots.
- **Copy:** **sneak-right-click** a pipe face to read its current filter back into the held item —
  replicate a sorting wall without rebuilding the config.
- **Inspect:** the item's **tooltip lists the full configuration** (mode line + every entry;
  tag entries in gold) — in your inventory and in the Configurator panel's face slots.

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

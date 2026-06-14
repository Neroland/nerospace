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

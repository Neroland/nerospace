# Cargo Pad

The **Cargo Pad** is the endpoint of an automated supply line: a launch-pad plate with a 27-slot hold,
a pipe-fed fuel buffer and a route selector. Build one where goods leave, one where they should land,
put a [Cargo Rocket](Cargo-Rocket) on the first, and cargo flies between them with nobody at either pad.

## Recipe

Nerosteel ingots around a [Cargo Hull](Cargo-Rocket) on top, two [Rocket Launch Pads](Rocket-Launch-Pad)
either side of a Block of Nerosium in the middle, nerosteel along the bottom. It sits on the Greenxertz
material ladder, after the Tier 2 rocket.

## Building the pad

A Cargo Pad **is** a launch pad block. Build the usual complete **3×3** pad around it (the Cargo Pad may be
any cell of the square) — a cargo rocket needs a Tier 2 formation, the same gate a Tier 2 crewed rocket
has. Right-click the pad with an empty hand while sneaking to read the tier it forms.

- An adjacent **[Fuel Tank](Fuel-Tank)** pumps into the docked cargo rocket exactly as it does for crewed
  rockets. Alternatively pipe rocket fuel straight into the Cargo Pad: its fuel buffer (16,000 mB by
  default) accepts rocket fuel only and is pumped into the rocket at 80 mB/tick.
- **[Universal Pipes](Universal-Pipe)** connect to every face. The hold uses Neroland Core's side
  configuration (the STORAGE preset — every face is in/out), so pipes push cargo in at the origin and
  pull delivered cargo out at the destination; basic and advanced [pipe filters](Pipe-Filters-and-Upgrades)
  work unchanged. Use the [Configurator](Configurator) to make a face input- or output-only.
- The hold has 27 slots; the server's `cargoPadSlots` setting (default 18) decides how many are usable.
  Locked slots are drawn darker in the GUI.

## Ownership

Placing a Cargo Pad makes you its owner. Only the owner (or anyone, while a pad is unowned) can change
its destination, schedule and flags; anyone may still put items in through the GUI or pipes. Other
players can route cargo **to** your pad only if you mark it **Public** in the GUI. A Name Tag renames
the pad; the name is what other players see in their destination list.

Founded [orbital stations](Station-Charter) count as destinations too: a flight to a station lands on a
Cargo Pad placed within six blocks of the station's landing pad.

## The GUI

Right-click the pad (or its rocket) with an empty hand.

| Control | Meaning |
| --- | --- |
| `<` `>` `x` | Cycle through the pads you may ship to (your own, ones shared with you, public pads, your stations), or clear the destination. |
| Schedule | **Manual** (the Launch button only), **launch when full** (every usable slot holds something), or **every N min** (`-`/`+` change N). |
| Buffer / Rocket gauges | Fuel in the pad's buffer and in the docked rocket. |
| Trip line | The quote for what is in the hold right now: fuel the rocket must hold and the flight time. |
| Return empty | After unloading, the rocket flies back empty to this pad instead of staying at the destination. The return fuel is charged at launch. |
| Public / Private | Whether other players may route cargo here. |
| Launch | Lights when a fuelled cargo rocket is docked, a destination is set and the hold is not empty. |
| Status line | The verdict of the last launch attempt, then the state of the last flight (in transit with a countdown, holding, delivered, crated). |

## Flight, arrival and holding

Travel time and fuel come from the planet registry (see the [Public API](Public-API) for the formula):
a flight takes at least a minute, longer across dimensions and between heavy worlds; fuel grows with
the payload and the origin's gravity. Nothing is rendered in transit — the flight is a server-side
timer that **survives a server restart**.

On arrival the rocket materialises on the destination pad and unloads one stack every two ticks so pipes
keep up. If the destination is not loaded, the flight waits — Nerospace never loads a chunk to deliver.
If the destination pad is gone, another rocket is docked on it, or its hold is full, the flight
**holds** and retries every 30 seconds (`cargoHoldRetrySeconds`); after 30 minutes of holding
(`cargoHoldTimeoutMinutes`) the remaining cargo is dropped as a **Cargo Crate** item at the destination
position. Cargo is never deleted. Right-click a crate to unpack it.

Comparator output follows the hold's fill level.

In a creative world, `/nerospace gallery` builds a ready-made freight line (two routed pads, a fuelled
rocket, a loaded hold) north of the rocket row — open pad A and press Launch to watch a delivery.

## Privacy

The pad stores its owner's UUID, and optionally the UUIDs of players the owner shared it with, in the
world save only. `/neroland data eraseme` removes your routes, unlinks your pads (they stay as shared
world content), takes you off every access list and detaches you from your flights — the cargo itself
is still delivered or crated. Nothing here is logged or sent anywhere. See `PRIVACY.md`.

---

See also: [Cargo Rocket](Cargo-Rocket), [Rocket Launch Pad](Rocket-Launch-Pad), [Fuel Tank](Fuel-Tank),
[Universal Pipe](Universal-Pipe), [Configuration](Configuration).

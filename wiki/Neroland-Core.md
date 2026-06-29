# Neroland Core

Nerospace is built on **Neroland Core** (mod id `nerolandcore`), the shared foundation library for the
Neroland mod ecosystem. Core is a **required dependency** — it must be installed alongside Nerospace and
loads first. Modpacks and servers running Nerospace need both jars (for the matching Minecraft version
and loader); the launcher will refuse to start Nerospace without a compatible `nerolandcore` `1.0`–`2.0`.

Building on Core means Nerospace no longer reinvents the systems every Neroland mod shares. In practice
that gives you four things.

Core also now provides the **shared storage blocks** — the [Battery](Battery), [Fluid Tank](Fluid-Tank),
[Gas Tank](Gas-Tank), and [Item Store](Item-Store) (plus their [Creative](Creative-Source-Blocks)
variants) moved out of Nerospace into Core as of `nerolandcore` 1.1.0, so every Neroland mod uses one
set of endpoints. They craft and behave as before, and Nerospace's [Universal Pipe](Universal-Pipe)
still connects to them.

## One power network across mods

Every Nerospace generator, battery, machine and pipe now exposes its energy on Core's shared
`nerolandcore:energy` capability, and Nerospace pipes read neighbours through Core's lookup. So a
Nerospace [Universal Pipe](Universal-Pipe) can carry power straight into another Neroland mod's machine,
and another mod's cable can feed a Nerospace machine — it is one continuous network, not separate grids
that stop at the mod boundary. Nerospace's own internal energy still works exactly as before; this only
adds cross-mod reach.

## Shared progression milestones

As you progress, Nerospace quietly opens Core's ecosystem-wide **progression gates** so other Neroland
mods can react to where you are in the journey: building your first generator opens *Industrial Power*,
your first rocket launch opens *Reached Orbit*, founding a station with a [Station Charter](Station-Charter)
opens *First Colony*, and reaching a far planet or terraforming a world to life opens *Deep Space*. This
is one-directional — Nerospace drives the shared gates but still paces itself by the [Star Guide](Star-Guide),
which remains the authority on your Nerospace progress. You can inspect the shared gates in-game with
`/neroland gate list`.

## One privacy / data-erasure path

Nerospace registers its player-keyed data with Core's shared erasure hook, so a single
`/neroland data eraseme` (or an admin's `/neroland data erase <uuid>`, or Core's inactivity retention
sweep) clears your Nerospace data too — your station ownership is unlinked (the station itself stays as
shared world content) and your oxygen and Star-Guide state reset. In keeping with POPIA/GDPR, Nerospace
keys this data only by player UUID and never logs your identity. See `PRIVACY.md` for the full picture.

## One config-reload command and a shared tab

Nerospace's [configuration](Configuration) is now managed by Core: it lives in `config/nerospace.properties`
and reloads live with `/neroland config reload`, and the gameplay-balance multipliers are
server-authoritative (a client uses the server's values when connected). The anonymous-telemetry opt-out
stays a personal, per-client choice that a server can never override.

Finally, Nerospace's signature materials and key items also appear in Core's shared **Neroland** creative
tab, so a pack with several Neroland mods shows one organised tab — while Nerospace's full catalogue still
lives in its own dedicated Nerospace tab.

---

See also: [Configuration](Configuration), [Star Guide](Star-Guide), [Universal Pipe](Universal-Pipe),
[Station Charter](Station-Charter).

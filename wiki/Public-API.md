# Public Integration API

Nerospace `1.0.0-beta.8` provides a stable API for other Nero mods without exposing its internal world,
rocket, or registry managers.

## Available contracts

- Planet identity, traits, gravity, and current-world lookup.
- Historical planet visits and first-visit events.
- Read-only atmosphere, oxygen, hazard, gravity, and terraforming snapshots.
- Bounded oxygen contributions for plants and other optional providers.
- Reversible regional terraforming overlays with a server-installed claim policy.
- Read-only stations and cargo-route discovery.

Unloaded environment queries return vacuum and zero oxygen. Oxygen and terraforming state contains no
player attribution. Planet visits store only a UUID plus planet ids, without timestamps or route history,
and are removed by Neroland Core's shared data-erasure command.

The developer reference and examples live in
[`../common/src/main/java/za/co/neroland/nerospace/api/README.md`](../common/src/main/java/za/co/neroland/nerospace/api/README.md).

## See also

- [Planet Gravity](Planet-Gravity.md)
- [Oxygen Generator](Oxygen-Generator.md)
- [Terraformer](Terraformer.md)
- [Neroland Core](Neroland-Core.md)
- [Home](Home.md)

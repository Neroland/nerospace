/**
 * Nerospace public API — the <b>only</b> supported integration surface for other Neroland mods.
 *
 * <p><b>Stability contract.</b> Every type in this package is semver-stable: within a major version,
 * existing public types, methods, record components and enum constants will not be removed, renamed, or
 * have their meaning changed. New additive members may appear in minor versions. Anything <em>outside</em>
 * this package (e.g. {@code registry.ModDimensions}, {@code rocket.StationRegistry},
 * {@code world.gravity.GravityManager}, {@code world.OxygenManager}) is internal and may change without
 * notice — consumers must not depend on it.</p>
 *
 * <p><b>What it offers.</b></p>
 * <ul>
 *   <li>{@link za.co.neroland.nerospace.api.NerospacePlanets} — planet identity ({@link
 *       za.co.neroland.nerospace.api.PlanetId}), the {@code currentPlanet(Entity)} hook, enumeration of all
 *       planets, and read-only {@link za.co.neroland.nerospace.api.PlanetTraits} (default gravity, airless
 *       flag, {@link za.co.neroland.nerospace.api.Hazard}).</li>
 *   <li>{@link za.co.neroland.nerospace.api.NerospaceStations} — read-only station destinations as
 *       immutable {@link za.co.neroland.nerospace.api.StationInfo} records (id, name, planet, position,
 *       route capacity), with lookup by id and listing by planet.</li>
 *   <li>{@link za.co.neroland.nerospace.api.NerospaceRoutes} — the cargo-rocket route catalog for
 *       logistics consumers: {@link za.co.neroland.nerospace.api.RouteEndpoint}s (Home + every Nerospace
 *       body) and directed {@link za.co.neroland.nerospace.api.CargoRoute}s carrying the minimum rocket
 *       tier, per-launch fuel cost (mB, config-scaled) and canonical transit duration (ticks), plus
 *       {@code isOpen} liveness checks.</li>
 *   <li>{@link za.co.neroland.nerospace.api.NerospaceVisits} and {@link
 *       za.co.neroland.nerospace.api.PlanetVisitEvents} — erasable historical planet arrivals.</li>
 *   <li>{@link za.co.neroland.nerospace.api.NerospaceEnvironment} and {@link
 *       za.co.neroland.nerospace.api.NerospaceOxygen} — immutable atmosphere queries and bounded,
 *       unattributed external oxygen contributions.</li>
 *   <li>{@link za.co.neroland.nerospace.api.NerospaceTerraforming} — claim-authorized reversible
 *       regional overlays with no owner identity in returned snapshots.</li>
 * </ul>
 *
 * <p><b>Design.</b> These are thin facades over owning stores/managers. All returned collections and
 * records are immutable; no mutable internal collection, block entity, or manager is leaked.</p>
 *
 * <p><b>Privacy (POPIA/GDPR).</b> Station ownership ties to a player. This API never exposes raw owner
 * UUIDs; ownership is queryable only as a per-player boolean
 * ({@link za.co.neroland.nerospace.api.NerospaceStations#ownedBy}). Because the facade reads the live
 * registry, it automatically honours Neroland Core's {@code PlayerDataErasure} hook that Nerospace
 * registers: a purged player's stations are anonymised at the source and {@code ownedBy} returns
 * {@code false} thereafter.</p>
 */
package za.co.neroland.nerospace.api;

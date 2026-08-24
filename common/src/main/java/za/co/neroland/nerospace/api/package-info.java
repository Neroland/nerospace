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
 *   <li>{@link za.co.neroland.nerospace.api.NerospaceEnvironment} — the read-only "what is it like here?"
 *       query, returning an immutable {@link za.co.neroland.nerospace.api.EnvironmentSnapshot}
 *       ({@link za.co.neroland.nerospace.api.Atmosphere}, hazard, gravity, oxygen, terraforming stage,
 *       breathability). Fails closed to a vacuum snapshot on unloaded chunks and never forces chunk
 *       loading.</li>
 *   <li>{@link za.co.neroland.nerospace.api.NerospaceVisits} and
 *       {@link za.co.neroland.nerospace.api.PlanetVisitEvents} — historical planet-visit queries and a
 *       first-visit event, for progression that must survive being installed after the fact.</li>
 *   <li>{@link za.co.neroland.nerospace.api.NerospaceOxygen} — bounded, expiring external oxygen
 *       contributions, so another mod's life support can feed Nerospace's atmosphere truth.</li>
 *   <li>{@link za.co.neroland.nerospace.api.NerospaceTerraforming} — claim-gated, reversible regional
 *       terraforming overlays ({@link za.co.neroland.nerospace.api.TerraformRequest} in,
 *       {@link za.co.neroland.nerospace.api.TerraformRegion} out) that change what Nerospace reports
 *       without rewriting a single block. Denies every mutation until a server installs a
 *       {@link za.co.neroland.nerospace.api.TerraformClaimPolicy}.</li>
 * </ul>
 *
 * <p><b>Design.</b> This is a thin facade — it holds no state of its own beyond the saved data backing
 * visits, oxygen contributions and overlays, and wraps the internal registry/managers. All returned
 * collections and records are immutable; no mutable internal collection or block entity is leaked.</p>
 *
 * <p><b>Privacy (POPIA/GDPR).</b> Station ownership ties to a player. This API never exposes raw owner
 * UUIDs; ownership is queryable only as a per-player boolean
 * ({@link za.co.neroland.nerospace.api.NerospaceStations#ownedBy}). Because the facade reads the live
 * registry, it automatically honours Neroland Core's {@code PlayerDataErasure} hook that Nerospace
 * registers: a purged player's stations are anonymised at the source and {@code ownedBy} returns
 * {@code false} thereafter.</p>
 *
 * <p>Planet visits are the other player-keyed surface. The store holds a UUID and a set of planet ids and
 * nothing else — no timestamps, coordinates or movement trail — and it is registered with the same
 * {@code PlayerDataErasure} hook, which drops the row and rewrites the saved-data backup immediately.
 * Oxygen contributions and terraforming overlays record no player at all: source and region ids are
 * caller-owned and <b>must never encode player identity</b>, because Nerospace persists them verbatim.</p>
 */
package za.co.neroland.nerospace.api;

/**
 * Nerospace public <b>route API</b> — cargo pads, routes and flights for logistics consumers.
 *
 * <p><b>Stability contract.</b> Like its parent package, every type here is semver-stable: within a major
 * version no public type, method, record component or enum constant is removed, renamed or changes
 * meaning; minor versions may add members. The implementation lives in the internal
 * {@code za.co.neroland.nerospace.route} package and is reached only through
 * {@link za.co.neroland.nerospace.api.route.RouteApi#instance()} — never depend on it directly.</p>
 *
 * <p><b>What it offers.</b></p>
 * <ul>
 *   <li>{@link za.co.neroland.nerospace.api.route.RouteApi} — list the pads a player may ship to, quote a
 *       shipment (travel ticks + fuel), request a flight, query flight state, cancel a held flight.</li>
 *   <li>{@link za.co.neroland.nerospace.api.route.PadHandle},
 *       {@link za.co.neroland.nerospace.api.route.RouteHandle},
 *       {@link za.co.neroland.nerospace.api.route.FlightHandle} — immutable snapshots.</li>
 *   <li>{@link za.co.neroland.nerospace.api.route.RouteEvents} — a subscription bus for pad registration
 *       and flight departure / arrival / hold / drop.</li>
 * </ul>
 *
 * <p><b>Design.</b> Endpoints are <em>pads</em> (and founded stations, which count as pads), never bare
 * dimensions: a rocket lands somewhere specific. Travel time and fuel are functions of the planet
 * registry (see {@code docs/CARGO-ROCKETS.md} §4), computed at quote time — query per shipment rather
 * than caching across config reloads. The registry never loads a dimension or a chunk to answer a query;
 * arrival into an unloaded chunk is deferred until it loads.</p>
 *
 * <p><b>Privacy (POPIA/GDPR).</b> Pads, routes and flights carry an owner, and this surface never exposes
 * that UUID: ownership is queryable only as a per-player boolean ({@code ownedBy(UUID)}), and
 * {@link za.co.neroland.nerospace.api.route.RouteApi#padsVisibleTo} returns only the pads the given player
 * owns, is listed on, or that are public — never a server-wide roster, which would map every base on the
 * server. Erasure through Neroland Core's {@code PlayerDataErasure} removes the player's routes,
 * anonymises their pads and drops them from access lists; in-flight cargo is delivered or crated, never
 * deleted, because items are not personal data — the ownership record is.</p>
 */
package za.co.neroland.nerospace.api.route;

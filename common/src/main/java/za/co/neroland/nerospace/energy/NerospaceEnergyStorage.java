package za.co.neroland.nerospace.energy;

/**
 * Loader-neutral energy storage interface. Each loader exposes it through its own block-lookup
 * mechanism (NeoForge {@code BlockCapability}, Fabric {@code BlockApiLookup}) so the mod's own
 * generators, batteries and machines interoperate on both loaders. Cross-mod energy interop
 * (NeoForge's {@code Capabilities.Energy} / the Fabric energy libraries) is deferred — those
 * libraries have not ported to 26.x, and the mod is standalone for now.
 */
public interface NerospaceEnergyStorage {

    long getAmount();

    long getCapacity();

    /** @return energy actually inserted (0 if none). */
    long insert(long maxAmount, boolean simulate);

    /** @return energy actually extracted (0 if none). */
    long extract(long maxAmount, boolean simulate);
}

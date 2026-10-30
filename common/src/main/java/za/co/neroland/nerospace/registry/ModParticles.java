package za.co.neroland.nerospace.registry;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;

/**
 * The six ambient planet particle types. They are spawned on the client only (by {@code client.WeatherAmbience}),
 * drawn from {@code assets/nerospace/particles/<id>.json} and {@code textures/particle/<id>_0..3.png}; each loader
 * registers the client providers through {@code client.particle.ClientParticles}.
 */
public final class ModParticles {

    public static final RegistrationProvider<net.minecraft.core.particles.ParticleType<?>> PARTICLE_TYPES =
            RegistrationProvider.get(Registries.PARTICLE_TYPE, NerospaceCommon.MOD_ID);

    public static final RegistryEntry<SimpleParticleType> DUST = register("dust");
    public static final RegistryEntry<SimpleParticleType> ASH = register("ash");
    public static final RegistryEntry<SimpleParticleType> SPORE = register("spore");
    public static final RegistryEntry<SimpleParticleType> DIAMOND_DUST = register("diamond_dust");
    public static final RegistryEntry<SimpleParticleType> EMBER = register("ember");
    public static final RegistryEntry<SimpleParticleType> NEBULA_MOTE = register("nebula_mote");

    /** Every type id, in registration order (SkyAssetsTest checks each has its JSON and four frames). */
    public static final String[] IDS = {"dust", "ash", "spore", "diamond_dust", "ember", "nebula_mote"};

    private static RegistryEntry<SimpleParticleType> register(String path) {
        // The constructor is protected outside NeoForge's access transformer: an empty subclass works everywhere.
        return PARTICLE_TYPES.register(path, key -> new SimpleParticleType(false) {
        });
    }

    private ModParticles() {
    }

    public static void init() {
    }
}

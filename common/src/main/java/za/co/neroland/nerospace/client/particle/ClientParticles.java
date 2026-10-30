package za.co.neroland.nerospace.client.particle;

import java.util.function.Function;

import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

import za.co.neroland.nerospace.registry.ModParticles;

/**
 * Cross-loader particle-provider wiring: the provider set lives here once and each loader passes its own
 * registration ({@link Sink}): NeoForge and Forge {@code RegisterParticleProvidersEvent.registerSpriteSet}, Fabric
 * {@code ParticleProviderRegistry}.
 */
public final class ClientParticles {

    /** A loader's sprite-set provider registration. */
    public interface Sink {
        void register(SimpleParticleType type, Function<SpriteSet, ParticleProvider<SimpleParticleType>> factory);
    }

    public static void registerAll(Sink sink) {
        sink.register(ModParticles.DUST.get(), s -> new AmbientParticle.Provider(s, AmbientParticle.Kind.DUST));
        sink.register(ModParticles.ASH.get(), s -> new AmbientParticle.Provider(s, AmbientParticle.Kind.ASH));
        sink.register(ModParticles.SPORE.get(), s -> new AmbientParticle.Provider(s, AmbientParticle.Kind.SPORE));
        sink.register(ModParticles.DIAMOND_DUST.get(),
                s -> new AmbientParticle.Provider(s, AmbientParticle.Kind.DIAMOND_DUST));
        sink.register(ModParticles.EMBER.get(), s -> new AmbientParticle.Provider(s, AmbientParticle.Kind.EMBER));
        sink.register(ModParticles.NEBULA_MOTE.get(),
                s -> new AmbientParticle.Provider(s, AmbientParticle.Kind.NEBULA_MOTE));
    }

    private ClientParticles() {
    }
}

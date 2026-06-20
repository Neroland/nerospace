package za.co.neroland.nerospace.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import za.co.neroland.nerospace.entity.CinderStalker;
import za.co.neroland.nerospace.entity.EmberStrutter;
import za.co.neroland.nerospace.entity.FrostStrider;
import za.co.neroland.nerospace.entity.Greenling;
import za.co.neroland.nerospace.entity.MeadowLoper;
import za.co.neroland.nerospace.entity.QuartzCrawler;
import za.co.neroland.nerospace.entity.RuinWarden;
import za.co.neroland.nerospace.entity.WoollyDrift;
import za.co.neroland.nerospace.entity.XertzStalker;

/**
 * Cross-loader default-attribute table for the ported mobs. The loaders apply it differently
 * (NeoForge {@code EntityAttributeCreationEvent#put(type, supplier)}; Fabric
 * {@code FabricDefaultAttributeRegistry#register(type, builder)}), so this exposes the
 * {@link AttributeSupplier.Builder}s and lets each loader consume them.
 */
public final class ModEntityAttributes {

    /** Receives each (entity type, attribute builder) pair for loader-specific registration. */
    public interface Sink {
        void accept(EntityType<? extends LivingEntity> type, AttributeSupplier.Builder builder);
    }

    public static void forEach(Sink sink) {
        sink.accept(ModEntities.XERTZ_STALKER.get(), XertzStalker.createAttributes());
        sink.accept(ModEntities.QUARTZ_CRAWLER.get(), QuartzCrawler.createAttributes());
        sink.accept(ModEntities.GREENLING.get(), Greenling.createAttributes());
        sink.accept(ModEntities.RUIN_WARDEN.get(), RuinWarden.createAttributes());
        sink.accept(ModEntities.CINDER_STALKER.get(), CinderStalker.createAttributes());
        sink.accept(ModEntities.FROST_STRIDER.get(), FrostStrider.createAttributes());
        sink.accept(ModEntities.MEADOW_LOPER.get(), MeadowLoper.createAttributes());
        sink.accept(ModEntities.EMBER_STRUTTER.get(), EmberStrutter.createAttributes());
        sink.accept(ModEntities.WOOLLY_DRIFT.get(), WoollyDrift.createAttributes());
    }

    private ModEntityAttributes() {
    }
}

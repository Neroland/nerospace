package za.co.neroland.nerospace.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import za.co.neroland.nerospace.Nerospace;

/**
 * Creature ambience sound events (Phase 10 polish). Each creature gets an <b>ambient</b>, <b>hurt</b>
 * and <b>death</b> event registered under the {@code nerospace} namespace.
 *
 * <p>No {@code .ogg} files ship yet: the matching entries in {@code assets/nerospace/sounds.json}
 * alias a fitting vanilla sound event as a placeholder ({@code "type": "event"}). Because the events
 * are registered here under our own ids, swapping in real audio later is purely a resource change —
 * drop {@code .ogg} files under {@code assets/nerospace/sounds/entity/<creature>/} and flip the
 * sounds.json entry from an {@code event} alias to a {@code sound} file. No code changes needed.</p>
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, Nerospace.MODID);

    // --- Xertz Stalker (Greenxertz, hostile crystalline predator) -----------
    public static final Supplier<SoundEvent> XERTZ_STALKER_AMBIENT = register("entity.xertz_stalker.ambient");
    public static final Supplier<SoundEvent> XERTZ_STALKER_HURT = register("entity.xertz_stalker.hurt");
    public static final Supplier<SoundEvent> XERTZ_STALKER_DEATH = register("entity.xertz_stalker.death");

    // --- Quartz Crawler (Greenxertz, neutral geode skitterer) ---------------
    public static final Supplier<SoundEvent> QUARTZ_CRAWLER_AMBIENT = register("entity.quartz_crawler.ambient");
    public static final Supplier<SoundEvent> QUARTZ_CRAWLER_HURT = register("entity.quartz_crawler.hurt");
    public static final Supplier<SoundEvent> QUARTZ_CRAWLER_DEATH = register("entity.quartz_crawler.death");

    // --- Greenling (Greenxertz, passive sprout) -----------------------------
    public static final Supplier<SoundEvent> GREENLING_AMBIENT = register("entity.greenling.ambient");
    public static final Supplier<SoundEvent> GREENLING_HURT = register("entity.greenling.hurt");
    public static final Supplier<SoundEvent> GREENLING_DEATH = register("entity.greenling.death");

    // --- Machines ------------------------------------------------------------
    /** Fuel Tank actively pumping fuel into a rocket (soft interval gurgle; aliased to vanilla). */
    public static final Supplier<SoundEvent> FUEL_TANK_PUMP = register("block.fuel_tank.pump");

    // --- Cinder Stalker (Cindara, hostile volcanic hulk) --------------------
    public static final Supplier<SoundEvent> CINDER_STALKER_AMBIENT = register("entity.cinder_stalker.ambient");
    public static final Supplier<SoundEvent> CINDER_STALKER_HURT = register("entity.cinder_stalker.hurt");
    public static final Supplier<SoundEvent> CINDER_STALKER_DEATH = register("entity.cinder_stalker.death");

    // --- Frost Strider (Glacira, hostile stilt-legged ice predator) ---------
    public static final Supplier<SoundEvent> FROST_STRIDER_AMBIENT = register("entity.frost_strider.ambient");
    public static final Supplier<SoundEvent> FROST_STRIDER_HURT = register("entity.frost_strider.hurt");
    public static final Supplier<SoundEvent> FROST_STRIDER_DEATH = register("entity.frost_strider.death");

    private static Supplier<SoundEvent> register(String path) {
        Identifier id = Identifier.fromNamespaceAndPath(Nerospace.MODID, path);
        return SOUND_EVENTS.register(path, () -> SoundEvent.createVariableRangeEvent(id));
    }

    private ModSounds() {
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}

package za.co.neroland.nerospace.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;


import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;

/**
 * Creature ambience sound events, ported cross-loader through {@link RegistrationProvider}. No
 * {@code .ogg} files ship: {@code assets/nerospace/sounds.json} aliases a fitting vanilla event for
 * each ({@code "type": "event"}); swapping in real audio later is a pure resource change.
 */
public final class ModSounds {

    public static final RegistrationProvider<SoundEvent> SOUND_EVENTS =
            RegistrationProvider.get(Registries.SOUND_EVENT, NerospaceCommon.MOD_ID);

    public static final RegistryEntry<SoundEvent> XERTZ_STALKER_AMBIENT = register("entity.xertz_stalker.ambient");
    public static final RegistryEntry<SoundEvent> XERTZ_STALKER_HURT = register("entity.xertz_stalker.hurt");
    public static final RegistryEntry<SoundEvent> XERTZ_STALKER_DEATH = register("entity.xertz_stalker.death");

    public static final RegistryEntry<SoundEvent> QUARTZ_CRAWLER_AMBIENT = register("entity.quartz_crawler.ambient");
    public static final RegistryEntry<SoundEvent> QUARTZ_CRAWLER_HURT = register("entity.quartz_crawler.hurt");
    public static final RegistryEntry<SoundEvent> QUARTZ_CRAWLER_DEATH = register("entity.quartz_crawler.death");

    public static final RegistryEntry<SoundEvent> GREENLING_AMBIENT = register("entity.greenling.ambient");
    public static final RegistryEntry<SoundEvent> GREENLING_HURT = register("entity.greenling.hurt");
    public static final RegistryEntry<SoundEvent> GREENLING_DEATH = register("entity.greenling.death");

    public static final RegistryEntry<SoundEvent> CINDER_STALKER_AMBIENT = register("entity.cinder_stalker.ambient");
    public static final RegistryEntry<SoundEvent> CINDER_STALKER_HURT = register("entity.cinder_stalker.hurt");
    public static final RegistryEntry<SoundEvent> CINDER_STALKER_DEATH = register("entity.cinder_stalker.death");

    public static final RegistryEntry<SoundEvent> FROST_STRIDER_AMBIENT = register("entity.frost_strider.ambient");
    public static final RegistryEntry<SoundEvent> FROST_STRIDER_HURT = register("entity.frost_strider.hurt");
    public static final RegistryEntry<SoundEvent> FROST_STRIDER_DEATH = register("entity.frost_strider.death");

    public static final RegistryEntry<SoundEvent> MEADOW_LOPER_AMBIENT = register("entity.meadow_loper.ambient");
    public static final RegistryEntry<SoundEvent> MEADOW_LOPER_HURT = register("entity.meadow_loper.hurt");
    public static final RegistryEntry<SoundEvent> MEADOW_LOPER_DEATH = register("entity.meadow_loper.death");

    public static final RegistryEntry<SoundEvent> EMBER_STRUTTER_AMBIENT = register("entity.ember_strutter.ambient");
    public static final RegistryEntry<SoundEvent> EMBER_STRUTTER_HURT = register("entity.ember_strutter.hurt");
    public static final RegistryEntry<SoundEvent> EMBER_STRUTTER_DEATH = register("entity.ember_strutter.death");

    public static final RegistryEntry<SoundEvent> WOOLLY_DRIFT_AMBIENT = register("entity.woolly_drift.ambient");
    public static final RegistryEntry<SoundEvent> WOOLLY_DRIFT_HURT = register("entity.woolly_drift.hurt");
    public static final RegistryEntry<SoundEvent> WOOLLY_DRIFT_DEATH = register("entity.woolly_drift.death");

    private static RegistryEntry<SoundEvent> register(String path) {
        Identifier id = NerospaceCommon.id(NerospaceCommon.requireNonNull(path));
        return SOUND_EVENTS.register(path, key -> SoundEvent.createVariableRangeEvent(id));
    }

    private ModSounds() {
    }

    public static void init() {
    }
}

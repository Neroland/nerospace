package za.co.neroland.nerospace.forge;

import java.util.Set;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.MissingMappingsEvent;

/**
 * Forge world-compat aliases for the storage blocks that moved from Nerospace into
 * Neroland Core. Existing saves placed {@code nerospace:battery},
 * {@code nerospace:fluid_tank}, {@code nerospace:gas_tank}, {@code nerospace:item_store}
 * (+ their {@code creative_*} variants); those ids no longer exist here, so on load
 * Forge fires {@link MissingMappingsEvent}. This remaps each missing block / item id
 * to its {@code nerolandcore:} equivalent, preserving placed blocks across the move.
 *
 * <p>Forge only — modern NeoForge removed the missing-mappings event and Fabric has
 * no registry-remap hook, so this compatibility path is not available on those loaders.
 */
public final class ForgeStorageAliases {

    private static final Set<String> MOVED = Set.of(
            "battery", "fluid_tank", "gas_tank", "item_store",
            "creative_battery", "creative_fluid_tank", "creative_gas_tank", "creative_item_store");

    private ForgeStorageAliases() {
    }

    public static void register() {
        MissingMappingsEvent.BUS.addListener(ForgeStorageAliases::onMissingMappings);
    }

    private static void onMissingMappings(MissingMappingsEvent event) {
        remap(event, Registries.BLOCK, BuiltInRegistries.BLOCK);
        remap(event, Registries.ITEM, BuiltInRegistries.ITEM);
    }

    private static <T> void remap(MissingMappingsEvent event, ResourceKey<? extends Registry<T>> registryKey,
            Registry<T> registry) {
        for (MissingMappingsEvent.Mapping<T> mapping : event.getMappings(registryKey, "nerospace")) {
            String path = mapping.getKey().getPath();
            if (MOVED.contains(path)) {
                T value = registry.getValue(Identifier.fromNamespaceAndPath("nerolandcore", path));
                if (value != null) {
                    mapping.remap(value);
                }
            }
        }
    }
}

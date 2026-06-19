package za.co.neroland.nerospace.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;
import za.co.neroland.nerospace.storage.BatteryBlockEntity;
import za.co.neroland.nerospace.storage.ItemStoreBlockEntity;

/**
 * Block-entity types, shared by both loaders via {@link RegistrationProvider} over the vanilla
 * {@code BLOCK_ENTITY_TYPE} registry. Registration is loader-agnostic; only mod-pipe capability
 * exposure (next step) needs the platform seam.
 */
public final class ModBlockEntities {

    public static final RegistrationProvider<BlockEntityType<?>> BLOCK_ENTITIES =
            RegistrationProvider.get(Registries.BLOCK_ENTITY_TYPE, NerospaceCommon.MOD_ID);

    public static final RegistryEntry<BlockEntityType<ItemStoreBlockEntity>> ITEM_STORE =
            BLOCK_ENTITIES.register("item_store",
                    key -> new BlockEntityType<>(ItemStoreBlockEntity::new, java.util.Set.of(ModBlocks.ITEM_STORE.get())));

    public static final RegistryEntry<BlockEntityType<BatteryBlockEntity>> BATTERY =
            BLOCK_ENTITIES.register("battery",
                    key -> new BlockEntityType<>(BatteryBlockEntity::new, java.util.Set.of(ModBlocks.BATTERY.get())));

    private ModBlockEntities() {
    }

    public static void init() {
    }
}

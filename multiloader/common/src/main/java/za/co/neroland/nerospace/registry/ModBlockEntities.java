package za.co.neroland.nerospace.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;
import za.co.neroland.nerospace.machine.CombustionGeneratorBlockEntity;
import za.co.neroland.nerospace.machine.NerosiumGrinderBlockEntity;
import za.co.neroland.nerospace.machine.PassiveGeneratorBlockEntity;
import za.co.neroland.nerospace.pipe.UniversalPipeBlockEntity;
import za.co.neroland.nerospace.storage.BatteryBlockEntity;
import za.co.neroland.nerospace.storage.FluidTankBlockEntity;
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

    public static final RegistryEntry<BlockEntityType<FluidTankBlockEntity>> FLUID_TANK =
            BLOCK_ENTITIES.register("fluid_tank",
                    key -> new BlockEntityType<>(FluidTankBlockEntity::new, java.util.Set.of(ModBlocks.FLUID_TANK.get())));

    public static final RegistryEntry<BlockEntityType<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR =
            BLOCK_ENTITIES.register("combustion_generator",
                    key -> new BlockEntityType<>(CombustionGeneratorBlockEntity::new, java.util.Set.of(ModBlocks.COMBUSTION_GENERATOR.get())));

    public static final RegistryEntry<BlockEntityType<NerosiumGrinderBlockEntity>> NEROSIUM_GRINDER =
            BLOCK_ENTITIES.register("nerosium_grinder",
                    key -> new BlockEntityType<>(NerosiumGrinderBlockEntity::new, java.util.Set.of(ModBlocks.NEROSIUM_GRINDER.get())));

    public static final RegistryEntry<BlockEntityType<PassiveGeneratorBlockEntity>> PASSIVE_GENERATOR =
            BLOCK_ENTITIES.register("passive_generator",
                    key -> new BlockEntityType<>(PassiveGeneratorBlockEntity::new, java.util.Set.of(ModBlocks.PASSIVE_GENERATOR.get())));

    public static final RegistryEntry<BlockEntityType<UniversalPipeBlockEntity>> UNIVERSAL_PIPE =
            BLOCK_ENTITIES.register("universal_pipe",
                    key -> new BlockEntityType<>(UniversalPipeBlockEntity::new, java.util.Set.of(ModBlocks.UNIVERSAL_PIPE.get())));

    private ModBlockEntities() {
    }

    public static void init() {
    }
}

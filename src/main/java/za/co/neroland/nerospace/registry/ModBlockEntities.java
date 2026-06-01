package za.co.neroland.nerospace.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.machine.FuelTankBlockEntity;
import za.co.neroland.nerospace.machine.NerosiumGrinderBlockEntity;

/**
 * Block entity types (Phase 2).
 */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Nerospace.MODID);

    public static final Supplier<BlockEntityType<NerosiumGrinderBlockEntity>> NEROSIUM_GRINDER = BLOCK_ENTITY_TYPES.register(
            "nerosium_grinder",
            () -> new BlockEntityType<>(
                    NerosiumGrinderBlockEntity::new,
                    // Only allow OP players to load NBT data: false.
                    false,
                    ModBlocks.NEROSIUM_GRINDER.get()));

    public static final Supplier<BlockEntityType<FuelTankBlockEntity>> FUEL_TANK = BLOCK_ENTITY_TYPES.register(
            "fuel_tank",
            () -> new BlockEntityType<>(
                    FuelTankBlockEntity::new,
                    false,
                    ModBlocks.FUEL_TANK.get()));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}

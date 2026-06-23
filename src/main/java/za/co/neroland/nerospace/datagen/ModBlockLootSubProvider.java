package za.co.neroland.nerospace.datagen;

import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Block loot tables for Nerospace. Storage blocks drop themselves; ores drop raw nerosium with
 * fortune applied via {@link #createOreDrop}.
 */
public class ModBlockLootSubProvider extends BlockLootSubProvider {

    public ModBlockLootSubProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.NEROSIUM_BLOCK.get());
        dropSelf(ModBlocks.RAW_NEROSIUM_BLOCK.get());
        dropSelf(ModBlocks.NEROSIUM_GRINDER.get());

        // Solar panels (SOLAR_PANEL_DESIGN). Tier 1 drops itself; the Tier 2/3 multiblocks drop nothing
        // per cell — the block returns exactly one item for the whole unit on break (playerWillDestroy).
        dropSelf(ModBlocks.SOLAR_PANEL_T1.get());
        add(ModBlocks.SOLAR_PANEL_T2.get(), noDrop());
        add(ModBlocks.SOLAR_PANEL_T3.get(), noDrop());

        add(ModBlocks.NEROSIUM_ORE.get(),
                block -> createOreDrop(block, ModItems.RAW_NEROSIUM.get()));
        add(ModBlocks.DEEPSLATE_NEROSIUM_ORE.get(),
                block -> createOreDrop(block, ModItems.RAW_NEROSIUM.get()));

        // Phase 4 — rockets.
        dropSelf(ModBlocks.ROCKET_LAUNCH_PAD.get());
        dropSelf(ModBlocks.LAUNCH_GANTRY.get());

        // Star Guide pedestal (the installed book pops separately via the BE's remove hook).
        dropSelf(ModBlocks.STAR_GUIDE.get());

        // Phase 8a — fuel tank.
        dropSelf(ModBlocks.FUEL_TANK.get());

        // Fuel Refinery (BALANCE_COMPAT_AUDIT.md §3).
        dropSelf(ModBlocks.FUEL_REFINERY.get());

        // Phase 8c — oxygen generator.
        dropSelf(ModBlocks.OXYGEN_GENERATOR.get());

        // Terraform design — terraformer.
        dropSelf(ModBlocks.TERRAFORMER.get());

        // Deeper terraforming — hydration module + terraform monitor.
        dropSelf(ModBlocks.HYDRATION_MODULE.get());
        dropSelf(ModBlocks.TERRAFORM_MONITOR.get());

        // Quarry / Miner (the frame has noLootTable — drops nothing, like the rocket fuel block).
        dropSelf(ModBlocks.QUARRY_CONTROLLER.get());
        dropSelf(ModBlocks.QUARRY_LANDMARK.get());

        // Power grid.
        dropSelf(ModBlocks.UNIVERSAL_PIPE.get());
        dropSelf(ModBlocks.COMBUSTION_GENERATOR.get());
        dropSelf(ModBlocks.PASSIVE_GENERATOR.get());

        // Storage endpoints + creative sources.
        dropSelf(ModBlocks.BATTERY.get());
        dropSelf(ModBlocks.CREATIVE_BATTERY.get());
        dropSelf(ModBlocks.FLUID_TANK.get());
        dropSelf(ModBlocks.CREATIVE_FLUID_TANK.get());
        dropSelf(ModBlocks.GAS_TANK.get());
        dropSelf(ModBlocks.CREATIVE_GAS_TANK.get());
        dropSelf(ModBlocks.ITEM_STORE.get());
        dropSelf(ModBlocks.CREATIVE_ITEM_STORE.get());
        dropSelf(ModBlocks.TRASH_CAN.get());

        // Phase 3 — Greenxertz.
        dropSelf(ModBlocks.NEROSTEEL_BLOCK.get());
        add(ModBlocks.NEROSTEEL_ORE.get(),
                block -> createOreDrop(block, ModItems.RAW_NEROSTEEL.get()));
        // Xertz quartz behaves like nether quartz: the gem drops directly (fortune-affected).
        add(ModBlocks.XERTZ_QUARTZ_ORE.get(),
                block -> createOreDrop(block, ModItems.XERTZ_QUARTZ.get()));

        // Phase 7 — Cindara.
        dropSelf(ModBlocks.CINDRITE_BLOCK.get());
        add(ModBlocks.CINDRITE_ORE.get(),
                block -> createOreDrop(block, ModItems.CINDRITE.get()));

        // Glacira (NEW_DESTINATION_DESIGN.md).
        dropSelf(ModBlocks.GLACITE_BLOCK.get());
        add(ModBlocks.GLACITE_ORE.get(),
                block -> createOreDrop(block, ModItems.GLACITE.get()));

        // Phase 7c — station blocks.
        dropSelf(ModBlocks.STATION_FLOOR.get());
        dropSelf(ModBlocks.STATION_WALL.get());

        // Developer diagnostics — Sentry test block (drops itself so it is reusable).
        dropSelf(ModBlocks.SENTRY_TEST.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream()
                .map(holder -> (Block) holder.value())
                .toList();
    }
}

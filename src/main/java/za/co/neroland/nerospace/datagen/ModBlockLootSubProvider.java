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

        add(ModBlocks.NEROSIUM_ORE.get(),
                block -> createOreDrop(block, ModItems.RAW_NEROSIUM.get()));
        add(ModBlocks.DEEPSLATE_NEROSIUM_ORE.get(),
                block -> createOreDrop(block, ModItems.RAW_NEROSIUM.get()));

        // Phase 4 — rockets.
        dropSelf(ModBlocks.ROCKET_LAUNCH_PAD.get());

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

        // Phase 7c — station blocks.
        dropSelf(ModBlocks.STATION_FLOOR.get());
        dropSelf(ModBlocks.STATION_WALL.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream()
                .map(holder -> (Block) holder.value())
                .toList();
    }
}

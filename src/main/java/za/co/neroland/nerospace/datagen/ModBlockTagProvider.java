package za.co.neroland.nerospace.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModTags;

/**
 * Block tags: mining rules (pickaxe + iron-tier requirement) and conventional {@code c} material
 * tags so future tech mods can discover Nerospace ores and storage blocks.
 */
public class ModBlockTagProvider extends BlockTagsProvider {

    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, Nerospace.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.NEROSIUM_ORE.get(),
                        ModBlocks.DEEPSLATE_NEROSIUM_ORE.get(),
                        ModBlocks.NEROSIUM_BLOCK.get(),
                        ModBlocks.RAW_NEROSIUM_BLOCK.get(),
                        ModBlocks.NEROSIUM_GRINDER.get(),
                        ModBlocks.NEROSTEEL_ORE.get(),
                        ModBlocks.XERTZ_QUARTZ_ORE.get(),
                        ModBlocks.NEROSTEEL_BLOCK.get(),
                        ModBlocks.CINDRITE_ORE.get(),
                        ModBlocks.CINDRITE_BLOCK.get(),
                        ModBlocks.STATION_FLOOR.get(),
                        ModBlocks.STATION_WALL.get(),
                        ModBlocks.FUEL_TANK.get());

        this.tag(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.NEROSIUM_ORE.get(),
                        ModBlocks.DEEPSLATE_NEROSIUM_ORE.get(),
                        ModBlocks.NEROSIUM_BLOCK.get(),
                        ModBlocks.RAW_NEROSIUM_BLOCK.get(),
                        // Nerosteel is iron-tier; xertz quartz ore needs only any pickaxe (omitted here).
                        ModBlocks.NEROSTEEL_ORE.get(),
                        ModBlocks.NEROSTEEL_BLOCK.get(),
                        ModBlocks.CINDRITE_ORE.get(),
                        ModBlocks.CINDRITE_BLOCK.get(),
                        ModBlocks.STATION_FLOOR.get(),
                        ModBlocks.STATION_WALL.get(),
                        ModBlocks.FUEL_TANK.get());

        this.tag(Tags.Blocks.ORES)
                .add(ModBlocks.NEROSIUM_ORE.get(), ModBlocks.DEEPSLATE_NEROSIUM_ORE.get(),
                        ModBlocks.NEROSTEEL_ORE.get(), ModBlocks.XERTZ_QUARTZ_ORE.get(),
                        ModBlocks.CINDRITE_ORE.get());
        this.tag(ModTags.Blocks.ORES_NEROSIUM)
                .add(ModBlocks.NEROSIUM_ORE.get(), ModBlocks.DEEPSLATE_NEROSIUM_ORE.get());
        this.tag(ModTags.Blocks.ORES_NEROSTEEL)
                .add(ModBlocks.NEROSTEEL_ORE.get());
        this.tag(ModTags.Blocks.ORES_XERTZ_QUARTZ)
                .add(ModBlocks.XERTZ_QUARTZ_ORE.get());

        this.tag(Tags.Blocks.STORAGE_BLOCKS)
                .add(ModBlocks.NEROSIUM_BLOCK.get(), ModBlocks.RAW_NEROSIUM_BLOCK.get(),
                        ModBlocks.NEROSTEEL_BLOCK.get(), ModBlocks.CINDRITE_BLOCK.get());
        this.tag(ModTags.Blocks.STORAGE_BLOCKS_NEROSIUM)
                .add(ModBlocks.NEROSIUM_BLOCK.get());
        this.tag(ModTags.Blocks.STORAGE_BLOCKS_RAW_NEROSIUM)
                .add(ModBlocks.RAW_NEROSIUM_BLOCK.get());
        this.tag(ModTags.Blocks.STORAGE_BLOCKS_NEROSTEEL)
                .add(ModBlocks.NEROSTEEL_BLOCK.get());
    }
}

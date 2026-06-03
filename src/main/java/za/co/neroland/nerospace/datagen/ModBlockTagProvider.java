package za.co.neroland.nerospace.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
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
                        ModBlocks.FUEL_TANK.get(),
                        ModBlocks.OXYGEN_GENERATOR.get(),
                        ModBlocks.TERRAFORMER.get(),
                        ModBlocks.UNIVERSAL_PIPE.get(),
                        ModBlocks.COMBUSTION_GENERATOR.get(),
                        ModBlocks.PASSIVE_GENERATOR.get());

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
                        ModBlocks.FUEL_TANK.get(),
                        ModBlocks.OXYGEN_GENERATOR.get(),
                        ModBlocks.TERRAFORMER.get(),
                        ModBlocks.UNIVERSAL_PIPE.get(),
                        ModBlocks.COMBUSTION_GENERATOR.get(),
                        ModBlocks.PASSIVE_GENERATOR.get());

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

        // --- Oxygen field classification (terraform design §1.4) -----------
        // Full solid cubes already seal via the runtime fallback; these tags make the classification
        // authoritative + moddable. Glass seals naturally (it is a full collision block).
        this.tag(ModTags.Blocks.OXYGEN_SEALING)
                .add(ModBlocks.STATION_WALL.get(),
                        ModBlocks.STATION_FLOOR.get(),
                        ModBlocks.NEROSIUM_BLOCK.get(),
                        ModBlocks.RAW_NEROSIUM_BLOCK.get(),
                        ModBlocks.NEROSTEEL_BLOCK.get(),
                        ModBlocks.CINDRITE_BLOCK.get());

        // Leaky / partial-flow blocks (non-full cubes that still bleed to the void).
        this.tag(ModTags.Blocks.OXYGEN_LEAKS)
                .addTag(BlockTags.FENCES)
                .addTag(BlockTags.WALLS)
                .addTag(BlockTags.SLABS)
                .addTag(BlockTags.TRAPDOORS)
                .addTag(BlockTags.FENCE_GATES);

        // Oxygen sources feeding the field.
        this.tag(ModTags.Blocks.OXYGEN_SOURCE)
                .add(ModBlocks.OXYGEN_GENERATOR.get());

        // --- Terraform conversion table (terraform design §2.2) ------------
        // Exposed surfaces a Terraformer turns into grass (the planets generate overworld terrain,
        // so this covers stone/dirt/sand/gravel families; tags keep it moddable).
        this.tag(ModTags.Blocks.TERRAFORM_TO_GRASS)
                .add(Blocks.STONE, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.PODZOL, Blocks.GRAVEL,
                        Blocks.SAND, Blocks.RED_SAND, Blocks.SANDSTONE, Blocks.ANDESITE, Blocks.DIORITE,
                        Blocks.GRANITE, Blocks.TUFF, Blocks.CALCITE, Blocks.NETHERRACK, Blocks.BLACKSTONE,
                        Blocks.BASALT, Blocks.END_STONE, Blocks.TERRACOTTA, Blocks.CLAY, Blocks.MUD,
                        Blocks.SNOW_BLOCK, Blocks.MYCELIUM)
                .addTag(BlockTags.DIRT);

        // Sub-surface blocks turned into dirt (and the layer the Tier-3 ore seeding targets).
        this.tag(ModTags.Blocks.TERRAFORM_TO_DIRT)
                .add(Blocks.STONE, Blocks.DEEPSLATE, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.GRAVEL,
                        Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE, Blocks.TUFF, Blocks.CALCITE,
                        Blocks.NETHERRACK, Blocks.BLACKSTONE, Blocks.BASALT, Blocks.END_STONE,
                        Blocks.SANDSTONE, Blocks.SAND, Blocks.RED_SAND);
    }
}

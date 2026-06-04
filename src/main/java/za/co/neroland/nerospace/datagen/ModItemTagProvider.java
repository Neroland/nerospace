package za.co.neroland.nerospace.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.registry.ModTags;

/**
 * Item tags mirroring the block material tags, plus ingot and raw-material conventional tags.
 */
public class ModItemTagProvider extends ItemTagsProvider {

    public ModItemTagProvider(PackOutput output,
                              CompletableFuture<HolderLookup.Provider> lookupProvider,
                              CompletableFuture<TagsProvider.TagLookup<Block>> blockTags) {
        super(output, lookupProvider, Nerospace.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(Tags.Items.INGOTS).add(ModItems.NEROSIUM_INGOT.get());
        this.tag(ModTags.Items.INGOTS_NEROSIUM).add(ModItems.NEROSIUM_INGOT.get());

        this.tag(Tags.Items.DUSTS).add(ModItems.NEROSIUM_DUST.get());
        this.tag(ModTags.Items.DUSTS_NEROSIUM).add(ModItems.NEROSIUM_DUST.get());

        this.tag(Tags.Items.RAW_MATERIALS).add(ModItems.RAW_NEROSIUM.get());
        this.tag(ModTags.Items.RAW_MATERIALS_NEROSIUM).add(ModItems.RAW_NEROSIUM.get());

        this.tag(Tags.Items.ORES)
                .add(ModItems.NEROSIUM_ORE_ITEM.get(), ModItems.DEEPSLATE_NEROSIUM_ORE_ITEM.get(),
                        ModItems.NEROSTEEL_ORE_ITEM.get(), ModItems.XERTZ_QUARTZ_ORE_ITEM.get(),
                        ModItems.CINDRITE_ORE_ITEM.get());
        this.tag(ModTags.Items.ORES_NEROSIUM)
                .add(ModItems.NEROSIUM_ORE_ITEM.get(), ModItems.DEEPSLATE_NEROSIUM_ORE_ITEM.get());
        this.tag(ModTags.Items.ORES_NEROSTEEL)
                .add(ModItems.NEROSTEEL_ORE_ITEM.get());
        this.tag(ModTags.Items.ORES_XERTZ_QUARTZ)
                .add(ModItems.XERTZ_QUARTZ_ORE_ITEM.get());

        this.tag(Tags.Items.STORAGE_BLOCKS)
                .add(ModItems.NEROSIUM_BLOCK_ITEM.get(), ModItems.RAW_NEROSIUM_BLOCK_ITEM.get(),
                        ModItems.NEROSTEEL_BLOCK_ITEM.get(), ModItems.CINDRITE_BLOCK_ITEM.get());
        this.tag(ModTags.Items.STORAGE_BLOCKS_NEROSIUM)
                .add(ModItems.NEROSIUM_BLOCK_ITEM.get());
        this.tag(ModTags.Items.STORAGE_BLOCKS_RAW_NEROSIUM)
                .add(ModItems.RAW_NEROSIUM_BLOCK_ITEM.get());
        this.tag(ModTags.Items.STORAGE_BLOCKS_NEROSTEEL)
                .add(ModItems.NEROSTEEL_BLOCK_ITEM.get());

        // Nerosteel material tags.
        this.tag(Tags.Items.INGOTS).add(ModItems.NEROSTEEL_INGOT.get());
        this.tag(ModTags.Items.INGOTS_NEROSTEEL).add(ModItems.NEROSTEEL_INGOT.get());
        this.tag(Tags.Items.RAW_MATERIALS).add(ModItems.RAW_NEROSTEEL.get());
        this.tag(ModTags.Items.RAW_MATERIALS_NEROSTEEL).add(ModItems.RAW_NEROSTEEL.get());

        // Xertz quartz is a gem-style drop.
        this.tag(Tags.Items.GEMS).add(ModItems.XERTZ_QUARTZ.get());
        this.tag(ModTags.Items.GEMS_XERTZ_QUARTZ).add(ModItems.XERTZ_QUARTZ.get());

        // Cindrite is a gem-style drop (and the Tier 2 Oxygen Suit's repair material).
        this.tag(Tags.Items.GEMS).add(ModItems.CINDRITE.get());
        this.tag(ModTags.Items.GEMS_CINDRITE).add(ModItems.CINDRITE.get());
    }
}

package za.co.neroland.nerospace.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;

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
        super(output, lookupProvider, blockTags, Nerospace.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(Tags.Items.INGOTS).add(ModItems.NEROSIUM_INGOT.get());
        this.tag(ModTags.Items.INGOTS_NEROSIUM).add(ModItems.NEROSIUM_INGOT.get());

        this.tag(Tags.Items.RAW_MATERIALS).add(ModItems.RAW_NEROSIUM.get());
        this.tag(ModTags.Items.RAW_MATERIALS_NEROSIUM).add(ModItems.RAW_NEROSIUM.get());

        this.tag(Tags.Items.ORES)
                .add(ModItems.NEROSIUM_ORE_ITEM.get(), ModItems.DEEPSLATE_NEROSIUM_ORE_ITEM.get());
        this.tag(ModTags.Items.ORES_NEROSIUM)
                .add(ModItems.NEROSIUM_ORE_ITEM.get(), ModItems.DEEPSLATE_NEROSIUM_ORE_ITEM.get());

        this.tag(Tags.Items.STORAGE_BLOCKS)
                .add(ModItems.NEROSIUM_BLOCK_ITEM.get(), ModItems.RAW_NEROSIUM_BLOCK_ITEM.get());
        this.tag(ModTags.Items.STORAGE_BLOCKS_NEROSIUM)
                .add(ModItems.NEROSIUM_BLOCK_ITEM.get());
        this.tag(ModTags.Items.STORAGE_BLOCKS_RAW_NEROSIUM)
                .add(ModItems.RAW_NEROSIUM_BLOCK_ITEM.get());
    }
}

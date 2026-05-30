package za.co.neroland.nerospace.datagen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.data.PackOutput;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * Generates block models + blockstates + client items, and flat item models.
 *
 * <p>Block items are generated automatically by {@link ModelProvider} from the default block
 * model location, so only non-block items need explicit item models here. Textures referenced by
 * these models must be supplied at {@code assets/nerospace/textures/{block,item}/&lt;name&gt;.png};
 * missing textures render as the "missing texture" placeholder but do not fail data generation.</p>
 */
public class ModModelProvider extends ModelProvider {

    public ModModelProvider(PackOutput output) {
        super(output, Nerospace.MODID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // Full cube blocks (cube_all): texture from assets/nerospace/textures/block/<name>.png
        blockModels.createTrivialCube(ModBlocks.NEROSIUM_ORE.get());
        blockModels.createTrivialCube(ModBlocks.DEEPSLATE_NEROSIUM_ORE.get());
        blockModels.createTrivialCube(ModBlocks.NEROSIUM_BLOCK.get());
        blockModels.createTrivialCube(ModBlocks.RAW_NEROSIUM_BLOCK.get());

        // Flat (item/generated) item models.
        itemModels.generateFlatItem(ModItems.RAW_NEROSIUM.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.NEROSIUM_INGOT.get(), ModelTemplates.FLAT_ITEM);

        // Handheld (item/handheld) model for the pickaxe.
        itemModels.generateFlatItem(ModItems.NEROSIUM_PICKAXE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
    }
}

package za.co.neroland.nerospace.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModItems;

/**
 * en_us translations. Generated into {@code src/generated/resources}; there must be no
 * hand-written {@code en_us.json} in {@code src/main/resources} or Gradle will report a duplicate
 * resource.
 */
public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(PackOutput output) {
        super(output, Nerospace.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        // Creative tab.
        add("itemGroup.nerospace", "Nerospace");

        // Blocks.
        add(ModBlocks.NEROSIUM_ORE.get(), "Nerosium Ore");
        add(ModBlocks.DEEPSLATE_NEROSIUM_ORE.get(), "Deepslate Nerosium Ore");
        add(ModBlocks.NEROSIUM_BLOCK.get(), "Block of Nerosium");
        add(ModBlocks.RAW_NEROSIUM_BLOCK.get(), "Block of Raw Nerosium");

        // Items.
        add(ModItems.RAW_NEROSIUM.get(), "Raw Nerosium");
        add(ModItems.NEROSIUM_INGOT.get(), "Nerosium Ingot");
        add(ModItems.NEROSIUM_PICKAXE.get(), "Nerosium Pickaxe");
    }
}

package za.co.neroland.nerospace.datagen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplate;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;

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
        // Grinder uses a trivial cube for now (single variant applies to all facings); a directional
        // oriented model can be layered on later without changing the block.
        blockModels.createTrivialCube(ModBlocks.NEROSIUM_GRINDER.get());

        // Phase 3 — Greenxertz ores + storage block.
        blockModels.createTrivialCube(ModBlocks.NEROSTEEL_ORE.get());
        blockModels.createTrivialCube(ModBlocks.XERTZ_QUARTZ_ORE.get());
        blockModels.createTrivialCube(ModBlocks.NEROSTEEL_BLOCK.get());

        // Phase 4 — launch pad: textured full cube (proper flat/raised shape comes with the planned
        // 3x3 multiblock pad). A hand-authored flat slab model caused missing-texture at runtime, so
        // this uses the reliable cube_all generator.
        blockModels.createTrivialCube(ModBlocks.ROCKET_LAUNCH_PAD.get());

        // Phase 8a — fuel tank machine (auto-fuels a rocket on an adjacent pad).
        blockModels.createTrivialCube(ModBlocks.FUEL_TANK.get());

        // Phase 8c — oxygen generator machine.
        blockModels.createTrivialCube(ModBlocks.OXYGEN_GENERATOR.get());

        // Terraform design — terraformer machine.
        blockModels.createTrivialCube(ModBlocks.TERRAFORMER.get());

        // Power grid — connection-aware translucent pipe (multipart: core + one arm per connected
        // face; translucency comes from the texture's alpha) + generators.
        registerUniversalPipe(blockModels);
        blockModels.createTrivialCube(ModBlocks.COMBUSTION_GENERATOR.get());
        blockModels.createTrivialCube(ModBlocks.PASSIVE_GENERATOR.get());

        // Storage endpoints + creative sources.
        blockModels.createTrivialCube(ModBlocks.BATTERY.get());
        blockModels.createTrivialCube(ModBlocks.CREATIVE_BATTERY.get());
        blockModels.createTrivialCube(ModBlocks.FLUID_TANK.get());
        blockModels.createTrivialCube(ModBlocks.CREATIVE_FLUID_TANK.get());
        blockModels.createTrivialCube(ModBlocks.GAS_TANK.get());
        blockModels.createTrivialCube(ModBlocks.CREATIVE_GAS_TANK.get());
        blockModels.createTrivialCube(ModBlocks.ITEM_STORE.get());
        blockModels.createTrivialCube(ModBlocks.CREATIVE_ITEM_STORE.get());

        // Phase 7 — Cindara ore + storage block.
        blockModels.createTrivialCube(ModBlocks.CINDRITE_ORE.get());
        blockModels.createTrivialCube(ModBlocks.CINDRITE_BLOCK.get());

        // Phase 7c — station building blocks.
        blockModels.createTrivialCube(ModBlocks.STATION_FLOOR.get());
        blockModels.createTrivialCube(ModBlocks.STATION_WALL.get());

        // Phase 7b — the rocket fuel liquid block: a particle-only blockstate (the fluid itself is
        // drawn by the FluidType render, not a block model).
        blockModels.createParticleOnlyBlock(ModBlocks.ROCKET_FUEL_BLOCK.get());

        // Flat (item/generated) item models.
        itemModels.generateFlatItem(ModItems.RAW_NEROSIUM.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.NEROSIUM_INGOT.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.NEROSIUM_DUST.get(), ModelTemplates.FLAT_ITEM);
        // Phase 3 items.
        itemModels.generateFlatItem(ModItems.RAW_NEROSTEEL.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.NEROSTEEL_INGOT.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.XERTZ_QUARTZ.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.GREENXERTZ_NAVIGATOR.get(), ModelTemplates.FLAT_ITEM);

        // Phase 4 — rocket items + fuel canister.
        itemModels.generateFlatItem(ModItems.ROCKET_FUEL_CANISTER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.ROCKET_TIER_1.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.ROCKET_TIER_2.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.ROCKET_TIER_3.get(), ModelTemplates.FLAT_ITEM);

        // Phase 7 — Cindara.
        itemModels.generateFlatItem(ModItems.CINDRITE.get(), ModelTemplates.FLAT_ITEM);

        // Phase 7b — rocket fuel bucket.
        itemModels.generateFlatItem(ModItems.ROCKET_FUEL_BUCKET.get(), ModelTemplates.FLAT_ITEM);

        // Phase 7 polish — destination compasses.
        itemModels.generateFlatItem(ModItems.STATION_COMPASS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.GREENXERTZ_COMPASS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.CINDARA_COMPASS.get(), ModelTemplates.FLAT_ITEM);

        // Handheld (item/handheld) model for the pickaxe.
        itemModels.generateFlatItem(ModItems.NEROSIUM_PICKAXE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);

        // Power grid — the Configurator tool.
        itemModels.generateFlatItem(ModItems.CONFIGURATOR.get(), ModelTemplates.FLAT_HANDHELD_ITEM);

        // Phase 8d — oxygen suit (inventory item models; the worn layer is the equipment asset).
        itemModels.generateFlatItem(ModItems.OXYGEN_SUIT_HELMET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.OXYGEN_SUIT_CHESTPLATE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.OXYGEN_SUIT_LEGGINGS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.OXYGEN_SUIT_BOOTS.get(), ModelTemplates.FLAT_ITEM);

        // Phase 10e — spawn eggs (custom flat egg icons, not the procedural tinted template).
        itemModels.generateFlatItem(ModItems.XERTZ_STALKER_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.QUARTZ_CRAWLER_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.GREENLING_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.CINDER_STALKER_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
    }

    /**
     * The Universal Pipe: a multipart blockstate — always the translucent core (4..12 cube), plus one
     * arm per connected face (the north arm model rotated for the other five). Translucency comes from
     * the {@code universal_pipe_glass} texture's alpha (26.1 derives chunk layers from the sprites).
     */
    private void registerUniversalPipe(BlockModelGenerators blockModels) {
        Block pipe = ModBlocks.UNIVERSAL_PIPE.get();
        var glass = TextureMapping.getBlockTexture(pipe, "_glass");
        TextureMapping mapping = new TextureMapping().put(TextureSlot.ALL, glass).put(TextureSlot.PARTICLE, glass);

        ExtendedModelTemplate coreTemplate = ExtendedModelTemplateBuilder.builder()
                .requiredTextureSlot(TextureSlot.ALL)
                .requiredTextureSlot(TextureSlot.PARTICLE)
                .ambientOcclusion(false)
                .element(e -> e.from(4, 4, 4).to(12, 12, 12)
                        .allFaces((dir, face) -> face.texture(TextureSlot.ALL)))
                .build();
        ExtendedModelTemplate armTemplate = ExtendedModelTemplateBuilder.builder()
                .requiredTextureSlot(TextureSlot.ALL)
                .requiredTextureSlot(TextureSlot.PARTICLE)
                .ambientOcclusion(false)
                .element(e -> e.from(4, 4, 0).to(12, 12, 4)
                        .allFaces((dir, face) -> face.texture(TextureSlot.ALL)))
                .build();

        Identifier core = coreTemplate.create(
                ModelLocationUtils.getModelLocation(pipe, "_core"), mapping, blockModels.modelOutput);
        Identifier arm = armTemplate.create(
                ModelLocationUtils.getModelLocation(pipe, "_arm"), mapping, blockModels.modelOutput);

        blockModels.blockStateOutput.accept(MultiPartGenerator.multiPart(pipe)
                .with(BlockModelGenerators.plainVariant(core))
                .with(new ConditionBuilder().term(BlockStateProperties.NORTH, true),
                        BlockModelGenerators.plainVariant(arm))
                .with(new ConditionBuilder().term(BlockStateProperties.EAST, true),
                        BlockModelGenerators.plainVariant(arm).with(BlockModelGenerators.Y_ROT_90))
                .with(new ConditionBuilder().term(BlockStateProperties.SOUTH, true),
                        BlockModelGenerators.plainVariant(arm).with(BlockModelGenerators.Y_ROT_180))
                .with(new ConditionBuilder().term(BlockStateProperties.WEST, true),
                        BlockModelGenerators.plainVariant(arm).with(BlockModelGenerators.Y_ROT_270))
                .with(new ConditionBuilder().term(BlockStateProperties.UP, true),
                        BlockModelGenerators.plainVariant(arm).with(BlockModelGenerators.X_ROT_270))
                .with(new ConditionBuilder().term(BlockStateProperties.DOWN, true),
                        BlockModelGenerators.plainVariant(arm).with(BlockModelGenerators.X_ROT_90)));

        // The block item shows the core cube.
        blockModels.registerSimpleItemModel(pipe, core);
    }
}

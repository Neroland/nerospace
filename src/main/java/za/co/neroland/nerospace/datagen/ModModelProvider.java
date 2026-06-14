package za.co.neroland.nerospace.datagen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
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
        // Art overhaul §3: every machine is a shaped, element-built model (visual only — collision
        // stays the full cube, which is the oxygen-sealing contract). FACING machines dispatch the
        // model on the blockstate so the front follows the placer.
        registerShapedMachines(blockModels);

        // Phase 3 — Greenxertz ores + storage block.
        blockModels.createTrivialCube(ModBlocks.NEROSTEEL_ORE.get());
        blockModels.createTrivialCube(ModBlocks.XERTZ_QUARTZ_ORE.get());
        blockModels.createTrivialCube(ModBlocks.NEROSTEEL_BLOCK.get());

        // Phase 4 — launch pad: textured full cube (proper flat/raised shape comes with the planned
        // 3x3 multiblock pad). A hand-authored flat slab model caused missing-texture at runtime, so
        // this uses the reliable cube_all generator.
        // Launch pad: a flat 3px plate matching its collision shape (LAUNCH_PAD_DESIGN.md sign-off),
        // not the old near-full cube. Element-built like the pipe's custom models.
        Block pad = ModBlocks.ROCKET_LAUNCH_PAD.get();
        var padTexture = TextureMapping.getBlockTexture(pad);
        TextureMapping padMapping = new TextureMapping()
                .put(TextureSlot.ALL, padTexture).put(TextureSlot.PARTICLE, padTexture);
        ExtendedModelTemplate padTemplate = ExtendedModelTemplateBuilder.builder()
                .requiredTextureSlot(TextureSlot.ALL)
                .requiredTextureSlot(TextureSlot.PARTICLE)
                .element(e -> e.from(0, 0, 0).to(16, 3, 16)
                        .allFaces((dir, face) -> face.texture(TextureSlot.ALL)))
                .build();
        Identifier padModel = padTemplate.create(
                ModelLocationUtils.getModelLocation(pad), padMapping, blockModels.modelOutput);
        blockModels.blockStateOutput.accept(
                BlockModelGenerators.createSimpleBlock(pad, BlockModelGenerators.plainVariant(padModel)));

        // Launch Gantry — shaped tower in registerShapedMachines (art overhaul §3).

        // Power grid — connection-aware translucent pipe (multipart: core + one arm per connected
        // face; translucency comes from the texture's alpha). Machines + storage endpoints are
        // shaped in registerShapedMachines (art overhaul §3).
        registerUniversalPipe(blockModels);

        // Phase 7 — Cindara ore + storage block.
        blockModels.createTrivialCube(ModBlocks.CINDRITE_ORE.get());
        blockModels.createTrivialCube(ModBlocks.CINDRITE_BLOCK.get());

        // Glacira ore + storage block (NEW_DESTINATION_DESIGN.md).
        blockModels.createTrivialCube(ModBlocks.GLACITE_ORE.get());
        blockModels.createTrivialCube(ModBlocks.GLACITE_BLOCK.get());

        // Phase 7c — station building blocks.
        blockModels.createTrivialCube(ModBlocks.STATION_FLOOR.get());
        blockModels.createTrivialCube(ModBlocks.STATION_WALL.get());

        // Quarry / Miner (MINER_DESIGN): controller + landmark cubes; the frame is a real 3-D open
        // structural frame (four corner posts + edge rails, see-through centre — BuildCraft-style),
        // built from the same beam recipe as the tanks. Emissive via the bright strut texture + the
        // block's light level; .noOcclusion() (ModBlocks) stops the open gaps culling the world behind.
        blockModels.createTrivialCube(ModBlocks.QUARRY_CONTROLLER.get());
        blockModels.createTrivialCube(ModBlocks.QUARRY_LANDMARK.get());
        registerQuarryFrame(blockModels);

        // Developer diagnostics — Sentry test block (hidden; /give only).
        blockModels.createTrivialCube(ModBlocks.SENTRY_TEST.get());

        // Station Core (MULTI_STATION_DESIGN.md; founding-only, but the model/blockstate still gen).
        blockModels.createTrivialCube(ModBlocks.STATION_CORE.get());

        // Phase 7b — the rocket fuel liquid block: a particle-only blockstate (the fluid itself is
        // drawn by the FluidType render, not a block model).
        blockModels.createParticleOnlyBlock(ModBlocks.ROCKET_FUEL_BLOCK.get());

        // Star Guide pedestal — shaped in registerShapedMachines (art overhaul §3).

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
        itemModels.generateFlatItem(ModItems.ROCKET_TIER_4.get(), ModelTemplates.FLAT_ITEM);

        // Phase 7 — Cindara.
        itemModels.generateFlatItem(ModItems.CINDRITE.get(), ModelTemplates.FLAT_ITEM);

        // Glacira (NEW_DESTINATION_DESIGN.md).
        itemModels.generateFlatItem(ModItems.GLACITE.get(), ModelTemplates.FLAT_ITEM);

        // Phase 7b — rocket fuel bucket.
        itemModels.generateFlatItem(ModItems.ROCKET_FUEL_BUCKET.get(), ModelTemplates.FLAT_ITEM);

        // Phase 7 polish — destination compasses.
        itemModels.generateFlatItem(ModItems.STATION_COMPASS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.GREENXERTZ_COMPASS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.CINDARA_COMPASS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.GLACIRA_COMPASS.get(), ModelTemplates.FLAT_ITEM);

        // Handheld (item/handheld) model for the pickaxe.
        itemModels.generateFlatItem(ModItems.NEROSIUM_PICKAXE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);

        // Power grid — the Configurator tool + filter/upgrade modules.
        itemModels.generateFlatItem(ModItems.CONFIGURATOR.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        itemModels.generateFlatItem(ModItems.PIPE_FILTER.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.SPEED_UPGRADE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.CAPACITY_UPGRADE.get(), ModelTemplates.FLAT_ITEM);

        // Quarry / Miner — frame casing + cross-machine upgrade module cards.
        itemModels.generateFlatItem(ModItems.FRAME_CASING.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.SPEED_MODULE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.EFFICIENCY_MODULE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.FORTUNE_MODULE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.SILK_TOUCH_MODULE.get(), ModelTemplates.FLAT_ITEM);

        // Phase 8d — oxygen suit (inventory item models; the worn layer is the equipment asset).
        itemModels.generateFlatItem(ModItems.OXYGEN_SUIT_HELMET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.OXYGEN_SUIT_CHESTPLATE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.OXYGEN_SUIT_LEGGINGS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.OXYGEN_SUIT_BOOTS.get(), ModelTemplates.FLAT_ITEM);

        // Suit-and-station integration — Tier 2 (cindrite-upgraded) oxygen suit.
        itemModels.generateFlatItem(ModItems.OXYGEN_SUIT_T2_HELMET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.OXYGEN_SUIT_T2_CHESTPLATE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.OXYGEN_SUIT_T2_LEGGINGS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.OXYGEN_SUIT_T2_BOOTS.get(), ModelTemplates.FLAT_ITEM);

        // Hazard suit variants (SUIT_HAZARD_DESIGN.md).
        itemModels.generateFlatItem(ModItems.OXYGEN_SUIT_HEAT_HELMET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.OXYGEN_SUIT_HEAT_CHESTPLATE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.OXYGEN_SUIT_HEAT_LEGGINGS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.OXYGEN_SUIT_HEAT_BOOTS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.OXYGEN_SUIT_COLD_HELMET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.OXYGEN_SUIT_COLD_CHESTPLATE.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.OXYGEN_SUIT_COLD_LEGGINGS.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.OXYGEN_SUIT_COLD_BOOTS.get(), ModelTemplates.FLAT_ITEM);

        // Star Guide book.
        itemModels.generateFlatItem(ModItems.STAR_GUIDE_BOOK.get(), ModelTemplates.FLAT_ITEM);

        // Station Charter (MULTI_STATION_DESIGN.md).
        itemModels.generateFlatItem(ModItems.STATION_CHARTER.get(), ModelTemplates.FLAT_ITEM);

        // Phase 10e — spawn eggs (custom flat egg icons, not the procedural tinted template).
        itemModels.generateFlatItem(ModItems.XERTZ_STALKER_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.QUARTZ_CRAWLER_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.GREENLING_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.CINDER_STALKER_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.FROST_STRIDER_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);

        // Terraform livestock (DEEPER_TERRAFORM_DESIGN.md §5): eggs + drops.
        itemModels.generateFlatItem(ModItems.MEADOW_LOPER_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.EMBER_STRUTTER_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.WOOLLY_DRIFT_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.LOPER_HAUNCH.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.STRUTTER_DRUMSTICK.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.DRIFT_FLEECE.get(), ModelTemplates.FLAT_ITEM);
    }

    // --- Art overhaul §3: shaped machines (element-built; visual only, collision untouched) -----

    /** The tank-content core texture slot ({@code fluid/gas/fuel_tank_core.png}). */
    private static final TextureSlot CORE = TextureSlot.create("core");

    /** side (= the block's own texture) + particle; optional {@code _front} / {@code _top}. */
    private TextureMapping machineMapping(Block block, boolean front, boolean top) {
        var side = TextureMapping.getBlockTexture(block);
        TextureMapping mapping = new TextureMapping()
                .put(TextureSlot.SIDE, side)
                .put(TextureSlot.PARTICLE, side);
        if (front) {
            mapping.put(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front"));
        }
        if (top) {
            mapping.put(TextureSlot.TOP, TextureMapping.getBlockTexture(block, "_top"));
        }
        return mapping;
    }

    /** Creates the block's default-location model and wires the blockstate (rotating if facing). */
    private void shapedBlock(BlockModelGenerators blockModels, Block block,
            ExtendedModelTemplate template, TextureMapping mapping, boolean facing) {
        Identifier model = template.create(
                ModelLocationUtils.getModelLocation(block), mapping, blockModels.modelOutput);
        if (facing) {
            blockModels.blockStateOutput.accept(
                    MultiVariantGenerator.dispatch(block, BlockModelGenerators.plainVariant(model))
                            .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
        } else {
            blockModels.blockStateOutput.accept(
                    BlockModelGenerators.createSimpleBlock(block, BlockModelGenerators.plainVariant(model)));
        }
    }

    /** A template builder with the standard machine slots pre-declared. */
    private static ExtendedModelTemplateBuilder machineTemplate(boolean front, boolean top) {
        ExtendedModelTemplateBuilder builder = ExtendedModelTemplateBuilder.builder()
                .requiredTextureSlot(TextureSlot.SIDE)
                .requiredTextureSlot(TextureSlot.PARTICLE);
        if (front) {
            builder.requiredTextureSlot(TextureSlot.FRONT);
        }
        if (top) {
            builder.requiredTextureSlot(TextureSlot.TOP);
        }
        return builder;
    }

    /** All faces of the element take {@code slot}, except {@code north} -> FRONT when given. */
    private static void faces(net.neoforged.neoforge.client.model.generators.template.ElementBuilder e,
            TextureSlot slot, TextureSlot north) {
        e.allFaces((dir, face) -> face.texture(
                north != null && dir == net.minecraft.core.Direction.NORTH ? north : slot));
    }

    private void registerShapedMachines(BlockModelGenerators blockModels) {
        // Nerosium Grinder (FACING): body + hopper-mouth rim; teeth painted into the front texture.
        shapedBlock(blockModels, ModBlocks.NEROSIUM_GRINDER.get(), machineTemplate(true, true)
                .element(e -> { e.from(0, 0, 0).to(16, 14, 16); faces(e, TextureSlot.SIDE, TextureSlot.FRONT);
                    e.face(net.minecraft.core.Direction.UP, f -> f.texture(TextureSlot.TOP)); })
                .element(e -> { e.from(0, 14, 0).to(16, 16, 2); faces(e, TextureSlot.SIDE, null); })
                .element(e -> { e.from(0, 14, 14).to(16, 16, 16); faces(e, TextureSlot.SIDE, null); })
                .element(e -> { e.from(0, 14, 2).to(2, 16, 14); faces(e, TextureSlot.SIDE, null); })
                .element(e -> { e.from(14, 14, 2).to(16, 16, 14); faces(e, TextureSlot.SIDE, null); })
                .build(), machineMapping(ModBlocks.NEROSIUM_GRINDER.get(), true, true), true);

        // Combustion Generator (FACING): body + offset chimney stub; firebox front.
        shapedBlock(blockModels, ModBlocks.COMBUSTION_GENERATOR.get(), machineTemplate(true, true)
                .element(e -> { e.from(0, 0, 0).to(16, 13, 16); faces(e, TextureSlot.SIDE, TextureSlot.FRONT);
                    e.face(net.minecraft.core.Direction.UP, f -> f.texture(TextureSlot.TOP)); })
                .element(e -> { e.from(9, 13, 9).to(14, 16, 14); faces(e, TextureSlot.SIDE, null); })
                .build(), machineMapping(ModBlocks.COMBUSTION_GENERATOR.get(), true, true), true);

        // Fuel Refinery (no FACING): a body under a central refining stack (single side texture).
        shapedBlock(blockModels, ModBlocks.FUEL_REFINERY.get(), machineTemplate(false, false)
                .element(e -> { e.from(0, 0, 0).to(16, 13, 16); faces(e, TextureSlot.SIDE, null); })
                .element(e -> { e.from(5, 13, 5).to(11, 16, 11); faces(e, TextureSlot.SIDE, null); })
                .build(), machineMapping(ModBlocks.FUEL_REFINERY.get(), false, false), false);

        // Passive Generator: pedestal under a floating collector panel.
        shapedBlock(blockModels, ModBlocks.PASSIVE_GENERATOR.get(), machineTemplate(false, true)
                .element(e -> { e.from(3, 0, 3).to(13, 7, 13); faces(e, TextureSlot.SIDE, null); })
                .element(e -> { e.from(0, 7, 0).to(16, 11, 16); faces(e, TextureSlot.SIDE, null);
                    e.face(net.minecraft.core.Direction.UP, f -> f.texture(TextureSlot.TOP)); })
                .build(), machineMapping(ModBlocks.PASSIVE_GENERATOR.get(), false, true), false);

        // Oxygen Generator: body + two-step electrolysis dome (dome faces use the top texture).
        shapedBlock(blockModels, ModBlocks.OXYGEN_GENERATOR.get(), machineTemplate(false, true)
                .element(e -> { e.from(0, 0, 0).to(16, 11, 16); faces(e, TextureSlot.SIDE, null); })
                .element(e -> { e.from(3, 11, 3).to(13, 14, 13); faces(e, TextureSlot.TOP, null); })
                .element(e -> { e.from(5, 14, 5).to(11, 16, 11); faces(e, TextureSlot.TOP, null); })
                .build(), machineMapping(ModBlocks.OXYGEN_GENERATOR.get(), false, true), false);

        // Terraformer (FACING): soil tray under the machine body; core lens on the front texture.
        shapedBlock(blockModels, ModBlocks.TERRAFORMER.get(), machineTemplate(true, true)
                .element(e -> { e.from(0, 0, 0).to(16, 4, 16); faces(e, TextureSlot.TOP, null); })
                .element(e -> { e.from(1, 4, 1).to(15, 16, 15); faces(e, TextureSlot.SIDE, TextureSlot.FRONT); })
                .build(), machineMapping(ModBlocks.TERRAFORMER.get(), true, true), true);

        // Hydration Module (FACING): body + front melt-window plate + top tank ridge.
        shapedBlock(blockModels, ModBlocks.HYDRATION_MODULE.get(), machineTemplate(true, true)
                .element(e -> { e.from(0, 0, 1).to(16, 15, 16); faces(e, TextureSlot.SIDE, null); })
                .element(e -> { e.from(2, 2, 0).to(14, 13, 1); faces(e, TextureSlot.FRONT, null); })
                .element(e -> { e.from(3, 15, 4).to(13, 16, 12); faces(e, TextureSlot.TOP, null); })
                .build(), machineMapping(ModBlocks.HYDRATION_MODULE.get(), true, true), true);

        // Terraform Monitor (FACING): foot + pillar + tilted screen (front = the display).
        shapedBlock(blockModels, ModBlocks.TERRAFORM_MONITOR.get(), machineTemplate(true, false)
                .element(e -> { e.from(4, 0, 4).to(12, 2, 12); faces(e, TextureSlot.SIDE, null); })
                .element(e -> { e.from(6, 2, 6).to(10, 6, 10); faces(e, TextureSlot.SIDE, null); })
                .element(e -> { e.from(1, 5, 6).to(15, 14, 8); faces(e, TextureSlot.SIDE, TextureSlot.FRONT);
                    e.rotation(r -> r.origin(8, 6, 7).singleAxis(net.minecraft.core.Direction.Axis.X, -22.5F)); })
                .build(), machineMapping(ModBlocks.TERRAFORM_MONITOR.get(), true, false), true);

        // Battery: body + twin terminal caps.
        shapedBlock(blockModels, ModBlocks.BATTERY.get(), machineTemplate(false, true)
                .element(e -> { e.from(1, 0, 1).to(15, 14, 15); faces(e, TextureSlot.SIDE, null); })
                .element(e -> { e.from(3, 14, 3).to(7, 16, 7); faces(e, TextureSlot.TOP, null); })
                .element(e -> { e.from(9, 14, 9).to(13, 16, 13); faces(e, TextureSlot.TOP, null); })
                .build(), machineMapping(ModBlocks.BATTERY.get(), false, true), false);
        shapedBlock(blockModels, ModBlocks.CREATIVE_BATTERY.get(), machineTemplate(false, true)
                .element(e -> { e.from(1, 0, 1).to(15, 14, 15); faces(e, TextureSlot.SIDE, null); })
                .element(e -> { e.from(3, 14, 3).to(7, 16, 7); faces(e, TextureSlot.TOP, null); })
                .element(e -> { e.from(9, 14, 9).to(13, 16, 13); faces(e, TextureSlot.TOP, null); })
                .build(), machineMapping(ModBlocks.CREATIVE_BATTERY.get(), false, true), false);

        // Item Store (FACING): body + drawer-front plate (handle painted into the front texture).
        shapedBlock(blockModels, ModBlocks.ITEM_STORE.get(), machineTemplate(true, true)
                .element(e -> { e.from(0, 0, 1).to(16, 16, 16); faces(e, TextureSlot.SIDE, null);
                    e.face(net.minecraft.core.Direction.UP, f -> f.texture(TextureSlot.TOP)); })
                .element(e -> { e.from(1, 1, 0).to(15, 15, 1); faces(e, TextureSlot.FRONT, null); })
                .build(), machineMapping(ModBlocks.ITEM_STORE.get(), true, true), true);
        // Creative Item Store keeps the plain cube (no drawer — it conjures items, not stores them).
        blockModels.createTrivialCube(ModBlocks.CREATIVE_ITEM_STORE.get());
        // Trash Can — plain textured cube.
        blockModels.createTrivialCube(ModBlocks.TRASH_CAN.get());

        // Tanks: a corner-beam frame around a visible content core (real depth, no cutout needed).
        registerTank(blockModels, ModBlocks.FLUID_TANK.get());
        registerTank(blockModels, ModBlocks.CREATIVE_FLUID_TANK.get());
        registerTank(blockModels, ModBlocks.GAS_TANK.get());
        registerTank(blockModels, ModBlocks.CREATIVE_GAS_TANK.get());
        registerTank(blockModels, ModBlocks.FUEL_TANK.get());

        // Launch Gantry: open service tower — four corner posts, a mid brace, a top platform.
        ExtendedModelTemplateBuilder gantry = machineTemplate(false, true);
        for (int[] c : new int[][] {{0, 0}, {13, 0}, {0, 13}, {13, 13}}) {
            int x = c[0];
            int z = c[1];
            gantry.element(e -> { e.from(x, 0, z).to(x + 3, 14, z + 3); faces(e, TextureSlot.SIDE, null); });
        }
        shapedBlock(blockModels, ModBlocks.LAUNCH_GANTRY.get(), gantry
                .element(e -> { e.from(3, 7, 3).to(13, 9, 13); faces(e, TextureSlot.SIDE, null); })
                .element(e -> { e.from(0, 14, 0).to(16, 16, 16); faces(e, TextureSlot.SIDE, null);
                    e.face(net.minecraft.core.Direction.UP, f -> f.texture(TextureSlot.TOP)); })
                .build(), machineMapping(ModBlocks.LAUNCH_GANTRY.get(), false, true), false);

        // Star Guide: pedestal — base slab, column, star-faced crown (single texture).
        var starTexture = TextureMapping.getBlockTexture(ModBlocks.STAR_GUIDE.get());
        TextureMapping starMapping = new TextureMapping()
                .put(TextureSlot.ALL, starTexture).put(TextureSlot.PARTICLE, starTexture);
        ExtendedModelTemplate starTemplate = ExtendedModelTemplateBuilder.builder()
                .requiredTextureSlot(TextureSlot.ALL)
                .requiredTextureSlot(TextureSlot.PARTICLE)
                .element(e -> e.from(1, 0, 1).to(15, 3, 15)
                        .allFaces((dir, face) -> face.texture(TextureSlot.ALL)))
                .element(e -> e.from(5, 3, 5).to(11, 10, 11)
                        .allFaces((dir, face) -> face.texture(TextureSlot.ALL)))
                .element(e -> e.from(2, 10, 2).to(14, 13, 14)
                        .allFaces((dir, face) -> face.texture(TextureSlot.ALL)))
                .build();
        shapedBlock(blockModels, ModBlocks.STAR_GUIDE.get(), starTemplate, starMapping, false);
    }

    /**
     * The quarry frame: a 3-D OPEN structural frame — four 2px corner posts + top/bottom edge rails,
     * with a see-through centre (no content core, unlike {@link #registerTank}). Reuses the tank's
     * beam layout so the gantry reads as a matching machine. The block's own {@code quarry_frame}
     * texture skins every strut.
     */
    private void registerQuarryFrame(BlockModelGenerators blockModels) {
        Block block = ModBlocks.QUARRY_FRAME.get();
        var beam = TextureMapping.getBlockTexture(block);
        TextureMapping mapping = new TextureMapping()
                .put(TextureSlot.SIDE, beam)
                .put(TextureSlot.PARTICLE, beam);
        ExtendedModelTemplateBuilder builder = ExtendedModelTemplateBuilder.builder()
                .requiredTextureSlot(TextureSlot.SIDE)
                .requiredTextureSlot(TextureSlot.PARTICLE)
                .ambientOcclusion(false); // emissive struts read flat, no AO darkening at the joints
        // four vertical corner posts
        for (int[] c : new int[][] {{0, 0}, {14, 0}, {0, 14}, {14, 14}}) {
            int x = c[0];
            int z = c[1];
            builder.element(e -> { e.from(x, 0, z).to(x + 2, 16, z + 2); faces(e, TextureSlot.SIDE, null); });
        }
        // horizontal rails, bottom + top, both axes (the centre stays open)
        for (int y : new int[] {0, 14}) {
            int yy = y;
            builder.element(e -> { e.from(2, yy, 0).to(14, yy + 2, 2); faces(e, TextureSlot.SIDE, null); });
            builder.element(e -> { e.from(2, yy, 14).to(14, yy + 2, 16); faces(e, TextureSlot.SIDE, null); });
            builder.element(e -> { e.from(0, yy, 2).to(2, yy + 2, 14); faces(e, TextureSlot.SIDE, null); });
            builder.element(e -> { e.from(14, yy, 2).to(16, yy + 2, 14); faces(e, TextureSlot.SIDE, null); });
        }
        shapedBlock(blockModels, block, builder.build(), mapping, false);
    }

    /** A tank: 12 frame beams (the block's own texture) around a {@code <name>_core} content core. */
    private void registerTank(BlockModelGenerators blockModels, Block block) {
        var frame = TextureMapping.getBlockTexture(block);
        TextureMapping mapping = new TextureMapping()
                .put(TextureSlot.SIDE, frame)
                .put(TextureSlot.PARTICLE, frame)
                .put(CORE, TextureMapping.getBlockTexture(block, "_core"));
        ExtendedModelTemplateBuilder builder = ExtendedModelTemplateBuilder.builder()
                .requiredTextureSlot(TextureSlot.SIDE)
                .requiredTextureSlot(TextureSlot.PARTICLE)
                .requiredTextureSlot(CORE)
                // content core, visible between the beams
                .element(e -> { e.from(2, 2, 2).to(14, 14, 14); faces(e, CORE, null); });
        // four vertical corner posts
        for (int[] c : new int[][] {{0, 0}, {14, 0}, {0, 14}, {14, 14}}) {
            int x = c[0];
            int z = c[1];
            builder.element(e -> { e.from(x, 0, z).to(x + 2, 16, z + 2); faces(e, TextureSlot.SIDE, null); });
        }
        // horizontal rails, bottom + top, both axes
        for (int y : new int[] {0, 14}) {
            int yy = y;
            builder.element(e -> { e.from(2, yy, 0).to(14, yy + 2, 2); faces(e, TextureSlot.SIDE, null); });
            builder.element(e -> { e.from(2, yy, 14).to(14, yy + 2, 16); faces(e, TextureSlot.SIDE, null); });
            builder.element(e -> { e.from(0, yy, 2).to(2, yy + 2, 14); faces(e, TextureSlot.SIDE, null); });
            builder.element(e -> { e.from(14, yy, 2).to(16, yy + 2, 14); faces(e, TextureSlot.SIDE, null); });
        }
        shapedBlock(blockModels, block, builder.build(), mapping, false);
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

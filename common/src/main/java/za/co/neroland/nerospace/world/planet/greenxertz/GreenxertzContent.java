package za.co.neroland.nerospace.world.planet.greenxertz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
//? if <26.3 {
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
//?}

import za.co.neroland.nerospace.block.CappedSpreadBlock;
import za.co.neroland.nerospace.block.rules.SporeSpreadRules;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModFeatures;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;
import za.co.neroland.nerospace.world.planet.greenxertz.block.AmberSapOreBlock;
import za.co.neroland.nerospace.world.planet.greenxertz.block.BioBarkLogBlock;
import za.co.neroland.nerospace.world.planet.greenxertz.block.GlowcapFungusBlock;
import za.co.neroland.nerospace.world.planet.greenxertz.block.SporePodBlock;
import za.co.neroland.nerospace.world.planet.greenxertz.block.XertzVineBlock;
import za.co.neroland.nerospace.world.planet.greenxertz.world.GiantGlowcapFeature;

/**
 * Every new Greenxertz block and item, registered through the shared providers. The 1.x Greenxertz ids (Nerosteel
 * Ore, Xertz Quartz Ore, Xertz Quartz) stay where they are in {@code ModBlocks}/{@code ModItems}; this class only
 * lists them in the planets tab. Blocks reuse the shared behaviour bases: Spore Moss is the capped spreader
 * ({@link CappedSpreadBlock}, x3 in a spore bloom, point nausea), the Spore Pod bursts into the gated spore cloud
 * ({@code SporeBurstBlock}), Xertz Soil is the only rootable substrate (tag {@code nerospace:xertz_rootable}).
 * Class loading registers everything; {@link #init()} only forces the load.
 */
public final class GreenxertzContent {

    // --- Blocks (creative-tab order) ---------------------------------------------------------------------------
    public static final RegistryEntry<Block> XERTZ_SOIL = ModBlocks.BLOCKS.register("xertz_soil",
            key -> new Block(props(key).mapColor(MapColor.DIRT).strength(0.5F, 0.5F).sound(SoundType.ROOTED_DIRT)));

    public static final RegistryEntry<BioBarkLogBlock> BIO_BARK_LOG = ModBlocks.BLOCKS.register("bio_bark_log",
            key -> new BioBarkLogBlock(props(key).mapColor(MapColor.COLOR_BROWN).strength(2.0F, 2.0F)
                    .sound(SoundType.WOOD)));

    /** Carries the heal-on-wake behaviour ({@link BioBarkHeal}); the block itself is a plain log. */
    public static final RegistryEntry<RotatedPillarBlock> STRIPPED_BIO_BARK_LOG = ModBlocks.BLOCKS.register(
            "stripped_bio_bark_log",
            key -> new RotatedPillarBlock(props(key).mapColor(MapColor.WOOD).strength(2.0F, 2.0F)
                    .sound(SoundType.WOOD)));

    public static final RegistryEntry<XertzVineBlock> XERTZ_VINE = ModBlocks.BLOCKS.register("xertz_vine",
            key -> new XertzVineBlock(props(key).mapColor(MapColor.COLOR_GREEN).strength(0.2F, 0.2F)
                    .noCollision().noOcclusion().randomTicks().sound(SoundType.WEEPING_VINES)));

    public static final RegistryEntry<CappedSpreadBlock> SPORE_MOSS = ModBlocks.BLOCKS.register("spore_moss",
            key -> new CappedSpreadBlock(props(key).mapColor(MapColor.COLOR_LIGHT_GREEN).strength(0.1F, 0.1F)
                    .randomTicks().sound(SoundType.MOSS_CARPET).lightLevel(s -> SporeSpreadRules.LIGHT)));

    public static final RegistryEntry<GlowcapFungusBlock> GLOWCAP_FUNGUS = ModBlocks.BLOCKS.register(
            "glowcap_fungus",
            key -> new GlowcapFungusBlock(props(key).mapColor(MapColor.COLOR_LIGHT_GREEN).instabreak()
                    .noCollision().noOcclusion().sound(SoundType.FUNGUS).lightLevel(s -> 12)));

    public static final RegistryEntry<Block> GLOWCAP_BLOCK = ModBlocks.BLOCKS.register("glowcap_block",
            key -> new Block(props(key).mapColor(MapColor.COLOR_LIGHT_GREEN).strength(0.3F, 0.3F)
                    .sound(SoundType.FUNGUS).lightLevel(s -> 10)));

    public static final RegistryEntry<SporePodBlock> SPORE_POD = ModBlocks.BLOCKS.register("spore_pod",
            key -> new SporePodBlock(props(key).mapColor(MapColor.COLOR_PINK).strength(0.3F, 0.3F)
                    .noCollision().noOcclusion().sound(SoundType.FUNGUS)));

    public static final RegistryEntry<AmberSapOreBlock> AMBER_SAP_ORE = ModBlocks.BLOCKS.register("amber_sap_ore",
            key -> new AmberSapOreBlock(props(key).mapColor(MapColor.COLOR_ORANGE).strength(2.5F, 2.5F)
                    .requiresCorrectToolForDrops().sound(SoundType.WOOD)));

    // --- Block items ---------------------------------------------------------------------------------------
    public static final RegistryEntry<BlockItem> XERTZ_SOIL_ITEM = blockItem("xertz_soil", XERTZ_SOIL);
    public static final RegistryEntry<BlockItem> BIO_BARK_LOG_ITEM = blockItem("bio_bark_log", BIO_BARK_LOG);
    public static final RegistryEntry<BlockItem> STRIPPED_BIO_BARK_LOG_ITEM = blockItem("stripped_bio_bark_log",
            STRIPPED_BIO_BARK_LOG);
    public static final RegistryEntry<BlockItem> XERTZ_VINE_ITEM = blockItem("xertz_vine", XERTZ_VINE);
    public static final RegistryEntry<BlockItem> SPORE_MOSS_ITEM = blockItem("spore_moss", SPORE_MOSS);
    public static final RegistryEntry<BlockItem> GLOWCAP_FUNGUS_ITEM = blockItem("glowcap_fungus", GLOWCAP_FUNGUS);
    public static final RegistryEntry<BlockItem> GLOWCAP_BLOCK_ITEM = blockItem("glowcap_block", GLOWCAP_BLOCK);
    public static final RegistryEntry<BlockItem> SPORE_POD_ITEM = blockItem("spore_pod", SPORE_POD);
    public static final RegistryEntry<BlockItem> AMBER_SAP_ORE_ITEM = blockItem("amber_sap_ore", AMBER_SAP_ORE);

    // --- Items ---------------------------------------------------------------------------------------------
    /** Amber, the biotech/seed ingredient. */
    public static final RegistryEntry<Item> AMBER_SAP = item("amber_sap");
    public static final RegistryEntry<Item> XERTZ_FIBRE = item("xertz_fibre");
    public static final RegistryEntry<SporeBombItem> SPORE_BOMB = ModItems.ITEMS.register("spore_bomb",
            key -> new SporeBombItem(new Item.Properties().setId(key).stacksTo(16)));

    // --- Worldgen feature type (Giant Glowcap: worldgen + bone meal share GiantGlowcap.grow) ------------------
    //? if >=26.3 {
    /*public static final RegistryEntry<com.mojang.serialization.MapCodec<GiantGlowcapFeature>> GIANT_GLOWCAP_FEATURE =
            ModFeatures.FEATURE_TYPES.register("greenxertz_giant_glowcap", key -> GiantGlowcapFeature.CODEC);
    *///?} else {
    public static final RegistryEntry<GiantGlowcapFeature> GIANT_GLOWCAP_FEATURE =
            ModFeatures.FEATURES.register("greenxertz_giant_glowcap",
                    key -> new GiantGlowcapFeature(NoneFeatureConfiguration.CODEC));
    //?}

    private GreenxertzContent() {
    }

    /** Forces class loading (and so registration). Called once from the shared registry init. */
    public static void init() {
    }

    /**
     * The Greenxertz run of the planets tab, in planets-tab order: Xertz Soil, Bio-Bark Log, Stripped Bio-Bark Log,
     * Xertz Vine, Spore Moss, Glowcap Fungus, Glowcap Block, Spore Pod, Amber Sap Ore, the existing Nerosteel Ore and
     * Xertz Quartz Ore, then Amber Sap, Xertz Fibre and Spore Bomb.
     */
    public static List<Supplier<? extends ItemLike>> creativeContents() {
        List<Supplier<? extends ItemLike>> out = new ArrayList<>();
        out.add(XERTZ_SOIL_ITEM);
        out.add(BIO_BARK_LOG_ITEM);
        out.add(STRIPPED_BIO_BARK_LOG_ITEM);
        out.add(XERTZ_VINE_ITEM);
        out.add(SPORE_MOSS_ITEM);
        out.add(GLOWCAP_FUNGUS_ITEM);
        out.add(GLOWCAP_BLOCK_ITEM);
        out.add(SPORE_POD_ITEM);
        out.add(AMBER_SAP_ORE_ITEM);
        out.add(ModItems.NEROSTEEL_ORE_ITEM);
        out.add(ModItems.XERTZ_QUARTZ_ORE_ITEM);
        out.add(AMBER_SAP);
        out.add(XERTZ_FIBRE);
        out.add(SPORE_BOMB);
        return Collections.unmodifiableList(out);
    }

    // --- helpers -------------------------------------------------------------------------------------------

    private static BlockBehaviour.Properties props(ResourceKey<Block> key) {
        return BlockBehaviour.Properties.of().setId(key);
    }

    private static RegistryEntry<BlockItem> blockItem(String name, RegistryEntry<? extends Block> block) {
        // 26.x block items resolve their name from item.nerospace.<id>; the lang fragment mirrors every block key.
        return ModItems.ITEMS.register(name, key -> new BlockItem(block.get(), new Item.Properties().setId(key)));
    }

    private static RegistryEntry<Item> item(String name) {
        return ModItems.ITEMS.register(name, key -> new Item(new Item.Properties().setId(key)));
    }
}

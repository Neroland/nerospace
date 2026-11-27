package za.co.neroland.nerospace.world.planet.europa;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import za.co.neroland.nerospace.block.rules.VentBloomRules;
import za.co.neroland.nerospace.item.NerospaceSpawnEggItem;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModEntities;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;
import za.co.neroland.nerospace.world.planet.europa.block.BrineBlock;
import za.co.neroland.nerospace.world.planet.europa.block.CryoventBlock;
import za.co.neroland.nerospace.world.planet.europa.block.DeuteriumIceBlock;
import za.co.neroland.nerospace.world.planet.europa.block.EuropiumLampBlock;
import za.co.neroland.nerospace.world.planet.europa.block.GripIceBlock;
import za.co.neroland.nerospace.world.planet.europa.block.HydrothermalVentBlock;
import za.co.neroland.nerospace.world.planet.europa.block.MeltingIceBlock;
import za.co.neroland.nerospace.world.planet.europa.block.OxidantIceBlock;
import za.co.neroland.nerospace.world.planet.europa.block.PenitenteBladeBlock;
import za.co.neroland.nerospace.world.planet.europa.block.RaftIceBlock;
import za.co.neroland.nerospace.world.planet.europa.block.SeepSaltBlock;
import za.co.neroland.nerospace.world.planet.europa.block.ShellIceBlock;
import za.co.neroland.nerospace.world.planet.europa.block.SmokerChimneyBlock;
import za.co.neroland.nerospace.world.planet.europa.block.TubeMatBlock;
import za.co.neroland.nerospace.world.planet.europa.block.WarmBasaltBlock;
import za.co.neroland.nerospace.world.planet.europa.entity.LanternDrifter;
import za.co.neroland.nerospace.world.planet.europa.entity.ShellSkater;
import za.co.neroland.nerospace.world.planet.europa.entity.VentRay;

/**
 * Europa's blocks and items, registered through the shared providers. Nothing registers until {@link #init()} loads
 * the class (called once from the shared registry init, after {@code ModBlocks}/{@code ModItems}).
 *
 * <p>Registration order is fixed: new entries are appended, never inserted.</p>
 */
public final class EuropaContent {

    /** A piston breaks the block instead of moving it (renamed DESTROY to POPPED in 26.3). */
    //? if >=26.3 {
    /*private static final PushReaction BREAKS_WHEN_PUSHED = PushReaction.POPPED;
    *///?} else {
    private static final PushReaction BREAKS_WHEN_PUSHED = PushReaction.DESTROY;
    //?}

    // --- blocks ---------------------------------------------------------------------------------
    public static final RegistryEntry<MeltingIceBlock> EUROPA_ICE = block("europa_ice", MeltingIceBlock::new,
            p -> p.mapColor(MapColor.SNOW).strength(0.8F, 0.8F)
                    .sound(SoundType.GLASS));

    public static final RegistryEntry<RaftIceBlock> CHAOS_ICE = block("chaos_ice", RaftIceBlock::new,
            p -> p.mapColor(MapColor.WOOL).strength(1.0F, 1.0F)
                    .sound(SoundType.GLASS));

    public static final RegistryEntry<GripIceBlock> CUT_EUROPA_ICE = block("cut_europa_ice", GripIceBlock::new,
            p -> p.mapColor(MapColor.ICE).strength(1.5F, 3.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS).speedFactor(GripIceBlock.SPEED));

    public static final RegistryEntry<Block> EUROPA_ICE_BRICKS = block("europa_ice_bricks", Block::new,
            p -> p.mapColor(MapColor.ICE).strength(1.5F, 3.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS));

    public static final RegistryEntry<SeepSaltBlock> LINEAE_SALT = block("lineae_salt", SeepSaltBlock::new,
            p -> p.mapColor(MapColor.TERRACOTTA_RED).strength(0.9F, 0.9F)
                    .sound(SoundType.SAND));

    public static final RegistryEntry<PenitenteBladeBlock> PENITENTE_BLADE = block("penitente_blade", PenitenteBladeBlock::new,
            p -> p.mapColor(MapColor.SNOW).strength(0.4F, 0.4F)
                    .sound(SoundType.GLASS).noOcclusion());

    public static final RegistryEntry<ShellIceBlock> SHELL_ICE = block("shell_ice", ShellIceBlock::new,
            p -> p.mapColor(MapColor.LAPIS).strength(2.0F, 4.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS));

    public static final RegistryEntry<CryoventBlock> CRYOVENT = block("cryovent", CryoventBlock::new,
            p -> p.mapColor(MapColor.SNOW).strength(2.5F, 6.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS));

    public static final RegistryEntry<HydrothermalVentBlock> HYDROTHERMAL_VENT = block("hydrothermal_vent", HydrothermalVentBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(3.0F, 9.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.BASALT).lightLevel(HydrothermalVentBlock::light));

    public static final RegistryEntry<WarmBasaltBlock> VENT_BASALT = block("vent_basalt", WarmBasaltBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(2.0F, 9.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.BASALT));

    public static final RegistryEntry<Block> POLISHED_VENT_BASALT = block("polished_vent_basalt", Block::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(2.0F, 9.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.BASALT));

    public static final RegistryEntry<Block> VENT_BASALT_BRICKS = block("vent_basalt_bricks", Block::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(2.0F, 9.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.BASALT));

    public static final RegistryEntry<TubeMatBlock> TUBE_MAT = block("tube_mat", TubeMatBlock::new,
            p -> p.mapColor(MapColor.COLOR_CYAN).strength(0.2F, 0.2F)
                    .sound(SoundType.WET_GRASS).lightLevel(s -> VentBloomRules.LIGHT).noOcclusion());

    public static final RegistryEntry<SmokerChimneyBlock> SMOKER_CHIMNEY = block("smoker_chimney", SmokerChimneyBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(1.5F, 6.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.BASALT));

    public static final RegistryEntry<DeuteriumIceBlock> DEUTERIUM_ICE = block("deuterium_ice", DeuteriumIceBlock::new,
            p -> p.mapColor(MapColor.LAPIS).strength(3.0F, 3.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS).lightLevel(s -> 3));

    public static final RegistryEntry<OxidantIceBlock> OXIDANT_ICE = block("oxidant_ice", OxidantIceBlock::new,
            p -> p.mapColor(MapColor.SNOW).strength(3.0F, 3.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS));

    /** Moved to Neroland Core 1.25.0 ({@code nerolandcore:brine_salt_ore}). */
    public static final RegistryEntry<Block> BRINE_SALT_ORE =
            za.co.neroland.nerospace.registry.RegistrationProvider.alias(za.co.neroland.nerolandcore.ore.CoreOres.BRINE_SALT_ORE);

    /** Moved to Neroland Core 1.25.0 ({@code nerolandcore:vent_sulfide_ore}). */
    public static final RegistryEntry<Block> VENT_SULFIDE_ORE =
            za.co.neroland.nerospace.registry.RegistrationProvider.alias(za.co.neroland.nerolandcore.ore.CoreOres.VENT_SULFIDE_ORE);

    /** Moved to Neroland Core 1.25.0 ({@code nerolandcore:europium_ore}). */
    public static final RegistryEntry<Block> EUROPIUM_ORE =
            za.co.neroland.nerospace.registry.RegistrationProvider.alias(za.co.neroland.nerolandcore.ore.CoreOres.EUROPIUM_ORE);

    public static final RegistryEntry<EuropiumLampBlock> EUROPIUM_LAMP = block("europium_lamp", EuropiumLampBlock::new,
            p -> p.mapColor(MapColor.COLOR_RED).strength(0.6F, 0.6F)
                    .sound(SoundType.GLASS).lightLevel(EuropiumLampBlock::light));

    /** Brine: a fluid-like block with no item form of its own (the bucket is its only item). */
    public static final RegistryEntry<BrineBlock> BRINE = blockNoItem("brine", BrineBlock::new,
            p -> p.mapColor(MapColor.WARPED_STEM).strength(100.0F, 100.0F).noCollision().replaceable().noLootTable()
                    .noOcclusion().pushReaction(BREAKS_WHEN_PUSHED).sound(SoundType.EMPTY));

    // --- block items ----------------------------------------------------------------------------
    public static final RegistryEntry<BlockItem> EUROPA_ICE_ITEM = blockItem("europa_ice", EUROPA_ICE);
    public static final RegistryEntry<BlockItem> CHAOS_ICE_ITEM = blockItem("chaos_ice", CHAOS_ICE);
    public static final RegistryEntry<BlockItem> CUT_EUROPA_ICE_ITEM = blockItem("cut_europa_ice", CUT_EUROPA_ICE);
    public static final RegistryEntry<BlockItem> EUROPA_ICE_BRICKS_ITEM = blockItem("europa_ice_bricks", EUROPA_ICE_BRICKS);
    public static final RegistryEntry<BlockItem> LINEAE_SALT_ITEM = blockItem("lineae_salt", LINEAE_SALT);
    public static final RegistryEntry<BlockItem> PENITENTE_BLADE_ITEM = blockItem("penitente_blade", PENITENTE_BLADE);
    public static final RegistryEntry<BlockItem> SHELL_ICE_ITEM = blockItem("shell_ice", SHELL_ICE);
    public static final RegistryEntry<BlockItem> CRYOVENT_ITEM = blockItem("cryovent", CRYOVENT);
    public static final RegistryEntry<BlockItem> HYDROTHERMAL_VENT_ITEM = blockItem("hydrothermal_vent", HYDROTHERMAL_VENT);
    public static final RegistryEntry<BlockItem> VENT_BASALT_ITEM = blockItem("vent_basalt", VENT_BASALT);
    public static final RegistryEntry<BlockItem> POLISHED_VENT_BASALT_ITEM = blockItem("polished_vent_basalt", POLISHED_VENT_BASALT);
    public static final RegistryEntry<BlockItem> VENT_BASALT_BRICKS_ITEM = blockItem("vent_basalt_bricks", VENT_BASALT_BRICKS);
    public static final RegistryEntry<BlockItem> TUBE_MAT_ITEM = blockItem("tube_mat", TUBE_MAT);
    public static final RegistryEntry<BlockItem> SMOKER_CHIMNEY_ITEM = blockItem("smoker_chimney", SMOKER_CHIMNEY);
    public static final RegistryEntry<BlockItem> DEUTERIUM_ICE_ITEM = blockItem("deuterium_ice", DEUTERIUM_ICE);
    public static final RegistryEntry<BlockItem> OXIDANT_ICE_ITEM = blockItem("oxidant_ice", OXIDANT_ICE);
    /** Moved to Neroland Core 1.25.0 ({@code nerolandcore:brine_salt_ore}). */
    public static final RegistryEntry<BlockItem> BRINE_SALT_ORE_ITEM =
            za.co.neroland.nerospace.registry.RegistrationProvider.alias(za.co.neroland.nerolandcore.ore.CoreOres.BRINE_SALT_ORE_ITEM);
    /** Moved to Neroland Core 1.25.0 ({@code nerolandcore:vent_sulfide_ore}). */
    public static final RegistryEntry<BlockItem> VENT_SULFIDE_ORE_ITEM =
            za.co.neroland.nerospace.registry.RegistrationProvider.alias(za.co.neroland.nerolandcore.ore.CoreOres.VENT_SULFIDE_ORE_ITEM);
    /** Moved to Neroland Core 1.25.0 ({@code nerolandcore:europium_ore}). */
    public static final RegistryEntry<BlockItem> EUROPIUM_ORE_ITEM =
            za.co.neroland.nerospace.registry.RegistrationProvider.alias(za.co.neroland.nerolandcore.ore.CoreOres.EUROPIUM_ORE_ITEM);
    public static final RegistryEntry<BlockItem> EUROPIUM_LAMP_ITEM = blockItem("europium_lamp", EUROPIUM_LAMP);

    // --- items ----------------------------------------------------------------------------------
    public static final RegistryEntry<Item> DEUTERIUM_SHARD = item("deuterium_shard", Item::new);
    public static final RegistryEntry<Item> DEUTERIUM_CELL = item("deuterium_cell", Item::new);
    public static final RegistryEntry<Item> OXIDANT_CRYSTAL = item("oxidant_crystal", Item::new);
    /** Moved to Neroland Core 1.25.0 ({@code nerolandcore:brine_salt}). */
    public static final RegistryEntry<Item> BRINE_SALT =
            za.co.neroland.nerospace.registry.RegistrationProvider.alias(za.co.neroland.nerolandcore.ore.CoreOres.BRINE_SALT);
    /** Moved to Neroland Core 1.25.0 ({@code nerolandcore:raw_vent_sulfide}). */
    public static final RegistryEntry<Item> RAW_VENT_SULFIDE =
            za.co.neroland.nerospace.registry.RegistrationProvider.alias(za.co.neroland.nerolandcore.ore.CoreOres.RAW_VENT_SULFIDE);
    /** Moved to Neroland Core 1.25.0 ({@code nerolandcore:raw_europium}). */
    public static final RegistryEntry<Item> RAW_EUROPIUM =
            za.co.neroland.nerospace.registry.RegistrationProvider.alias(za.co.neroland.nerolandcore.ore.CoreOres.RAW_EUROPIUM);
    /** Places one Brine block; crafting with it returns the empty bucket. */
    public static final RegistryEntry<BrineBucketItem> BRINE_BUCKET = ModItems.ITEMS.register("brine_bucket",
            key -> new BrineBucketItem(new Item.Properties().setId(key).stacksTo(1).craftRemainder(Items.BUCKET)));

    // --- entities (design 9.1-9.3) --------------------------------------------------------------
    /** Ocean drifter; natural spawns in the abyss, vent fields and terraformed sea, and the terraform herd. */
    public static final RegistryEntry<EntityType<LanternDrifter>> LANTERN_DRIFTER = ModEntities.ENTITY_TYPES.register(
            "lantern_drifter", key -> EntityType.Builder.<LanternDrifter>of(LanternDrifter::new,
                    MobCategory.WATER_AMBIENT).sized(0.5F, 0.6F).eyeHeight(0.3F).clientTrackingRange(6).build(key));
    public static final RegistryEntry<Item> LANTERN_DRIFTER_SPAWN_EGG = ModItems.ITEMS.register("lantern_drifter_spawn_egg",
            key -> new NerospaceSpawnEggItem(new Item.Properties().setId(key), LANTERN_DRIFTER));
    /** Ocean-floor glider of the vent fields. */
    public static final RegistryEntry<EntityType<VentRay>> VENT_RAY = ModEntities.ENTITY_TYPES.register(
            "vent_ray", key -> EntityType.Builder.<VentRay>of(VentRay::new, MobCategory.WATER_CREATURE)
                    .sized(1.4F, 0.4F).eyeHeight(0.2F).clientTrackingRange(8).build(key));
    public static final RegistryEntry<Item> VENT_RAY_SPAWN_EGG = ModItems.ITEMS.register("vent_ray_spawn_egg",
            key -> new NerospaceSpawnEggItem(new Item.Properties().setId(key), VENT_RAY));
    /** Ice-shell grazer of the surface and the shell. */
    public static final RegistryEntry<EntityType<ShellSkater>> SHELL_SKATER = ModEntities.ENTITY_TYPES.register(
            "shell_skater", key -> EntityType.Builder.<ShellSkater>of(ShellSkater::new, MobCategory.CREATURE)
                    .sized(0.7F, 0.5F).eyeHeight(0.35F).clientTrackingRange(8).build(key));
    public static final RegistryEntry<Item> SHELL_SKATER_SPAWN_EGG = ModItems.ITEMS.register("shell_skater_spawn_egg",
            key -> new NerospaceSpawnEggItem(new Item.Properties().setId(key), SHELL_SKATER));

    private EuropaContent() {
    }

    /** Forces class load, which registers everything above. */
    public static void init() {
    }

    /**
     * Creative order: surface ice set, lineae, penitente, shell ice, brine bucket, vents, basalt family, tube mat,
     * chimney, the ores, then items. The europium ore, gem and lamp go last.
     */
    public static List<Supplier<? extends ItemLike>> creativeContents() {
        return List.of(EUROPA_ICE_ITEM, CHAOS_ICE_ITEM, CUT_EUROPA_ICE_ITEM, EUROPA_ICE_BRICKS_ITEM,
                LINEAE_SALT_ITEM, PENITENTE_BLADE_ITEM, SHELL_ICE_ITEM, BRINE_BUCKET, CRYOVENT_ITEM,
                HYDROTHERMAL_VENT_ITEM, VENT_BASALT_ITEM, POLISHED_VENT_BASALT_ITEM, VENT_BASALT_BRICKS_ITEM,
                TUBE_MAT_ITEM, SMOKER_CHIMNEY_ITEM, DEUTERIUM_ICE_ITEM, OXIDANT_ICE_ITEM, BRINE_SALT_ORE_ITEM,
                VENT_SULFIDE_ORE_ITEM, DEUTERIUM_SHARD, DEUTERIUM_CELL, OXIDANT_CRYSTAL, BRINE_SALT,
                RAW_VENT_SULFIDE, EUROPIUM_ORE_ITEM, RAW_EUROPIUM, EUROPIUM_LAMP_ITEM);
    }

    private static <B extends Block> RegistryEntry<B> block(String name,
            Function<BlockBehaviour.Properties, B> factory, UnaryOperator<BlockBehaviour.Properties> props) {
        return ModBlocks.BLOCKS.register(name, (ResourceKey<Block> key) ->
                factory.apply(props.apply(BlockBehaviour.Properties.of().setId(key))));
    }

    /** A block with no {@link BlockItem} (brine). Same registration as {@link #block}; named so the intent shows. */
    private static <B extends Block> RegistryEntry<B> blockNoItem(String name,
            Function<BlockBehaviour.Properties, B> factory, UnaryOperator<BlockBehaviour.Properties> props) {
        return block(name, factory, props);
    }

    private static RegistryEntry<BlockItem> blockItem(String name, RegistryEntry<? extends Block> block) {
        return ModItems.ITEMS.register(name, key -> new BlockItem(block.get(), new Item.Properties().setId(key)));
    }

    private static <I extends Item> RegistryEntry<I> item(String name, Function<Item.Properties, I> factory) {
        return ModItems.ITEMS.register(name, key -> factory.apply(new Item.Properties().setId(key)));
    }
}

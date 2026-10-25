package za.co.neroland.nerospace.world.station;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import za.co.neroland.nerospace.block.AirtightBlock;
import za.co.neroland.nerospace.block.AirtightWindowBlock;
import za.co.neroland.nerospace.block.GravityOverrideBlock;
import za.co.neroland.nerospace.block.rules.BlockGravityRules;
import za.co.neroland.nerospace.registry.ModBlockEntities;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;
import za.co.neroland.nerospace.world.station.block.HydroponicsTrayBlock;
import za.co.neroland.nerospace.world.station.block.SolarWingBlock;
import za.co.neroland.nerospace.world.station.block.SolarWingBlockEntity;
import za.co.neroland.nerospace.world.station.block.TelescopeBlock;
import za.co.neroland.nerospace.world.station.block.TrussBlock;

/**
 * The orbital station's blocks and items: Hull Plating, Hull Window, Truss, Solar Wing, Gravity
 * Plate, Hydroponics Tray and Telescope. (The Docking Ring is registered by the rocket package.) Nothing registers
 * until {@link #init()} loads the class, called once from the shared registry init after the core providers.
 */
public final class StationContent {

    // --- blocks ---------------------------------------------------------------------------------
    public static final RegistryEntry<AirtightBlock> HULL_PLATING = block("hull_plating", AirtightBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(4.0F, 12.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    public static final RegistryEntry<AirtightWindowBlock> HULL_WINDOW = block("hull_window",
            AirtightWindowBlock::new,
            p -> p.mapColor(MapColor.COLOR_CYAN).strength(2.0F, 12.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.GLASS).noOcclusion().isRedstoneConductor((s, l, pos) -> false)
                    .isSuffocating((s, l, pos) -> false));

    public static final RegistryEntry<TrussBlock> TRUSS = block("truss", TrussBlock::new,
            p -> p.mapColor(MapColor.COLOR_GRAY).strength(1.0F, 6.0F).sound(SoundType.COPPER_GRATE).noOcclusion()
                    .isRedstoneConductor((s, l, pos) -> false).isSuffocating((s, l, pos) -> false));

    public static final RegistryEntry<SolarWingBlock> SOLAR_WING = block("solar_wing", SolarWingBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLUE).strength(1.5F, 6.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.METAL).noOcclusion());

    /** Random ticks: a plate placed without {@code onPlace} (structure, world edit) registers on its first tick. */
    public static final RegistryEntry<GravityOverrideBlock> GRAVITY_PLATE = block("gravity_plate",
            p -> new GravityOverrideBlock(p, BlockGravityRules.Kind.PLATE),
            p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(2.0F, 12.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.METAL).lightLevel(s -> 3).randomTicks());

    public static final RegistryEntry<HydroponicsTrayBlock> HYDROPONICS_TRAY = block("hydroponics_tray",
            HydroponicsTrayBlock::new,
            p -> p.mapColor(MapColor.COLOR_GREEN).strength(1.0F, 6.0F).sound(SoundType.METAL));

    public static final RegistryEntry<TelescopeBlock> TELESCOPE = block("telescope", TelescopeBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(3.0F, 12.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.METAL).noOcclusion());

    // --- block entities -------------------------------------------------------------------------
    public static final RegistryEntry<BlockEntityType<SolarWingBlockEntity>> SOLAR_WING_BE =
            ModBlockEntities.BLOCK_ENTITIES.register("solar_wing",
                    key -> new BlockEntityType<>(SolarWingBlockEntity::new, java.util.Set.of(SOLAR_WING.get())));

    // --- block items ----------------------------------------------------------------------------
    public static final RegistryEntry<BlockItem> HULL_PLATING_ITEM = blockItem("hull_plating", HULL_PLATING);
    public static final RegistryEntry<BlockItem> HULL_WINDOW_ITEM = blockItem("hull_window", HULL_WINDOW);
    public static final RegistryEntry<BlockItem> TRUSS_ITEM = blockItem("truss", TRUSS);
    public static final RegistryEntry<BlockItem> SOLAR_WING_ITEM = blockItem("solar_wing", SOLAR_WING);
    public static final RegistryEntry<BlockItem> GRAVITY_PLATE_ITEM = blockItem("gravity_plate", GRAVITY_PLATE);
    public static final RegistryEntry<BlockItem> HYDROPONICS_TRAY_ITEM = blockItem("hydroponics_tray",
            HYDROPONICS_TRAY);
    public static final RegistryEntry<BlockItem> TELESCOPE_ITEM = blockItem("telescope", TELESCOPE);

    private StationContent() {
    }

    /** Forces class load, which registers everything above. */
    public static void init() {
    }

    /** Creative order: hull, window, truss, wing, plate, tray, telescope. */
    public static List<Supplier<? extends ItemLike>> creativeContents() {
        return List.of(HULL_PLATING_ITEM, HULL_WINDOW_ITEM, TRUSS_ITEM, SOLAR_WING_ITEM, GRAVITY_PLATE_ITEM,
                HYDROPONICS_TRAY_ITEM, TELESCOPE_ITEM);
    }

    private static <B extends Block> RegistryEntry<B> block(String name,
            Function<BlockBehaviour.Properties, B> factory, UnaryOperator<BlockBehaviour.Properties> props) {
        return ModBlocks.BLOCKS.register(name, (ResourceKey<Block> key) ->
                factory.apply(props.apply(BlockBehaviour.Properties.of().setId(key))));
    }

    private static RegistryEntry<BlockItem> blockItem(String name, RegistryEntry<? extends Block> block) {
        return ModItems.ITEMS.register(name, key -> new BlockItem(block.get(), new Item.Properties().setId(key)));
    }
}

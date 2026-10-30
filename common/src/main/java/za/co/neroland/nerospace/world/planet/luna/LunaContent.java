package za.co.neroland.nerospace.world.planet.luna;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import za.co.neroland.nerospace.block.FootprintBlock;
import za.co.neroland.nerospace.block.LightLensBlock;
import za.co.neroland.nerospace.block.SolarSinterBlock;
import za.co.neroland.nerospace.block.rules.CheeseRules;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.item.NerospaceSpawnEggItem;
import za.co.neroland.nerospace.registry.ModEntities;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;
import za.co.neroland.nerospace.world.planet.luna.block.CheeseWheelBlock;
import za.co.neroland.nerospace.world.planet.luna.block.Helium3PocketBlock;
import za.co.neroland.nerospace.world.planet.luna.entity.MoonHopper;

/**
 * Luna's blocks and items, registered through the shared providers. Nothing registers until {@link #init()} loads
 * the class (called once from the shared registry init, after {@code ModBlocks}/{@code ModItems}).
 */
public final class LunaContent {

    /** Moon Cheese eat time in seconds (as fast as dried kelp). */
    public static final float CHEESE_EAT_SECONDS = 0.8F;

    // --- blocks ---------------------------------------------------------------------------------
    public static final RegistryEntry<FootprintBlock> REGOLITH = block("regolith", FootprintBlock::new,
            p -> p.mapColor(MapColor.STONE).strength(0.6F, 0.6F).sound(SoundType.GRAVEL));

    public static final RegistryEntry<Block> POLISHED_LUNAR_BASALT = block("polished_lunar_basalt", Block::new,
            p -> p.mapColor(MapColor.DEEPSLATE).strength(2.0F, 9.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE));

    public static final RegistryEntry<SolarSinterBlock> LUNAR_BASALT = block("lunar_basalt",
            p -> new SolarSinterBlock(p, () -> POLISHED_LUNAR_BASALT.get().defaultBlockState()),
            p -> p.mapColor(MapColor.DEEPSLATE).strength(2.0F, 9.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE).randomTicks());

    public static final RegistryEntry<Block> ANORTHOSITE = block("anorthosite", Block::new,
            p -> p.mapColor(MapColor.QUARTZ).strength(1.5F, 6.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.CALCITE));

    public static final RegistryEntry<LightLensBlock> CRATER_GLASS = block("crater_glass", LightLensBlock::new,
            p -> p.mapColor(MapColor.SAND).strength(0.3F, 0.3F).sound(SoundType.GLASS).noOcclusion()
                    .lightLevel(s -> s.getValue(LightLensBlock.LENS))
                    .isRedstoneConductor((s, l, pos) -> false));

    public static final RegistryEntry<Block> ILMENITE_ORE = block("ilmenite_ore", Block::new,
            p -> p.mapColor(MapColor.STONE).strength(3.0F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.STONE));

    public static final RegistryEntry<Helium3PocketBlock> HELIUM3_POCKET = block("helium3_pocket",
            Helium3PocketBlock::new,
            p -> p.mapColor(MapColor.STONE).strength(3.0F, 3.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.NETHER_ORE).lightLevel(s -> 2));

    public static final RegistryEntry<Block> CHEESE_ORE = block("cheese_ore", Block::new,
            p -> p.mapColor(MapColor.STONE).strength(3.0F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.STONE));

    public static final RegistryEntry<CheeseWheelBlock> CHEESE_WHEEL = block("cheese_wheel", CheeseWheelBlock::new,
            p -> p.mapColor(MapColor.GOLD).strength(0.5F, 0.5F).sound(SoundType.WOOL).forceSolidOn());

    // --- block items ----------------------------------------------------------------------------
    public static final RegistryEntry<BlockItem> REGOLITH_ITEM = blockItem("regolith", REGOLITH);
    public static final RegistryEntry<BlockItem> LUNAR_BASALT_ITEM = blockItem("lunar_basalt", LUNAR_BASALT);
    public static final RegistryEntry<BlockItem> POLISHED_LUNAR_BASALT_ITEM =
            blockItem("polished_lunar_basalt", POLISHED_LUNAR_BASALT);
    public static final RegistryEntry<BlockItem> ANORTHOSITE_ITEM = blockItem("anorthosite", ANORTHOSITE);
    public static final RegistryEntry<BlockItem> CRATER_GLASS_ITEM = blockItem("crater_glass", CRATER_GLASS);
    public static final RegistryEntry<BlockItem> ILMENITE_ORE_ITEM = blockItem("ilmenite_ore", ILMENITE_ORE);
    public static final RegistryEntry<BlockItem> HELIUM3_POCKET_ITEM = blockItem("helium3_pocket", HELIUM3_POCKET);
    public static final RegistryEntry<BlockItem> CHEESE_ORE_ITEM = blockItem("cheese_ore", CHEESE_ORE);
    public static final RegistryEntry<BlockItem> CHEESE_WHEEL_ITEM = blockItem("cheese_wheel", CHEESE_WHEEL);

    // --- items ----------------------------------------------------------------------------------
    public static final RegistryEntry<Item> RAW_ILMENITE = item("raw_ilmenite", Item::new);
    public static final RegistryEntry<Item> TITANIUM_INGOT = item("titanium_ingot", Item::new);
    public static final RegistryEntry<Item> HELIUM3_CANISTER = item("helium3_canister", Item::new);
    public static final RegistryEntry<MoonCheeseItem> MOON_CHEESE = ModItems.ITEMS.register("moon_cheese",
            key -> new MoonCheeseItem(new Item.Properties().setId(key).food(cheeseFood(), cheeseConsumable())));

    // --- entities (terraform fauna) -----------------------------------------------------
    /** The Living-meadow grazer; seeded by TerraformFauna and spawned by terraformed_luna_living. */
    public static final RegistryEntry<EntityType<MoonHopper>> MOON_HOPPER = ModEntities.ENTITY_TYPES.register(
            "moon_hopper", key -> EntityType.Builder.<MoonHopper>of(MoonHopper::new, MobCategory.CREATURE)
                    .sized(0.5F, 0.6F).eyeHeight(0.45F).clientTrackingRange(8).build(key));
    public static final RegistryEntry<Item> MOON_HOPPER_SPAWN_EGG = ModItems.ITEMS.register("moon_hopper_spawn_egg",
            key -> new NerospaceSpawnEggItem(new Item.Properties().setId(key), MOON_HOPPER));

    private LunaContent() {
    }

    /** Forces class load, which registers everything above. */
    public static void init() {
    }

    static FoodProperties cheeseFood() {
        return new FoodProperties.Builder().nutrition(CheeseRules.NUTRITION).saturationModifier(CheeseRules.SATURATION)
                .alwaysEdible().build();
    }

    static Consumable cheeseConsumable() {
        return Consumables.defaultFood().consumeSeconds(CHEESE_EAT_SECONDS)
                .onConsume(new ApplyStatusEffectsConsumeEffect(
                        new MobEffectInstance(MobEffects.REGENERATION, CheeseRules.REGEN_TICKS, 0)))
                .build();
    }

    /** Creative order: blocks first, stone before its ores, then the items. */
    public static List<Supplier<? extends ItemLike>> creativeContents() {
        return List.of(REGOLITH_ITEM, LUNAR_BASALT_ITEM, POLISHED_LUNAR_BASALT_ITEM, ANORTHOSITE_ITEM,
                CRATER_GLASS_ITEM, ILMENITE_ORE_ITEM, HELIUM3_POCKET_ITEM, CHEESE_ORE_ITEM, CHEESE_WHEEL_ITEM,
                RAW_ILMENITE, TITANIUM_INGOT, HELIUM3_CANISTER, MOON_CHEESE);
    }

    private static <B extends Block> RegistryEntry<B> block(String name,
            Function<BlockBehaviour.Properties, B> factory, UnaryOperator<BlockBehaviour.Properties> props) {
        return ModBlocks.BLOCKS.register(name, (ResourceKey<Block> key) ->
                factory.apply(props.apply(BlockBehaviour.Properties.of().setId(key))));
    }

    private static RegistryEntry<BlockItem> blockItem(String name, RegistryEntry<? extends Block> block) {
        return ModItems.ITEMS.register(name, key -> new BlockItem(block.get(), new Item.Properties().setId(key)));
    }

    private static <I extends Item> RegistryEntry<I> item(String name, Function<Item.Properties, I> factory) {
        return ModItems.ITEMS.register(name, key -> factory.apply(new Item.Properties().setId(key)));
    }
}

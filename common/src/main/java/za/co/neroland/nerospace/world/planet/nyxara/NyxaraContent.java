package za.co.neroland.nerospace.world.planet.nyxara;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.block.GravityOverrideBlock;
import za.co.neroland.nerospace.block.LightAbsorbingBlock;
import za.co.neroland.nerospace.block.WeatherFlipBlock;
import za.co.neroland.nerospace.block.rules.BlockGravityRules;
import za.co.neroland.nerospace.block.rules.ClusterLootRules;
import za.co.neroland.nerospace.block.rules.FootglowRules;
import za.co.neroland.nerospace.block.rules.HushRules;
import za.co.neroland.nerospace.block.rules.PrismBeamRules;
import za.co.neroland.nerospace.block.rules.SkySoakRules;
import za.co.neroland.nerospace.block.rules.SpireChimeRules;
import za.co.neroland.nerospace.block.rules.StarlitRules;
import za.co.neroland.nerospace.block.rules.WarmthRules;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.item.NerospaceSpawnEggItem;
import za.co.neroland.nerospace.registry.ModEntities;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.registry.RegistrationProvider;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;
import za.co.neroland.nerospace.world.planet.nyxara.block.BuddingNyxCrystalBlock;
import za.co.neroland.nerospace.world.planet.nyxara.block.CrazedCrystalBlock;
import za.co.neroland.nerospace.world.planet.nyxara.block.CrystalSpireBlock;
import za.co.neroland.nerospace.world.planet.nyxara.block.MirrorCrystalBlock;
import za.co.neroland.nerospace.world.planet.nyxara.block.NyxCrystalClusterBlock;
import za.co.neroland.nerospace.world.planet.nyxara.block.NyxPrismBlock;
import za.co.neroland.nerospace.world.planet.nyxara.block.RoseCrystalBlock;
import za.co.neroland.nerospace.world.planet.nyxara.block.ShadowLichenBlock;
import za.co.neroland.nerospace.world.planet.nyxara.block.ShardDriftBlock;
import za.co.neroland.nerospace.world.planet.nyxara.block.SmokeCrystalBlock;
import za.co.neroland.nerospace.world.planet.nyxara.block.TwilightCrystalBlock;
import za.co.neroland.nerospace.world.planet.nyxara.block.VoidCrystalBlock;
import za.co.neroland.nerospace.world.planet.nyxara.entity.DuskMoth;
import za.co.neroland.nerospace.world.planet.nyxara.worldgen.FixedXBiomeSource;

/**
 * Every Nyxara block and item, plus the {@code nerospace:fixed_x} biome source codec, registered through the shared
 * providers. Blocks reuse the shared behaviour bases: Nyx Stone absorbs block light ({@link LightAbsorbingBlock}),
 * Dawnband Stone glows during a flare ({@link WeatherFlipBlock}), the Gravity Shard Block lifts entities above it
 * ({@link GravityOverrideBlock}). Void Crystal is in {@code #nerospace:beam_refractors}; Shadow Lichen spreads in the
 * dark and withers in flares. Class loading registers everything; {@link #init()} only forces the load.
 */
public final class NyxaraContent {

    // --- Biome source codec (7.3) -------------------------------------------------------------------------
    public static final RegistrationProvider<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            RegistrationProvider.get(Registries.BIOME_SOURCE, NerospaceCommon.MOD_ID);

    public static final RegistryEntry<MapCodec<FixedXBiomeSource>> FIXED_X = BIOME_SOURCES.register("fixed_x",
            key -> FixedXBiomeSource.CODEC);

    /** A piston breaks the block instead of moving it (renamed DESTROY to POPPED in 26.3). */
    //? if >=26.3 {
    /*private static final PushReaction BREAKS_WHEN_PUSHED = PushReaction.POPPED;
    *///?} else {
    private static final PushReaction BREAKS_WHEN_PUSHED = PushReaction.DESTROY;
    //?}

    // --- Blocks (creative-tab order) -------------------------------------------------------------
    public static final RegistryEntry<LightAbsorbingBlock> NYX_STONE = ModBlocks.BLOCKS.register("nyx_stone",
            key -> new LightAbsorbingBlock(props(key).mapColor(MapColor.COLOR_PURPLE).strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));

    public static final RegistryEntry<WeatherFlipBlock> DAWNBAND_STONE = ModBlocks.BLOCKS.register("dawnband_stone",
            key -> new WeatherFlipBlock(props(key).mapColor(MapColor.COLOR_ORANGE).strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE)
                    .lightLevel(s -> WarmthRules.light(s.getValue(WeatherFlipBlock.FLARING)))));

    public static final RegistryEntry<ShadowLichenBlock> SHADOW_LICHEN = ModBlocks.BLOCKS.register("shadow_lichen",
            key -> new ShadowLichenBlock(props(key).mapColor(MapColor.COLOR_BLACK).replaceable().noCollision()
                    .strength(0.2F).sound(SoundType.GLOW_LICHEN).randomTicks()));

    public static final RegistryEntry<VoidCrystalBlock> VOID_CRYSTAL = ModBlocks.BLOCKS.register("void_crystal",
            key -> new VoidCrystalBlock(props(key).mapColor(MapColor.COLOR_PURPLE).strength(1.5F, 1.5F)
                    .requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s -> VoidCrystalBlock.LIGHT)));

    public static final RegistryEntry<Block> VOIDGLASS_ORE = ModBlocks.BLOCKS.register("voidglass_ore",
            key -> new Block(props(key).mapColor(MapColor.COLOR_PURPLE).strength(3.5F, 3.5F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE).lightLevel(s -> 1)));

    public static final RegistryEntry<GravityOverrideBlock> GRAVITY_SHARD_BLOCK = ModBlocks.BLOCKS.register(
            "gravity_shard_block",
            key -> new GravityOverrideBlock(props(key).mapColor(MapColor.COLOR_MAGENTA).strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s -> 5)
                    .randomTicks(),
                    BlockGravityRules.Kind.SHARD));

    // --- Crystal blocks (glassy crystal surface; see each class for its behaviour) ------------------------
    public static final RegistryEntry<Block> SUNGLASS_CRYSTAL = ModBlocks.BLOCKS.register("sunglass_crystal",
            key -> new Block(props(key).mapColor(MapColor.SAND).strength(1.5F, 3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.AMETHYST)));

    public static final RegistryEntry<CrazedCrystalBlock> CRAZED_CRYSTAL = ModBlocks.BLOCKS.register("crazed_crystal",
            key -> new CrazedCrystalBlock(props(key).mapColor(MapColor.SNOW).strength(0.6F, 0.6F)
                    .requiresCorrectToolForDrops().sound(SoundType.GLASS)));

    public static final RegistryEntry<ShardDriftBlock> SHARD_DRIFT = ModBlocks.BLOCKS.register("shard_drift",
            key -> new ShardDriftBlock(props(key).mapColor(MapColor.SAND).replaceable().strength(0.2F, 0.2F)
                    .sound(SoundType.AMETHYST).forceSolidOff().pushReaction(BREAKS_WHEN_PUSHED)));

    public static final RegistryEntry<TwilightCrystalBlock> TWILIGHT_CRYSTAL = ModBlocks.BLOCKS.register(
            "twilight_crystal",
            key -> new TwilightCrystalBlock(props(key).mapColor(MapColor.COLOR_PURPLE).strength(1.5F, 3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.AMETHYST)
                    .lightLevel(s -> SkySoakRules.light(s.getValue(TwilightCrystalBlock.GLOW)))));

    public static final RegistryEntry<RoseCrystalBlock> ROSE_CRYSTAL = ModBlocks.BLOCKS.register("rose_crystal",
            key -> new RoseCrystalBlock(props(key).mapColor(MapColor.COLOR_PINK).strength(1.5F, 3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.AMETHYST)
                    .lightLevel(s -> FootglowRules.light(s.getValue(RoseCrystalBlock.LIT)))));

    public static final RegistryEntry<CrystalSpireBlock> CRYSTAL_SPIRE = ModBlocks.BLOCKS.register("crystal_spire",
            key -> new CrystalSpireBlock(props(key).mapColor(MapColor.COLOR_PURPLE).strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.AMETHYST).noOcclusion()
                    .lightLevel(s -> SpireChimeRules.LIGHT)));

    public static final RegistryEntry<NyxPrismBlock> NYX_PRISM = ModBlocks.BLOCKS.register("nyx_prism",
            key -> new NyxPrismBlock(props(key).mapColor(MapColor.COLOR_MAGENTA).strength(1.5F, 1.5F)
                    .requiresCorrectToolForDrops().sound(SoundType.AMETHYST).noOcclusion()
                    .lightLevel(s -> PrismBeamRules.light(s.getValue(WeatherFlipBlock.FLARING)))));

    public static final RegistryEntry<BuddingNyxCrystalBlock> BUDDING_NYX_CRYSTAL = ModBlocks.BLOCKS.register(
            "budding_nyx_crystal",
            key -> new BuddingNyxCrystalBlock(props(key).mapColor(MapColor.COLOR_PURPLE).strength(1.5F, 1.5F)
                    .requiresCorrectToolForDrops().sound(SoundType.AMETHYST).randomTicks()
                    .pushReaction(BREAKS_WHEN_PUSHED)));

    public static final RegistryEntry<NyxCrystalClusterBlock> NYX_CRYSTAL_CLUSTER = ModBlocks.BLOCKS.register(
            "nyx_crystal_cluster",
            key -> new NyxCrystalClusterBlock(props(key).mapColor(MapColor.COLOR_PURPLE).strength(1.5F, 1.5F)
                    .sound(SoundType.AMETHYST_CLUSTER).noCollision().noOcclusion()
                    .pushReaction(BREAKS_WHEN_PUSHED)
                    .lightLevel(s -> ClusterLootRules.light(s.getValue(NyxCrystalClusterBlock.STAGE)))));

    public static final RegistryEntry<SmokeCrystalBlock> SMOKE_CRYSTAL = ModBlocks.BLOCKS.register("smoke_crystal",
            key -> new SmokeCrystalBlock(props(key).mapColor(MapColor.COLOR_BLACK).strength(1.5F, 3.0F)
                    .requiresCorrectToolForDrops().sound(SmokeCrystalBlock.HUSH).lightLevel(s -> HushRules.LIGHT)));

    public static final RegistryEntry<MirrorCrystalBlock> MIRROR_CRYSTAL = ModBlocks.BLOCKS.register("mirror_crystal",
            key -> new MirrorCrystalBlock(neverViewBlocking(props(key)).mapColor(MapColor.COLOR_BLACK)
                    .strength(0.8F, 0.8F).requiresCorrectToolForDrops().sound(SoundType.GLASS).noOcclusion()
                    .isValidSpawn((s, l, p, t) -> false).isRedstoneConductor((s, l, p) -> false)
                    .isSuffocating((s, l, p) -> false)
                    .lightLevel(s -> StarlitRules.light(s.getValue(MirrorCrystalBlock.STARLIT)))));

    // --- Block items ---------------------------------------------------------------------------------------
    public static final RegistryEntry<BlockItem> NYX_STONE_ITEM = blockItem("nyx_stone", NYX_STONE);
    public static final RegistryEntry<BlockItem> DAWNBAND_STONE_ITEM = blockItem("dawnband_stone", DAWNBAND_STONE);
    public static final RegistryEntry<BlockItem> SHADOW_LICHEN_ITEM = blockItem("shadow_lichen", SHADOW_LICHEN);
    public static final RegistryEntry<BlockItem> VOID_CRYSTAL_ITEM = blockItem("void_crystal", VOID_CRYSTAL);
    public static final RegistryEntry<BlockItem> VOIDGLASS_ORE_ITEM = blockItem("voidglass_ore", VOIDGLASS_ORE);
    public static final RegistryEntry<BlockItem> GRAVITY_SHARD_BLOCK_ITEM = blockItem("gravity_shard_block",
            GRAVITY_SHARD_BLOCK);
    public static final RegistryEntry<BlockItem> SUNGLASS_CRYSTAL_ITEM = blockItem("sunglass_crystal", SUNGLASS_CRYSTAL);
    public static final RegistryEntry<BlockItem> CRAZED_CRYSTAL_ITEM = blockItem("crazed_crystal", CRAZED_CRYSTAL);
    public static final RegistryEntry<BlockItem> SHARD_DRIFT_ITEM = blockItem("shard_drift", SHARD_DRIFT);
    public static final RegistryEntry<BlockItem> TWILIGHT_CRYSTAL_ITEM = blockItem("twilight_crystal", TWILIGHT_CRYSTAL);
    public static final RegistryEntry<BlockItem> ROSE_CRYSTAL_ITEM = blockItem("rose_crystal", ROSE_CRYSTAL);
    public static final RegistryEntry<BlockItem> CRYSTAL_SPIRE_ITEM = blockItem("crystal_spire", CRYSTAL_SPIRE);
    public static final RegistryEntry<BlockItem> NYX_PRISM_ITEM = blockItem("nyx_prism", NYX_PRISM);
    public static final RegistryEntry<BlockItem> BUDDING_NYX_CRYSTAL_ITEM = blockItem("budding_nyx_crystal",
            BUDDING_NYX_CRYSTAL);
    public static final RegistryEntry<BlockItem> NYX_CRYSTAL_CLUSTER_ITEM = blockItem("nyx_crystal_cluster",
            NYX_CRYSTAL_CLUSTER);
    public static final RegistryEntry<BlockItem> SMOKE_CRYSTAL_ITEM = blockItem("smoke_crystal", SMOKE_CRYSTAL);
    public static final RegistryEntry<BlockItem> MIRROR_CRYSTAL_ITEM = blockItem("mirror_crystal", MIRROR_CRYSTAL);

    // --- Items ---------------------------------------------------------------------------------------------
    /** Voidglass, the Nyxara ore drop; ground into the Voidglass Lens. */
    public static final RegistryEntry<Item> VOIDGLASS = item("voidglass");
    /** Optics part (telescope / beam work). */
    public static final RegistryEntry<Item> VOIDGLASS_LENS = item("voidglass_lens");
    /** Nine make a Gravity Shard Block; a block breaks back into nine. */
    public static final RegistryEntry<Item> GRAVITY_SHARD = item("gravity_shard");
    /** Sheared from Shadow Lichen. */
    public static final RegistryEntry<Item> NIGHT_FIBRE = item("night_fibre");
    /** Void Crystal mined without Silk Touch. */
    public static final RegistryEntry<Item> VOID_SHARD = item("void_shard");
    /** The Nyxara crystal drop (clusters, crazed crystal, drifts, mirror crystal, prisms); four make Twilight Crystal. */
    public static final RegistryEntry<Item> CRYSTAL_SHARD = item("crystal_shard");

    // --- Entities (terraform fauna) ---------------------------------------------------------------
    /** Ambient flier of the twilight grove (light <= 7 only); spawned by terraformed_nyxara_living. */
    public static final RegistryEntry<EntityType<DuskMoth>> DUSK_MOTH = ModEntities.ENTITY_TYPES.register(
            "dusk_moth", key -> EntityType.Builder.<DuskMoth>of(DuskMoth::new, MobCategory.AMBIENT)
                    .sized(0.5F, 0.5F).eyeHeight(0.3F).clientTrackingRange(8).build(key));
    public static final RegistryEntry<Item> DUSK_MOTH_SPAWN_EGG = ModItems.ITEMS.register("dusk_moth_spawn_egg",
            key -> new NerospaceSpawnEggItem(new Item.Properties().setId(key), DUSK_MOTH));

    private NyxaraContent() {
    }

    /** Forces class loading (and so registration). Called once from the shared registry init. */
    public static void init() {
    }

    /**
     * The Nyxara run of the planets tab, in order: Nyx Stone, Dawnband Stone, Shadow Lichen, Void Crystal,
     * Voidglass Ore, Gravity Shard Block, the crystal blocks (Sunglass, Crazed, Shard Drift, Twilight, Rose, Spire,
     * Prism, Budding, Cluster, Smoke, Mirror), then Voidglass, Voidglass Lens, Gravity Shard, Night Fibre, Void Shard,
     * Crystal Shard.
     */
    public static List<Supplier<? extends ItemLike>> creativeContents() {
        List<Supplier<? extends ItemLike>> out = new ArrayList<>();
        out.add(NYX_STONE_ITEM);
        out.add(DAWNBAND_STONE_ITEM);
        out.add(SHADOW_LICHEN_ITEM);
        out.add(VOID_CRYSTAL_ITEM);
        out.add(VOIDGLASS_ORE_ITEM);
        out.add(GRAVITY_SHARD_BLOCK_ITEM);
        out.add(SUNGLASS_CRYSTAL_ITEM);
        out.add(CRAZED_CRYSTAL_ITEM);
        out.add(SHARD_DRIFT_ITEM);
        out.add(TWILIGHT_CRYSTAL_ITEM);
        out.add(ROSE_CRYSTAL_ITEM);
        out.add(CRYSTAL_SPIRE_ITEM);
        out.add(NYX_PRISM_ITEM);
        out.add(BUDDING_NYX_CRYSTAL_ITEM);
        out.add(NYX_CRYSTAL_CLUSTER_ITEM);
        out.add(SMOKE_CRYSTAL_ITEM);
        out.add(MIRROR_CRYSTAL_ITEM);
        out.add(VOIDGLASS);
        out.add(VOIDGLASS_LENS);
        out.add(GRAVITY_SHARD);
        out.add(NIGHT_FIBRE);
        out.add(VOID_SHARD);
        out.add(CRYSTAL_SHARD);
        return Collections.unmodifiableList(out);
    }

    // --- helpers -------------------------------------------------------------------------------------------

    private static BlockBehaviour.Properties props(ResourceKey<Block> key) {
        return BlockBehaviour.Properties.of().setId(key);
    }

    /**
     * Never view-blocking, like glass: a camera inside a mirror lake gets no inside-block overlay. Set explicitly
     * because before 26.3 the default is captured from the default suffocation predicate at construction. The
     * predicate gained a near-plane box argument in 26.3.
     */
    private static BlockBehaviour.Properties neverViewBlocking(BlockBehaviour.Properties p) {
        //? if >=26.3 {
        /*return p.isViewBlocking((s, l, pos, box) -> false);
        *///?} else {
        return p.isViewBlocking((s, l, pos) -> false);
        //?}
    }

    private static RegistryEntry<BlockItem> blockItem(String name, RegistryEntry<? extends Block> block) {
        // 26.x block items resolve their name from item.nerospace.<id>; the lang fragment mirrors every block key.
        return ModItems.ITEMS.register(name, key -> new BlockItem(block.get(), new Item.Properties().setId(key)));
    }

    private static RegistryEntry<Item> item(String name) {
        return ModItems.ITEMS.register(name, key -> new Item(new Item.Properties().setId(key)));
    }
}

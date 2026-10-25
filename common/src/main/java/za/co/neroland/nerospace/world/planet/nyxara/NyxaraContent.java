package za.co.neroland.nerospace.world.planet.nyxara;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.block.GravityOverrideBlock;
import za.co.neroland.nerospace.block.LightAbsorbingBlock;
import za.co.neroland.nerospace.block.WeatherFlipBlock;
import za.co.neroland.nerospace.block.rules.BlockGravityRules;
import za.co.neroland.nerospace.block.rules.WarmthRules;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.registry.RegistrationProvider;
import za.co.neroland.nerospace.registry.RegistrationProvider.RegistryEntry;
import za.co.neroland.nerospace.world.planet.nyxara.block.ShadowLichenBlock;
import za.co.neroland.nerospace.world.planet.nyxara.block.VoidCrystalBlock;
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

    // --- Blocks (creative-tab order, 002 §6) -------------------------------------------------------------
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

    // --- Block items ---------------------------------------------------------------------------------------
    public static final RegistryEntry<BlockItem> NYX_STONE_ITEM = blockItem("nyx_stone", NYX_STONE);
    public static final RegistryEntry<BlockItem> DAWNBAND_STONE_ITEM = blockItem("dawnband_stone", DAWNBAND_STONE);
    public static final RegistryEntry<BlockItem> SHADOW_LICHEN_ITEM = blockItem("shadow_lichen", SHADOW_LICHEN);
    public static final RegistryEntry<BlockItem> VOID_CRYSTAL_ITEM = blockItem("void_crystal", VOID_CRYSTAL);
    public static final RegistryEntry<BlockItem> VOIDGLASS_ORE_ITEM = blockItem("voidglass_ore", VOIDGLASS_ORE);
    public static final RegistryEntry<BlockItem> GRAVITY_SHARD_BLOCK_ITEM = blockItem("gravity_shard_block",
            GRAVITY_SHARD_BLOCK);

    // --- Items ---------------------------------------------------------------------------------------------
    /** Voidglass, the D3 ore drop; ground into the Voidglass Lens. */
    public static final RegistryEntry<Item> VOIDGLASS = item("voidglass");
    /** Optics part (telescope / beam work). */
    public static final RegistryEntry<Item> VOIDGLASS_LENS = item("voidglass_lens");
    /** Nine make a Gravity Shard Block; a block breaks back into nine. */
    public static final RegistryEntry<Item> GRAVITY_SHARD = item("gravity_shard");
    /** Sheared from Shadow Lichen. */
    public static final RegistryEntry<Item> NIGHT_FIBRE = item("night_fibre");
    /** Void Crystal mined without Silk Touch. */
    public static final RegistryEntry<Item> VOID_SHARD = item("void_shard");

    private NyxaraContent() {
    }

    /** Forces class loading (and so registration). Called once from the shared registry init. */
    public static void init() {
    }

    /**
     * The Nyxara run of the planets tab, in 002 §6 order: Nyx Stone, Dawnband Stone, Shadow Lichen, Void Crystal,
     * Voidglass Ore, Gravity Shard Block, then Voidglass, Voidglass Lens, Gravity Shard, Night Fibre, Void Shard.
     */
    public static List<Supplier<? extends ItemLike>> creativeContents() {
        List<Supplier<? extends ItemLike>> out = new ArrayList<>();
        out.add(NYX_STONE_ITEM);
        out.add(DAWNBAND_STONE_ITEM);
        out.add(SHADOW_LICHEN_ITEM);
        out.add(VOID_CRYSTAL_ITEM);
        out.add(VOIDGLASS_ORE_ITEM);
        out.add(GRAVITY_SHARD_BLOCK_ITEM);
        out.add(VOIDGLASS);
        out.add(VOIDGLASS_LENS);
        out.add(GRAVITY_SHARD);
        out.add(NIGHT_FIBRE);
        out.add(VOID_SHARD);
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

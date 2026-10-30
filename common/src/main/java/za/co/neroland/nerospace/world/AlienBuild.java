package za.co.neroland.nerospace.world;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.village.AlienTrades;
import za.co.neroland.nerospace.village.VillagerCulture;
import za.co.neroland.nerospace.world.planet.cindara.CindaraContent;
import za.co.neroland.nerospace.world.planet.glacira.GlaciraContent;
import za.co.neroland.nerospace.world.planet.nyxara.NyxaraContent;

/**
 * Shared procedural pieces for the alien structures — a small kit that makes the hamlet, ruin and
 * mega-city read as futuristic alien architecture: tile podiums, brick walls with glowing crystal
 * window bands, taller lit corner pillars, tapered roofs and crystal spires.
 *
 * <p>Each villager culture builds the same shapes in its own {@link Palette}: Xertz in the alien block set
 * (the 1.6.0 look), Ember-kin in cindrite / basalt / magma glass, Frostbound in glacite / packed ice / blue ice,
 * Duskborn in nyx stone / void crystal / tinted glass. Writes go through a {@link Site}, which drops anything
 * outside the feature's writable {@link StructureRegion}.</p>
 */
public final class AlienBuild {

    /** The six block roles a settlement is built from. Suppliers, so palettes can be declared before registries fill. */
    public record Palette(Supplier<BlockState> bricks, Supplier<BlockState> cracked, Supplier<BlockState> tile,
            Supplier<BlockState> pillar, Supplier<BlockState> lamp, Supplier<BlockState> crystal) {
    }

    public static final Palette XERTZ = new Palette(
            () -> ModBlocks.ALIEN_BRICKS.get().defaultBlockState(),
            () -> ModBlocks.CRACKED_ALIEN_BRICKS.get().defaultBlockState(),
            () -> ModBlocks.ALIEN_TILE.get().defaultBlockState(),
            () -> ModBlocks.ALIEN_PILLAR.get().defaultBlockState(),
            () -> ModBlocks.ALIEN_LAMP.get().defaultBlockState(),
            () -> ModBlocks.ALIEN_CRYSTAL_BLOCK.get().defaultBlockState());

    public static final Palette EMBER_KIN = new Palette(
            () -> ModBlocks.CINDRITE_BLOCK.get().defaultBlockState(),
            () -> Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
            () -> Blocks.POLISHED_BASALT.defaultBlockState(),
            () -> CindaraContent.BASALT_SPIRE.get().defaultBlockState(),
            () -> Blocks.SHROOMLIGHT.defaultBlockState(),
            () -> CindaraContent.MAGMA_GLASS.get().defaultBlockState());

    public static final Palette FROSTBOUND = new Palette(
            () -> ModBlocks.GLACITE_BLOCK.get().defaultBlockState(),
            () -> GlaciraContent.PACKED_CRYO_ICE.get().defaultBlockState(),
            () -> Blocks.PACKED_ICE.defaultBlockState(),
            // Packed-ice pillars, not Permafrost: a Living terraformer thaws permafrost under the surface heightmap,
            // which would turn a pillar top into grass.
            () -> Blocks.PACKED_ICE.defaultBlockState(),
            () -> Blocks.SEA_LANTERN.defaultBlockState(),
            // Blue ice, not plain ice: window bands sit next to lamps and plain ice would melt.
            () -> Blocks.BLUE_ICE.defaultBlockState());

    public static final Palette DUSKBORN = new Palette(
            () -> NyxaraContent.NYX_STONE.get().defaultBlockState(),
            () -> Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState(),
            () -> Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
            () -> Blocks.PURPUR_PILLAR.defaultBlockState(),
            () -> NyxaraContent.VOID_CRYSTAL.get().defaultBlockState(),
            () -> Blocks.TINTED_GLASS.defaultBlockState());

    private AlienBuild() {
    }

    /** The palette a culture builds in; Xertz for none or an unknown culture. */
    public static Palette paletteFor(@Nullable VillagerCulture culture) {
        if (culture == null) {
            return XERTZ;
        }
        return switch (culture.id()) {
            case AlienTrades.EMBER_KIN -> EMBER_KIN;
            case AlienTrades.FROSTBOUND -> FROSTBOUND;
            case AlienTrades.DUSKBORN -> DUSKBORN;
            default -> XERTZ;
        };
    }

    // --- 1.6.0 accessors (Xertz palette) -------------------------------------------------------------------

    public static BlockState bricks() {
        return XERTZ.bricks().get();
    }

    public static BlockState cracked() {
        return XERTZ.cracked().get();
    }

    public static BlockState tile() {
        return XERTZ.tile().get();
    }

    public static BlockState pillar() {
        return XERTZ.pillar().get();
    }

    public static BlockState lamp() {
        return XERTZ.lamp().get();
    }

    public static BlockState crystal() {
        return XERTZ.crystal().get();
    }

    public static BlockState air() {
        return Blocks.AIR.defaultBlockState();
    }

    /**
     * One structure placement in progress: the level, the palette and the writable region. {@link #set} silently
     * skips any block outside the region (it would be dropped by vanilla anyway, with an error line per block).
     */
    public static final class Site {
        private final WorldGenLevel level;
        private final Palette palette;
        @Nullable
        private final StructureRegion region;
        private final BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        private int skipped;

        public Site(WorldGenLevel level, Palette palette, @Nullable StructureRegion region) {
            this.level = level;
            this.palette = palette;
            this.region = region;
        }

        public WorldGenLevel level() {
            return this.level;
        }

        public Palette palette() {
            return this.palette;
        }

        /** Blocks skipped because they fell outside the region (0 for every shipped structure). */
        public int skipped() {
            return this.skipped;
        }

        public boolean inRegion(int x, int z) {
            return this.region == null || this.region.contains(x, z);
        }

        public void set(int x, int y, int z, BlockState state) {
            if (!inRegion(x, z)) {
                this.skipped++;
                return;
            }
            this.m.set(x, y, z);
            this.level.setBlock(this.m, state, 2);
        }

        public BlockState bricks() {
            return this.palette.bricks().get();
        }

        public BlockState cracked() {
            return this.palette.cracked().get();
        }

        public BlockState tile() {
            return this.palette.tile().get();
        }

        public BlockState pillar() {
            return this.palette.pillar().get();
        }

        public BlockState lamp() {
            return this.palette.lamp().get();
        }

        public BlockState crystal() {
            return this.palette.crystal().get();
        }
    }

    /** The 1.6.0 entry point: Xertz palette, no region guard. */
    public static void tower(WorldGenLevel level, int cx, int baseY, int cz, int r, int height,
            boolean weathered, RandomSource rand, BlockPos.MutableBlockPos m) {
        tower(new Site(level, XERTZ, null), cx, baseY, cz, r, height, weathered, rand);
    }

    /**
     * A futuristic alien building. Tile podium, brick walls with a glowing crystal window band, taller
     * lit corner pillars, a tapered roof and a crystal spire. {@code weathered} → ruined variant
     * (cracked bricks, broken walls, no spire). {@code r} = half-footprint, {@code height} = wall height.
     * Origin is the building centre at ground level {@code baseY}. Footprint half-size is {@code r + 1}.
     */
    public static void tower(Site site, int cx, int baseY, int cz, int r, int height,
            boolean weathered, RandomSource rand) {
        BlockState wall = weathered ? site.cracked() : site.bricks();
        BlockState air = air();
        int band = Math.max(1, height / 2);

        for (int dx = -r - 1; dx <= r + 1; dx++) {
            for (int dz = -r - 1; dz <= r + 1; dz++) {
                site.set(cx + dx, baseY - 1, cz + dz, site.tile());
            }
        }
        for (int dy = 0; dy <= height + 2; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    site.set(cx + dx, baseY + dy, cz + dz, air);
                }
            }
        }

        for (int dy = 0; dy < height; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    boolean perimeter = Math.abs(dx) == r || Math.abs(dz) == r;
                    if (!perimeter) {
                        continue;
                    }
                    boolean corner = Math.abs(dx) == r && Math.abs(dz) == r;
                    if (corner) {
                        continue;
                    }
                    boolean door = dz == -r && dx == 0 && dy <= 1;
                    if (door) {
                        continue;
                    }
                    if (weathered && rand.nextFloat() < 0.28F) {
                        continue;
                    }
                    boolean window = dy == band;
                    site.set(cx + dx, baseY + dy, cz + dz, window ? site.crystal() : wall);
                }
            }
        }

        int pillarTop = weathered ? height - 1 : height + 1;
        for (int sx : new int[] {-r, r}) {
            for (int sz : new int[] {-r, r}) {
                for (int dy = 0; dy <= pillarTop; dy++) {
                    site.set(cx + sx, baseY + dy, cz + sz, site.pillar());
                }
                if (!weathered) {
                    site.set(cx + sx, baseY + pillarTop + 1, cz + sz, site.lamp());
                }
            }
        }

        for (int dx = -(r - 1); dx <= r - 1; dx++) {
            for (int dz = -(r - 1); dz <= r - 1; dz++) {
                if (weathered && rand.nextFloat() < 0.35F) {
                    continue;
                }
                site.set(cx + dx, baseY + height, cz + dz, dx == 0 && dz == 0 ? site.crystal() : site.tile());
            }
        }
        site.set(cx, baseY + 1, cz, site.lamp());

        if (!weathered) {
            for (int dy = 1; dy <= 3; dy++) {
                site.set(cx, baseY + height + dy, cz, site.crystal());
            }
        }
    }
}

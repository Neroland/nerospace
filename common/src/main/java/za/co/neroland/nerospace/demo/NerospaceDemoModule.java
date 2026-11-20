package za.co.neroland.nerospace.demo;

import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerolandcore.demo.DemoBuildContext;
import za.co.neroland.nerolandcore.demo.DemoFootprint;
import za.co.neroland.nerolandcore.demo.DemoModule;
import za.co.neroland.nerolandcore.demo.DemoRegistry;
import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.command.GallerySkyStates;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.item.CompassLanding;
import za.co.neroland.nerospace.machine.CombustionGeneratorBlockEntity;
import za.co.neroland.nerospace.machine.FuelRefineryBlockEntity;
import za.co.neroland.nerospace.machine.NerosiumGrinderBlockEntity;
import za.co.neroland.nerospace.pipe.PipeIoMode;
import za.co.neroland.nerospace.pipe.PipeResourceType;
import za.co.neroland.nerospace.pipe.UniversalPipeBlockEntity;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.rocket.Destinations;
import za.co.neroland.nerospace.rocket.RocketEntity;
import za.co.neroland.nerospace.rocket.RocketLaunchPadBlock;
import za.co.neroland.nerospace.rocket.RocketTier;
import za.co.neroland.nerospace.world.planet.ClockSource;
import za.co.neroland.nerospace.world.planet.PlanetClock;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.PlanetWeather;
import za.co.neroland.nerospace.world.planet.Planets;
import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.planet.nyxara.worldgen.NyxaraBand;

/**
 * Nerospace's exhibit in a Neroland Core <b>Nero Mods Demo</b> world: a launch row with every rocket tier on the pad
 * footprint it needs, a compact working machine line, and a row of walk-on travel pads, one per Nerospace world. Each
 * pad leads to a small viewing outpost on that world, held in the world's signature sky state from the
 * {@code /nerospace gallery sky} table ({@link GallerySkyStates}), with a pad back to the hub.
 *
 * <p>Everything is placed at fixed offsets and fixed world coordinates, so the same seed always gives the same
 * exhibit. Nothing about the player is read or stored; rockets and machines are placed unowned.
 */
public final class NerospaceDemoModule implements DemoModule {

    private static final DemoFootprint FOOTPRINT = DemoFootprint.of(36, 31);

    /** Plot row (z offset) of the launch pads, the machine line and the travel pads. */
    private static final int LAUNCH_Z = 7;
    private static final int MACHINE_Z = 17;
    private static final int TRAVEL_Z = 25;

    /** How far an outpost deck stands above the ground it is built over, and the longest leg under it. */
    private static final int DECK_RISE = 3;
    private static final int MAX_LEG = 12;

    /**
     * One viewing outpost.
     *
     * @param world the world's lang id ({@code /nerospace gallery sky} name)
     * @param state the sky state to hold it in
     * @param x     the column to stand on (the outpost searches outwards from here for dry ground)
     * @param z     the column to stand on
     * @param yaw   the facing on arrival, or {@code NaN} for the sky state's own facing
     */
    record Outpost(String world, String state, int x, int z, float yaw) {
    }

    /**
     * The stops, in pad order. The viewing columns are the ones the gallery capture already frames well with the
     * fixed demo seed (each world's centre column; Nyxara's is moved into the terminator band by its sky state).
     */
    static final List<Outpost> OUTPOSTS = List.of(
            new Outpost("station", "sunrise", 0, 0, Float.NaN),
            new Outpost("luna", "earthrise", 0, 0, Float.NaN),
            // Mars: west of the centre column is a cliff, so the deck faces north-west over the open plains.
            new Outpost("mars", "blue_sunset", 0, 0, 135.0F),
            new Outpost("cindara", "eclipse", 0, 0, Float.NaN),
            new Outpost("glacira", "aurora", 0, 0, Float.NaN),
            new Outpost("greenxertz", "twin_noon", 0, 0, Float.NaN),
            new Outpost("nyxara", "band", 0, 0, Float.NaN));

    /** Registers the module with Core; additive, never fails Nerospace's init. */
    public static void register() {
        try {
            DemoRegistry.register(new NerospaceDemoModule());
        } catch (RuntimeException | LinkageError e) {
            NerospaceCommon.LOGGER.warn("[Nerospace] demo module not registered: {}", e.getClass().getSimpleName());
        }
    }

    @Override
    public String modId() {
        return NerospaceCommon.MOD_ID;
    }

    @Override
    public Component displayName() {
        return Component.translatable("demo.nerospace.name");
    }

    @Override
    public Identifier icon() {
        return Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "textures/item/rocket_tier_1.png");
    }

    @Override
    public int sortOrder() {
        return 100;
    }

    @Override
    public DemoFootprint footprint() {
        return FOOTPRINT;
    }

    @Override
    public List<Component> guide() {
        return List.of(Component.translatable("demo.nerospace.guide.1"),
                Component.translatable("demo.nerospace.guide.2"),
                Component.translatable("demo.nerospace.guide.3"));
    }

    @Override
    public void build(DemoBuildContext ctx) {
        buildLaunchRow(ctx);
        buildMachineLine(ctx);
        buildTravelPads(ctx);
    }

    // --- launch row ------------------------------------------------------------------------------------------

    /** Every rocket tier on the pad it needs: T1 a single pad, T2 3x3, T3 3x3 in a Station Wall ring, T4 5x5. */
    private static void buildLaunchRow(DemoBuildContext ctx) {
        BlockState pad = ModBlocks.ROCKET_LAUNCH_PAD.get().defaultBlockState();
        int z = LAUNCH_Z;
        // T1: one pad, with the classic pad-side Fuel Tank.
        ctx.set(ctx.at(4, 1, z), pad);
        ctx.set(ctx.at(6, 1, z), ModBlocks.FUEL_TANK.get().defaultBlockState());
        rocket(ctx, ctx.at(4, 1, z), RocketTier.TIER_1);
        ctx.label(ctx.at(4, 9, z), Component.translatable("demo.nerospace.label.tier", 1));
        // T2: 3x3.
        square(ctx, ctx.at(10, 1, z - 1), 3, pad);
        rocket(ctx, ctx.at(11, 1, z), RocketTier.TIER_2);
        ctx.label(ctx.at(11, 11, z), Component.translatable("demo.nerospace.label.tier", 2));
        // T3: 3x3 ringed with Station Wall.
        BlockState wall = ModBlocks.STATION_WALL.get().defaultBlockState();
        for (int dx = -1; dx <= 3; dx++) {
            for (int dz = -1; dz <= 3; dz++) {
                if (dx == -1 || dx == 3 || dz == -1 || dz == 3) {
                    ctx.set(ctx.at(17 + dx, 1, z - 1 + dz), wall);
                }
            }
        }
        square(ctx, ctx.at(17, 1, z - 1), 3, pad);
        rocket(ctx, ctx.at(18, 1, z), RocketTier.TIER_3);
        ctx.label(ctx.at(18, 12, z), Component.translatable("demo.nerospace.label.tier", 3));
        // T4: the Heavy Launch Complex, a full 5x5 with a Launch Gantry and a Fuel Tank on its sides.
        square(ctx, ctx.at(26, 1, z - 2), 5, pad);
        ctx.set(ctx.at(25, 1, z), ModBlocks.LAUNCH_GANTRY.get().defaultBlockState());
        ctx.set(ctx.at(31, 1, z), ModBlocks.FUEL_TANK.get().defaultBlockState());
        rocket(ctx, ctx.at(28, 1, z), RocketTier.TIER_4);
        ctx.label(ctx.at(28, 15, z), Component.translatable("demo.nerospace.label.tier", 4));
    }

    private static void square(DemoBuildContext ctx, BlockPos corner, int size, BlockState state) {
        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                ctx.set(corner.offset(dx, 0, dz), state);
            }
        }
    }

    /** A rocket standing on the pad block at {@code pad}, tanks full, owned by nobody. */
    private static void rocket(DemoBuildContext ctx, BlockPos pad, RocketTier tier) {
        ServerLevel level = ctx.level();
        RocketEntity rocket = new RocketEntity(level, pad.getX() + 0.5D,
                pad.getY() + RocketLaunchPadBlock.SURFACE_HEIGHT, pad.getZ() + 0.5D, tier);
        rocket.addFuel(tier.fuelCapacity());
        rocket.addOxygen(tier.oxygenCapacity());
        level.addFreshEntity(rocket);
    }

    // --- machine line ----------------------------------------------------------------------------------------

    /**
     * The first power line and the fuel line, running: Combustion Generator (coal) to a Nerosium Grinder (raw
     * nerosium) with a Passive Generator alongside, then an endless Core battery powering a Fuel Refinery that
     * pipes rocket fuel into a Fuel Tank. Pipe faces are set the way a player would set them with a Configurator.
     */
    private static void buildMachineLine(DemoBuildContext ctx) {
        ServerLevel level = ctx.level();
        int z = MACHINE_Z;
        BlockState pipe = ModBlocks.UNIVERSAL_PIPE.get().defaultBlockState();
        BlockPos gen = ctx.at(5, 1, z);
        ctx.set(gen, ModBlocks.COMBUSTION_GENERATOR.get().defaultBlockState());
        ctx.set(ctx.at(6, 1, z), pipe);
        BlockPos grinder = ctx.at(7, 1, z);
        ctx.set(grinder, ModBlocks.NEROSIUM_GRINDER.get().defaultBlockState());
        ctx.set(ctx.at(6, 1, z + 1), ModBlocks.PASSIVE_GENERATOR.get().defaultBlockState());
        if (level.getBlockEntity(gen) instanceof CombustionGeneratorBlockEntity g) {
            g.setItem(CombustionGeneratorBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL, 64));
        }
        if (level.getBlockEntity(grinder) instanceof NerosiumGrinderBlockEntity g) {
            g.setItem(NerosiumGrinderBlockEntity.INPUT_SLOT, new ItemStack(ModItems.RAW_NEROSIUM.get(), 64));
        }
        ctx.label(ctx.at(6, 4, z), Component.translatable("demo.nerospace.label.power_line"));

        int bx = 14;
        ctx.set(ctx.at(bx, 1, z),
                za.co.neroland.nerolandcore.registry.ModBlocks.CREATIVE_BATTERY.get().defaultBlockState());
        ctx.set(ctx.at(bx + 1, 1, z), pipe);
        BlockPos refinery = ctx.at(bx + 2, 1, z);
        ctx.set(refinery, ModBlocks.FUEL_REFINERY.get().defaultBlockState());
        ctx.set(ctx.at(bx + 3, 1, z), pipe);
        ctx.set(ctx.at(bx + 4, 1, z), ModBlocks.FUEL_TANK.get().defaultBlockState());
        allModes(level, ctx.at(bx + 1, 1, z), Direction.WEST, PipeIoMode.IN);
        allModes(level, ctx.at(bx + 1, 1, z), Direction.EAST, PipeIoMode.OUT);
        allModes(level, ctx.at(bx + 3, 1, z), Direction.WEST, PipeIoMode.IN);
        allModes(level, ctx.at(bx + 3, 1, z), Direction.EAST, PipeIoMode.OUT);
        if (level.getBlockEntity(refinery) instanceof FuelRefineryBlockEntity r) {
            r.setItem(FuelRefineryBlockEntity.CARBON_SLOT, new ItemStack(Items.COAL, 64));
            r.setItem(FuelRefineryBlockEntity.CATALYST_SLOT, new ItemStack(Items.BLAZE_POWDER, 64));
        }
        ctx.label(ctx.at(bx + 2, 4, z), Component.translatable("demo.nerospace.label.fuel_line"));

        int ox = 25;
        ctx.set(ctx.at(ox, 1, z), ModBlocks.SOLAR_PANEL.get().defaultBlockState());
        ctx.set(ctx.at(ox + 1, 1, z), ModBlocks.OXYGEN_GENERATOR.get().defaultBlockState());
        ctx.set(ctx.at(ox + 2, 1, z), ModBlocks.ALIEN_LAMP.get().defaultBlockState());
        ctx.label(ctx.at(ox + 1, 4, z), Component.translatable("demo.nerospace.label.life_support"));
    }

    private static void allModes(ServerLevel level, BlockPos pos, Direction face, PipeIoMode mode) {
        if (level.getBlockEntity(pos) instanceof UniversalPipeBlockEntity pipe) {
            for (PipeResourceType type : PipeResourceType.VALUES) {
                pipe.setMode(face, type, mode);
            }
        }
    }

    // --- travel pads and outposts ----------------------------------------------------------------------------

    private static void buildTravelPads(DemoBuildContext ctx) {
        int x = 4;
        for (Outpost outpost : OUTPOSTS) {
            PlanetDefinition planet = planet(outpost.world());
            if (planet == null) {
                continue;
            }
            ServerLevel world = ctx.server().getLevel(planet.dimension());
            if (world == null) {
                continue; // that world is disabled on this server: no pad
            }
            try {
                Arrival arrival = buildOutpost(ctx, world, planet, outpost);
                BlockPos plate = ctx.at(x, 1, TRAVEL_Z);
                ctx.travelPad(plate, planet.dimension(), arrival.at(), arrival.yaw(), arrival.pitch());
                // Labels float well above the plate: an armor stand touching a pressure plate would hold it down.
                ctx.guide(ctx.at(x, 5, TRAVEL_Z), List.of(
                        Component.translatable("demo.nerospace.label.travel",
                                Destinations.displayName(planet.dimension()))
                                .withStyle(ChatFormatting.AQUA),
                        Component.translatable("demo.nerospace.label.sky." + outpost.state())));
            } catch (RuntimeException e) {
                NerospaceCommon.LOGGER.warn("[Nerospace] demo outpost on {} not built", outpost.world(), e);
            }
            x += 4;
        }
    }

    /** Where a travel pad lands the player: feet position and facing. */
    record Arrival(Vec3 at, float yaw, float pitch) {
    }

    private static PlanetDefinition planet(String langId) {
        for (PlanetDefinition p : Planets.ALL) {
            if (p.langId().equals(langId)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Holds {@code world} in the outpost's sky state, finds a dry viewing column near the outpost's spot, lays a
     * small platform there with a pad back to the hub behind the viewer, and returns where to arrive.
     */
    private static Arrival buildOutpost(DemoBuildContext ctx, ServerLevel world, PlanetDefinition planet,
            Outpost outpost) {
        Optional<GallerySkyStates.State> found = GallerySkyStates.find(planet.langId(), outpost.state());
        GallerySkyStates.State state = found.orElse(null);
        int x = outpost.x();
        int z = outpost.z();
        if (state != null) {
            holdSky(world, planet, state);
            if (world.dimension().equals(ModDimensions.NYXARA_LEVEL)) {
                x = GallerySkyStates.viewX(state.view(), x, NyxaraBand.width(world), NyxaraBand.originX(world),
                        NyxaraBand.edgeNoise(world));
            }
        }
        float yaw = !Float.isNaN(outpost.yaw()) ? outpost.yaw()
                : state == null || Float.isNaN(state.yaw()) ? -90.0F : state.yaw();
        float pitch = state == null || Float.isNaN(state.pitch()) ? 0.0F : Mth.clamp(state.pitch(), -30.0F, 30.0F);

        // A small observation deck a few blocks above the ground, so nearby slopes do not block the view, on four
        // Station Wall legs.
        BlockPos feet = viewingSpot(world, x, z).above(DECK_RISE);
        BlockState floor = ModBlocks.STATION_FLOOR.get().defaultBlockState();
        BlockState leg = ModBlocks.STATION_WALL.get().defaultBlockState();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.setBlockAndUpdate(feet.offset(dx, -1, dz), floor);
                for (int dy = 0; dy <= 2; dy++) {
                    world.setBlockAndUpdate(feet.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (int dx = -2; dx <= 2; dx += 4) {
            for (int dz = -2; dz <= 2; dz += 4) {
                for (int dy = 2; dy <= 2 + MAX_LEG; dy++) {
                    BlockPos p = feet.offset(dx, -dy, dz);
                    if (!world.getBlockState(p).isAir() && world.getFluidState(p).isEmpty()) {
                        break;
                    }
                    world.setBlockAndUpdate(p, leg);
                }
            }
        }
        // The return pad sits behind the viewer, so the view itself stays clear.
        Direction facing = Direction.fromYRot(yaw);
        BlockPos back = feet.relative(facing.getOpposite(), 2);
        ctx.returnPad(world, back);
        ctx.guide(world, back.above(4), List.of(
                Destinations.displayName(planet.dimension()).copy().withStyle(ChatFormatting.GOLD),
                Component.translatable("demo.nerospace.label.return")));
        return new Arrival(new Vec3(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D), yaw, pitch);
    }

    /**
     * Sets and freezes the world's clock to the state's phase and clears (or starts) its weather, through the same
     * clock and weather APIs {@code /nerospace gallery sky} uses.
     */
    private static void holdSky(ServerLevel world, PlanetDefinition planet, GallerySkyStates.State state) {
        boolean movingClock = planet.clockSource() == ClockSource.PLANET_CLOCK && !planet.traits().fixedClock();
        if (state.setsPhase() && (!movingClock || NerospaceConfig.clockEnabled())) {
            PlanetClock.set(world, state.phase());
            if (movingClock) {
                PlanetClock.setFrozen(world, true);
            }
        }
        if (state.weather() == GallerySkyStates.Weather.START) {
            if (PlanetWeather.enabledFor(planet)) {
                PlanetWeather.startAtPeak(world);
            }
        } else if (state.weather() == GallerySkyStates.Weather.START_SECONDARY) {
            if (PlanetWeather.enabledFor(planet)) {
                PlanetWeather.startSecondaryAtPeak(world);
            }
        } else if (PlanetWeather.event(world) != WeatherEvent.NONE) {
            PlanetWeather.clear(world);
        }
    }

    /** Feet position of a dry column near ({@code x}, {@code z}), searching outwards; a fixed height if none. */
    private static BlockPos viewingSpot(ServerLevel level, int x, int z) {
        for (int r = 0; r <= 32; r += 8) {
            for (int dx = -r; dx <= r; dx += Math.max(8, r)) {
                for (int dz = -r; dz <= r; dz += Math.max(8, r)) {
                    if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r) {
                        continue;
                    }
                    int cx = x + dx;
                    int cz = z + dz;
                    level.getChunk(cx >> 4, cz >> 4);
                    int feet = CompassLanding.feetY(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz),
                            level.getMinY());
                    if (feet >= 0 && level.getFluidState(new BlockPos(cx, feet - 1, cz)).isEmpty()) {
                        return new BlockPos(cx, feet, cz);
                    }
                }
            }
        }
        level.getChunk(x >> 4, z >> 4);
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, (top > level.getMinY() ? top : CompassLanding.PLATFORM_Y) + 1, z);
    }
}

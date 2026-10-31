package za.co.neroland.nerospace.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.config.NerospaceConfig;
import za.co.neroland.nerospace.item.CompassLanding;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.rocket.Destinations;
import za.co.neroland.nerospace.telemetry.NerospaceTelemetry;
import za.co.neroland.nerospace.world.planet.ClockSource;
import za.co.neroland.nerospace.world.planet.PlanetClock;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.PlanetWeather;
import za.co.neroland.nerospace.world.planet.Planets;
import za.co.neroland.nerospace.world.planet.WeatherEvent;
import za.co.neroland.nerospace.world.planet.WeatherMachine;
import za.co.neroland.nerospace.world.planet.nyxara.worldgen.NyxaraBand;

/**
 * {@code /nerospace clock <planet> [get | set <phase> | freeze | thaw]} and
 * {@code /nerospace weather <planet> [get | start [intensity] | clear]} — operator tools (permission level 2), and
 * {@code /nerospace gallery sky ...} ({@link #gallerySky}), the sky showcase built on them.
 * A planet whose dimension is not present on the server is refused with a message, never a crash. Logs carry
 * the planet and action only (no player identity).
 */
public final class PlanetCommands {

    private PlanetCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("nerospace")
                .then(Commands.literal("clock")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("planet", StringArgumentType.word())
                                .suggests(PlanetCommands::suggestPlanets)
                                .executes(ctx -> run(ctx, "clock get", () -> clockGet(ctx)))
                                .then(Commands.literal("get")
                                        .executes(ctx -> run(ctx, "clock get", () -> clockGet(ctx))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("phase", DoubleArgumentType.doubleArg(0.0, 1.0))
                                                .executes(ctx -> run(ctx, "clock set", () -> clockSet(ctx)))))
                                .then(Commands.literal("freeze")
                                        .executes(ctx -> run(ctx, "clock freeze", () -> clockFreeze(ctx, true))))
                                .then(Commands.literal("thaw")
                                        .executes(ctx -> run(ctx, "clock thaw", () -> clockFreeze(ctx, false))))))
                .then(Commands.literal("weather")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("planet", StringArgumentType.word())
                                .suggests(PlanetCommands::suggestPlanets)
                                .executes(ctx -> run(ctx, "weather get", () -> weatherGet(ctx)))
                                .then(Commands.literal("get")
                                        .executes(ctx -> run(ctx, "weather get", () -> weatherGet(ctx))))
                                .then(Commands.literal("start")
                                        .executes(ctx -> run(ctx, "weather start", () -> weatherStart(ctx, 1.0F)))
                                        .then(Commands.argument("intensity", FloatArgumentType.floatArg(
                                                        WeatherMachine.MIN_TARGET, WeatherMachine.MAX_TARGET))
                                                .executes(ctx -> run(ctx, "weather start", () -> weatherStart(ctx,
                                                        FloatArgumentType.getFloat(ctx, "intensity"))))))
                                .then(Commands.literal("clear")
                                        .executes(ctx -> run(ctx, "weather clear", () -> weatherClear(ctx))))));
    }

    private static CompletableFuture<Suggestions> suggestPlanets(CommandContext<CommandSourceStack> ctx,
            SuggestionsBuilder builder) {
        var server = ctx.getSource().getServer();
        return SharedSuggestionProvider.suggest(Planets.ALL.stream()
                .filter(p -> p.isAvailable(server)).map(PlanetDefinition::langId), builder);
    }

    /** The planet level named by the argument, or null after sending the failure message. */
    private static ServerLevel level(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "planet");
        PlanetDefinition planet = Planets.ALL.stream().filter(p -> p.langId().equals(name)).findFirst().orElse(null);
        if (planet == null) {
            ctx.getSource().sendFailure(Component.translatable("command.nerospace.planet.unknown", name));
            return null;
        }
        ServerLevel level = ctx.getSource().getServer().getLevel(planet.dimension());
        if (level == null) {
            ctx.getSource().sendFailure(Component.translatable("command.nerospace.planet.unavailable",
                    Destinations.displayName(planet.dimension())));
        }
        return level;
    }

    private static Component planetName(ServerLevel level) {
        return Destinations.displayName(level.dimension());
    }

    private static int clockGet(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = level(ctx);
        if (level == null) {
            return 0;
        }
        double phase = PlanetClock.phase(level);
        double daylight = PlanetClock.daylight(level);
        long day = PlanetClock.dayNumber(level);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.nerospace.clock.get", planetName(level),
                String.format(java.util.Locale.ROOT, "%.3f", phase),
                String.format(java.util.Locale.ROOT, "%.2f", daylight), day), false);
        return 1;
    }

    private static int clockSet(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = level(ctx);
        if (level == null) {
            return 0;
        }
        double phase = DoubleArgumentType.getDouble(ctx, "phase");
        if (!PlanetClock.set(level, phase)) {
            ctx.getSource().sendFailure(Component.translatable("command.nerospace.clock.unsupported", planetName(level)));
            return 0;
        }
        NerospaceCommon.LOGGER.info("[Nerospace] operator set the {} clock", level.dimension().identifier());
        ctx.getSource().sendSuccess(() -> Component.translatable("command.nerospace.clock.set", planetName(level),
                String.format(java.util.Locale.ROOT, "%.3f", phase)), true);
        return 1;
    }

    private static int clockFreeze(CommandContext<CommandSourceStack> ctx, boolean frozen) {
        ServerLevel level = level(ctx);
        if (level == null) {
            return 0;
        }
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet != null && planet.clockSource() == ClockSource.LEVEL_TIME) {
            ctx.getSource().sendFailure(Component.translatable("command.nerospace.clock.vanilla", planetName(level)));
            return 0;
        }
        if (!PlanetClock.setFrozen(level, frozen)) {
            ctx.getSource().sendFailure(Component.translatable("command.nerospace.clock.unsupported", planetName(level)));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable(frozen
                ? "command.nerospace.clock.frozen" : "command.nerospace.clock.thawed", planetName(level)), true);
        return 1;
    }

    private static int weatherGet(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = level(ctx);
        if (level == null) {
            return 0;
        }
        WeatherMachine m = PlanetWeather.current(level);
        if (m == null) {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.nerospace.weather.none", planetName(level)), false);
            return 1;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.nerospace.weather.get", planetName(level),
                Component.translatable("weather.nerospace." + (m.active() ? m.event : "none").toString().toLowerCase(java.util.Locale.ROOT)),
                m.state.name().toLowerCase(java.util.Locale.ROOT),
                String.format(java.util.Locale.ROOT, "%.2f", m.intensity)), false);
        return 1;
    }

    private static int weatherStart(CommandContext<CommandSourceStack> ctx, float intensity) {
        ServerLevel level = level(ctx);
        if (level == null) {
            return 0;
        }
        if (!PlanetWeather.start(level, intensity)) {
            ctx.getSource().sendFailure(Component.translatable("command.nerospace.weather.none", planetName(level)));
            return 0;
        }
        NerospaceCommon.LOGGER.info("[Nerospace] operator started weather on {}", level.dimension().identifier());
        ctx.getSource().sendSuccess(() -> Component.translatable("command.nerospace.weather.started", planetName(level)), true);
        return 1;
    }

    private static int weatherClear(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = level(ctx);
        if (level == null) {
            return 0;
        }
        if (!PlanetWeather.clear(level)) {
            ctx.getSource().sendFailure(Component.translatable("command.nerospace.weather.none", planetName(level)));
            return 0;
        }
        NerospaceCommon.LOGGER.info("[Nerospace] operator cleared weather on {}", level.dimension().identifier());
        ctx.getSource().sendSuccess(() -> Component.translatable("command.nerospace.weather.cleared", planetName(level)), true);
        return 1;
    }

    // --- /nerospace gallery sky -------------------------------------------------------------------------

    /**
     * {@code /nerospace gallery sky [reset [<planet>] | <planet> <state> [here]]}: holds a world's sky in one of its
     * showcase states for screenshots. Attached under the {@code gallery} node; operators only (level 2), like the
     * clock and weather commands. A state sets the clock through {@link PlanetClock#set} and freezes it (both sync
     * the clock payload to everyone on that world at once), and starts or clears the weather through the same state
     * machine as {@code /nerospace weather start}, already at full strength. {@code here} moves the player to a
     * viewing spot on that world, decided by the server.
     */
    static LiteralArgumentBuilder<CommandSourceStack> gallerySky() {
        return Commands.literal("sky")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> run(ctx, "gallery sky", () -> skyList(ctx)))
                .then(Commands.literal("reset")
                        .executes(ctx -> run(ctx, "gallery sky reset", () -> skyReset(ctx, false)))
                        .then(Commands.argument("planet", StringArgumentType.word())
                                .suggests(PlanetCommands::suggestPlanets)
                                .executes(ctx -> run(ctx, "gallery sky reset", () -> skyReset(ctx, true)))))
                .then(Commands.argument("planet", StringArgumentType.word())
                        .suggests(PlanetCommands::suggestPlanets)
                        .then(Commands.argument("state", StringArgumentType.word())
                                .suggests(PlanetCommands::suggestSkyStates)
                                .executes(ctx -> run(ctx, "gallery sky", () -> skyState(ctx, false)))
                                .then(Commands.literal("here")
                                        .executes(ctx -> run(ctx, "gallery sky here", () -> skyState(ctx, true))))));
    }

    /** Only the states of the planet typed before it. */
    private static CompletableFuture<Suggestions> suggestSkyStates(CommandContext<CommandSourceStack> ctx,
            SuggestionsBuilder builder) {
        String planet;
        try {
            planet = StringArgumentType.getString(ctx, "planet");
        } catch (IllegalArgumentException ex) {
            return builder.buildFuture();
        }
        return SharedSuggestionProvider.suggest(GallerySkyStates.names(planet), builder);
    }

    private static int skyList(CommandContext<CommandSourceStack> ctx) {
        var server = ctx.getSource().getServer();
        ctx.getSource().sendSuccess(() -> Component.translatable("command.nerospace.gallery.sky.header"), false);
        for (PlanetDefinition planet : Planets.ALL) {
            if (!planet.isAvailable(server)) {
                continue;
            }
            String states = String.join(", ", GallerySkyStates.names(planet.langId()));
            ctx.getSource().sendSuccess(() -> Component.translatable("command.nerospace.gallery.sky.planet",
                    planet.langId(), Destinations.displayName(planet.dimension()), states), false);
        }
        return 1;
    }

    private static int skyState(CommandContext<CommandSourceStack> ctx, boolean here) {
        ServerLevel level = level(ctx);
        if (level == null) {
            return 0;
        }
        PlanetDefinition planet = Planets.get(level.dimension());
        String word = StringArgumentType.getString(ctx, "state");
        Optional<GallerySkyStates.State> found = GallerySkyStates.find(planet.langId(), word);
        if (found.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("command.nerospace.gallery.sky.unknown_state",
                    planetName(level), word, String.join(", ", GallerySkyStates.names(planet.langId()))));
            return 0;
        }
        GallerySkyStates.State s = found.get();
        ServerPlayer player = ctx.getSource().getPlayer();
        if (here && player == null) {
            ctx.getSource().sendFailure(Component.translatable("command.nerospace.player_only"));
            return 0;
        }
        boolean movingClock = planet.clockSource() == ClockSource.PLANET_CLOCK && !planet.traits().fixedClock();
        if (s.setsPhase() && movingClock && !NerospaceConfig.clockEnabled()) {
            ctx.getSource().sendFailure(Component.translatable("command.nerospace.gallery.sky.clock_off",
                    planetName(level)));
            return 0;
        }

        // The clock: the same API as /nerospace clock set, then held still (a vanilla-day world keeps running).
        if (s.setsPhase()) {
            PlanetClock.set(level, s.phase());
            if (movingClock) {
                PlanetClock.setFrozen(level, true);
            }
        }
        // The weather: the same state machine as /nerospace weather start, at full strength; otherwise clear skies.
        boolean weatherOff = false;
        if (s.weather() == GallerySkyStates.Weather.START) {
            if (PlanetWeather.enabledFor(planet)) {
                PlanetWeather.startAtPeak(level);
            } else {
                weatherOff = true;
            }
        } else if (PlanetWeather.event(level) != WeatherEvent.NONE) {
            PlanetWeather.clear(level);
        }
        if (here) {
            moveToView(player, level, s);
        }
        NerospaceCommon.LOGGER.info("[Nerospace] operator set the {} gallery sky to {}", level.dimension().identifier(),
                s.name());

        boolean runningDay = s.setsPhase() && planet.clockSource() == ClockSource.LEVEL_TIME;
        ctx.getSource().sendSuccess(() -> Component.translatable(runningDay
                ? "command.nerospace.gallery.sky.set_running" : "command.nerospace.gallery.sky.set",
                planetName(level), s.name()), true);
        if (weatherOff) {
            ctx.getSource().sendFailure(Component.translatable("command.nerospace.gallery.sky.weather_off",
                    planetName(level)));
        }
        if (here) {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.nerospace.gallery.sky.moved",
                    planetName(level)), false);
        } else if (s.view() != GallerySkyStates.View.STAY && level.dimension().equals(ModDimensions.NYXARA_LEVEL)) {
            int fromX = ctx.getSource().getLevel() == level ? Mth.floor(ctx.getSource().getPosition().x)
                    : NyxaraBand.originX(level);
            int x = GallerySkyStates.viewX(s.view(), fromX, NyxaraBand.width(level), NyxaraBand.originX(level),
                    NyxaraBand.edgeNoise(level));
            ctx.getSource().sendSuccess(() -> Component.translatable("command.nerospace.gallery.sky.position",
                    planetName(level), x), false);
        }
        return 1;
    }

    /**
     * Moves the player to a safe, loaded viewing spot on {@code level} and turns them as the state asks: their own
     * column when they are already on that world (Nyxara: the band or a side, past the ragged terminator), the
     * world's centre column otherwise. Never onto a fluid surface or into the void.
     */
    private static void moveToView(ServerPlayer player, ServerLevel level, GallerySkyStates.State s) {
        boolean onPlanet = player.level() == level;
        double x = onPlanet ? player.getX() : 0.5D;
        double z = onPlanet ? player.getZ() : 0.5D;
        if (level.dimension().equals(ModDimensions.NYXARA_LEVEL)) {
            int vx = GallerySkyStates.viewX(s.view(), Mth.floor(x), NyxaraBand.width(level),
                    NyxaraBand.originX(level), NyxaraBand.edgeNoise(level));
            if (vx != Mth.floor(x)) {
                x = vx + 0.5D;
            }
        }
        float yaw = Float.isNaN(s.yaw()) ? player.getYRot() : s.yaw();
        float pitch = Float.isNaN(s.pitch()) ? player.getXRot() : s.pitch();
        if (onPlanet && Mth.floor(x) == player.getBlockX()) {
            // Already standing on that world, in the right place: only turn.
            player.teleportTo(level, player.getX(), player.getY(), player.getZ(), Set.of(), yaw, pitch, true);
            return;
        }
        BlockPos feet = viewingSpot(level, Mth.floor(x), Mth.floor(z));
        player.teleportTo(level, feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D, Set.of(), yaw, pitch, true);
    }

    /** Feet position of a dry column near ({@code x}, {@code z}), searching outwards; a small floor if none. */
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
        // Void or wet all round (the station, a lava sea): lay a small floor, only into empty space or fluid.
        level.getChunk(x >> 4, z >> 4);
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int floorY = top > level.getMinY() ? top : CompassLanding.PLATFORM_Y;
        BlockState floor = ModBlocks.STATION_FLOOR.get().defaultBlockState();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos p = new BlockPos(x + dx, floorY, z + dz);
                if (level.getBlockState(p).isAir() || !level.getFluidState(p).isEmpty()) {
                    level.setBlockAndUpdate(p, floor);
                }
            }
        }
        return new BlockPos(x, floorY + 1, z);
    }

    private static int skyReset(CommandContext<CommandSourceStack> ctx, boolean onePlanet) {
        if (onePlanet) {
            ServerLevel level = level(ctx);
            if (level == null) {
                return 0;
            }
            resetSky(level);
            ctx.getSource().sendSuccess(() -> Component.translatable("command.nerospace.gallery.sky.reset",
                    planetName(level)), true);
            return 1;
        }
        var server = ctx.getSource().getServer();
        int count = 0;
        for (PlanetDefinition planet : Planets.ALL) {
            ServerLevel level = server.getLevel(planet.dimension());
            if (level != null) {
                resetSky(level);
                count++;
            }
        }
        int done = count;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.nerospace.gallery.sky.reset_all", done), true);
        return 1;
    }

    /** Thaws the clock (or drops a fixed clock's override) and ends any weather on one world. */
    private static void resetSky(ServerLevel level) {
        PlanetDefinition planet = Planets.get(level.dimension());
        if (planet == null) {
            return;
        }
        if (planet.clockSource() == ClockSource.PLANET_CLOCK) {
            if (planet.traits().fixedClock()) {
                PlanetClock.resetPinned(level);
            } else {
                PlanetClock.setFrozen(level, false);
            }
        }
        if (PlanetWeather.event(level) != WeatherEvent.NONE) {
            PlanetWeather.clear(level);
        }
        NerospaceCommon.LOGGER.info("[Nerospace] operator reset the {} gallery sky", level.dimension().identifier());
    }

    private static int run(CommandContext<CommandSourceStack> ctx, String name, Body body) {
        try {
            return body.run();
        } catch (RuntimeException ex) {
            NerospaceTelemetry.captureHandledException(ex, "command", "/nerospace " + name);
            NerospaceCommon.LOGGER.error("[Nerospace] /nerospace {} failed", name, ex);
            ctx.getSource().sendFailure(Component.translatable("command.nerospace.failed", name));
            return 0;
        }
    }

    @FunctionalInterface
    private interface Body {
        int run();
    }
}

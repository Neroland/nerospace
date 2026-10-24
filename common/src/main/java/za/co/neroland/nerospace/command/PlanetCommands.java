package za.co.neroland.nerospace.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.rocket.Destinations;
import za.co.neroland.nerospace.telemetry.NerospaceTelemetry;
import za.co.neroland.nerospace.world.planet.ClockSource;
import za.co.neroland.nerospace.world.planet.PlanetClock;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.PlanetWeather;
import za.co.neroland.nerospace.world.planet.Planets;
import za.co.neroland.nerospace.world.planet.WeatherMachine;

/**
 * {@code /nerospace clock <planet> [get | set <phase> | freeze | thaw]} and
 * {@code /nerospace weather <planet> [get | start [intensity] | clear]} — operator tools (permission level 2).
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

package za.co.neroland.nerospace.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;

import za.co.neroland.nerospace.block.light.LightAbsorbing;
import za.co.neroland.nerospace.block.light.RelightBench;
import za.co.neroland.nerospace.platform.Services;

/**
 * {@code /nerospace dev relightbench <count>} — development-environment, operator-only timing of a 16-chunk block
 * relight with 0 vs {@code count} Nyx Stone (measurement). Results go to the command source only.
 */
public final class DevCommands {

    private DevCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("nerospace")
                .then(Commands.literal("dev")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("relightbench")
                                .then(Commands.argument("count", IntegerArgumentType.integer(0, 4096))
                                        .executes(ctx -> relightBench(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "count"))))));
    }

    private static int relightBench(CommandSourceStack source, int count) {
        if (!Services.PLATFORM.isDevelopmentEnvironment()) {
            source.sendFailure(Component.translatable("command.nerospace.dev.relightbench_unavailable"));
            return 0;
        }
        ChunkPos centre = ChunkPos.containing(BlockPos.containing(source.getPosition()));
        double[] ms = RelightBench.run(source.getLevel(), centre, count, 0x5EEDL);
        if (ms == null) {
            source.sendFailure(Component.literal("relightbench: the 4x4 chunk area is not fully loaded"));
            return 0;
        }
        int chunks = RelightBench.SIDE * RelightBench.SIDE;
        String delta = ms[0] > 0 ? String.format("%+.1f%%", (ms[1] - ms[0]) / ms[0] * 100.0) : "n/a";
        String enabled = LightAbsorbing.enabled() ? "on" : "off";
        source.sendSuccess(() -> Component.translatable("command.nerospace.dev.relightbench",
                chunks, 0, String.format("%.2f", ms[0]), enabled), false);
        source.sendSuccess(() -> Component.translatable("command.nerospace.dev.relightbench",
                chunks, count, String.format("%.2f", ms[1]), enabled + ", " + delta), false);
        return 1;
    }
}

package za.co.neroland.nerospace.command;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.Command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModEntities;

/**
 * Creative-only debug commands (cheats / op level 2). {@code /nerospace gallery} builds a showcase
 * platform near the player: every Nerospace block floating two blocks above the floor (so all faces
 * are visible) on a ~3-block grid, a small "connected structure" cluster, and each creature spawned
 * twice — once with AI and once frozen (NoAI) — for inspection.
 */
@EventBusSubscriber(modid = Nerospace.MODID)
public final class NerospaceCommands {

    private static final int SPACING = 3;     // blocks between display cells
    private static final int FLOAT_ABOVE = 3; // display sits this many blocks above the floor (2 air gap)

    private NerospaceCommands() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        // Player-only; the executor further restricts to creative. (Commands themselves require the
        // world to have cheats/commands enabled, so this is effectively creative + commands gated.)
        event.getDispatcher().register(
                Commands.literal("nerospace")
                        .requires(src -> src.getPlayer() != null)
                        .then(Commands.literal("gallery")
                                .executes(ctx -> buildGallery(ctx.getSource()))));
    }

    private static int buildGallery(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Run this as a player."));
            return 0;
        }
        if (!player.getAbilities().instabuild) {
            source.sendFailure(Component.literal("The Nerospace gallery is creative-only."));
            return 0;
        }
        ServerLevel level = player.level();
        BlockPos origin = player.blockPosition();

        List<Block> blocks = new ArrayList<>();
        for (var holder : ModBlocks.BLOCKS.getEntries()) {
            Block block = holder.value();
            if (block != ModBlocks.ROCKET_FUEL_BLOCK.get()) { // skip the fluid block (renders oddly free-standing)
                blocks.add(block);
            }
        }

        int cols = (int) Math.ceil(Math.sqrt(Math.max(1, blocks.size())));
        int rows = (int) Math.ceil(blocks.size() / (double) cols);
        int ox = origin.getX() + 4;
        int oz = origin.getZ() + 4;
        int fy = origin.getY();

        BlockState floor = ModBlocks.STATION_FLOOR.get().defaultBlockState();

        // Floor slab under the whole grid (with a 1-block margin).
        for (int gx = -1; gx <= cols * SPACING; gx++) {
            for (int gz = -1; gz <= rows * SPACING; gz++) {
                level.setBlockAndUpdate(new BlockPos(ox + gx, fy, oz + gz), floor);
            }
        }
        // Floating block displays (2 air blocks below each → visible from all angles).
        for (int i = 0; i < blocks.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            level.setBlockAndUpdate(
                    new BlockPos(ox + col * SPACING, fy + FLOAT_ABOVE, oz + row * SPACING),
                    blocks.get(i).defaultBlockState());
        }

        // A small "connected structure": a 3x3 launch pad flanked by the machines, on its own floor pad.
        int sx = origin.getX() + 4;
        int sz = origin.getZ() - 6;
        for (int dx = -1; dx <= 5; dx++) {
            for (int dz = -1; dz <= 3; dz++) {
                level.setBlockAndUpdate(new BlockPos(sx + dx, fy, sz + dz), floor);
            }
        }
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                level.setBlockAndUpdate(new BlockPos(sx + dx, fy + 1, sz + dz),
                        ModBlocks.ROCKET_LAUNCH_PAD.get().defaultBlockState());
            }
        }
        level.setBlockAndUpdate(new BlockPos(sx + 4, fy + 1, sz), ModBlocks.FUEL_TANK.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(sx + 4, fy + 1, sz + 1), ModBlocks.OXYGEN_GENERATOR.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(sx + 4, fy + 1, sz + 2), ModBlocks.TERRAFORMER.get().defaultBlockState());

        // Creatures: each spawned twice — live (AI) and frozen (NoAI) — on a small floor strip.
        int mx = origin.getX() + 4;
        int mz = origin.getZ() - 12;
        for (int dx = -1; dx <= 4 * 4; dx++) {
            for (int dz = -1; dz <= 3; dz++) {
                level.setBlockAndUpdate(new BlockPos(mx + dx, fy, mz + dz), floor);
            }
        }
        List<EntityType<? extends Mob>> creatures = List.of(
                ModEntities.XERTZ_STALKER.get(), ModEntities.QUARTZ_CRAWLER.get(),
                ModEntities.GREENLING.get(), ModEntities.CINDER_STALKER.get());
        for (int i = 0; i < creatures.size(); i++) {
            spawnShowcase(level, creatures.get(i), new BlockPos(mx + i * 4, fy + 1, mz), false);     // AI
            spawnShowcase(level, creatures.get(i), new BlockPos(mx + i * 4, fy + 1, mz + 2), true);  // frozen
        }

        source.sendSuccess(() -> Component.literal("Built the Nerospace gallery: "
                + blocks.size() + " blocks, a structure cluster, and 4 creatures (AI + frozen)."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static void spawnShowcase(ServerLevel level, EntityType<? extends Mob> type, BlockPos pos, boolean noAi) {
        Mob mob = type.spawn(level, pos, EntitySpawnReason.COMMAND);
        if (mob != null) {
            mob.setNoAi(noAi);
            mob.setPersistenceRequired();
        }
    }
}

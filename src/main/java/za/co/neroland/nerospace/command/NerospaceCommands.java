package za.co.neroland.nerospace.command;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.Command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.pipe.PipeIoMode;
import za.co.neroland.nerospace.pipe.PipeResourceType;
import za.co.neroland.nerospace.pipe.UniversalPipeBlockEntity;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModEntities;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.storage.CreativeFluidTankBlockEntity;
import za.co.neroland.nerospace.storage.CreativeItemStoreBlockEntity;

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

        // A connected POWER-GRID demo: generators → Universal Pipe → a powered Grinder.
        int gx = origin.getX() + 4;
        int gz = origin.getZ() - 18;
        for (int dx = -1; dx <= 4; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlockAndUpdate(new BlockPos(gx + dx, fy, gz + dz), floor);
            }
        }
        level.setBlockAndUpdate(new BlockPos(gx, fy + 1, gz), ModBlocks.COMBUSTION_GENERATOR.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(gx + 1, fy + 1, gz), ModBlocks.UNIVERSAL_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(gx + 2, fy + 1, gz), ModBlocks.UNIVERSAL_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(gx + 3, fy + 1, gz), ModBlocks.NEROSIUM_GRINDER.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(gx + 1, fy + 1, gz + 1), ModBlocks.PASSIVE_GENERATOR.get().defaultBlockState());

        // FOUR LIVE PIPE SCENARIOS: creative source → 3 pipes → sink, one row per resource layer.
        // The source-touching face is set IN (pull-only — otherwise the pipe would void its buffer
        // back into the endless source) and the sink-touching face OUT, mirroring real Configurator use.
        int px = origin.getX() + 4;
        int pz = origin.getZ() - 24;
        Block[][] scenarioRows = {
                {ModBlocks.CREATIVE_BATTERY.get(), ModBlocks.BATTERY.get()},
                {ModBlocks.CREATIVE_FLUID_TANK.get(), ModBlocks.FLUID_TANK.get()},
                {ModBlocks.CREATIVE_GAS_TANK.get(), ModBlocks.GAS_TANK.get()},
                {ModBlocks.CREATIVE_ITEM_STORE.get(), ModBlocks.ITEM_STORE.get()},
        };
        for (int row = 0; row < scenarioRows.length; row++) {
            int rz = pz - row * 3;
            for (int dx = -1; dx <= 5; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    level.setBlockAndUpdate(new BlockPos(px + dx, fy, rz + dz), floor);
                }
            }
            level.setBlockAndUpdate(new BlockPos(px, fy + 1, rz), scenarioRows[row][0].defaultBlockState());
            for (int dx = 1; dx <= 3; dx++) {
                level.setBlockAndUpdate(new BlockPos(px + dx, fy + 1, rz),
                        ModBlocks.UNIVERSAL_PIPE.get().defaultBlockState());
            }
            level.setBlockAndUpdate(new BlockPos(px + 4, fy + 1, rz), scenarioRows[row][1].defaultBlockState());
            setAllModes(level, new BlockPos(px + 1, fy + 1, rz), Direction.WEST, PipeIoMode.IN);
            setAllModes(level, new BlockPos(px + 3, fy + 1, rz), Direction.EAST, PipeIoMode.OUT);
        }
        // Pre-configure the endless sources so the rows run on arrival.
        if (level.getBlockEntity(new BlockPos(px, fy + 1, pz - 3)) instanceof CreativeFluidTankBlockEntity tank) {
            tank.setSource(FluidResource.of(Fluids.WATER));
        }
        if (level.getBlockEntity(new BlockPos(px, fy + 1, pz - 9)) instanceof CreativeItemStoreBlockEntity store) {
            store.setSource(ItemResource.of(ModItems.NEROSIUM_INGOT.get()));
        }

        // Suit displays + a LOADED Star Guide pedestal (book installed → the hologram runs).
        int ax = origin.getX() + 4;
        int az = origin.getZ() - 36;
        for (int dx = -1; dx <= 6; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlockAndUpdate(new BlockPos(ax + dx, fy, az + dz), floor);
            }
        }
        spawnSuitStand(level, new BlockPos(ax, fy + 1, az), false,
                Component.literal("Oxygen Suit"));
        spawnSuitStand(level, new BlockPos(ax + 2, fy + 1, az), true,
                Component.literal("Tier 2 Oxygen Suit"));
        BlockPos guidePos = new BlockPos(ax + 5, fy + 1, az);
        level.setBlockAndUpdate(guidePos, ModBlocks.STAR_GUIDE.get().defaultBlockState());
        if (level.getBlockEntity(guidePos)
                instanceof za.co.neroland.nerospace.progression.StarGuideBlockEntity guide) {
            guide.installBook(new ItemStack(ModItems.STAR_GUIDE_BOOK.get()));
        }

        // Heavy Launch Complex demo: 5x5 pad + Launch Gantry + Fuel Tank, with a Tier 3 rocket
        // standing on it (the ring-free T3 path) — see LAUNCH_PAD_DESIGN.md.
        int hx = origin.getX() + 4;
        int hz = origin.getZ() - 46;
        for (int dx = -2; dx <= 6; dx++) {
            for (int dz = -2; dz <= 6; dz++) {
                level.setBlockAndUpdate(new BlockPos(hx + dx, fy, hz + dz), floor);
            }
        }
        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < 5; dz++) {
                level.setBlockAndUpdate(new BlockPos(hx + dx, fy + 1, hz + dz),
                        ModBlocks.ROCKET_LAUNCH_PAD.get().defaultBlockState());
            }
        }
        level.setBlockAndUpdate(new BlockPos(hx - 1, fy + 1, hz + 2),
                ModBlocks.LAUNCH_GANTRY.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(hx + 5, fy + 1, hz + 2),
                ModBlocks.FUEL_TANK.get().defaultBlockState());
        za.co.neroland.nerospace.rocket.RocketEntity heavyRocket =
                new za.co.neroland.nerospace.rocket.RocketEntity(level,
                        hx + 2.5D, fy + 1 + za.co.neroland.nerospace.rocket.RocketLaunchPadBlock.SURFACE_HEIGHT,
                        hz + 2.5D, za.co.neroland.nerospace.rocket.RocketTier.TIER_3);
        level.addFreshEntity(heavyRocket);

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
                + blocks.size() + " blocks, a structure cluster, a power-grid demo, 4 live pipe "
                + "scenarios (energy/fluid/gas/items), suit stands (T1 + T2), a loaded Star Guide "
                + "pedestal, a Heavy Launch Complex (5x5 + gantry + tank + T3 rocket), and "
                + "4 creatures (AI + frozen)."), false);
        return Command.SINGLE_SUCCESS;
    }

    /** An invulnerable, named armor stand wearing the full Tier 1 or Tier 2 Oxygen Suit. */
    private static void spawnSuitStand(ServerLevel level, BlockPos pos, boolean tier2, Component name) {
        ArmorStand stand = EntityType.ARMOR_STAND.spawn(level, pos, EntitySpawnReason.COMMAND);
        if (stand == null) {
            return;
        }
        stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(
                tier2 ? ModItems.OXYGEN_SUIT_T2_HELMET.get() : ModItems.OXYGEN_SUIT_HELMET.get()));
        stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(
                tier2 ? ModItems.OXYGEN_SUIT_T2_CHESTPLATE.get() : ModItems.OXYGEN_SUIT_CHESTPLATE.get()));
        stand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(
                tier2 ? ModItems.OXYGEN_SUIT_T2_LEGGINGS.get() : ModItems.OXYGEN_SUIT_LEGGINGS.get()));
        stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(
                tier2 ? ModItems.OXYGEN_SUIT_T2_BOOTS.get() : ModItems.OXYGEN_SUIT_BOOTS.get()));
        stand.setCustomName(name);
        stand.setCustomNameVisible(true);
        stand.setInvulnerable(true);
    }

    /** Set one face of the pipe at {@code pos} to {@code mode} for ALL four resource layers. */
    private static void setAllModes(ServerLevel level, BlockPos pos, Direction face, PipeIoMode mode) {
        if (level.getBlockEntity(pos) instanceof UniversalPipeBlockEntity pipe) {
            for (PipeResourceType type : PipeResourceType.VALUES) {
                pipe.setMode(face, type, mode);
            }
        }
    }

    private static void spawnShowcase(ServerLevel level, EntityType<? extends Mob> type, BlockPos pos, boolean noAi) {
        Mob mob = type.spawn(level, pos, EntitySpawnReason.COMMAND);
        if (mob != null) {
            mob.setNoAi(noAi);
            mob.setPersistenceRequired();
        }
    }
}

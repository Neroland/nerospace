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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import za.co.neroland.nerospace.Nerospace;
import za.co.neroland.nerospace.machine.CombustionGeneratorBlockEntity;
import za.co.neroland.nerospace.machine.FuelRefineryBlockEntity;
import za.co.neroland.nerospace.machine.HydrationModuleBlockEntity;
import za.co.neroland.nerospace.machine.NerosiumGrinderBlockEntity;
import za.co.neroland.nerospace.pipe.PipeIoMode;
import za.co.neroland.nerospace.pipe.PipeResourceType;
import za.co.neroland.nerospace.pipe.UniversalPipeBlockEntity;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModEntities;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.rocket.RocketEntity;
import za.co.neroland.nerospace.rocket.RocketLaunchPadBlock;
import za.co.neroland.nerospace.rocket.RocketTier;
import za.co.neroland.nerospace.storage.CreativeFluidTankBlockEntity;
import za.co.neroland.nerospace.storage.CreativeItemStoreBlockEntity;

/**
 * Creative-only debug commands (cheats / op level 2). {@code /nerospace gallery} builds a showcase
 * platform near the player: every Nerospace block floating two blocks above the floor (so all faces
 * are visible) on a ~3-block grid, every machine RUNNING the way it is meant to be wired in
 * survival (fuelled, powered and fed — except the Terraformer and Oxygen Generator, which sit
 * behind an off lever so the world-changing machines only run when deliberately switched on), all
 * four rocket tiers standing on their required pad formations, every suit variant on a stand, and
 * each creature spawned twice — once with AI and once frozen (NoAI) — for inspection.
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

        // MACHINES, ALL RUNNING (one strip, four wired clusters — each exactly the survival hookup):
        //   A. Combustion Generator (coal) → pipe → Grinder (raw nerosium), Passive Generator feeding in.
        //   B. Creative Battery → pipe → Fuel Refinery (coal + blaze powder) → pipe → Fuel Tank.
        //   C. Creative Battery → pipe → Oxygen Generator — parked behind an OFF lever.
        //   D. Creative Battery → pipe → Terraformer + touching Hydration Module (glacite) and
        //      Terraform Monitor — parked behind an OFF lever (it WILL reshape the area when on).
        int sx = origin.getX() + 4;
        int sz = origin.getZ() - 6;
        for (int dx = -1; dx <= 27; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlockAndUpdate(new BlockPos(sx + dx, fy, sz + dz), floor);
            }
        }
        BlockState lever = Blocks.LEVER.defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.FACING, Direction.EAST);

        // A: the classic first power line.
        level.setBlockAndUpdate(new BlockPos(sx, fy + 1, sz), ModBlocks.COMBUSTION_GENERATOR.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(sx + 1, fy + 1, sz), ModBlocks.UNIVERSAL_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(sx + 2, fy + 1, sz), ModBlocks.NEROSIUM_GRINDER.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(sx + 1, fy + 1, sz + 1), ModBlocks.PASSIVE_GENERATOR.get().defaultBlockState());
        if (level.getBlockEntity(new BlockPos(sx, fy + 1, sz)) instanceof CombustionGeneratorBlockEntity gen) {
            gen.setItem(CombustionGeneratorBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL, 64));
        }
        if (level.getBlockEntity(new BlockPos(sx + 2, fy + 1, sz)) instanceof NerosiumGrinderBlockEntity grinder) {
            grinder.setItem(NerosiumGrinderBlockEntity.INPUT_SLOT, new ItemStack(ModItems.RAW_NEROSIUM.get(), 64));
        }

        // B: refining line — power in from the endless battery, fuel out into a Fuel Tank.
        int bx = sx + 6;
        level.setBlockAndUpdate(new BlockPos(bx, fy + 1, sz), ModBlocks.CREATIVE_BATTERY.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(bx + 1, fy + 1, sz), ModBlocks.UNIVERSAL_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(bx + 2, fy + 1, sz), ModBlocks.FUEL_REFINERY.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(bx + 3, fy + 1, sz), ModBlocks.UNIVERSAL_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(bx + 4, fy + 1, sz), ModBlocks.FUEL_TANK.get().defaultBlockState());
        setAllModes(level, new BlockPos(bx + 1, fy + 1, sz), Direction.WEST, PipeIoMode.IN);
        setAllModes(level, new BlockPos(bx + 1, fy + 1, sz), Direction.EAST, PipeIoMode.OUT);
        setAllModes(level, new BlockPos(bx + 3, fy + 1, sz), Direction.WEST, PipeIoMode.IN);
        setAllModes(level, new BlockPos(bx + 3, fy + 1, sz), Direction.EAST, PipeIoMode.OUT);
        if (level.getBlockEntity(new BlockPos(bx + 2, fy + 1, sz)) instanceof FuelRefineryBlockEntity refinery) {
            refinery.setItem(FuelRefineryBlockEntity.CARBON_SLOT, new ItemStack(Items.COAL, 64));
            refinery.setItem(FuelRefineryBlockEntity.CATALYST_SLOT, new ItemStack(Items.BLAZE_POWDER, 64));
        }

        // C: oxygen generator behind its lever (off until flipped — then the bubble forms).
        int cx = sx + 13;
        level.setBlockAndUpdate(new BlockPos(cx, fy + 1, sz), ModBlocks.CREATIVE_BATTERY.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(cx + 1, fy + 1, sz), ModBlocks.UNIVERSAL_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(cx + 2, fy + 1, sz), ModBlocks.OXYGEN_GENERATOR.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(cx + 3, fy + 1, sz), lever);
        setAllModes(level, new BlockPos(cx + 1, fy + 1, sz), Direction.WEST, PipeIoMode.IN);
        setAllModes(level, new BlockPos(cx + 1, fy + 1, sz), Direction.EAST, PipeIoMode.OUT);

        // D: terraformer cluster behind its lever, with the full deeper-terraform support crew.
        int tx = sx + 19;
        level.setBlockAndUpdate(new BlockPos(tx, fy + 1, sz), ModBlocks.CREATIVE_BATTERY.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(tx + 1, fy + 1, sz), ModBlocks.UNIVERSAL_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(tx + 2, fy + 1, sz), ModBlocks.TERRAFORMER.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(tx + 2, fy + 1, sz + 1), ModBlocks.HYDRATION_MODULE.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(tx + 2, fy + 1, sz - 1), ModBlocks.TERRAFORM_MONITOR.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(tx + 3, fy + 1, sz), lever);
        setAllModes(level, new BlockPos(tx + 1, fy + 1, sz), Direction.WEST, PipeIoMode.IN);
        setAllModes(level, new BlockPos(tx + 1, fy + 1, sz), Direction.EAST, PipeIoMode.OUT);
        if (level.getBlockEntity(new BlockPos(tx + 2, fy + 1, sz + 1)) instanceof HydrationModuleBlockEntity module) {
            module.setItem(HydrationModuleBlockEntity.INPUT_SLOT, new ItemStack(ModItems.GLACITE.get(), 64));
        }

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

        // Suit displays (every variant) + a LOADED Star Guide pedestal (book installed → hologram runs).
        int ax = origin.getX() + 4;
        int az = origin.getZ() - 36;
        for (int dx = -1; dx <= 11; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlockAndUpdate(new BlockPos(ax + dx, fy, az + dz), floor);
            }
        }
        spawnSuitStand(level, new BlockPos(ax, fy + 1, az), Component.literal("Oxygen Suit"),
                ModItems.OXYGEN_SUIT_HELMET.get(), ModItems.OXYGEN_SUIT_CHESTPLATE.get(),
                ModItems.OXYGEN_SUIT_LEGGINGS.get(), ModItems.OXYGEN_SUIT_BOOTS.get());
        spawnSuitStand(level, new BlockPos(ax + 2, fy + 1, az), Component.literal("Tier 2 Oxygen Suit"),
                ModItems.OXYGEN_SUIT_T2_HELMET.get(), ModItems.OXYGEN_SUIT_T2_CHESTPLATE.get(),
                ModItems.OXYGEN_SUIT_T2_LEGGINGS.get(), ModItems.OXYGEN_SUIT_T2_BOOTS.get());
        spawnSuitStand(level, new BlockPos(ax + 4, fy + 1, az), Component.literal("Thermal Suit"),
                ModItems.OXYGEN_SUIT_HEAT_HELMET.get(), ModItems.OXYGEN_SUIT_HEAT_CHESTPLATE.get(),
                ModItems.OXYGEN_SUIT_HEAT_LEGGINGS.get(), ModItems.OXYGEN_SUIT_HEAT_BOOTS.get());
        spawnSuitStand(level, new BlockPos(ax + 6, fy + 1, az), Component.literal("Cryo Suit"),
                ModItems.OXYGEN_SUIT_COLD_HELMET.get(), ModItems.OXYGEN_SUIT_COLD_CHESTPLATE.get(),
                ModItems.OXYGEN_SUIT_COLD_LEGGINGS.get(), ModItems.OXYGEN_SUIT_COLD_BOOTS.get());
        BlockPos guidePos = new BlockPos(ax + 9, fy + 1, az);
        level.setBlockAndUpdate(guidePos, ModBlocks.STAR_GUIDE.get().defaultBlockState());
        if (level.getBlockEntity(guidePos)
                instanceof za.co.neroland.nerospace.progression.StarGuideBlockEntity guide) {
            guide.installBook(new ItemStack(ModItems.STAR_GUIDE_BOOK.get()));
        }

        // ROCKET ROW: every tier on the pad formation it actually requires (RocketItem gating):
        //   T1 + T2: a full 3x3 pad.  T3: a 3x3 pad ringed with Station Wall.
        //   T4: the Heavy Launch Complex — full 5x5 pad + a Launch Gantry on its border ring.
        int rx = origin.getX() + 4;
        int rz0 = origin.getZ() - 48;
        for (int dx = -2; dx <= 31; dx++) {
            for (int dz = -3; dz <= 5; dz++) {
                level.setBlockAndUpdate(new BlockPos(rx + dx, fy, rz0 + dz), floor);
            }
        }
        BlockState pad = ModBlocks.ROCKET_LAUNCH_PAD.get().defaultBlockState();
        // T1 (3x3 + the classic pad-side Fuel Tank).
        fillPad(level, new BlockPos(rx, fy + 1, rz0), 3, pad);
        level.setBlockAndUpdate(new BlockPos(rx + 3, fy + 1, rz0 + 1), ModBlocks.FUEL_TANK.get().defaultBlockState());
        spawnRocket(level, rx + 1, fy + 1, rz0 + 1, RocketTier.TIER_1);
        // T2 (3x3).
        fillPad(level, new BlockPos(rx + 8, fy + 1, rz0), 3, pad);
        spawnRocket(level, rx + 9, fy + 1, rz0 + 1, RocketTier.TIER_2);
        // T3 (3x3 ringed with Station Wall).
        fillPad(level, new BlockPos(rx + 16, fy + 1, rz0), 3, pad);
        BlockState wall = ModBlocks.STATION_WALL.get().defaultBlockState();
        for (int dx = -1; dx <= 3; dx++) {
            for (int dz = -1; dz <= 3; dz++) {
                if (dx == -1 || dx == 3 || dz == -1 || dz == 3) {
                    level.setBlockAndUpdate(new BlockPos(rx + 16 + dx, fy + 1, rz0 + dz), wall);
                }
            }
        }
        spawnRocket(level, rx + 17, fy + 1, rz0 + 1, RocketTier.TIER_3);
        // T4 (Heavy Launch Complex: 5x5 + gantry + fuel tank).
        fillPad(level, new BlockPos(rx + 24, fy + 1, rz0 - 1), 5, pad);
        level.setBlockAndUpdate(new BlockPos(rx + 23, fy + 1, rz0 + 1),
                ModBlocks.LAUNCH_GANTRY.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(rx + 29, fy + 1, rz0 + 1),
                ModBlocks.FUEL_TANK.get().defaultBlockState());
        spawnRocket(level, rx + 26, fy + 1, rz0 + 1, RocketTier.TIER_4);

        // Creatures: each spawned twice — live (AI) and frozen (NoAI) — on a small floor strip.
        int mx = origin.getX() + 4;
        int mz = origin.getZ() - 12;
        for (int dx = -1; dx <= 8 * 4; dx++) {
            for (int dz = -1; dz <= 3; dz++) {
                level.setBlockAndUpdate(new BlockPos(mx + dx, fy, mz + dz), floor);
            }
        }
        List<EntityType<? extends Mob>> creatures = List.of(
                ModEntities.XERTZ_STALKER.get(), ModEntities.QUARTZ_CRAWLER.get(),
                ModEntities.GREENLING.get(), ModEntities.CINDER_STALKER.get(),
                ModEntities.FROST_STRIDER.get(),
                // Terraform livestock (DEEPER_TERRAFORM_DESIGN.md §5).
                ModEntities.MEADOW_LOPER.get(), ModEntities.EMBER_STRUTTER.get(),
                ModEntities.WOOLLY_DRIFT.get());
        for (int i = 0; i < creatures.size(); i++) {
            spawnShowcase(level, creatures.get(i), new BlockPos(mx + i * 4, fy + 1, mz), false);     // AI
            spawnShowcase(level, creatures.get(i), new BlockPos(mx + i * 4, fy + 1, mz + 2), true);  // frozen
        }

        source.sendSuccess(() -> Component.literal("Built the Nerospace gallery: "
                + blocks.size() + " blocks, 4 RUNNING machine clusters (grinder line, fuel refinery "
                + "line, oxygen generator + lever, terraformer crew + lever — flip a lever to start "
                + "those two), 4 live pipe scenarios (energy/fluid/gas/items), all 4 suit variants, "
                + "a loaded Star Guide pedestal, all 4 rocket tiers on their required pads (3x3, "
                + "3x3, walled ring, Heavy Launch Complex), and 8 creatures (AI + frozen)."), false);
        return Command.SINGLE_SUCCESS;
    }

    /** A full {@code size x size} square of launch pads with min-corner {@code corner}. */
    private static void fillPad(ServerLevel level, BlockPos corner, int size, BlockState pad) {
        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                level.setBlockAndUpdate(corner.offset(dx, 0, dz), pad);
            }
        }
    }

    /** A rocket standing on the pad surface of the pad block at {@code (x, y, z)}. */
    private static void spawnRocket(ServerLevel level, int x, int y, int z, RocketTier tier) {
        level.addFreshEntity(new RocketEntity(level,
                x + 0.5D, y + RocketLaunchPadBlock.SURFACE_HEIGHT, z + 0.5D, tier));
    }

    /** An invulnerable, named armor stand wearing the given four-piece suit. */
    private static void spawnSuitStand(ServerLevel level, BlockPos pos, Component name,
            Item helmet, Item chestplate, Item leggings, Item boots) {
        ArmorStand stand = EntityType.ARMOR_STAND.spawn(level, pos, EntitySpawnReason.COMMAND);
        if (stand == null) {
            return;
        }
        stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(helmet));
        stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chestplate));
        stand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(leggings));
        stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(boots));
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

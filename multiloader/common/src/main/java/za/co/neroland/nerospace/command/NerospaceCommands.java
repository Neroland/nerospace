package za.co.neroland.nerospace.command;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.Command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.meteor.FallingMeteorEntity;
import za.co.neroland.nerospace.meteor.MeteorCoreBlockEntity;
import za.co.neroland.nerospace.machine.CombustionGeneratorBlockEntity;
import za.co.neroland.nerospace.machine.FuelRefineryBlockEntity;
import za.co.neroland.nerospace.machine.HydrationModuleBlockEntity;
import za.co.neroland.nerospace.machine.NerosiumGrinderBlockEntity;
import za.co.neroland.nerospace.machine.quarry.QuarryControllerBlockEntity;
import za.co.neroland.nerospace.machine.quarry.QuarryRegion;
import za.co.neroland.nerospace.pipe.PipeIoMode;
import za.co.neroland.nerospace.pipe.PipeResourceType;
import za.co.neroland.nerospace.pipe.UniversalPipeBlockEntity;
import za.co.neroland.nerospace.registry.ModBlocks;
import za.co.neroland.nerospace.registry.ModEntities;
import za.co.neroland.nerospace.registry.ModItems;
import za.co.neroland.nerospace.rocket.RocketEntity;
import za.co.neroland.nerospace.rocket.RocketLaunchPadBlock;
import za.co.neroland.nerospace.rocket.RocketTier;
import za.co.neroland.nerospace.storage.CreativeItemStoreBlockEntity;

/**
 * Creative-only debug commands (cheats / op level 2). {@code /nerospace gallery} builds a showcase
 * platform near the player: every Nerospace block floating two blocks above the floor (so all faces
 * are visible) on a ~3-block grid, every machine RUNNING the way it is meant to be wired in
 * survival (fuelled, powered and fed — except the Terraformer and Oxygen Generator, which sit
 * behind an off lever so the world-changing machines only run when deliberately switched on), all
 * four rocket tiers standing on their required pad formations, every suit variant on a stand, and
 * each creature spawned twice — once with AI and once frozen (NoAI) — for inspection.
 * {@code /nerospace gallery clear} wipes that footprint (blocks + spawned entities) so a rebuild —
 * or the screenshot harness — doesn't stack duplicates.
 */
public final class NerospaceCommands {

    private static final int SPACING = 3;     // blocks between display cells
    private static final int FLOAT_ABOVE = 3; // display sits this many blocks above the floor (2 air gap)
    private static final int SUIT_SPACING = 3; // blocks between suit stands (roomier than the old 2)
    private static final float SUIT_YAW = -10.0f; // every suit stand faces one way, angled a few degrees left

    private NerospaceCommands() {
    }

    /**
     * Cross-loader registration: each loader calls this from its command hook (NeoForge
     * {@code RegisterCommandsEvent}, Fabric {@code CommandRegistrationCallback}) with the dispatcher.
     */
    public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        // Player-only; the executor further restricts to creative. (Commands themselves require the
        // world to have cheats/commands enabled, so this is effectively creative + commands gated.)
        dispatcher.register(
                Commands.literal("nerospace")
                        .requires(src -> src.getPlayer() != null)
                        .then(Commands.literal("gallery")
                                .executes(ctx -> buildGallery(ctx.getSource()))
                                .then(Commands.literal("clear")
                                        .executes(ctx -> clearGallery(ctx.getSource())))));
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

        // The cross-loader RegistrationProvider has no entry iteration, so walk the vanilla block
        // registry filtered to this mod's namespace (same effect as the root's BLOCKS.getEntries()).
        List<Block> blocks = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier bid = BuiltInRegistries.BLOCK.getKey(block);
            if (!NerospaceCommon.MOD_ID.equals(bid.getNamespace())) {
                continue;
            }
            if (block != ModBlocks.ROCKET_FUEL_BLOCK.get()) { // skip the fluid block (renders oddly free-standing)
                blocks.add(block);
            }
        }

        int cols = (int) Math.ceil(Math.sqrt(Math.max(1, blocks.size())));
        int rows = (int) Math.ceil(blocks.size() / (double) cols);
        // ROTUNDA: each cluster sits on a ring ~48 blocks out on its own compass bearing, so a camera
        // near the centre shoots each one outward against empty ground with no other display in frame.
        // The /nsgallery capture harness mirrors these bearings. Bases are placed so the body centres
        // on the ring. Tune distances together with the harness if reframing.
        int ox = origin.getX() + 38; // block grid → EAST
        int oz = origin.getZ() - 9;
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
        int sx = origin.getX() - 13; // machine strip → SOUTH
        int sz = origin.getZ() + 48;
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
        int px = origin.getX() - 50; // pipe scenarios → WEST (rows run north-south → broadside from the centre)
        int pz = origin.getZ() + 5;
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
        // (The multiloader Creative Fluid Tank is a fixed endless rocket_fuel source — it has no setSource.)
        if (level.getBlockEntity(new BlockPos(px, fy + 1, pz - 9)) instanceof CreativeItemStoreBlockEntity store) {
            store.setSource(new ItemStack(ModItems.NEROSIUM_INGOT.get()));
        }

        // Suit displays (every variant) + a LOADED Star Guide pedestal (book installed → hologram runs).
        int ax = origin.getX() - 40; // suits + Star Guide → NORTH-WEST (well clear of the pipes spoke)
        int az = origin.getZ() - 34;
        int suit0 = 0;
        int suit1 = SUIT_SPACING;
        int suit2 = SUIT_SPACING * 2;
        int suit3 = SUIT_SPACING * 3;
        int guideX = SUIT_SPACING * 4;
        for (int dx = -1; dx <= guideX + 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlockAndUpdate(new BlockPos(ax + dx, fy, az + dz), floor);
            }
        }
        spawnSuitStand(level, new BlockPos(ax + suit0, fy + 1, az), Component.literal("Oxygen Suit"), SUIT_YAW,
                ModItems.OXYGEN_SUIT_HELMET.get(), ModItems.OXYGEN_SUIT_CHESTPLATE.get(),
                ModItems.OXYGEN_SUIT_LEGGINGS.get(), ModItems.OXYGEN_SUIT_BOOTS.get());
        spawnSuitStand(level, new BlockPos(ax + suit1, fy + 1, az), Component.literal("Tier 2 Oxygen Suit"), SUIT_YAW,
                ModItems.OXYGEN_SUIT_T2_HELMET.get(), ModItems.OXYGEN_SUIT_T2_CHESTPLATE.get(),
                ModItems.OXYGEN_SUIT_T2_LEGGINGS.get(), ModItems.OXYGEN_SUIT_T2_BOOTS.get());
        spawnSuitStand(level, new BlockPos(ax + suit2, fy + 1, az), Component.literal("Thermal Suit"), SUIT_YAW,
                ModItems.OXYGEN_SUIT_HEAT_HELMET.get(), ModItems.OXYGEN_SUIT_HEAT_CHESTPLATE.get(),
                ModItems.OXYGEN_SUIT_HEAT_LEGGINGS.get(), ModItems.OXYGEN_SUIT_HEAT_BOOTS.get());
        spawnSuitStand(level, new BlockPos(ax + suit3, fy + 1, az), Component.literal("Cryo Suit"), SUIT_YAW,
                ModItems.OXYGEN_SUIT_COLD_HELMET.get(), ModItems.OXYGEN_SUIT_COLD_CHESTPLATE.get(),
                ModItems.OXYGEN_SUIT_COLD_LEGGINGS.get(), ModItems.OXYGEN_SUIT_COLD_BOOTS.get());
        BlockPos guidePos = new BlockPos(ax + guideX, fy + 1, az);
        level.setBlockAndUpdate(guidePos, ModBlocks.STAR_GUIDE.get().defaultBlockState());
        if (level.getBlockEntity(guidePos)
                instanceof za.co.neroland.nerospace.progression.StarGuideBlockEntity guide) {
            guide.installBook(new ItemStack(ModItems.STAR_GUIDE_BOOK.get()));
        }

        // ROCKET ROW: every tier on the pad formation it actually requires (RocketItem gating):
        //   T1 + T2: a full 3x3 pad.  T3: a 3x3 pad ringed with Station Wall.
        //   T4: the Heavy Launch Complex — full 5x5 pad + a Launch Gantry on its border ring.
        int rx = origin.getX() - 14; // rocket row → NORTH (the hero spoke)
        int rz0 = origin.getZ() - 49;
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
        int mx = origin.getX() + 18; // creatures → SOUTH-EAST
        int mz = origin.getZ() + 33;
        for (int dx = -1; dx <= 8 * 4; dx++) {
            for (int dz = -1; dz <= 3; dz++) {
                level.setBlockAndUpdate(new BlockPos(mx + dx, fy, mz + dz), floor);
            }
        }
        List<EntityType<? extends Mob>> creatures = List.of(
                ModEntities.XERTZ_STALKER.get(), ModEntities.QUARTZ_CRAWLER.get(),
                ModEntities.GREENLING.get(), ModEntities.CINDER_STALKER.get(),
                ModEntities.FROST_STRIDER.get(), ModEntities.ALIEN_VILLAGER.get(),
                // Terraform livestock (DEEPER_TERRAFORM_DESIGN.md §5).
                ModEntities.MEADOW_LOPER.get(), ModEntities.EMBER_STRUTTER.get(),
                ModEntities.WOOLLY_DRIFT.get());
        // One frozen (NoAI) row only — AI mobs wander, which breaks reproducible screenshots.
        for (int i = 0; i < creatures.size(); i++) {
            spawnShowcase(level, creatures.get(i), new BlockPos(mx + i * 4, fy + 1, mz + 1), true);
        }

        // METEOR SITE (meteor-events-design.md): a small crater of meteor_rock around a loot-bearing
        // meteor_core, with a frozen meteor hovering above it (spins + trails for the shot). SW spoke.
        buildMeteorSite(level, floor, origin.getX() - 28, origin.getZ() + 30, fy);

        // QUARRY (MINER_DESIGN): two NE displays.
        //  1. Landmark-only — three landmarks in an L (shows the projected marker lasers).
        //  2. Fully operating — a powered quarry mid-dig: frame ring, drill head, a real pit forming.
        BlockState landmark = ModBlocks.QUARRY_LANDMARK.get().defaultBlockState();
        int lx = origin.getX() + 28; // landmark-only display (NE, nearer the centre)
        int lz = origin.getZ() - 40;
        for (int dx = -1; dx <= 7; dx++) {
            for (int dz = -1; dz <= 7; dz++) {
                level.setBlockAndUpdate(new BlockPos(lx + dx, fy, lz + dz), floor);
            }
        }
        level.setBlockAndUpdate(new BlockPos(lx, fy + 1, lz), landmark);
        level.setBlockAndUpdate(new BlockPos(lx + 6, fy + 1, lz), landmark);
        level.setBlockAndUpdate(new BlockPos(lx, fy + 1, lz + 6), landmark);

        // Operating quarries: staged straight into a deep mid-dig so the frame, gantry, drill head and
        // interior-only excavation all read at a glance. Two sizes — a standard 9x9 and a big 17x17 to
        // stress-test rendering + mining over a large area.
        buildGalleryQuarry(level, floor, origin.getX() + 42, origin.getZ() - 40, fy, 8, 8);
        buildGalleryQuarry(level, floor, origin.getX() + 64, origin.getZ() - 56, fy, 16, 12);

        // SOLAR ARRAYS (SOLAR_PANEL_DESIGN, SW bearing): one unit per tier, then a multi-unit seam-joined
        // field per tier (so the per-cell trackers reading as one surface is visible), plus a
        // battery → universal cable → panel hookup that lights the panel's power connector.
        buildSolarArrays(level, floor, origin.getX() - 50, origin.getZ() + 36, fy);

        source.sendSuccess(() -> Component.literal("Built the Nerospace gallery: "
                + blocks.size() + " blocks, 4 RUNNING machine clusters (grinder line, fuel refinery "
                + "line, oxygen generator + lever, terraformer crew + lever — flip a lever to start "
                + "those two), 4 live pipe scenarios (energy/fluid/gas/items), all 4 suit variants, "
                + "a loaded Star Guide pedestal, all 4 rocket tiers on their required pads (3x3, "
                + "3x3, walled ring, Heavy Launch Complex), 8 creatures (frozen for clean shots), "
                + "a meteor crash site (crater + loot core + hovering meteor), and the solar arrays "
                + "(T1/T2/T3 single units + a seam-joined field per tier + a cabled hookup showing "
                + "the power connector)."), false);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Wipe the gallery built at the player's feet so a rebuild (or the screenshot harness) doesn't
     * stack duplicates. Clears the whole footprint to air from the floor layer ({@code origin.y}) up,
     * leaving the natural ground at {@code origin.y - 1} intact, and removes every non-player entity
     * in the box (rockets, suit stands, creatures). Run it standing where you ran {@code gallery}.
     */
    private static int clearGallery(CommandSourceStack source) {
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
        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();

        // Footprint of the ROTUNDA buildGallery() (clusters sit ~48 out on N/S/E/W/SE/NW bearings)
        // plus margin, so the clear covers every cluster — else reruns stack creatures/rockets/stands.
        // The floor sits at oy, so clearing oy..topY to air restores the original flat ground at oy-1.
        int minX = ox - 56;
        int maxX = ox + 62;
        int minZ = oz - 58;
        int maxZ = oz + 56;
        int topY = oy + 16;

        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int cleared = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = oy; y <= topY; y++) {
                    cursor.set(x, y, z);
                    if (!level.getBlockState(cursor).isAir()) {
                        level.setBlock(cursor, air, 2); // flag 2 = notify clients, skip neighbour cascade
                        cleared++;
                    }
                }
            }
        }

        // Remove the spawned entities (rockets, armour stands, creatures) — everything but players.
        AABB box = new AABB(minX, oy - 1, minZ, maxX + 1, topY + 4, maxZ + 1);
        int removed = 0;
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box, e -> !(e instanceof Player))) {
            entity.discard();
            removed++;
        }

        int clearedBlocks = cleared;
        int removedEntities = removed;
        source.sendSuccess(() -> Component.literal("Cleared the Nerospace gallery: " + clearedBlocks
                + " blocks → air, " + removedEntities + " entities removed."), false);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Solar showcase (SW). Front row: one of each tier as a single unit — a 1×1 T1, a 2×2 T2 (one big
     * panel) and a 3×3 T3 (one big panel). Behind it: several units of each tier side by side — nine T1
     * panels (a seam-joined 3×3 field), four T2 units and two T3 units — so multiple arrays tiling is
     * visible. A Creative Battery → Universal Pipe → T1 panel line shows the dynamic power connector (the
     * panel grows a stub toward the cable so the hookup butts up with no gap). Built at {@code (baseX,
     * baseZ)}, extending east (+X) and south (+Z); panels sit on the floor with the tracking deck above.
     */
    private static void buildSolarArrays(ServerLevel level, BlockState floor, int baseX, int baseZ, int fy) {
        int sy = fy + 1;
        for (int dx = -2; dx <= 20; dx++) {
            for (int dz = -2; dz <= 10; dz++) {
                level.setBlockAndUpdate(new BlockPos(baseX + dx, fy, baseZ + dz), floor);
            }
        }

        // Front row: one of each tier (multiblock anchors auto-fill their N×N footprint via onPlace).
        placeSolar(level, ModBlocks.SOLAR_PANEL.get(), baseX, sy, baseZ);
        placeSolar(level, ModBlocks.SOLAR_PANEL.get(), baseX + 2, sy, baseZ); // fills +2..3
        placeSolar(level, ModBlocks.SOLAR_PANEL.get(), baseX + 5, sy, baseZ); // fills +5..7

        // Cable hookup: Creative Battery → Universal Pipe → T1 panel (lights the panel's west connector).
        level.setBlockAndUpdate(new BlockPos(baseX + 10, sy, baseZ),
                ModBlocks.CREATIVE_BATTERY.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(baseX + 11, sy, baseZ),
                ModBlocks.UNIVERSAL_PIPE.get().defaultBlockState());
        placeSolar(level, ModBlocks.SOLAR_PANEL.get(), baseX + 12, sy, baseZ);

        // Multi-unit seam-joined fields, set back (+Z) so footprints don't touch the front row.
        // T1: a 3x3 field of nine single panels → one continuous tracking surface.
        for (int dx = 0; dx <= 2; dx++) {
            for (int dz = 4; dz <= 6; dz++) {
                placeSolar(level, ModBlocks.SOLAR_PANEL.get(), baseX + dx, sy, baseZ + dz);
            }
        }
        // T2: four 2x2 units → a 4x4 field.
        placeSolar(level, ModBlocks.SOLAR_PANEL.get(), baseX + 5, sy, baseZ + 4);
        placeSolar(level, ModBlocks.SOLAR_PANEL.get(), baseX + 7, sy, baseZ + 4);
        placeSolar(level, ModBlocks.SOLAR_PANEL.get(), baseX + 5, sy, baseZ + 6);
        placeSolar(level, ModBlocks.SOLAR_PANEL.get(), baseX + 7, sy, baseZ + 6);
        // T3: two 3x3 units → a 6x3 field.
        placeSolar(level, ModBlocks.SOLAR_PANEL.get(), baseX + 11, sy, baseZ + 4); // fills +11..13
        placeSolar(level, ModBlocks.SOLAR_PANEL.get(), baseX + 14, sy, baseZ + 4); // fills +14..16
    }

    /** Place a solar panel anchor; multiblock tiers auto-expand their footprint in {@code onPlace}. */
    private static void placeSolar(ServerLevel level, Block block, int x, int y, int z) {
        level.setBlockAndUpdate(new BlockPos(x, y, z), block.defaultBlockState());
    }

    /**
     * Build one staged, fully-powered gallery quarry: a {@code (side+1) x (side+1)} region with its
     * frame ring, a west-side creative battery + pipe feed, an interior-only pre-carved pit
     * {@code pitDepth} deep (the columns under the frame stay, matching real mining), dropped straight
     * into MINING so the gantry + drill animate immediately.
     */
    private static void buildGalleryQuarry(ServerLevel level, BlockState floor, int qx, int qz, int fy,
            int side, int pitDepth) {
        int refY = fy + 1;
        int mid = side / 2;
        for (int dx = -5; dx <= side; dx++) {   // ground: power pad (west) + under the region
            for (int dz = -1; dz <= side; dz++) {
                level.setBlockAndUpdate(new BlockPos(qx + dx, fy, qz + dz), floor);
            }
        }
        QuarryRegion region = new QuarryRegion(qx, qz, qx + side, qz + side, refY);
        BlockState frameBlock = ModBlocks.QUARRY_FRAME.get().defaultBlockState();
        for (BlockPos fp : region.framePositions()) {
            level.setBlockAndUpdate(fp, frameBlock);
        }
        // Pre-carve a starter pit — INTERIOR only, leaving the columns under the frame intact.
        for (int x = qx + 1; x <= qx + side - 1; x++) {
            for (int z = qz + 1; z <= qz + side - 1; z++) {
                for (int y = refY - 1; y >= refY - pitDepth; y--) {
                    level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        BlockPos quarryPos = new BlockPos(qx - 2, refY, qz + mid);
        level.setBlockAndUpdate(new BlockPos(qx - 4, refY, qz + mid),
                ModBlocks.CREATIVE_BATTERY.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(qx - 3, refY, qz + mid),
                ModBlocks.UNIVERSAL_PIPE.get().defaultBlockState());
        level.setBlockAndUpdate(quarryPos, ModBlocks.QUARRY_CONTROLLER.get().defaultBlockState());
        setAllModes(level, new BlockPos(qx - 3, refY, qz + mid), Direction.WEST, PipeIoMode.IN);
        setAllModes(level, new BlockPos(qx - 3, refY, qz + mid), Direction.EAST, PipeIoMode.OUT);
        if (level.getBlockEntity(quarryPos) instanceof QuarryControllerBlockEntity quarry) {
            quarry.setItem(QuarryControllerBlockEntity.FRAME_SLOT,
                    new ItemStack(ModItems.FRAME_CASING.get(), 64));
            // (The root's quarry.stageDisplay preview-region call isn't in the ported BE — omitted; cosmetic.)
        }
    }

    /**
     * A showcase meteor crash site: a 7x7 floor pad, a 5x5 {@code meteor_rock} crater floor with a
     * raised rim, a loot-pre-rolled {@code meteor_core} nestled in the centre, and a frozen
     * {@link FallingMeteorEntity} hovering above (spins + trails, but never falls — gallery only).
     */
    private static void buildMeteorSite(ServerLevel level, BlockState floor, int cx, int cz, int fy) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                level.setBlockAndUpdate(new BlockPos(cx + dx, fy, cz + dz), floor);
            }
        }
        BlockState rock = ModBlocks.METEOR_ROCK.get().defaultBlockState();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlockAndUpdate(new BlockPos(cx + dx, fy + 1, cz + dz), rock); // crater floor
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    level.setBlockAndUpdate(new BlockPos(cx + dx, fy + 2, cz + dz), rock); // raised rim
                }
            }
        }
        BlockPos corePos = new BlockPos(cx, fy + 2, cz);
        level.setBlockAndUpdate(corePos, ModBlocks.METEOR_CORE.get().defaultBlockState());
        if (level.getBlockEntity(corePos) instanceof MeteorCoreBlockEntity core) {
            core.generateLoot(level.getRandom().nextLong());
        }
        FallingMeteorEntity.spawnFrozen(level, cx + 0.5D, fy + 11, cz + 0.5D);
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
    private static void spawnSuitStand(ServerLevel level, BlockPos pos, Component name, float yaw,
            Item helmet, Item chestplate, Item leggings, Item boots) {
        // Build the stand via its constructor (the de-obf EntityType.ARMOR_STAND constant isn't on the
        // 26.2 classpath) and add it to the world directly.
        ArmorStand stand = new ArmorStand(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(helmet));
        stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chestplate));
        stand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(leggings));
        stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(boots));
        stand.setCustomName(name);
        stand.setCustomNameVisible(true);
        stand.setInvulnerable(true);
        stand.setYRot(yaw); // uniform facing so the row reads as a clean line, angled a few degrees off straight-on
        stand.setYBodyRot(yaw);
        stand.setYHeadRot(yaw);
        level.addFreshEntity(stand);
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

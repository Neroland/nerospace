package za.co.neroland.nerospace.world;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import za.co.neroland.nerospace.Config;
import za.co.neroland.nerospace.Nerospace;

/**
 * Cosmetic drift (DEEPER_TERRAFORM_DESIGN.md §2.3) — the passive lane of the hybrid stage engine:
 * settled terraformed land keeps sprouting sparse ground cover even while the machine idles. Pure
 * garnish on a hard budget ({@code terraformDriftPerSecond}, loaded-and-near-players only); it
 * carries no gameplay and switches off cleanly via {@code terraformDriftEnabled}.
 */
@EventBusSubscriber(modid = Nerospace.MODID)
public final class TerraformDrift {

    /** How close a player must be for a drift placement to bother happening. */
    private static final double PLAYER_RANGE = 64.0D;

    private TerraformDrift() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.getGameTime() % 20 != 0
                || !Config.TERRAFORM_DRIFT_ENABLED.get()
                || level.players().isEmpty()) {
            return;
        }
        int budget = Config.TERRAFORM_DRIFT_PER_SECOND.get();
        if (budget <= 0) {
            return;
        }

        List<long[]> machines = new ArrayList<>();
        TerraformManager.get(level).forEachMachine((center, r1, r2, r3) -> {
            if (r1 > 0) {
                machines.add(new long[] {center.asLong(), r1, r3});
            }
        });
        if (machines.isEmpty()) {
            return;
        }

        RandomSource rnd = level.getRandom();
        for (int i = 0; i < budget; i++) {
            long[] machine = machines.get(rnd.nextInt(machines.size()));
            driftOnce(level, BlockPos.of(machine[0]), (int) machine[1], (int) machine[2], rnd);
        }
    }

    /** One budgeted placement attempt at a random point inside the machine's rooted disc. */
    private static void driftOnce(ServerLevel level, BlockPos center, int rootedRadius,
            int lifeRadius, RandomSource rnd) {
        double angle = rnd.nextDouble() * Math.PI * 2.0D;
        double dist = Math.sqrt(rnd.nextDouble()) * rootedRadius; // area-uniform
        int x = center.getX() + (int) Math.round(Math.cos(angle) * dist);
        int z = center.getZ() + (int) Math.round(Math.sin(angle) * dist);
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return;
        }
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        BlockPos above = new BlockPos(x, surfaceY, z);
        if (!level.hasNearbyAlivePlayer(x + 0.5D, surfaceY, z + 0.5D, PLAYER_RANGE)) {
            return;
        }
        BlockPos ground = above.below();
        if (!level.getBlockState(ground).is(Blocks.GRASS_BLOCK) || !level.getBlockState(above).isAir()) {
            return;
        }
        // Mostly grass tufts, sometimes a flower; on Living ground the rare extra sapling (§2.3).
        double roll = rnd.nextDouble();
        long dx = x - center.getX();
        long dz = z - center.getZ();
        boolean living = lifeRadius > 0 && dx * dx + dz * dz <= (long) lifeRadius * lifeRadius;
        Block plant;
        if (living && roll < 0.04D) {
            plant = Blocks.OAK_SAPLING;
        } else if (roll < 0.25D) {
            plant = rnd.nextBoolean() ? Blocks.POPPY : Blocks.DANDELION;
        } else {
            plant = Blocks.SHORT_GRASS;
        }
        level.setBlock(above, plant.defaultBlockState(), Block.UPDATE_CLIENTS);
    }
}

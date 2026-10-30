package za.co.neroland.nerospace.block;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.ItemMagnetRules;

/**
 * Magnetic ore (Hematite): broken with an iron-or-better tool, its drops are pulled toward the breaker
 * (≤ 3 blocks, 0.2 b/t) for 20 ticks. The breaker is held only as a live object in a weak map for those ticks —
 * never persisted, never logged. {@link #tickMagnets} runs from the server tick ({@code PlanetTicks}).
 */
public class MagneticOreBlock extends Block {

    public static final MapCodec<MagneticOreBlock> CODEC = BlockCodecs.simple(MagneticOreBlock::new);

    /** Item entity → breaker, for the item's first {@link ItemMagnetRules#TICKS} ticks. */
    private static final Map<ItemEntity, Player> PULLS = Collections.synchronizedMap(new WeakHashMap<>());
    /**
     * Breaker per position between playerWillDestroy and spawnAfterBreak (same tick), held weakly so a break that never
     * reaches spawnAfterBreak (creative, cancelled) cannot pin a player object; cleared on use and every tick.
     */
    private static final Map<Level, Map<BlockPos, WeakReference<Player>>> BREAKING =
            Collections.synchronizedMap(new WeakHashMap<>());

    public MagneticOreBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends MagneticOreBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            ItemStack tool = player.getMainHandItem();
            // Iron tier or better = can harvest diamond ore.
            if (tool.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState())) {
                BREAKING.computeIfAbsent(level, l -> new java.util.HashMap<>())
                        .put(pos.immutable(), new WeakReference<>(player));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropXp) {
        super.spawnAfterBreak(state, level, pos, tool, dropXp);
        Map<BlockPos, WeakReference<Player>> pending = BREAKING.get(level);
        WeakReference<Player> ref = pending == null ? null : pending.remove(pos);
        Player breaker = ref == null ? null : ref.get();
        if (breaker == null) {
            return;
        }
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.0),
                e -> e.getAge() <= 1)) {
            PULLS.put(item, breaker);
        }
    }

    /** Per server tick: nudge each tracked drop toward its breaker; forget it after 20 ticks or when out of range. */
    public static void tickMagnets() {
        if (!BREAKING.isEmpty()) {
            BREAKING.clear();
        }
        if (PULLS.isEmpty()) {
            return;
        }
        synchronized (PULLS) {
            PULLS.entrySet().removeIf(e -> {
                ItemEntity item = e.getKey();
                Player to = e.getValue();
                if (!item.isAlive() || !to.isAlive() || item.level() != to.level()
                        || !ItemMagnetRules.active(true, item.getAge())) {
                    return true;
                }
                Vec3 d = to.position().add(0.0, 0.5, 0.0).subtract(item.position());
                double[] v = ItemMagnetRules.pull(d.x, d.y, d.z, ItemMagnetRules.RANGE, ItemMagnetRules.SPEED);
                if (v != null) {
                    item.setDeltaMovement(v[0], v[1], v[2]);
                }
                return false;
            });
        }
    }
}

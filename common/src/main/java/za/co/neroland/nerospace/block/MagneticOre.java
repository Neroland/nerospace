package za.co.neroland.nerospace.block;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerolandcore.ore.CoreOres;
import za.co.neroland.nerolandcore.ore.OreBehaviour;
import za.co.neroland.nerolandcore.ore.OreBehaviours;
import za.co.neroland.nerospace.block.rules.ItemMagnetRules;

/**
 * Magnetic ore (Hematite, now {@code nerolandcore:hematite_ore}): broken with an iron-or-better tool, its drops are
 * pulled toward the breaker (≤ 3 blocks, 0.2 b/t) for 20 ticks. Attached to Core's ore block through
 * {@link OreBehaviours}, so the pull only happens while Nerospace is installed. The breaker is held only as a live
 * object in a weak map for those ticks — never persisted, never logged. {@link #tickMagnets} runs from the server
 * tick ({@code BlockTicks}).
 */
public final class MagneticOre implements OreBehaviour {

    /** Item entity → breaker, for the item's first {@link ItemMagnetRules#TICKS} ticks. */
    private static final Map<ItemEntity, Player> PULLS = Collections.synchronizedMap(new WeakHashMap<>());

    private MagneticOre() {
    }

    /** Attach the magnet to Core's Hematite Ore. Called once from {@code NerospaceCommon.init}. */
    public static void register() {
        OreBehaviours.register(CoreOres.HEMATITE_ORE.id(), new MagneticOre());
    }

    @Override
    public void afterPlayerBreak(Level level, Player player, BlockPos pos, BlockState state, ItemStack tool) {
        // Iron tier or better = can harvest diamond ore.
        if (!tool.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState())) {
            return;
        }
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.0),
                e -> e.getAge() <= 1)) {
            PULLS.put(item, player);
        }
    }

    /** Per server tick: nudge each tracked drop toward its breaker; forget it after 20 ticks or when out of range. */
    public static void tickMagnets() {
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

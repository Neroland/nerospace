package za.co.neroland.nerospace.meteor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * The "box in the middle" of a meteor (meteor-events design §5): stores the loot rolled once when
 * the meteor lands, so contents are fixed per meteor (no re-roll exploit) and identical for every
 * player who reaches it. v1 is break-to-loot — {@link MeteorCoreBlock} spills these stacks when the
 * core is broken; a clickable container GUI is a noted follow-up.
 *
 * <p>Cross-loader port note: the meteor config keys are not yet ported, so the bonus-roll count is
 * inlined to the root's shipped default (3). The full config seam is a deferred batch.</p>
 */
public class MeteorCoreBlockEntity extends BlockEntity {

    /** Inlined from {@code Config.METEOR_LOOT_BONUS_ROLLS} (root default) until the config seam lands. */
    private static final int METEOR_LOOT_BONUS_ROLLS = 3;

    private final List<ItemStack> loot = new ArrayList<>();

    public MeteorCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.METEOR_CORE.get(), pos, state);
    }

    /** Rolls and stores loot from {@code seed} (idempotent — only the first non-empty roll sticks). */
    public void generateLoot(long seed) {
        if (!this.loot.isEmpty()) {
            return;
        }
        this.loot.addAll(MeteorLoot.roll(RandomSource.create(seed), METEOR_LOOT_BONUS_ROLLS));
        setChanged();
    }

    /**
     * Break-to-loot: spill the stored stacks the moment the core is removed (mirrors the Station
     * Core's charter pop). The block has no loot table, so this is the only drop path — the rolled
     * contents survive rather than a fresh roll.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (ItemStack stack : this.loot) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(serverLevel,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy());
            }
        }
        if (!this.loot.isEmpty()) {
            serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 0.7F);
        }
        this.loot.clear();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Loot", ItemStack.OPTIONAL_CODEC.listOf(), this.loot);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.loot.clear();
        this.loot.addAll(input.read("Loot", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of()));
    }
}

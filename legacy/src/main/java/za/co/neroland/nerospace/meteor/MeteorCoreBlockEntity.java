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

import za.co.neroland.nerospace.Config;
import za.co.neroland.nerospace.registry.ModBlockEntities;

/**
 * The "box in the middle" of a meteor (meteor-events design §5): stores the loot rolled once when
 * the meteor lands, so contents are fixed per meteor (no re-roll exploit) and identical for every
 * player who reaches it. v1 is break-to-loot — {@link MeteorCoreBlock} spills these stacks when the
 * core is broken; a clickable container GUI is a noted follow-up.
 */
public class MeteorCoreBlockEntity extends BlockEntity {

    private final List<ItemStack> loot = new ArrayList<>();

    public MeteorCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.METEOR_CORE.get(), pos, state);
    }

    /** Rolls and stores loot from {@code seed} (idempotent — only the first non-empty roll sticks). */
    public void generateLoot(long seed) {
        if (!this.loot.isEmpty()) {
            return;
        }
        this.loot.addAll(MeteorLoot.roll(RandomSource.create(seed), Config.METEOR_LOOT_BONUS_ROLLS.get()));
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

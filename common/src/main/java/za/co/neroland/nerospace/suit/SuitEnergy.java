package za.co.neroland.nerospace.suit;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

import za.co.neroland.nerolandcore.energy.ItemEnergyLookup;
import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;
import za.co.neroland.nerospace.config.NerospaceConfig;

/** The chest piece as a Core energy item. Returns null for every other stack. */
public final class SuitEnergy {

    private SuitEnergy() {
    }

    public static void init() {
        ItemEnergyLookup.register(stack -> {
            if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof SuitPieceItem piece)
                    || piece.piece() != SuitPiece.CHEST) {
                return null;
            }
            return new View(stack);
        });
    }

    public static final class View implements NeroEnergyStorage {
        private final ItemStack stack;

        public View(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public long getAmount() {
            return SuitStacks.core(this.stack).energy();
        }

        @Override
        public long getCapacity() {
            SuitLoadout loadout = SuitStacks.loadout(this.stack);
            return SuitEnergyMath.capacity(loadout.frameEnum(), loadout.moduleCount(SuitIds.POWER_CELL),
                    NerospaceConfig.suitEnergyCapacityMultiplier());
        }

        @Override
        public long insert(long maxAmount, boolean simulate) {
            if (maxAmount <= 0) {
                return 0;
            }
            long room = getCapacity() - getAmount();
            long moved = Math.min(maxAmount, Math.max(0, room));
            if (!simulate && moved > 0) {
                SuitStacks.setCore(this.stack, SuitStacks.core(this.stack).withEnergy((int) (getAmount() + moved)));
            }
            return moved;
        }

        @Override
        public long extract(long maxAmount, boolean simulate) {
            if (maxAmount <= 0) {
                return 0;
            }
            long moved = Math.min(maxAmount, getAmount());
            if (!simulate && moved > 0) {
                SuitStacks.setCore(this.stack, SuitStacks.core(this.stack).withEnergy((int) (getAmount() - moved)));
            }
            return moved;
        }
    }

    /** @return the view, or null when the stack is not a suit chest. */
    @Nullable
    public static NeroEnergyStorage find(ItemStack stack) {
        return ItemEnergyLookup.find(stack);
    }
}

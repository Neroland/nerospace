package za.co.neroland.nerospace.module;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A reusable bank of upgrade-module slots that any machine can embed (the quarry is the first consumer).
 * It owns the module slots and exposes the aggregated effects the host machine reads each tick. Effects
 * scale with the number of matching modules across the slots, each clamped so a stack of cards can't
 * trivialise balance.
 *
 * <p>Cross-loader port note: the root backed this with a NeoForge-transfer {@code MachineItemHandler};
 * the multiloader uses a plain {@link NonNullList} (the host machine exposes the slots through its own
 * vanilla {@code Container}/capability view).</p>
 */
public final class MachineModules {

    private static final double SPEED_PER_MODULE = 0.5D;
    private static final double EFFICIENCY_PER_MODULE = 0.15D;
    private static final double MIN_ENERGY_FRACTION = 0.25D;
    private static final int MAX_FORTUNE = 3;
    private static final double MAX_SPEED = 8.0D;

    private final @org.jspecify.annotations.NonNull NonNullList<ItemStack> items;
    private final Runnable onChange;

    public MachineModules(int slots, Runnable onChange) {
        this.items = NonNullList.withSize(slots, ItemStack.EMPTY);
        this.onChange = onChange;
    }

    /** The backing slot list (the host machine's Container view routes to it). */
    public @org.jspecify.annotations.NonNull NonNullList<ItemStack> items() {
        return this.items;
    }

    public int slots() {
        return this.items.size();
    }

    public ItemStack getStack(int index) {
        return this.items.get(index);
    }

    public void setStack(int index, @org.jspecify.annotations.NonNull ItemStack stack) {
        this.items.set(index, za.co.neroland.nerospace.NerospaceCommon.requireNonNull(stack));
        this.onChange.run();
    }

    private int count(ModuleType type) {
        int total = 0;
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty() && UpgradeModuleItem.typeOf(stack) == type) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** Work-cap multiplier (>= 1): more Speed modules = a higher per-cycle ceiling. */
    public double speedMultiplier() {
        return Math.min(MAX_SPEED, 1.0D + count(ModuleType.SPEED) * SPEED_PER_MODULE);
    }

    /** Energy-cost multiplier (<= 1): more Efficiency modules = cheaper work, floored. */
    public double energyMultiplier() {
        return Math.max(MIN_ENERGY_FRACTION, 1.0D - count(ModuleType.EFFICIENCY) * EFFICIENCY_PER_MODULE);
    }

    /** Effective Fortune level applied to harvested blocks (0 = none), ignored when {@link #silkTouch()}. */
    public int fortuneLevel() {
        return Math.min(MAX_FORTUNE, count(ModuleType.FORTUNE));
    }

    public boolean silkTouch() {
        return count(ModuleType.SILK_TOUCH) > 0;
    }

    // --- Persistence (host machine calls these from save/loadAdditional) --------

    public void save(ValueOutput output) {
        for (int i = 0; i < this.items.size(); i++) {
            output.store("Module" + i, za.co.neroland.nerospace.NerospaceCommon.ITEM_STACK_CODEC, this.items.get(i));
        }
    }

    public void load(ValueInput input) {
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, za.co.neroland.nerospace.NerospaceCommon.orElse(
                    input.read("Module" + i, za.co.neroland.nerospace.NerospaceCommon.ITEM_STACK_CODEC),
                    ItemStack.EMPTY));
        }
    }
}

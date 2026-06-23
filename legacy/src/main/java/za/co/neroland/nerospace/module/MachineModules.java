package za.co.neroland.nerospace.module;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import za.co.neroland.nerospace.machine.MachineItemHandler;

/**
 * A reusable bank of upgrade-module slots that any machine can embed (MINER_DESIGN — the quarry is
 * the first consumer). It owns a {@link MachineItemHandler} validated to {@link UpgradeModuleItem}s
 * (so only module cards can be inserted, by hand or by pipe) and exposes the aggregated effects the
 * host machine reads each tick. Effects scale with the number of matching modules across the slots,
 * each clamped so a stack of cards can't trivialise balance.
 */
public final class MachineModules {

    /** Each Speed module raises the work cap by this fraction. */
    private static final double SPEED_PER_MODULE = 0.5D;
    /** Each Efficiency module cuts energy cost by this fraction (floored — see {@link #energyMultiplier}). */
    private static final double EFFICIENCY_PER_MODULE = 0.15D;
    /** Energy cost never drops below this fraction of the base, however many Efficiency modules. */
    private static final double MIN_ENERGY_FRACTION = 0.25D;
    /** Fortune is capped at this level regardless of how many Fortune modules are present. */
    private static final int MAX_FORTUNE = 3;
    /** Speed multiplier ceiling. */
    private static final double MAX_SPEED = 8.0D;

    private final MachineItemHandler handler;

    public MachineModules(int slots, Runnable onChange) {
        this.handler = new MachineItemHandler(slots, onChange,
                (index, resource) -> UpgradeModuleItem.isModule(resource.toStack(1)));
    }

    /** The capability/GUI/Container-facing handler (the single source of truth for the slots). */
    public ResourceHandler<ItemResource> handler() {
        return this.handler;
    }

    /** Live accessor used by menus/containers. */
    public MachineItemHandler store() {
        return this.handler;
    }

    public int slots() {
        return this.handler.size();
    }

    private int count(ModuleType type) {
        int total = 0;
        for (int i = 0; i < this.handler.size(); i++) {
            ItemStack stack = this.handler.getStack(i);
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
        for (int i = 0; i < this.handler.size(); i++) {
            output.store("Module" + i, ItemStack.OPTIONAL_CODEC, this.handler.getStack(i));
        }
    }

    public void load(ValueInput input) {
        for (int i = 0; i < this.handler.size(); i++) {
            this.handler.setStack(i, input.read("Module" + i, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        }
    }
}

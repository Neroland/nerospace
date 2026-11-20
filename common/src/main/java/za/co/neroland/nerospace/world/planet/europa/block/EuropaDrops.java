package za.co.neroland.nerospace.world.planet.europa.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import za.co.neroland.nerospace.block.rules.EuropaDropRules;
import za.co.neroland.nerospace.world.planet.cindara.CindaraContent;

/**
 * The game-side half of {@link EuropaDropRules}: reads the tool and position out of the loot parameters and turns the
 * rule's id-and-count result back into item stacks. Every break path (a player, an explosion, the quarry) reaches the
 * blocks' {@code getDrops}, so the conditional drops hold for all of them.
 */
final class EuropaDrops {

    private EuropaDrops() {
    }

    /**
     * Whether this break is a Silk Touch one: the tool carries the enchantment, or the table already produced the
     * block itself (which only its silk-touch branch does). No tool parameter counts as not silk.
     */
    static boolean silk(LootParams.Builder params, BlockState state, List<ItemStack> base) {
        for (ItemStack s : base) {
            if (s.is(state.getBlock().asItem())) {
                return true;
            }
        }
        // The parameter's declared type differs between game versions; every caller passes an item stack.
        Object held = params.getOptionalParameter(LootContextParams.TOOL);
        return held instanceof ItemStack tool && !tool.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(params.getLevel()
                .registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), tool) > 0;
    }

    /** The broken block's position, or null when the parameters carry none. */
    static BlockPos origin(LootParams.Builder params) {
        Vec3 at = params.getOptionalParameter(LootContextParams.ORIGIN);
        return at == null ? null : BlockPos.containing(at);
    }

    static List<ItemStack> apply(EuropaDropRules.Kind kind, List<ItemStack> base, boolean silk, boolean flag,
            int charge) {
        List<EuropaDropRules.Stack> in = new ArrayList<>(base.size());
        for (ItemStack s : base) {
            in.add(new EuropaDropRules.Stack(BuiltInRegistries.ITEM.getKey(s.getItem()).toString(), s.getCount()));
        }
        List<EuropaDropRules.Stack> out = EuropaDropRules.apply(kind, in, silk, flag, charge);
        if (out == in || out.equals(in)) {
            return base;
        }
        List<ItemStack> result = new ArrayList<>(out.size());
        for (EuropaDropRules.Stack want : out) {
            ItemStack made = ItemStack.EMPTY;
            for (ItemStack s : base) {
                if (BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(want.id())) {
                    made = s.copyWithCount(want.count());
                    break;
                }
            }
            if (made.isEmpty() && EuropaDropRules.SULFUR.equals(want.id())) {
                made = new ItemStack(CindaraContent.SULFUR.get(), want.count());
            }
            if (!made.isEmpty()) {
                result.add(made);
            }
        }
        return result;
    }
}

package za.co.neroland.nerospace.world.planet.europa.block;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import za.co.neroland.nerolandcore.ore.CoreOres;
import za.co.neroland.nerolandcore.ore.OreBehaviour;
import za.co.neroland.nerolandcore.ore.OreBehaviours;
import za.co.neroland.nerospace.block.rules.EuropaDropRules;
import za.co.neroland.nerospace.block.rules.SulfideRules;

/**
 * Europa's wet-seam drop rules, attached to Core's ore blocks ({@code nerolandcore:brine_salt_ore},
 * {@code nerolandcore:vent_sulfide_ore}) through {@link OreBehaviours}. Without Nerospace they drop their plain loot.
 * Silk Touch lifts the block whole. Cost: on break only, 6 block reads.
 */
public final class EuropaOreBehaviours {

    private EuropaOreBehaviours() {
    }

    /** Called once from {@code NerospaceCommon.init}. */
    public static void register() {
        // Brine Salt Ore: mined with brine in any of the 6 neighbours it gives twice the salt (WetSeamRules,
        // 8 per stack at most).
        OreBehaviours.register(CoreOres.BRINE_SALT_ORE.id(), new OreBehaviour() {
            @Override
            public List<ItemStack> modifyDrops(BlockState state, LootParams.Builder params, List<ItemStack> base) {
                boolean silk = EuropaDrops.silk(params, state, base);
                BlockPos pos = EuropaDrops.origin(params);
                if (silk || pos == null || base.isEmpty()) {
                    return base;
                }
                return EuropaDrops.apply(EuropaDropRules.Kind.BRINE_SALT_ORE, base, false,
                        EuropaHeat.brineAdjacent(params.getLevel(), pos), 0);
            }
        });
        // Vent Sulfide Ore: mined wet (water or brine next to it) it gives its raw ore; mined dry the sulfide
        // oxidises and one sulfur is all that is left.
        OreBehaviours.register(CoreOres.VENT_SULFIDE_ORE.id(), new OreBehaviour() {
            @Override
            public List<ItemStack> modifyDrops(BlockState state, LootParams.Builder params, List<ItemStack> base) {
                boolean silk = EuropaDrops.silk(params, state, base);
                BlockPos pos = EuropaDrops.origin(params);
                if (silk || pos == null || base.isEmpty()) {
                    return base;
                }
                return EuropaDrops.apply(EuropaDropRules.Kind.VENT_SULFIDE_ORE, base, false,
                        SulfideRules.wet(EuropaHeat.liquidAdjacent(params.getLevel(), pos), false), 0);
            }
        });
    }
}

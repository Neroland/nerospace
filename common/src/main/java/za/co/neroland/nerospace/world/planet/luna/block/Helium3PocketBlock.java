package za.co.neroland.nerospace.world.planet.luna.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.nerospace.block.rules.GasPocketRules;
import za.co.neroland.nerospace.world.planet.luna.LunaContent;
import za.co.neroland.nerospace.world.planet.luna.LunaNumbers;

/**
 * Helium-3 Pocket (Luna). The loot table always drops the leaked yield (one canister); a sealed break — Silk Touch
 * or a Gas Tank on any of the six faces — adds the rest here so the total is {@link GasPocketRules#yield}. An
 * unsealed break vents a short burst of cold-flame particles: the leak.
 */
public class Helium3PocketBlock extends Block {

    public static final MapCodec<Helium3PocketBlock> CODEC = BlockCodecs.simple(Helium3PocketBlock::new);

    public Helium3PocketBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends Helium3PocketBlock> codec() {
        return CODEC;
    }

    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropXp) {
        super.spawnAfterBreak(state, level, pos, tool, dropXp);
        boolean silk = !tool.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), tool) > 0;
        boolean tank = gasTankAdjacent(level, pos);
        int extra = LunaNumbers.he3ExtraYield(silk, tank);
        if (extra > 0) {
            popResource(level, pos, new ItemStack(LunaContent.HELIUM3_CANISTER.get(), extra));
        }
        if (GasPocketRules.leaks(silk, tank)) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    GasPocketRules.LEAK_PARTICLES, 0.35, 0.35, 0.35, 0.02);
        }
    }

    private static boolean gasTankAdjacent(ServerLevel level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockState n = level.getBlockState(pos.relative(d));
            if (n.is(za.co.neroland.nerolandcore.registry.ModBlocks.GAS_TANK.get())
                    || n.is(za.co.neroland.nerolandcore.registry.ModBlocks.CREATIVE_GAS_TANK.get())) {
                return true;
            }
        }
        return false;
    }
}

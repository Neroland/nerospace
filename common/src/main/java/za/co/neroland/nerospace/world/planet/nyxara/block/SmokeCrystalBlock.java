package za.co.neroland.nerospace.world.planet.nyxara.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

import za.co.neroland.nerolandcore.registry.BlockCodecs;

/**
 * Smoke Crystal (Nyxara nightside): absorbs sound. Steps and falls on it are silent (vanilla's
 * {@code intentionally_empty} event), breaking and placing sound like amethyst, and it is in
 * {@code #minecraft:occludes_vibration_signals} and {@code #minecraft:dampens_vibrations}. Faint inner glow, light 1.
 */
public class SmokeCrystalBlock extends Block {

    public static final MapCodec<SmokeCrystalBlock> CODEC = BlockCodecs.simple(SmokeCrystalBlock::new);

    /** Amethyst break / place / hit with silent step and fall. */
    public static final SoundType HUSH = new SoundType(1.0F, 1.0F, SoundType.AMETHYST.getBreakSound(),
            SoundEvents.EMPTY, SoundType.AMETHYST.getPlaceSound(), SoundType.AMETHYST.getHitSound(), SoundEvents.EMPTY);

    public SmokeCrystalBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<? extends SmokeCrystalBlock> codec() {
        return CODEC;
    }
}

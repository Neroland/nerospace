package za.co.neroland.nerospace.world;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import net.minecraft.core.BlockPos;

import za.co.neroland.nerospace.registry.ModTags;

/**
 * Block sealing classification for the oxygen field (terraform design §1.4).
 *
 * <p>Data-driven via block tags so it stays moddable: {@code nerospace:oxygen_sealing} blocks all
 * flow (opaque cubes, glass, station walls — players can build airtight rooms with windows),
 * {@code nerospace:oxygen_leaks} members are non-full blocks that hold oxygen but bleed faster, and
 * air flows freely. Doors and trapdoors flow only while {@code open}, so opening a door leaks the
 * room. There are no hardcoded {@code Block} checks here beyond the generic door/trapdoor + full-cube
 * fallbacks.</p>
 */
public final class OxygenField {

    private OxygenField() {
    }

    /** @return true if a cell can hold oxygen (air, a leaky block, or an open door); false if it seals. */
    public static boolean canHold(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return true;
        }
        if (state.is(ModTags.Blocks.OXYGEN_SEALING)) {
            return false;
        }
        if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock) {
            return state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN);
        }
        if (state.is(ModTags.Blocks.OXYGEN_LEAKS)) {
            return true;
        }
        // Fallback: anything that isn't a full solid cube is treated as leaky (holds + bleeds);
        // a full solid cube seals even without an explicit tag.
        return !state.isCollisionShapeFullBlock(level, pos);
    }

    /** @return true for non-full / leaky cells that bleed oxygen to the void faster (openings). */
    public static boolean isLeaky(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }
        // Doors/trapdoors BEFORE the leaks tag: vanilla TRAPDOORS are in OXYGEN_LEAKS, but a
        // closed one is a wall (canHold false) and must never read as leaky.
        if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock) {
            return state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN);
        }
        return state.is(ModTags.Blocks.OXYGEN_LEAKS);
    }
}

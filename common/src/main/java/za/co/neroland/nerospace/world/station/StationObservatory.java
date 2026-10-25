package za.co.neroland.nerospace.world.station;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerospace.registry.ModDimensions;
import za.co.neroland.nerospace.rocket.StationRegistry;
import za.co.neroland.nerospace.rocket.StationRegistry.StationEntry;
import za.co.neroland.nerospace.world.planet.nyxara.NyxaraContent;
import za.co.neroland.nerospace.world.station.block.TelescopeBlock;

/**
 * The Observatory's reveal: how many locked destinations a player may see as
 * full LOCKED_TIER planet cards. Station-scoped — nothing is stored per player. The Rocket screen's planet card reads
 * {@link #revealFor}; the number is planet data only (the cards show traits, weather and clock, never players).
 */
public final class StationObservatory {

    private StationObservatory() {
    }

    /**
     * How many of the next locked destinations (in tier order) {@code player} may preview: non-zero only for a
     * {@code canUse} player standing inside the claim box of a RESEARCH station that has an Observatory.
     */
    public static int revealFor(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !ModDimensions.STATION_LEVEL.equals(level.dimension())) {
            return 0;
        }
        StationRegistry registry = StationRegistry.get(level.getServer());
        StationEntry entry = registry.entryAt(level.dimension(), player.blockPosition());
        if (entry == null || !StationRegistry.inClaimBox(entry, player.blockPosition())
                || !entry.hasModule(ModuleKind.OBSERVATORY) || !StationRegistry.canUse(entry, player)) {
            return 0;
        }
        return revealCount(level, entry);
    }

    /** The station's reveal count from its telescope column (0 when dormant or absent). */
    public static int revealCount(ServerLevel level, StationEntry entry) {
        if (!entry.hasModule(ModuleKind.OBSERVATORY)) {
            return 0;
        }
        BlockPos scope = ModuleTemplates.anchor(ModuleKind.OBSERVATORY, 0, entry.center()).above(3);
        if (!level.isLoaded(scope) || !(level.getBlockState(scope).getBlock() instanceof TelescopeBlock)) {
            return 0;
        }
        boolean research = entry.stationClass().traits().observatoryAllowed();
        int crystals = 0;
        for (int dy = 0; dy < ObservatoryRules.RANGE; dy++) {
            BlockPos p = entry.center().above(ObservatoryRules.START_DY + dy);
            if (level.isLoaded(p) && level.getBlockState(p).is(NyxaraContent.VOID_CRYSTAL.get())) {
                crystals++;
            }
        }
        return ObservatoryRules.revealCount(crystals, research);
    }

    /** Keeps the telescope's {@code DORMANT} state in step with the class (no block is ever removed). */
    public static void syncTelescope(ServerLevel level, StationEntry entry) {
        if (!entry.hasModule(ModuleKind.OBSERVATORY)) {
            return;
        }
        BlockPos scope = ModuleTemplates.anchor(ModuleKind.OBSERVATORY, 0, entry.center()).above(3);
        if (!level.isLoaded(scope)) {
            return;
        }
        BlockState state = level.getBlockState(scope);
        if (state.getBlock() instanceof TelescopeBlock) {
            boolean dormant = !entry.stationClass().traits().observatoryAllowed();
            if (state.getValue(TelescopeBlock.DORMANT) != dormant) {
                level.setBlockAndUpdate(scope, state.setValue(TelescopeBlock.DORMANT, dormant));
            }
        }
    }
}

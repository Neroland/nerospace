package za.co.neroland.nerospace.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.gas.NeroGasStorage;
import za.co.neroland.nerolandcore.gas.NeroGases;

import za.co.neroland.nerospace.gas.GasResource;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.platform.GasLookup;
import za.co.neroland.nerospace.storage.CoreTankBridge;

/**
 * Helpers for the Universal Pipe's gas layer, which carries Neroland Core's {@link Identifier}-keyed
 * gases (e.g. {@code nerospace:oxygen}, {@code nerotech:hydrogen}) rather than Nerospace's closed
 * {@link GasResource} enum: the neighbour lookup, the saved-id reader and the stream colour.
 */
public final class PipeGas {

    /** NeroTech's hydrogen id (the only non-oxygen gas with a dedicated stream colour). */
    private static final Identifier HYDROGEN_ID = Identifier.fromNamespaceAndPath("nerotech", "hydrogen");
    private static final int HYDROGEN_COLOR = 0xFFCEFFFF;   // pale blue
    private static final int OTHER_GAS_COLOR = 0xFFA8ACB4;  // neutral grey

    private PipeGas() {
    }

    /**
     * The gas storage behind {@code pos} as seen from {@code side}, on Core's surface, or {@code null}.
     *
     * <p>Order matters: Core's shared {@code nerolandcore:gas} capability is asked FIRST, and only when
     * that comes back {@code null} is Nerospace's own {@code nerospace:gas} asked (wrapped with
     * {@link CoreTankBridge#toCore}). The reasons, one per kind of neighbour:</p>
     * <ul>
     *   <li><b>NeroTech machines</b> (Electrolyzer, Chemical Processor, Gas Turbine) are registered on
     *       Core's capability ONLY, so without it the pipe never saw them.</li>
     *   <li><b>Core's Gas Tank / Creative Gas Tank / Trash Can</b> are registered on both. Core's is
     *       the lossless one: Nerospace's re-exposure goes through {@link CoreTankBridge#gas}, which
     *       can only express oxygen, so a tank of hydrogen looked empty and refused hydrogen.</li>
     *   <li><b>Nerospace's own machines</b> (Oxygen Generator, Launch Controller, the launch pad's gas
     *       proxy) are registered on {@code nerospace:gas} ONLY — never on Core's gas capability — so
     *       Core's answer for them is always {@code null} and the fallback reaches the side-config-gated
     *       registration. A face the side config has DISABLED returns {@code null} there too, so the
     *       gate cannot be walked around through Core's capability. The one Nerospace block entity on
     *       Core's gas capability is the Universal Pipe itself (ungated, exactly like its
     *       {@code nerospace:gas} exposure), and the pipe network never queries its own members.</li>
     * </ul>
     * <p>If a side-configured Nerospace machine is ever put on Core's gas capability, it must be
     * registered with its gated {@code sideConfig().gasView(side)} — as {@code registerCoreEnergy} does
     * for energy — or this ordering would bypass the gate.</p>
     */
    @Nullable
    public static NeroGasStorage find(Level level, BlockPos pos, @Nullable Direction side) {
        NeroGasStorage core = za.co.neroland.nerolandcore.platform.GasLookup.INSTANCE.find(level, pos, side);
        if (core != null) {
            return core;
        }
        NerospaceGasStorage own = GasLookup.INSTANCE.find(level, pos, side);
        return own == null ? null : CoreTankBridge.toCore(own);
    }

    /**
     * Read a pipe's saved {@code Gas} string. Pipes saved before the gas layer was Identifier-keyed
     * wrote a {@link GasResource} serialized name ({@code "oxygen"}/{@code "empty"}, no {@code ':'});
     * those map through {@link GasResource}. Anything else is parsed as an id, and legacy aliases are
     * folded by {@link CoreTankBridge#canonical}.
     */
    public static Identifier readSaved(String raw) {
        if (raw.indexOf(':') < 0) {
            return CoreTankBridge.toId(GasResource.byName(raw));
        }
        Identifier parsed = Identifier.tryParse(raw);
        return parsed == null ? NeroGases.EMPTY : CoreTankBridge.canonical(parsed);
    }

    /** Stream-pulse colour (ARGB) for {@code gas}: oxygen cyan, hydrogen pale blue, otherwise grey. */
    public static int streamColor(@Nullable Identifier gas) {
        if (NeroGases.isEmpty(gas) || CoreTankBridge.OXYGEN_ID.equals(CoreTankBridge.canonical(gas))) {
            return PipeResourceType.GAS.color();
        }
        if (HYDROGEN_ID.equals(gas)) {
            return HYDROGEN_COLOR;
        }
        return OTHER_GAS_COLOR;
    }
}

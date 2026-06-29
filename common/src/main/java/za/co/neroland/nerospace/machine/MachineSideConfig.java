package za.co.neroland.nerospace.machine;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.gas.NeroGasStorage;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;

import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;
import za.co.neroland.nerospace.storage.CoreTankBridge;

/**
 * Small shared glue for wiring Neroland Core's universal side-configuration onto Nerospace's
 * machines (Core 1.3.0, composable path). Nerospace ships its own machine hierarchy, so each
 * machine block-entity composes a {@link SideConfigComponent} rather than extending Core's base
 * machine — see the per-machine {@code buildConfig()} methods.
 *
 * <p>The component's gated views ({@code energyView}/{@code fluidView}/{@code gasView}) return
 * <em>Core</em> storage types and are {@code null} on a face whose mode is {@code DISABLED} (and
 * insert-/extract-only on INPUT/OUTPUT). Energy is already a {@code NeroEnergyStorage}, so the
 * energy view registers directly on Core's {@code nerolandcore:energy} capability (which the
 * Universal Pipe already queries through Nerospace's delegating energy lookup). Fluid and gas use
 * Nerospace's own interfaces, so the gated Core view is mapped back onto Nerospace's
 * {@code nerospace:fluid}/{@code nerospace:gas} capability via {@link CoreTankBridge} — the
 * Universal Pipe keeps querying those lookups unchanged while honouring per-face modes.
 *
 * <p>Side-config data is world/block state keyed by position; it holds no player identity, is never
 * logged and never sent to telemetry (POPIA/GDPR).
 */
public final class MachineSideConfig {

    /**
     * Per-tick auto-transfer budget (mB / FE) used by {@link SideConfigComponent#serverTick}. The
     * auto-eject / auto-input toggles default OFF on every channel, so this only takes effect once a
     * player explicitly enables a face's auto-transfer; it is a conservative, pipe-comparable rate.
     */
    public static final int TRANSFER_RATE = 1_000;

    private MachineSideConfig() {
    }

    /** The gated, Nerospace-typed fluid view for {@code side} (null on DISABLED faces). */
    @Nullable
    public static NerospaceFluidStorage fluidView(SideConfigComponent sideConfig,
            net.minecraft.core.Direction side) {
        NeroFluidStorage view = sideConfig.fluidView(side);
        return view == null ? null : CoreTankBridge.fluid(view);
    }

    /** The gated, Nerospace-typed gas view for {@code side} (null on DISABLED/INPUT faces). */
    @Nullable
    public static NerospaceGasStorage gasView(SideConfigComponent sideConfig,
            net.minecraft.core.Direction side) {
        NeroGasStorage view = sideConfig.gasView(side);
        return view == null ? null : CoreTankBridge.gas(view);
    }
}

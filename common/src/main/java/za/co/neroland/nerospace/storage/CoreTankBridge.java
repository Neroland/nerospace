package za.co.neroland.nerospace.storage;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;

import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.gas.NeroGasStorage;
import za.co.neroland.nerolandcore.gas.NeroGases;

import za.co.neroland.nerospace.fluid.NerospaceFluidStorage;
import za.co.neroland.nerospace.gas.GasResource;
import za.co.neroland.nerospace.gas.NerospaceGasStorage;

/**
 * Adapts Neroland Core's generic storage tanks back onto Nerospace's own
 * {@code nerospace:fluid} / {@code nerospace:gas} capabilities.
 *
 * <p>The Battery, Fluid Tank, Gas Tank and Item Store blocks now live in Core and
 * expose Core's {@code nerolandcore:*} capabilities. Energy and items already
 * cross over for free — Nerospace's energy lookup delegates to Core's, and the
 * Item Store is a vanilla {@code Container}. Fluid, and gas for Nerospace's own
 * {@link GasResource}-typed consumers (Oxygen airlocks, the Launch Controller),
 * are still queried on the mod's own {@code nerospace:fluid}/{@code nerospace:gas}
 * lookups, so each loader re-exposes Core's tank block-entities on those lookups
 * through these thin adapters (registered in the loader capability wiring). Gas
 * maps Core's {@link Identifier}-keyed gases to Nerospace's {@link GasResource}
 * (only oxygen exists today). The Universal Pipe's gas layer is Identifier-keyed
 * and asks Core's {@code nerolandcore:gas} first (see
 * {@link za.co.neroland.nerospace.pipe.PipeGas}), so it is not limited by this
 * mapping.
 *
 * <p>One oxygen across the ecosystem: {@code nerospace:oxygen} is THE oxygen id. NeroTech wrote
 * {@code nerotech:oxygen} before it adopted Nerospace's id, so {@link #canonical} folds that legacy
 * alias onto {@link #OXYGEN_ID}. Every place that interprets a gas id goes through it — the
 * {@link GasResource} mapping below and the Universal Pipe's Identifier-keyed gas layer — so there is
 * exactly one list of aliases.</p>
 */
public final class CoreTankBridge {

    /** Core gas id for Nerospace oxygen — matches the {@code gas.nerospace.oxygen} translation key. */
    public static final Identifier OXYGEN_ID = Identifier.fromNamespaceAndPath("nerospace", "oxygen");

    /** NeroTech's pre-unification oxygen id; read as {@link #OXYGEN_ID} everywhere. */
    public static final Identifier LEGACY_NEROTECH_OXYGEN_ID = Identifier.fromNamespaceAndPath("nerotech", "oxygen");

    private CoreTankBridge() {
    }

    /** Wrap a Core fluid store as a Nerospace fluid store (identical shape). */
    public static NerospaceFluidStorage fluid(NeroFluidStorage core) {
        return new NerospaceFluidStorage() {
            @Override
            public Fluid getFluid() {
                return core.getFluid();
            }

            @Override
            public long getAmount() {
                return core.getAmount();
            }

            @Override
            public long getCapacity() {
                return core.getCapacity();
            }

            @Override
            public long fill(Fluid fluid, long amount, boolean simulate) {
                return core.fill(fluid, amount, simulate);
            }

            @Override
            public long drain(long amount, boolean simulate) {
                return core.drain(amount, simulate);
            }
        };
    }

    /** Wrap a Core gas store as a Nerospace gas store, mapping gas ids to {@link GasResource}. */
    public static NerospaceGasStorage gas(NeroGasStorage core) {
        return new NerospaceGasStorage() {
            @Override
            public GasResource getGas() {
                return fromId(core.getGas());
            }

            @Override
            public long getAmount() {
                return core.getAmount();
            }

            @Override
            public long getCapacity() {
                return core.getCapacity();
            }

            @Override
            public long fill(GasResource gas, long amount, boolean simulate) {
                return fillCanonical(core, toId(gas), amount, simulate);
            }

            @Override
            public long drain(long amount, boolean simulate) {
                return core.drain(amount, simulate);
            }
        };
    }

    // --- Reverse adapters (Nerospace store -> Core store) -------------------
    //
    // The side-config component (Neroland Core 1.3.0) wraps a machine's storage as a Core
    // {@link NeroFluidStorage}/{@link NeroGasStorage} and hands back gated, Core-typed views
    // (fluidView/gasView). Nerospace's machines keep their own NerospaceFluidStorage/
    // NerospaceGasStorage stores, so these wrap them up to the Core surface for {@code withFluid}/
    // {@code withGas}. The gated Core view is then mapped back to Nerospace's own capability with
    // {@link #fluid}/{@link #gas} above, so the Universal Pipe (which queries Nerospace's fluid/gas
    // lookups) keeps working unchanged while honouring per-face modes.

    /** Wrap a Nerospace fluid store as a Core fluid store (identical shape). */
    public static NeroFluidStorage toCore(NerospaceFluidStorage nero) {
        return new NeroFluidStorage() {
            @Override
            public Fluid getFluid() {
                return nero.getFluid();
            }

            @Override
            public long getAmount() {
                return nero.getAmount();
            }

            @Override
            public long getCapacity() {
                return nero.getCapacity();
            }

            @Override
            public long fill(Fluid fluid, long amount, boolean simulate) {
                return nero.fill(fluid, amount, simulate);
            }

            @Override
            public long drain(long amount, boolean simulate) {
                return nero.drain(amount, simulate);
            }
        };
    }

    /** Wrap a Nerospace gas store as a Core gas store, mapping {@link GasResource} to gas ids. */
    public static NeroGasStorage toCore(NerospaceGasStorage nero) {
        return new NeroGasStorage() {
            @Override
            public Identifier getGas() {
                return toId(nero.getGas());
            }

            @Override
            public long getAmount() {
                return nero.getAmount();
            }

            @Override
            public long getCapacity() {
                return nero.getCapacity();
            }

            @Override
            public long fill(Identifier gas, long amount, boolean simulate) {
                return nero.fill(fromId(gas), amount, simulate);
            }

            @Override
            public long drain(long amount, boolean simulate) {
                return nero.drain(amount, simulate);
            }
        };
    }

    /**
     * The canonical id for {@code gas}: legacy aliases (today only {@code nerotech:oxygen}) fold onto the
     * id Nerospace uses; anything else — including {@code null}/empty — is returned unchanged.
     */
    public static Identifier canonical(Identifier gas) {
        return LEGACY_NEROTECH_OXYGEN_ID.equals(gas) ? OXYGEN_ID : gas;
    }

    /**
     * Fill {@code target} with {@code gas}, tolerating an alias: if the target already holds a gas whose
     * {@link #canonical} id matches, fill with the target's OWN spelling so a tank still holding legacy
     * {@code nerotech:oxygen} keeps accepting oxygen instead of refusing it as a different gas.
     */
    public static long fillCanonical(NeroGasStorage target, Identifier gas, long amount, boolean simulate) {
        Identifier held = target.getGas();
        Identifier use = !NeroGases.isEmpty(held) && !NeroGases.isEmpty(gas)
                && canonical(held).equals(canonical(gas)) ? held : gas;
        return target.fill(use, amount, simulate);
    }

    private static GasResource fromId(Identifier id) {
        return OXYGEN_ID.equals(canonical(id)) ? GasResource.OXYGEN : GasResource.EMPTY;
    }

    /** The Core gas id for a Nerospace {@link GasResource} ({@link NeroGases#EMPTY} for EMPTY). */
    public static Identifier toId(GasResource gas) {
        return gas == GasResource.OXYGEN ? OXYGEN_ID : NeroGases.EMPTY;
    }
}

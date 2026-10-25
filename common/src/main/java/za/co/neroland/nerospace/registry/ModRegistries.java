package za.co.neroland.nerospace.registry;

/**
 * Aggregates the cross-loader content registries. Called once from
 * {@link za.co.neroland.nerospace.NerospaceCommon#init()}. Order matters on the eager (Fabric)
 * loader: fluids before blocks/items (the liquid block + bucket resolve the fluid at construction),
 * and blocks before items (the block item references its block).
 */
public final class ModRegistries {

    private ModRegistries() {
    }

    public static void init() {
        ModSounds.init();
        ModDataComponents.init();
        za.co.neroland.nerospace.fluid.ModFluids.init();
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();
        ModMenuTypes.init();
        ModEntities.init();
        ModFeatures.init();
        // Planet content: each planet class registers its blocks before its block items through
        // the shared providers above, so these run after every provider exists and before the tab.
        za.co.neroland.nerospace.world.planet.luna.LunaContent.init();
        za.co.neroland.nerospace.world.planet.mars.MarsContent.init();
        za.co.neroland.nerospace.world.planet.cindara.CindaraContent.init();
        za.co.neroland.nerospace.world.planet.glacira.GlaciraContent.init();
        za.co.neroland.nerospace.world.planet.greenxertz.GreenxertzContent.init();
        za.co.neroland.nerospace.world.planet.nyxara.NyxaraContent.init();
        // Orbital station blocks (hull, truss, wings, plates, trays, telescope) - Stage 4 stations.
        za.co.neroland.nerospace.world.station.StationContent.init();
        // Stage 4A: the Docking Ring (a DOCK landing node) — block + item.
        za.co.neroland.nerospace.rocket.DockingContent.init();
        ModCreativeTab.init();
        za.co.neroland.nerospace.network.ModNetwork.init();
    }
}

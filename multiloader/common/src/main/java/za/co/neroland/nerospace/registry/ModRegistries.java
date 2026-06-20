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
        ModCreativeTab.init();
    }
}

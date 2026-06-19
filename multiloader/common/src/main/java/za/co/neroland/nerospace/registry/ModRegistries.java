package za.co.neroland.nerospace.registry;

/**
 * Aggregates the cross-loader content registries. Called once from
 * {@link za.co.neroland.nerospace.NerospaceCommon#init()}. Order matters:
 * blocks before items (the block item references its block).
 */
public final class ModRegistries {

    private ModRegistries() {
    }

    public static void init() {
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();
        ModMenuTypes.init();
    }
}

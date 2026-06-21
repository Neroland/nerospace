package za.co.neroland.nerospace.village;

import net.minecraft.world.level.block.Block;

/**
 * Village Core — the glowing centerpiece of alien hamlets / ruins / mega-cities.
 *
 * <p>Cross-loader port note: ported here as a plain decorative block. The root's interactive controller
 * (claim → teach-and-grow village construction, fetch quests, night raids) is a deep gameplay subsystem
 * (`VillageCoreBlockEntity` + `VillageBuildings` + config gates) deferred to its own batch; the structures
 * place this as their anchor centerpiece meanwhile.</p>
 */
public class VillageCoreBlock extends Block {

    public VillageCoreBlock(Properties properties) {
        super(properties);
    }
}

package za.co.neroland.nerospace.registry;

import java.util.function.Supplier;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.criterion.PlayerTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import za.co.neroland.nerospace.Nerospace;

/**
 * Custom advancement criteria (Star Guide / §1 advancements). {@link #TERRAFORMED_GROUND} is a
 * plain {@link PlayerTrigger} (trigger-on-call, like vanilla's tick/location triggers) fired from
 * {@code GreenxertzAtmosphere} when a player stands on permanently terraformed ground — there is
 * no vanilla criterion that can read the per-chunk TERRAFORMED attachment.
 */
public final class ModCriteria {

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGER_TYPES =
            DeferredRegister.create(Registries.TRIGGER_TYPE, Nerospace.MODID);

    public static final Supplier<PlayerTrigger> TERRAFORMED_GROUND =
            TRIGGER_TYPES.register("terraformed_ground", PlayerTrigger::new);

    /** Fired from the rocket's founding flow when a player founds a new station. */
    public static final Supplier<PlayerTrigger> FOUNDED_STATION =
            TRIGGER_TYPES.register("founded_station", PlayerTrigger::new);

    private ModCriteria() {
    }

    public static void register(IEventBus modEventBus) {
        TRIGGER_TYPES.register(modEventBus);
    }
}

package za.co.neroland.nerospace.registry;

import java.util.Optional;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.timeline.Timeline;

import za.co.neroland.nerospace.Nerospace;

/**
 * Custom dimension types (terraform/sky work). {@link #SPACE} is a from-scratch dimension type for the
 * airless space dimensions (Cindara, the Station): a static dark starfield ({@code Skybox.END}, no sun,
 * fixed time) with overworld-compatible height + skylight so terrain and the oxygen field behave
 * normally.
 *
 * <p>It must be a <b>registered</b> registry entry (not an inline {@code Holder.direct}): the clientbound
 * login packet encodes the player's {@code Holder<DimensionType>} as a registry reference, so an
 * unregistered/direct holder fails to encode and the client cannot join.</p>
 *
 * <p>26.1 made the sky a 3-value {@code DimensionType.Skybox} enum (NONE/OVERWORLD/END) and removed the
 * old {@code DimensionSpecialEffects} render hook, so a bespoke galaxy texture / small sun is not
 * available; END is the engine's starfield preset.</p>
 */
public final class ModDimensionTypes {

    public static final ResourceKey<DimensionType> SPACE = ResourceKey.create(
            Registries.DIMENSION_TYPE, Identifier.fromNamespaceAndPath(Nerospace.MODID, "space"));

    private ModDimensionTypes() {
    }

    public static void bootstrap(BootstrapContext<DimensionType> context) {
        context.register(SPACE, new DimensionType(
                true,                                       // hasFixedTime — static sky, no day/night
                true,                                       // hasSkyLight — keep terrain lit
                false,                                      // hasCeiling
                false,                                      // hasEnderDragonFight
                1.0D,                                       // coordinateScale
                -64, 384, 384,                              // minY, height, logicalHeight (overworld-like)
                BlockTags.INFINIBURN_OVERWORLD,
                0.0F,                                       // ambientLight
                new DimensionType.MonsterSettings(UniformInt.of(0, 7), 0),
                DimensionType.Skybox.END,                   // dark starfield
                CardinalLighting.Type.DEFAULT,
                EnvironmentAttributeMap.EMPTY,
                HolderSet.<Timeline>direct(),               // no day/night timeline
                Optional.empty()));                         // no world clock
    }
}

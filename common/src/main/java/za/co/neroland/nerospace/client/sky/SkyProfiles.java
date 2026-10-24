package za.co.neroland.nerospace.client.sky;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import za.co.neroland.nerospace.NerospaceCommon;
import za.co.neroland.nerospace.world.planet.PlanetDefinition;
import za.co.neroland.nerospace.world.planet.Planets;

/**
 * Client resource loader for sky profiles: for every planet, reads {@code assets/<ns>/sky/<path>.json} named by
 * its {@code skyProfile} trait and decodes it with {@link SkyProfile#CODEC}. Reloads with the resource packs
 * (F3+T). A missing or malformed file falls back to {@link SkyProfile#VANILLA} (keep the dimension's own sky) and
 * is logged once — never a crash.
 *
 * <p>Registered as a client reload listener by each loader's client setup; this class itself has no loader
 * imports.</p>
 */
public final class SkyProfiles implements ResourceManagerReloadListener {

    public static final SkyProfiles INSTANCE = new SkyProfiles();
    public static final Identifier ID = Identifier.fromNamespaceAndPath(NerospaceCommon.MOD_ID, "sky_profiles");

    private static volatile Map<Identifier, SkyProfile> profiles = Map.of();

    private SkyProfiles() {
    }

    /** The profile for a sky id (the planet's {@code skyProfile} trait), or {@link SkyProfile#VANILLA}. */
    public static SkyProfile get(Identifier skyId) {
        return profiles.getOrDefault(skyId, SkyProfile.VANILLA);
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<Identifier, SkyProfile> loaded = new HashMap<>();
        for (PlanetDefinition planet : Planets.ALL) {
            Identifier skyId = planet.traits().skyProfile();
            Identifier file = Identifier.fromNamespaceAndPath(skyId.getNamespace(), "sky/" + skyId.getPath() + ".json");
            Optional<Resource> resource = manager.getResource(file);
            if (resource.isEmpty()) {
                continue;
            }
            try (Reader reader = resource.get().openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                SkyProfile.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(err -> NerospaceCommon.LOGGER.warn("[Nerospace] sky profile {}: {}", file, err))
                        .ifPresent(profile -> loaded.put(skyId, profile));
            } catch (Exception ex) {
                NerospaceCommon.LOGGER.warn("[Nerospace] could not read sky profile {}: {}", file, ex.toString());
            }
        }
        profiles = Map.copyOf(loaded);
    }
}

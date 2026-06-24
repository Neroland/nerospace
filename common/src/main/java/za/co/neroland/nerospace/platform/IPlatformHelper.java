package za.co.neroland.nerospace.platform;

/**
 * The loader-specific behaviour the common module is allowed to depend on.
 *
 * <p>Each loader module ships exactly one implementation, registered via a
 * {@code META-INF/services} file so {@link Services} can load it with
 * {@link java.util.ServiceLoader}.
 *
 * <p>Grow this interface as the migration proceeds — it is the seam where
 * NeoForge capabilities, networking, config, attachments, etc. get their
 * cross-loader abstractions. See {@code docs/MULTILOADER.md} for the full
 * subsystem map.
 */
public interface IPlatformHelper {

    /** Human-readable platform name ("Fabric" / "NeoForge"). */
    String getPlatformName();

    /** True when running in a development (dev/data/test) environment. */
    boolean isDevelopmentEnvironment();

    /** True when the named mod is loaded. */
    boolean isModLoaded(String modId);

    /** True on the physical client (renderers, screens, HUD available). */
    boolean isClient();

    /** The loader config directory (NeoForge {@code FMLPaths.CONFIGDIR}, Fabric {@code getConfigDir}). */
    java.nio.file.Path getConfigDir();

    /** This mod's version string (for telemetry release tags), or "unknown" if unavailable. */
    String getModVersion();

    // --- Per-player oxygen (data-attachment seam) ---------------------------
    // NeoForge backs this with an AttachmentType registered via DeferredRegister; Fabric with the
    // data-attachment API. The value defaults to {@code OxygenManager.OXYGEN_MAX} and persists.

    /** The player's stored oxygen (millibuckets-of-air units), defaulting to full if unset. */
    int getOxygen(net.minecraft.world.entity.player.Player player);

    /** Stores the player's oxygen. */
    void setOxygen(net.minecraft.world.entity.player.Player player, int value);

    // --- Per-chunk terraform data (data-attachment seam) --------------------
    // The Terraformer permanently flags converted chunks: TERRAFORMED (breathable at/above the surface)
    // and TERRAFORM_STAGE (1 Rooted / 2 Hydrated / 3 Living). Same attachment APIs as the player oxygen,
    // applied to a LevelChunk target. Persist with the chunk; not synced (breathability is server-side).

    /** Whether the chunk has been terraformed (breathable ground). Defaults false. */
    boolean isTerraformed(net.minecraft.world.level.chunk.LevelChunk chunk);

    /** Flags the chunk as terraformed. */
    void setTerraformed(net.minecraft.world.level.chunk.LevelChunk chunk, boolean value);

    /** The highest terraform stage any column in the chunk has completed (0 none .. 3 Living). */
    int getTerraformStage(net.minecraft.world.level.chunk.LevelChunk chunk);

    /** Records the chunk's terraform stage. */
    void setTerraformStage(net.minecraft.world.level.chunk.LevelChunk chunk, int value);

    // --- Per-player Star Guide "seen" masks (data-attachment seam) -----------
    // One bitmask per chapter (bit i = step i acknowledged). Persists across logout, copies on death;
    // defaults to an empty list. Backs the Star Guide GUI's completed-but-unseen step pulse.

    /** The player's per-chapter Star Guide "seen" bitmasks (empty list if unset). */
    java.util.List<Integer> getStarGuideSeen(net.minecraft.world.entity.player.Player player);

    /** Stores the player's per-chapter Star Guide "seen" bitmasks. */
    void setStarGuideSeen(net.minecraft.world.entity.player.Player player, java.util.List<Integer> value);
}

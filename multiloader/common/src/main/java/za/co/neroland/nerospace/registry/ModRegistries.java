package za.co.neroland.nerospace.registry;

/**
 * Placeholder for cross-loader content registration.
 *
 * <p>The MultiLoader-Template approach (no Architectury API) does not provide a
 * shared {@code DeferredRegister}. Registration is performed per loader from the
 * loader entry points — NeoForge via {@code DeferredRegister}/{@code Registry}
 * events, Fabric via {@code Registry.register} — typically funnelled through a
 * small registration service added on top of the platform
 * {@link za.co.neroland.nerospace.platform.Services} seam.
 *
 * <p>This class is intentionally empty in the scaffold; it is the seam where the
 * migration (see {@code docs/MULTILOADER.md} §2) will introduce that service as
 * content is ported off the root project's NeoForge {@code DeferredRegister}s.
 */
public final class ModRegistries {

    private ModRegistries() {
    }
}

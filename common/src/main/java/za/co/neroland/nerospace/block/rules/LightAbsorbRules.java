package za.co.neroland.nerospace.block.rules;

/** Nyx Stone (002 §1.6, §11.1): block light propagated into a cell within 1 block of a Nyx Stone loses 2 extra. */
public final class LightAbsorbRules {

    public static final int ABSORB = 2;

    private LightAbsorbRules() {
    }

    public static int absorbedLevel(int incoming, boolean nearNyx) {
        return nearNyx ? Math.max(0, incoming - ABSORB) : incoming;
    }
}

package za.co.neroland.nerospace.world.station;

import com.mojang.serialization.Codec;

import za.co.neroland.nerospace.data.LenientDecode;

/**
 * Who may choose a station as a rocket destination. {@link #MEMBERS} hides the station from the
 * Rocket screen's station cycler for players who fail {@code canUse}; a rocket already in flight still lands.
 * Persisted by name, lenient: unknown → {@link #OPEN}.
 */
public enum DockingPolicy {
    OPEN,
    MEMBERS;

    public static final Codec<DockingPolicy> CODEC = Codec.STRING.xmap(DockingPolicy::parse, DockingPolicy::name);

    public static DockingPolicy parse(String name) {
        if (name != null && MEMBERS.name().equalsIgnoreCase(name.trim())) {
            return MEMBERS;
        }
        if (name != null && OPEN.name().equalsIgnoreCase(name.trim())) {
            return OPEN;
        }
        if (LenientDecode.unknownSaved(name, false)) {
            LenientDecode.warnOnce("docking policy");
        }
        return OPEN;
    }

    public DockingPolicy toggled() {
        return this == OPEN ? MEMBERS : OPEN;
    }
}

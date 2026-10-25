# Landing Pads

Where does a rocket come down? From 2.0 a rocket **always lands centred on a pad, facing the pad's front**,
or — when there is no pad to use — on an automatically placed **Landing Pod**. Arrivals are animated: the
rocket appears high above its landing spot and descends with its engines firing, with you seated inside
until touchdown.

## How the landing spot is chosen

The first rule that finds a spot wins:

1. **The pad you picked.** Use the pad button on the rocket's flight console to choose a named pad in the
   destination. If that pad has been broken, you are told and the next rules apply.
2. **Home pad.** A rocket flying back to the world it came from returns to the pad it launched from, if that
   pad still stands — named or not.
3. **Paired pad.** If you launched from a named pad (for example "Home Base") and the destination has a pad
   with exactly the same name, you land there. Automatic labels such as "Pad 3" never pair. If several pads
   share the name, the oldest one wins; a far-away, ambiguous name is ignored.
4. **Nearest pad.** Otherwise the nearest registered pad within 2048 blocks of the arrival area.
5. **Landing Pod.** If none of the above applies, a Landing Pod is placed for you.

A pad with another rocket already standing on it is skipped — a landing never stacks rockets.

## Registering a pad

Use a **Name Tag** on a [Rocket Launch Pad](Rocket-Launch-Pad) to register it as a landing spot (the tag's
name labels it; an unnamed tag gives "Pad N"). Sneak and use an empty hand on it to unregister it. Pads
registered before 2.0 keep working; the first time a rocket lands on one, its centre and front are worked out
and remembered.

A pad cannot be renamed while someone else is sitting in a rocket parked on it.

## Docking Ring

The **Docking Ring** is a 3×3 landing plate with hazard-stripe lights. Place it with room for the whole
3×3; it registers itself as a landing spot straight away (no Name Tag needed) and faces the way you were
looking. Breaking any part removes the whole ring and drops it as one item. Rockets of any tier can take off
from a Docking Ring.

At an orbital station, arriving rockets prefer the station's Docking Ring, then its original landing pad.
New stations are built with a Docking Ring; older stations keep their pad.

## Landing Pods

A Landing Pod needs a flat, solid 5×5 patch (no water, lava, leaves, ice or magma), three blocks of open space
above it, no structure in the way, and some distance from other players' protected areas (stations, cargo
pads you have no access to, terraformed ground) and from other pads. The search spirals out from the arrival
area — up to 24 blocks — and, if nothing fits, builds a small platform in the air above. An empty pod nearby
is reused, and pods are kept at least 32 blocks apart so they don't litter the landscape.

## Founding a station from a rocket

Choosing **New Station** in the dock list needs a **Station Charter** in your inventory, which is used up when
you arrive (never at launch, so an aborted trip costs nothing). Without a charter you dock at the shared origin
platform instead and get a message. The option is hidden in the dock list while you hold no charter.

## Good to know

- If a pad is broken while your rocket is coming down, it steers to the next spot instead.
- If you disconnect during the descent, the rocket finishes landing without you; you will be on the pad when
  you log back in.
- A rocket saved halfway through a descent finishes landing the moment its area loads again.

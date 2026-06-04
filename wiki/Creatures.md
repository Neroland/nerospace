# Creatures

The native life of the Nerospace planets. Each has bespoke geometry, a walk animation, a distinct
hand-tuned **texture palette**, and an **emissive glow** on its eyes/crystals/embers that shines in
the dark.

## Greenxertz

### Xertz Stalker — "Crystal Hunter" (hostile)
A tall, upright crystalline biped — the planet's apex predator. Uses a light-independent spawn rule, so
it's dangerous **day and night**. Strides toward prey on two legs with down-swept blade-arms.

**Look:** deep emerald/teal crystal hide shot through with angular cyan facets; its blade-arms, crest
and back-fins are brighter crystal with glowing edges. The facets and eyes are emissive, so it reads as
a cold cyan silhouette in the dark.

**Sounds:** chitters while idle, shrieks when hurt, shatters on death. *(Placeholder audio aliases
vanilla spider sounds until bespoke audio is recorded — see "Sound notes" below.)*

### Quartz Crawler — "Geode Skitterer" (neutral)
A low, domed six-legged crawler with a back crystal cluster. Skitters along the ground and leaves you
alone unless provoked.

**Look:** a pale, milky quartz carapace divided by darker seams, with faint rose-quartz veins. Only the
green crystal cluster on its back (and its eyes) glow — the plated shell itself stays solid, non-emissive
quartz.

**Sounds:** skitters while idle, cracks when hurt, crumbles on death. *(Placeholder aliases vanilla
silverfish sounds.)*

### Greenling — "Sprout" (passive)
A small, chubby grounded critter with an oversized cheeky head and a leaf crest. Harmless ambient life;
toddles around on two stubby legs.

**Look:** soft, friendly mottled greens with a lighter belly and gentle darker spots, topped by a leafy
crest. Deliberately the *only* creature that barely glows — just a tiny glint in its big dark eyes.

**Sounds:** chirps while idle, squeaks when hurt, wilts on death. *(Placeholder aliases vanilla panda
sounds for a soft, gentle voice.)*

## Cindara

### Cinder Stalker — "Magma Hulk" (hostile)
A heavy, horned quadruped of the volcanic moon, ridged with obsidian back-plates and glowing embers.
Fire-immune and aggressive — the main threat on Cindara.

**Look:** a charcoal/obsidian hide veined with branching, glowing orange-red lava cracks; obsidian horns
and back-plates whose ridge seams burn hot. The cracks and ember eyes are emissive, so it smoulders in
the dark.

**Sounds:** smoulders while idle, roars when hurt, cools on death. *(Placeholder aliases vanilla blaze
sounds.)*

---

## Sound notes

Each creature has three registered sound events under the `nerospace` namespace —
`entity.<creature>.ambient`, `.hurt`, and `.death` — defined in `assets/nerospace/sounds.json`. No
`.ogg` files ship yet: each entry currently **aliases a fitting vanilla sound event** (`"type":
"event"`) as a placeholder, chosen to suit the creature (spider for the Stalker, silverfish for the
Crawler, panda for the Greenling, blaze for the Cinder Stalker).

Because the events are registered under our own ids and every sound carries a Nerospace **subtitle**
key (e.g. `subtitles.nerospace.greenling.ambient`), dropping in real audio later is purely a resource
change: add `.ogg` files under `assets/nerospace/sounds/entity/<creature>/` and switch the sounds.json
entry from an `event` alias to a `sound` file. No code changes are needed.

---

**Spawn eggs:** all four creatures have creative spawn eggs in the Nerospace tab, each icon coloured to
match its creature — teal-and-cyan (Stalker), pale quartz with green flecks (Crawler), leafy green
(Greenling), and charred with ember spots (Cinder Stalker). For a quick A/B look (live vs frozen), use
the creative gallery command **`/nerospace gallery`**, which spawns each creature twice — once with AI
and once with NoAI — alongside a display of every block.

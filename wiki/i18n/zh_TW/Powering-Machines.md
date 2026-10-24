# Powering Machines

Most Nerospace machines need power (shown as **FE**). They do **not** make power themselves. If a
machine's power bar stays at **0%**, nothing is feeding it.

## The short version

1. Build a **generator**.
2. Put it **right next to** the machine (touching any side).
3. Put fuel in the generator.

That's it. Generators push power into every machine they touch on their own. You do not need a cable
for machines that touch the generator.

## Generators

| Generator                                    | Fuel                                                        | Power                                                                                                      | How long one item lasts                                                                                                                   |
| -------------------------------------------- | ----------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| [Combustion Generator](Combustion-Generator) | Coal, charcoal, coal block, blaze rod, Rocket Fuel Canister | 20 FE/tick                                                                                                 | Coal: 80 s · Coal block: 13 min · Blaze rod: 2 min · Canister: 3 min 20 s |
| [Passive Generator](Passive-Generator)       | Raw Nerosium, Nerosium Ingot or Nerosium Dust               | 8 FE/tick                                                                                                  | 20 minutes                                                                                                                                |
| [Solar Panel](Solar-Panel)                   | Sunlight (nothing to feed)               | Tier 1: 20 · Tier 2: 100 · Tier 3: 400 FE/tick in full sun | Forever                                                                                                                                   |

- The **Combustion** and **Passive** generators need **Nerosteel Ingots** to craft. Nerosteel Ore is
  found deep in the Overworld (below about y 16) — see [Nerosteel Ore](Nerosteel-Ore).
- **Solar panels** need Xertz Quartz, which you get later on Greenxertz.
- On airless worlds (the station and planets) solar panels make **double** power and work even under
  a roof.

## How much power do machines use?

| Machine                                | Uses                           | Good starter setup                                                                                                 |
| -------------------------------------- | ------------------------------ | ------------------------------------------------------------------------------------------------------------------ |
| [Nerosium Grinder](Nerosium-Grinder)   | 20 FE/tick while grinding      | 1 Combustion Generator (full speed) or 1 Passive Generator (about 40% speed) |
| [Oxygen Generator](Oxygen-Generator)   | 80 FE/tick while making oxygen | 4 Combustion Generators, or solar                                                                                  |
| [Fuel Refinery](Fuel-Refinery)         | 40 FE/tick while refining      | 2 Combustion Generators                                                                                            |
| [Quarry Controller](Quarry-Controller) | 40 FE per block mined          | 1–2 generators                                                                                                     |
| [Terraformer](Terraformer)             | 12–48 FE per column            | Several generators or Tier 2/3 solar                                                                               |

A machine that gets less power than it uses still works — it just runs slower, in bursts.

## Moving power further away

- **[Universal Pipe](Universal-Pipe)** — carries power, fluids, gas and items. Connect a generator to
  machines that are not touching it.
- **[Battery](Battery)** (from Neroland Core) — stores power so your machines keep running when the
  generator runs out of fuel or the sun goes down.
- **[Configurator](Configurator)** — right-click a machine face to choose what that side does (power
  in, items in, items out, off).

## Common problems

- **Power is 0%** — no generator is touching the machine or connected by pipe. Or the generator has no
  fuel.
- **Generator has fuel but machine is still 0%** — check the side with the
  [Configurator](Configurator); a face set to _off_ will not take power.
- **Generator stops burning** — its own storage is full. That is normal: it waits until something uses
  the power.

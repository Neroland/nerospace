# Combustion Generator

Burns fuel into energy (FE) for the pipe network.

## Obtaining

**Craft** (shaped): a nerosteel frame around a Furnace, wired with Redstone —

```text
N N N
N F N
N R N
```

`N` = Nerosteel Ingot · `F` = Furnace · `R` = Redstone

## How it works

- **Fuel slot** (GUI, hand or hopper/pipe fed): coal, charcoal, coal blocks, blaze rods, or Rocket

  Fuel Canisters.

- Burning generates **60 FE/t** (configurable) into a 50,000 FE buffer.
- The buffer is **extract-only**: connect a Universal Pipe (energy layer) and the network pulls the

  power to machines and Batteries.

- GUI shows the power buffer and burn progress; emits a comparator signal from its charge.

## Details

- ID: `nerospace:combustion_generator` · Tool: pickaxe, iron tier · Drops: itself
- Config: `combustionGeneratorFePerTick`

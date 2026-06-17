# Gas Tank

A pressurised store for one gas, filled and drained through the pipe network's gas layer.

## Obtaining

**Craft** (shaped): a sealed [Fluid Tank](Fluid-Tank) —

```text
N N N
N T N
N N N
```

`N` = Nerosteel Ingot · `T` = Fluid Tank

## How it works

- Holds **16,000 mB** (configurable) of one gas — **Oxygen** is the first gas in the mod.
- Universal Pipes (gas layer, green stream) fill and drain it on every side; pipe a surplus from your

  [Oxygen Generator](Oxygen-Generator) into it as a life-support buffer.

- Gas is gas: **breaking a gas-filled pipe (or the tank itself) vents the contents** — plan your

  plumbing before tearing it up.

- **Airlock:** a player wearing a full [Oxygen Suit](Oxygen-Suit) within a few blocks (default 3) of

  a tank holding Oxygen **refills the suit's air from it**, draining the gas — a tank by the base
  door is a working airlock. A Tier 2 suit refills twice as fast.

- Bare-hand right-click reads out the contents.

A **Creative Gas Tank** variant supplies endless Oxygen — see
[Creative Source Blocks](Creative-Source-Blocks).

## Details

- ID: `nerospace:gas_tank` · Tool: pickaxe, iron tier · Drops: itself
- Config: `gasTankCapacity`

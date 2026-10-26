# Passive Generator

<!-- nerospace:render -->

<p align="right"><img src="images/passive_generator.gif" alt="Passive Generator" width="150" align="right"></p>
<!-- /nerospace:render -->

由 Nerosium 核心提供的缓慢、无需照看的涓流能量。

## Obtaining

**合成**（有序）：Nerosteel 框架包围一个 Nerosium 块，并接上红石——

```text
N N N
N B N
N R N
```

`N` = Nerosteel锭 · `B` = Nerosium块 · `R` = 红石

## How it works

- **核心槽位**（通过界面、手动或漏斗／管道供给）：粗Nerosium、Nerosium锭或 Nerosium粉。
- 一个粗Nerosium、Nerosium锭或 Nerosium粉可运行 **24,000 刻（20 分钟）**，以 **8 FE/t** 的速率产能，存入 **100,000 FE** 的缓存。
- \*\*自动推送：\*\*它会向任何与之接触的机器或电池推送电力。管道也可以将电力抽出。 A pipe can also pull it out.
- Weaker than the Combustion Generator but ideal as set-and-forget base power.
- 一台 [Nerosium研磨机](Nerosium-Grinder.md) 耗电 20 FE/t，因此一台被动发电机能让研磨机以约 40% 的速度运行。作为起步已经足够。 That is fine for a start.

## 详细信息

- ID：`nerospace:passive_generator` · 工具：镐，铁级 · 掉落：自身
- 配置：产出随 `energyRateMultiplier` 缩放。参见[配置](Configuration.md)。 See [Configuration](Configuration).

# 燃烧发电机

> 本页为机器翻译草稿，仅供参考，以英文版本为准：[English](../../Combustion-Generator.md)。

<!-- nerospace:render -->
<p align="right"><img src="../../images/combustion_generator.gif" alt="Combustion Generator" width="150" align="right"></p>
<!-- /nerospace:render -->

燃烧燃料，为管道网络产生能量（FE）。

## 获取方式

**合成**（有序）：Nerosteel 框架包围一个熔炉，并接上红石——

```text
N N N
N F N
N R N
```

`N` = Nerosteel锭 · `F` = 熔炉 · `R` = 红石

## 工作原理

- **燃料槽位**（通过界面、手动或漏斗／管道供给）：煤炭、木炭、煤炭块、烈焰棒或火箭

  燃料罐。

- 燃烧时以 **20 FE/t** 的速率产能，存入 **100,000 FE** 的缓存。它只在缓存未满时燃烧燃料，

  因此不会浪费燃料。

- 燃烧时间：煤炭或木炭 **1,600 刻**，煤炭块 **16,000 刻**，烈焰棒 **2,400 刻**，

  火箭燃料罐 **4,000 刻**。

- **自动推送：**它会向任何与之接触的机器或电池推送电力。无需线缆。
- 通用管道（能量层）也可以将电力抽出。总输出最高可达 **1,280 FE/t**。

- 界面显示电力缓存和燃烧进度；根据其电量发出比较器信号。

## 详细信息

- ID：`nerospace:combustion_generator` · 工具：镐，铁级 · 掉落：自身
- 配置：产出随 `energyRateMultiplier` 缩放。参见[配置](Configuration.md)。

# 燃料精炼厂

<!-- nerospace:render -->

<p align="right"><img src="images/fuel_refinery.gif" alt="Fuel Refinery" width="150" align="right"></p>
<!-- /nerospace:render -->

将煤炭 + 烈焰粉 + 电网能量精炼为可通过管道输送的液态火箭燃料。

## 概述

The Fuel Refinery is the **logistics-grade** way to make rocket fuel. 燃料精炼厂是制造火箭燃料的**物流级**方式。无需手工合成燃料罐，只需为它供给原始**煤炭**（或木炭）、**烈焰粉**和**电网电力**，它就会在内部储罐中产出液态**火箭燃料**，[通用管道](Universal-Pipe.md)可以将其直接抽入[燃料储罐](Fuel-Tank.md)或停在发射台上的火箭。在你第一次发射之前就可以合成。 It is craftable before your first launch.

## Obtaining

**合成**（有序）：

```text
N G N
N F N
N R N
```

`N` = Nerosteel锭 · `G` = 玻璃 · `F` = 熔炉 · `R` = 红石

（材料基于标签，因此其他模组中等效的玻璃／红石也可以使用。）

## How it works

- **输入：**一个**碳**槽位（煤炭或木炭）和一个**催化剂**槽位（烈焰粉）。漏斗和 Hoppers and

  管道通过物品能力为其供给——煤炭会自动进入碳槽位，烈焰粉自动进入催化剂槽位。

- **Power:** grid power only. It holds **40,000 FE** and accepts up to 1,000 FE/tick. Feed it from a generator or Battery over

  [Universal Pipes](Universal-Pipe).

- \*\*精炼：\*\*每批消耗 **1 个煤炭 + 1 份烈焰粉 + 约 4,000 FE**，耗时约 100 刻，产出

  **2,000 mB** 火箭燃料——与两个手工合成的燃料罐提供的燃料相同，却无需铁锭，也无需
  反复点击。

- \*\*输出：\*\*内部 **8,000 mB** 储罐暴露流体能力，因此管道可以将燃料运走。

- \*\*自动推送：\*\*它会自行向任何与之接触的储罐或管道推送最多 **200 mB/t** 的燃料。

- \*\*比较器：\*\*根据其燃料量发出相应强度的红石信号。

`fuelCostMultiplier` 缩放电力消耗（FE/t），`machineSpeedMultiplier` 缩放周期时间。`energyRateMultiplier` 不适用。参见[配置](Configuration.md)。
`energyRateMultiplier` does not apply. See [Configuration](Configuration).

## 详细信息

- ID：`nerospace:fuel_refinery` · 工具：镐，铁级 · 掉落：自身

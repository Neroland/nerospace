# 燃料储罐

<!-- nerospace:render -->

<p align="right"><img src="images/fuel_tank.png" alt="Fuel Tank" width="150" align="right"></p>
<!-- /nerospace:render -->

储存火箭燃料，并自动为停在相邻发射台上的火箭加注燃料。

## 概述

燃料储罐是发射台自动化的第一步：它储存大量**火箭燃料**，并每刻将其泵入停在相连\*\*[火箭发射台](Rocket-Launch-Pad.md)\*\*上的火箭——这样你就不必用燃料罐手动为火箭加注。

## Obtaining

**合成**（有序）：

```text
N G N
G C G
N G N
```

`N` = Nerosteel锭 · `G` = 玻璃 · `C` = 火箭燃料罐

## How it works

- \*\*容量：\*\*32,000 mB 火箭燃料。

- **注入：**手持**火箭燃料桶**或**燃料罐**右键点击即可添加燃料（返还空桶）；

  returned); a fluid pipe can fill it via the fluid capability. Empty-hand right-click opens the
  **Fuel Tank screen**.

- **抽出：**手持**空桶**右键点击可取出一桶**火箭燃料桶**。

- \*\*燃料罐自动供给：**漏斗和[通用管道](Universal-Pipe.md)可以通过储罐的物品能力放入**火箭燃料

  罐\*\*——每个燃料罐会在下一刻转换为 **1,000 mB** 燃料。
  将其与[燃料精炼厂](Fuel-Refinery.md)（或一条燃料罐生产线）搭配，即可实现完全自动化加注。
  Pair it with a [Fuel Refinery](Fuel-Refinery) (or a canister line) for fully automated fuelling.

- \*\*自动加注：\*\*当相邻发射台上停有火箭时，储罐会向其泵送燃料。完整的 A

  complete **3×3 launch pad** pumps **4× faster** (160 vs 40 mB/tick). On a
  **[Heavy Launch Complex](Launch-Gantry)** (5×5 + gantry) the rate rises to **480 mB/t** (12×),
  filling a Tier 4's 24,000 mB tank in under a minute.

- **完全自动化：**发射台本身也会将火箭的**燃料进口槽位**代理为物品

  能力，因此漏斗／[通用管道](Universal-Pipe.md)可以将火箭燃料桶和燃料罐直接送入火箭——燃料储罐 + 一条物品线即可让发射准备完全无需动手。

- \*\*比较器：\*\*根据其储量发出相应强度的红石信号。

## 详细信息

- ID：`nerospace:fuel_tank` · 工具：镐，铁级 · 掉落：自身

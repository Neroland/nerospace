# 地形改造监视器

<!-- nerospace:render -->

<p align="right"><img src="images/terraform_monitor.gif" alt="Terraform Monitor" width="150" align="right"></p>
<!-- /nerospace:render -->

用于深度地形改造的读数方块：显示最近的[地形改造器](Terraformer.md)的各阶段半径、补水缓存和停滞原因，并通过比较器报告**当地地面的地形改造阶段**。

## Obtaining

**合成**（有序）：

```text
N G N
Q R Q
N N N
```

`N` = Nerosteel锭 · `G` = 玻璃 · `Q` = Xertz石英 · `R` = 红石

## How it works

- **可放置在**已改造土地上或其附近的**任何位置**。它会自动连接 **32 格**内最近的地形改造器 It links to the nearest Terraformer within

  **32 blocks** automatically (no wiring).

- **GUI readout:** the local column's stage (Dead / Rooted / Hydrated / Living), the linked

  machine's three stage radii, its hydration units, and a red "Needs glacite" warning when the
  water stage has stalled.

- \*\*比较器输出 = 当地阶段：\*\*0（荒芜）、5（扎根）、10（湿润）、15（生机）。在土地变为生机时打开 Open the

  牧场大门，或在地形改造蔓延到你的基地时发出警报。

## 详细信息

- ID：`nerospace:terraform_monitor` · 工具：镐，铁级 · 掉落：自身

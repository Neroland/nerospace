# 地形改造监视器

> 本页为机器翻译草稿，仅供参考，以英文版本为准：[English](../../Terraform-Monitor.md)。

<!-- nerospace:render -->
<p align="right"><img src="../../images/terraform_monitor.gif" alt="Terraform Monitor" width="150" align="right"></p>
<!-- /nerospace:render -->

用于深度地形改造的读数方块：显示最近的[地形改造器](Terraformer.md)的各阶段半径、补水缓存和停滞原因，并通过比较器报告**当地地面的地形改造阶段**。

## 获取方式

**合成**（有序）：

```text
N G N
Q R Q
N N N
```

`N` = Nerosteel锭 · `G` = 玻璃 · `Q` = Xertz石英 · `R` = 红石

## 工作原理

- **可放置在**已改造土地上或其附近的**任何位置**。它会自动连接 **32 格**内最近的地形改造器

  （无需接线）。

- **界面读数：**当地这一列的阶段（荒芜／扎根／湿润／生机），所连接机器的三个阶段半径、

  其补水单位，以及当水阶段停滞时显示的红色“需要 Glacite”警告。

- **比较器输出 = 当地阶段：**0（荒芜）、5（扎根）、10（湿润）、15（生机）。在土地变为生机时打开

  牧场大门，或在地形改造蔓延到你的基地时发出警报。

## 详细信息

- ID：`nerospace:terraform_monitor` · 工具：镐，铁级 · 掉落：自身

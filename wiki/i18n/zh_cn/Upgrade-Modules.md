# 升级模块与框架外壳

The crafting parts and tuning cards for the [Quarry Controller](Quarry-Controller). [采石场控制器](Quarry-Controller.md)的合成部件和调节卡。这些模块是一个**跨机器**系统——设计上让其他机器将来也能使用同样的卡。

## 框架外壳

The structural material the quarry spends to build its frame ring — **one casing per perimeter
cell** (terrain on the ring is replaced by frame; chests and machines on the ring are left in place). 采石场用来建造框架环的结构材料——**每个周边格子一个外壳**（框架环上的地形会被替换为框架；框架环上的箱子和机器会保留原位）。外壳也可以作为框架方块**放置**，因此你可以手动勾勒挖掘区域，而不使用标记；被玩家破坏的框架方块会掉落其外壳，而**挖掘完成后，仍立着的外壳会返还**到控制器的框架槽位中。

**合成**（有序，产出 4 个）：

```text
I I I
I   I
I I I
```

`I` = [Nerosteel锭](Items.md)

## 升级模块

Slot module cards into a machine's module slots to change how it works. A machine counts the modules
across its slots and sums their effects (you can stack several). The [Quarry Controller](Quarry-Controller)
has **1 module slot at Tier 1** (more at higher tiers).

| 模块         | 效果                           | 说明                                                                             |
| ---------- | ---------------------------- | ------------------------------------------------------------------------------ |
| **速度模块**   | 每个模块 +50% 工作速度               | Capped at ×8. 上限 ×8。单靠电力永远不会让机器变快——只有速度模块可以（且它们不会改变每个方块的能量消耗）。 |
| **效率模块**   | 每个模块 −15% 能量消耗               | Floors at 25% of the base cost.                                |
| **精准采集模块** | 对挖掘的方块应用时运                   | Stacks up to Fortune III.                                      |
| **时运模块**   | Mines blocks with Silk Touch | 存在时**覆盖**时运。                                                                   |
| **蒸发模块**   | 销毁液体而不是缓存它们                  | 保持采石场的流体缓存为空——无需用管道抽走任何东西。                                                     |

**合成**——所有模块共用一个框架，中心放置标志性物品：

```text
 N
R S R
 N
```

`N` = [Nerosteel锭](Items.md) · `R` = 红石 · `S` = 标志物品：

| 模块         | 标志物品 `S`     |
| ---------- | ------------ |
| 速度         | 糖            |
| 效率         | Lapis Lazuli |
| Fortune    | 钻石           |
| Silk Touch | 青金石          |
| 蒸发         | 烈焰粉          |

## 详细信息

- ID：`nerospace:frame_casing`、`nerospace:speed_module`、`nerospace:efficiency_module`、

  `nerospace:fortune_module`、`nerospace:silk_touch_module`、`nerospace:evaporator_module`

- 使用者：[采石场控制器](Quarry-Controller.md)（计划支持更多机器）

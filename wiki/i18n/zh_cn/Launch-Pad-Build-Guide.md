# 发射台建造指南

<!-- nerospace:render -->

<p align="right"><img src="images/rocket_launch_pad.png" alt="Rocket Launch Pad" width="150" align="right"></p>
<!-- /nerospace:render -->

为每个火箭等级建造发射台的分步讲解，从首次飞行用的单个方块到重型发射综合设施。完整的方块参考参见[火箭发射台](Rocket-Launch-Pad.md)和[发射塔架](Launch-Gantry.md)。 For the full block reference see
[Rocket Launch Pad](Rocket-Launch-Pad) and [Launch Gantry](Launch-Gantry).

## At a glance

每个火箭等级都需要更大的发射台。更大的发射台总是适用于更小的火箭。 A bigger pad always works for a smaller rocket.

| 发射台等级 | 建造内容                         | Launches     | 解锁         |
| ----- | ---------------------------- | ------------ | ---------- |
| **1** | 一个发射台方块                      | 1 级火箭        | 轨道空间站      |
| **2** | 完整的 3×3 发射台                  | 1–2 级火箭、货运火箭 | Greenxertz |
| **3** | 由 16 个空间站墙壁围绕的 3×3 发射台       | 1–3 级火箭      | Cindara    |
| **4** | 5×5 发射台加一个发射塔架（**重型发射综合设施**） | 所有火箭         | Glacira    |

1. 合成你想要的等级所需的部件。
2. 将它们铺设在平地上，**全部位于同一高度**。
3. Right-click a pad block with an **empty hand** to check which tier it reads as.
4. 手持火箭右键点击发射台进行部署，然后加注燃料。

## 1. 合成部件

| 部件                                | 配方                                                  | 产出 |
| --------------------------------- | --------------------------------------------------- | -- |
| [火箭发射台](Rocket-Launch-Pad.md)     | 8 个Nerosteel锭围绕一个 [Nerosium块](Block-of-Nerosium.md) | 1  |
| [Nerosium块](Block-of-Nerosium.md) | 9 个Nerosium锭                                        | 1  |
| [空间站墙壁](Station-Wall.md)          | 8 个Nerosteel锭围绕一个铁锭                                 | 8  |
| [发射塔架](Launch-Gantry.md)          | 6 个Nerosteel锭、铁栏杆和一个空间站墙壁（参见其页面）                    | 1  |

**从零开始，每个等级的花费：**

| 发射台等级 | 方块                 | Nerosteel锭 | Nerosium锭 | 其他           |
| ----- | ------------------ | ---------- | --------- | ------------ |
| 1     | 1 个发射台             | 8          | 9         | —            |
| 2     | 9 个发射台             | 72         | 81        | —            |
| 3     | 9 个发射台 + 16 个空间站墙壁 | 88         | 81        | 2 个铁锭        |
| 4     | 25 个发射台 + 1 个发射塔架  | 214        | 225       | 1 个铁锭、1 个铁栏杆 |

> **Tip:** every tier reuses the one before it. \*\*提示：\*\*每个等级都会复用上一级。从 2 级升到 3 级只需花费 16 个墙壁，而升级到 4 级时，你可以把这些墙壁拆回来（其中一个会用于塔架）。

## 2. 选择位置

- **Flat and level.** Pad blocks only join up when they sit **side by side at the same height**.
  Blocks touching only at a corner, or one block higher, don't count.
- **保持分隔。**每个与其他发射台方块接触的方块都会成为同一个发射台，而一个发射台只能停**一枚火箭**。想用于不同火箭的发射台之间至少要留一格空隙。 Leave at least a one-block gap between pads you want to use for different rockets.
- \*\*留出呼吸空间。\*\*在发射台约 6 格范围内你可以呼吸，因此刚抵达的玩家不会在着陆时窒息。更远的地方需要[氧气发生器](Oxygen-Generator.md)基地。 Anything further out needs an [Oxygen Generator](Oxygen-Generator) base.
- **在轨道空间站上**已经为你建好了一个 3×3 发射台。

## 3. 建造你的等级

下方布局图的图例（俯视）：`P` = 发射台，`W` = 空间站墙壁，`G` = 发射塔架，`F` = [燃料储罐](Fuel-Tank.md)（可选），`.` = 任意。

### 1 级：一个方块

```text
P
```

Place one Launch Pad. 放置一个发射台。这足以支持 1 级火箭和你第一次前往轨道空间站的旅程。

### 2 级：完整的 3×3

```text
P P P
P P P
P P P
```

All nine blocks, no gaps. Extra pad blocks attached to the side are fine, as long as a complete 3×3
square is in there.

### 3 级：由空间站墙壁围绕的 3×3

```text
W W W W W
W P P P W
W P P P W
W P P P W
W W W W W
```

Surround the 3×3 with **16 [Station Walls](Station-Wall)** at the **same height as the pads**,
corners included. 用 **16 个[空间站墙壁](Station-Wall.md)**在**与发射台相同的高度**上围住 3×3，包括四个角。16 个必须全部是空间站墙壁：只要有一个空缺或其他方块，发射台就会退回 2 级。

> 墙壁围圈使发射台旁边没有空位，因此燃料储罐无法放在 3 级发射台旁边。请改为从下方加注燃料（参见[加注燃料](#5-fuel-it)）。 Fuel
> it from below instead (see [Fuel it](#5-fuel-it)).

### 4 级：重型发射综合设施

```text
. . . G . . .
. P P P P P .
. P P P P P .
. P P P P P .
. P P P P P .
. P P P P P .
. . . F . . .
```

A complete **5×5** of Launch Pads, plus at least one **[Launch Gantry](Launch-Gantry)** right next to
the square at pad level. No Station Walls are needed.

- **Put the gantry in the middle of a side**, touching a pad block. A gantry on a corner still forms
  the complex, but it isn't touching any pad, so right-clicking it won't board the rocket.
- \*\*从 3 级升级：\*\*拆掉 16 个墙壁，用发射台填满这些位置组成 5×5，然后加上塔架。
- 重型综合设施其他任意一侧的燃料储罐都会以最快的速率泵送。

## 4. 检查并部署火箭

\*\*发射台会告诉你何时就绪。\*\*当你放置的方块完成某个等级时（3×3 的最后一个发射台、第 16 个空间站墙壁或塔架），快捷栏上方会出现一条消息，发射台会发出提示音并闪烁：

> ✔ 发射台就绪：**3 级**——放置一枚火箭

There is nothing else to switch on. Once you see that message, all that's left is to place a rocket.

**Right-click any pad block with an empty hand** to check it at any time. **空手右键点击任意发射台方块**即可随时检查。聊天栏会显示发射台的等级和大小，确认它可以放置火箭，并说明接下来要建造什么：

> 发射台 **2 级** · 9 个方块
> ✔ 就绪——放置一枚火箭（最高 **2 级**）
> 下一步：用 16 个空间站墙壁围住 3x3 以达到 3 级

Don't sneak while you do this: sneaking with an empty hand on a pad you named as a travel stop
removes it from the travel list.

**Deploy:** right-click the pad with the rocket item. The rocket stands in the centre of the 5×5 (or
the 3×3, if there's no 5×5). You can deploy on any pad, even a half-built one, and finish building
around the rocket. The size check only happens **at launch**. If the pad is too small, chat tells you
the tier it reads as. Still, it's easiest to finish the pad first so the rocket lands in the centre.

## 5. 加注燃料

| Pad             | 燃料储罐泵送速率          |
| --------------- | ----------------- |
| 不完整的发射台（不是完整方阵） | 每刻 40 mB          |
| 完整的 3×3         | 每刻 160 mB（快 4 倍）  |
| 重型发射综合设施        | 每刻 480 mB（快 12 倍） |

- **[Fuel Tank](Fuel-Tank):** place it beside any pad block, at pad level. It pumps into whichever
  rocket stands on the pad.
- \*\*[通用管道](Universal-Pipe.md)：\*\*任意发射台方块都接受火箭燃料（流体层）、氧气（气体层）和火箭燃料罐（物品层），并将它们送入停靠的火箭。这就是为 **3 级**发射台加注燃料的方式：从下方将管道接到发射台方块上。 This is the
  way to fuel a **Tier 3** pad: run the pipe up to a pad block from underneath.
- \*\*手动：\*\*手持火箭燃料罐右键点击火箭。

前往另一个世界的行程消耗 **2,500 mB**。请携带备用燃料罐，以便飞回家。 Carry spare canisters so you can fly home.

## 捷径：发射控制器

[发射控制器](Launch-Controller.md)可以为你建造发射台。

1. Place the controller facing open, flat ground. 将控制器朝向一片开阔平坦的地面放置。它会在**其前方**建造发射台，中心位于**6 格之外**，与控制器同高。
2. 将发射台、空间站墙壁和发射塔架装入它的三个槽位。
3. Pick a tier (T1–T4). The readout shows how many of each block are still needed.
4. 按下**切换全息投影**预览布局，然后按**建造发射台**。

It builds on top of what's there, so you can upgrade a pad tier by tier without tearing it down. For
Tier 4 it swaps the wall ring for pad blocks and puts the gantry in the middle of the far side. The
controller also stores fuel, oxygen and power and can launch the rocket from its screen.

## 故障排除

- **“此发射台被识别为 X 级，但这是一枚 Y 级火箭”** → 火箭下方的方阵不完整。空手右键点击发射台查看缺少什么。方阵必须是火箭所站的那一个，而不是同一发射台上其他位置的方阵。 Right-click a pad with an empty hand to see what's missing. The square has to be
  the one the rocket is standing on, not another one elsewhere on the same pad.
- **发射台被识别的等级低于预期** → 检查每个方块是否都在同一高度、是否缺少发射台方块，以及 3 级围圈是否包括四角在内的全部 16 个空间站墙壁。
- **"Pad occupied"** → the pad already has a rocket. Two pads touching each other count as one pad.
- **已部署的火箭不能再发射了** → 其下方的发射台方块、墙壁或塔架被破坏了。把它放回去。
  Put it back.
- **The gantry won't let you board** → it's on a corner. Move it to the middle of a side.
- **货运火箭**需要一个位于完整 3×3 之中的[货运平台](Cargo-Pad.md)。货运平台算作九个发射台方块之一。 The Cargo Pad counts as one of
  the nine pad blocks.

---

另请参阅：[火箭发射台](Rocket-Launch-Pad.md)、[发射塔架](Launch-Gantry.md)、[发射控制器](Launch-Controller.md)、[空间站墙壁](Station-Wall.md)、[燃料储罐](Fuel-Tank.md)、[入门指南](Getting-Started.md)。

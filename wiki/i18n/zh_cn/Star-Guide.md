# 星际指南

<!-- nerospace:render -->

<p align="right"><img src="images/star_guide.gif" alt="Star Guide" width="150" align="right"></p>
<!-- /nerospace:render -->

游戏内进度指南——一个展台、一本书和一棵实时任务树。

## 概述

星际指南是 Nerospace 内置的教程和进度追踪器：一棵包含 **10 个章节、45 个步骤**的交互式任务树，从你的第一块 Nerosium 矿石一直到自动化的星际补给线。完成情况通过本模组的**进度树**按玩家追踪，因此指南始终是实时的——无论在哪里完成某件事，任务树都会亮起。
Completion is tracked per player through the mod's **advancement tree**, so the guide is always
live — finish something anywhere and the tree lights up.

## Obtaining

**Star Guide Book** (shapeless): **Book + Raw Nerosium** — available minutes into a playthrough.
A **Nerosium Ore** or **Deepslate Nerosium Ore** block also works in place of the Raw Nerosium.

**星际指南**展台（有序）：

```text
. R .
. P .
S S S
```

`R` = 粗Nerosium（或一个 Nerosium矿石方块） · `P` = 任意木板 · `S` = 圆石

## How it works

- **Read anywhere:** right-click with the **Star Guide Book** in hand to open the guide GUI.

- \*\*展台：\*\*手持书右键点击空展台即可将书放上（类似讲台；破坏

  the pedestal or sneak-right-clicking pops the book back out). An installed pedestal projects a
  rotating **hologram of your next goal**, and right-clicking it opens the same GUI.

- \*\*任务树：\*\*各章节涵盖 \*\*Nerosium → 机器 → 电网 → 火箭技术 → 新世界 →

  [采矿](Quarry-Controller.md) → 真空求生 → 地形改造 → 陨石事件 →
  [货运](Cargo-Pad.md)\*\*。已完成的步骤会闪烁；每个步骤
  都附有指南文字，准确告诉你下一步该做什么。 Completed steps pulse; each step
  carries guide text telling you exactly what to do next.

- \*\*采矿章节：\*\*采石场自动化路线——[采石场标记](Quarry-Landmark.md) →

  [Frame Casing](Upgrade-Modules) → [Quarry Controller](Quarry-Controller) →
  [Upgrade Modules](Upgrade-Modules). Gated behind nerosteel (Greenxertz), since the frame is built
  from it.

- \*\*比较器：\*\*放有书时展台会发出信号。

## 详细信息

- ID：`nerospace:star_guide`（方块） · `nerospace:star_guide_book`（物品）

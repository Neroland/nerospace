# 行星重力

Every world in Nerospace pulls with its **own gravity**. Nerospace 中的每个世界都有**自己的重力**。主世界感觉正常，**轨道空间站**几乎失重，而各行星介于两者之间——因此你能跳多高、下落多快、扔出的物品能飞多远，都会随你所在的位置而变化。

## 重力速览

Values are relative to normal Overworld gravity (`1.0×`). These are the current defaults and are fully
adjustable with the [`gravityMultiplier`](Configuration) config option.

| 维度             | 重力      | 感受                |
| -------------- | ------- | ----------------- |
| 主世界            | `1.0×`  | 正常。               |
| **轨道空间站**      | `~0.1×` | 近乎失重——长时间、轻飘飘的跳跃。 |
| **Greenxertz** | `~0.6×` | 较轻——跳得明显更高，落地更轻柔。 |
| **Cindara**    | `~0.8×` | 略低——比家园稍微轻飘一点。    |
| **Glacira**    | `~0.4×` | 低——在冰面上大步而缓慢地跳跃。  |

重力越低，**跳得越高**、**下落越慢**，因此你受到的**摔落伤害也越少**——在主世界会受伤的长距离坠落，在 Glacira 上可以幸存。

## 它会影响什么

重力作用于**所有会下落的东西**，而不仅仅是你：

- **You and mobs** — movement, jump height, and fall speed/damage all scale. Native creatures bound
  around their low-gravity worlds the same way you do.
- **掉落物、箭、下落的方块、TNT 和经验球**——它们在低重力世界中会悬浮飘移，在高重力世界中则更快落定。
- **陨石**——坠落的[陨石](Meteor-Events.md)在低重力世界中**下降得更慢**，拉长其炽热的弧线。

由于重力是通过游戏自身的重力属性（对你和生物）施加的，移动保持流畅灵敏——在世界之间穿梭时不会出现延迟或回弹。

## 按生物群系变化

行星的重力是其**基准值**，但个别**生物群系**的引力可能比所在世界的默认值更轻或更重——陨石坑底部可能近乎失重，而致密的盆地感觉几乎正常。生物群系重力由数据驱动（数据包生物群系标签：`gravity_micro`、`gravity_low`、`gravity_high`），因此整合包无需代码即可调整或添加区域。 Biome gravity is data-driven (datapack biome tags: `gravity_micro`, `gravity_low`,
`gravity_high`), so packs can tune or add zones without code.

## 地形改造恢复正常重力

Reclaimed ground pulls its weight. As a [Terraformer](Terraformer) converts a dead, low-gravity
planet into living land, that **terraformed ground returns to normal, Earth-like gravity** — so a
fully terraformed base feels like home even on a moon that started out floaty. (An explicit biome
gravity tag, if a pack sets one, takes priority over this.)

## 配置

- 数值相对于正常的主世界重力（`1.0×`）。这些是当前默认值，可以通过 [`gravityMultiplier`](Configuration.md) 配置项完全调整。 Set it below `1` for an even floatier game everywhere, or above `1` for a heavier one.

## 在游戏中查看重力

创造模式／OP 命令 **`/nerospace gravity`** 会报告你所站位置的有效重力——数值、来源（生物群系、改造后的地面或行星默认值）以及你当前的重力属性。在调整整合包或提交错误报告时很方便。 Handy for tuning a pack or filing a bug report.

## 详细信息

- 来源优先级：**生物群系重力标签 → 改造后的地面（正常） → 行星默认值 → 正常**，然后按 `gravityMultiplier` 缩放。
- 影响：玩家、生物、掉落物、箭、下落的方块、已点燃的 TNT、经验球以及坠落的陨石。
- 生物群系标签：`nerospace:gravity_micro`、`nerospace:gravity_low`、`nerospace:gravity_high`。

# 氧气服

四件套护甲，在没有空气的世界中兼作个人生命维持系统。分为两个等级，另有两种用于极端世界的 2 级**危害变体**。 Two tiers, plus two
Tier-2-class **hazard variants** for the extreme worlds.

## 概述

穿着**全套**（全部四件）时，你在外星世界会消耗一个有限的空气罐来保持呼吸，而不会窒息。在任何可呼吸区域（氧气区域、改造后的地面、发射台安全区）中，空气罐会立即补满——自从氧气服与空间站集成后，在**气闸**处也可以补充（见下文）。 The tank refills instantly in any breathable zone (oxygen field,
terraformed ground, launch-pad safe zone) — and, since the suit-and-station integration, also at
**airlocks** (below).

|      | 1 级氧气服       | 2 级氧气服       |
| ---- | ------------ | ------------ |
| 空气罐  | 900          | 900          |
| 气闸补充 | 90 空气 / 10 刻 | 90 空气 / 10 刻 |
| 防护   | 钻石级          | 略强，+韧性       |
| 修复材料 | Nerosteel锭   | Cindrite     |

Every full suit (Tier 1, Tier 2, Thermal or Cryo) holds **900 air**. Without a suit you have only
**300 air** (your own lungs). A **mixed** set still works as life support. Any missing piece = no
life support.

**Drain:** with a full suit you lose **3 air** per check (every 10 ticks). With no suit you lose
**30 air** per check. An unshielded hazard (below) multiplies drain by **4**.

## 危害变体——耐热服与低温服

Cindara **炎热**，Glacira **寒冷**：没有防护时，行星危害会让你的氧气服超负荷工作——氧气以正常速率的 **4 倍**消耗（没有额外伤害；耗尽空气仍然是危险所在）。暴露时 HUD 徽章会以红色的**炎热！**／\*\*寒冷！\*\*发出警告，并伴有余烬飘散（Cindara）或蔓延的霜冻暗角（Glacira）作为反馈。 The HUD badge warns with a red **HEAT!** / **COLD!** while you're exposed, with ember
puffs (Cindara) or a creeping frost vignette (Glacira) as feedback.

|                                      | **耐热服**                  | **低温服**                 |
| ------------------------------------ | ------------------------ | ----------------------- |
| 防护                                   | Cindara 高温               | Glacira 严寒              |
| Craft (per piece) | 2 级部件 + 4 个 **Cindrite** | 2 级部件 + 4 个 **Glacite** |
| 修复材料                                 | Cindrite                 | Glacite                 |
| 等级                                   | 2 级（空气罐 900）             | 2 级（空气罐 900）            |

只有当**全部四件都与危害匹配**时，防护才会生效；此时 HUD 徽章显示**服装耐热**／**服装耐寒**，氧气以正常的 1 倍速率消耗。将变体部件与其他部件混搭仍可维持生命——只是会失去危害防护。 Mixing variant pieces with
other pieces keeps life support — you just lose the hazard shield.

## Progression note

Cindrite（耐热）在 Cindara 上开采，因此通常的流程是：穿着普通的 2 级氧气服冲进去，开采你的第一批 Cindrite，然后合成耐热服进行正式探险。Glacira 上的 Glacite 和低温服也是同理。 The same applies to
glacite and the Cryo Suit on Glacira.

## 气闸补充

在**装有氧气**的[气体储罐](Gas-Tank.md)或[氧气发生器](Oxygen-Generator.md)的 **4 格**范围内，穿着的氧气服会从该储存中补充。每套氧气服每次检查（每 10 刻）获得 **90 空气**。每单位空气消耗 **2 mB** 氧气（按 `fuelCostMultiplier` 缩放）。因此基地入口处的气体储罐就相当于一个气闸：走进去，补满，再出发。创造气体储罐也可以。 Every suit gets **90 air** per check (every 10 ticks).
Each air unit costs **2 mB** of oxygen (scaled by `fuelCostMultiplier`). A Gas Tank at the base entrance
therefore acts as an airlock: step inside, top up, head back out. The Creative Gas Tank works too.

## Obtaining

**1 级**（有序，Nerosteel）：头盔 `NNN / NGN`（G = 玻璃面罩），胸甲 `N N / NCN / NNN`（C = 火箭燃料罐生命维持核心），护腿和靴子全部用 Nerosteel。

**2 级**（有序）：每件 1 级部件被 4 个 **Cindrite** 包围——

```text
. C .
C P C
. C .
```

`P` = 对应的 1 级部件 · `C` = Cindrite

Cindrite 只能在 **Cindara** 上开采（需要 3 级火箭，而它本身又受[空间站墙壁](Station-Wall.md)限制），因此 2 级氧气服与 3 级火箭处于同一进度深度。

## 详细信息

- ID：`nerospace:oxygen_suit_{helmet,chestplate,leggings,boots}`、

  `nerospace:oxygen_suit_t2_{...}`、`nerospace:oxygen_suit_heat_{...}`（耐热）、
  `nerospace:oxygen_suit_cold_{...}`（低温）

- 配置：`oxygenCapacityMultiplier`（空气容量）和 `oxygenDrainMultiplier`（消耗速度）。参见 See

  [配置](Configuration.md)。

# 配置

> 本页为机器翻译草稿，仅供参考，以英文版本为准：[English](../../Configuration.md)。

Nerospace 的配置有意保持精简。本模组将基础平衡数值保存在代码中。整合包和服务器通过**六个倍率**来调整它们。其他键都是简单的开关和限制。

> **由 [Neroland Core](Neroland-Core.md) 管理。**配置文件为 `config/nerospace.properties`。它归 Core 所有。用 `/neroland config reload` 实时重载（无需重启）。大多数键都是**服务器权威的**：当你加入服务器时，你的游戏会使用服务器的值。唯一的例外是 `telemetryEnabled`。它是每个客户端的个人选择，服务器永远无法更改。现有文件会就地迁移。
>
> **只有本页列出的键存在。**旧版维基页面和旧版本列出了更多的键（氧气区域、大气、客户端视觉效果、地形改造器和陨石相关的键）。这些键已被移除。它们过去设定的值现在都固定在代码中。

## 六个倍率

六个倍率默认均为 `1.0`。每个都可以设为 **0.1 到 10.0**。每个缩放后的值都至少保持为 1，因此极端设置永远不会让机器停止运转或破坏进度。

| 键 | 缩放的内容 | 基础值（1.0 时） |
| --- | --- | --- |
| `energyRateMultiplier` | 发电机产生的 FE/t：燃烧发电机、被动发电机和太阳能板。它也会缩放太阳能板的缓存。它**不会**改变燃料储罐或研磨机的缓存。 | 燃烧 20 FE/t · 被动 8 FE/t · 太阳能 20 / 100 / 400 FE/t（1 / 2 / 3 级） |
| `oxygenDrainMultiplier` | 你不在安全区时空气消耗的速度。越高 = 越严苛。 | 不穿氧气服每次检查 30 空气 · 全套氧气服每次检查 3 空气（一次检查 = 10 刻） · 未防护的危害中 ×4 |
| `oxygenCapacityMultiplier` | 玩家和氧气服可容纳的空气量。 | 无氧气服 300 · 任意全套氧气服 900 |
| `fuelCostMultiplier` | 燃料和能量成本：每次火箭发射的燃料（永远不超过燃料箱容量）、气闸处每单位空气的氧气，以及机器能量成本。也包括货运燃料。 | 发射：前往另一个维度 2,500 mB，或同一维度内 500 mB + 每 100 格 60 mB · 气闸每单位空气 2 mB · 研磨机 20 FE/t · 氧气发生器每 mB 20 FE |
| `machineSpeedMultiplier` | 机器的工作速度。越高 = 越快（工作时间越短）。 | 研磨机每个物品 200 刻 |
| `gravityMultiplier` | 对每个世界[重力](Planet-Gravity.md)的全局缩放：玩家、生物、物品、弹射物和坠落的陨石。低于 1 = 更轻飘，高于 1 = 更沉重。 | 空间站 ~0.1× · Glacira ~0.4× · Greenxertz ~0.6× · Cindara ~0.8× · 主世界和改造后的地面 1.0× |

示例：`oxygenDrainMultiplier = 2.0` 会使空气只能维持一半的时间。`machineSpeedMultiplier = 2.0` 会使研磨机在 100 刻内完成一个物品。`energyRateMultiplier = 0.5` 会使发电机产出减半。

在 `10.0` 时，机器的输入上限（例如氧气发生器的 1,000 FE/t）可能会先于倍率后的速率成为真正的限制。这是有意为之。

## 固定的基础值

这些数值固定在代码中。在此列出，方便你规划建筑。

| 项目 | 数值 |
| --- | --- |
| 通用管道（每层） | 容纳 8,000 · 最多传输 1,000 FE/t 或 1,000 mB/t |
| 通用管道（物品） | 每格 8 刻 · 每 4 刻抽取 16 个物品 |
| 氧气发生器 | 产出 4 mB/t · 最多接收 1,000 FE/t · 50,000 FE 缓存 · 8,000 mB 储罐 |
| 气闸 | 每次检查（每 10 刻）90 空气 · 半径 4 格 |
| 火箭燃料箱 | 3,000 / 6,000 / 12,000 / 24,000 mB（1 / 2 / 3 / 4 级） |
| 地形改造器 | 每刻最多 48 列 · 最多接收 2,000 FE/t |
| 陨石 | 大约每 9,000 秒一颗 · 30 秒预警 · 距离 200–500 格 · 最多 4 个地点 |

## 其他键

### 遥测

| 键 | 默认值 | 含义 |
| --- | --- | --- |
| `telemetryEnabled` | `true` | 仅限 Nerospace 的匿名崩溃报告（Sentry，欧盟服务器，已按 POPIA/GDPR 清理——参见 `PRIVACY.md`）。设为 `false` 即可选择退出。在配置重载时生效。每位玩家为自己的客户端自行决定。 |

### 地形改造器

| 键 | 默认值 | 含义 |
| --- | --- | --- |
| `terraformerForceLoadEnabled` | `false` | 运行中的[地形改造器](Terraformer.md)会保持一小片区块处于加载状态，使你离开时它也能继续工作。默认关闭，因为它会消耗服务器内存和性能。 |

### NeroLink

| 键 | 默认值 | 含义 |
| --- | --- | --- |
| `linkModuleEnabled` | `true` | 与 NeroLink 配套应用共享 Nerospace 数据：你自己的火箭、空间站、行星、生命维持以及星际指南进度（绝不包括其他玩家的）。设为 `false` 后，应用会显示 Nerospace 不存在。由服务器决定。 |

### 货运火箭

[货运平台](Cargo-Pad.md)和货运航班的设置。全部由服务器决定。

| 键 | 默认值 | 范围 | 含义 |
| --- | --- | --- | --- |
| `cargoPadSlots` | `18` | 1–27 | 货运平台上可用的货舱槽位——一枚货运火箭的载荷。 |
| `cargoFuelCapacity` | `16000` | 1,000–64,000 | 货运平台的管道供给缓存可容纳的火箭燃料（mB）。 |
| `cargoTravelTimeMultiplier` | `1.0` | 0.1–10 | 缩放货运飞行时间（越高 = 越慢）。 |
| `cargoHoldRetrySeconds` | `30` | 5–600 | 等待中的航班多久重新尝试降落一次。 |
| `cargoHoldTimeoutMinutes` | `30` | 1–1,440 | 航班等待多久后，其货物会作为货运箱掉落在目的地（绝不删除）。 |
| `cargoFlightRetentionDays` | `7` | 1–90 | 已完成航班记录保留的游戏内天数。 |
| `cargoMaxFlightsPerOwner` | `16` | 1–128 | 一位玩家同时可拥有的进行中航班数。 |
| `cargoMaxPads` | `256` | 8–4,096 | 最多可注册的货运平台数量。 |

`fuelCostMultiplier` 也会缩放货运燃料。

### 经济（可选——需要 NeroEconomy）

信用点奖励和费用。只有安装了 **NeroEconomy** 模组时才会生效。每个金额默认均为 `0`，即关闭该功能。全部由服务器决定。

| 键 | 默认值 | 范围 | 含义 |
| --- | --- | --- | --- |
| `economyFirstVisitReward` | `0` | 0–1,000,000 | 玩家第一次抵达每颗行星时一次性支付的信用点。从 NeroEconomy 的储备金中支付，并受其每日支付上限限制。被拒绝的奖励不会再次尝试。 |
| `economyLaunchFee` | `0` | 0–1,000,000 | 每次载人或货运发射收取的信用点，在所有其他发射检查通过后扣除。无法支付的玩家无法发射（货运平台会显示*无法支付航班费用*）。 |
| `economyDockingFee` | `0` | 0–1,000,000 | 每次飞往轨道空间站的航班收取的信用点，在发射费之外另收。如果之后发射费被拒绝，对接费会退还。 |

费用进入 NeroEconomy 的储备金。没有 NeroEconomy 时，这些键不起任何作用。

### 采石场

| 键 | 默认值 | 范围 | 含义 |
| --- | --- | --- | --- |
| `quarryMaxSide` | `64` | 4–64 | 采石场标记区域的最长边（格）。 |
| `quarryFrameDecayTicks` | `600` | 20–24,000 | 遗留的框架方块崩解并掉落其外壳之前的基础延迟。每个方块等待 `base + random(base × 7)` 刻，因此在默认值下，框架环会在大约 30 秒到 4 分钟内逐渐瓦解。 |

### 外星村庄

| 键 | 默认值 | 含义 |
| --- | --- | --- |
| `alienRaidsEnabled` | `true` | 敌对生物会在夜间袭击已被认领的外星村庄。设为 `false` 可关闭袭击。在配置重载时生效。 |

### 聊天

| 键 | 默认值 | 含义 |
| --- | --- | --- |
| `welcomeMessageEnabled` | `true` | 玩家加入时发送两行 Nerospace 欢迎消息（星际指南书提示和反馈链接）。设为 `false` 即可静默加入。在配置重载时生效。由服务器决定。 |

## 已移除的键（供整合包作者参考）

这些键已不再存在。如果它们仍在你的文件中，也不会起任何作用。

- 并入倍率或固定在代码中：`atmosphereDamage`, `oxygenMax`, `oxygenDrainPerTick`,
  `oxygenSuitDrain`, `oxygenSuitT2Max`, `oxygenAirlockRadius`, `oxygenAirlockRefillPerCheck`,
  `oxygenAirlockMbPerAir`, `terraformEnergyPerBlock`, `terraformWorkIntervalTicks`,
  `energyPipeCapacity`, `energyPipeThroughput`, `fluidPipeCapacity`, `fluidPipeThroughput`,
  `gasPipeCapacity`, `gasPipeThroughput`, `itemPipeTicksPerBlock`, `itemPipeExtractAmount`,
  `itemPipeExtractPeriod`, `batteryCapacity`, `fluidTankCapacity`, `gasTankCapacity`,
  `combustionGeneratorFePerTick`, `passiveGeneratorFePerTick`.
- 大气和氧气区域：`atmosphereDamageEnabled`, `atmosphereSafeRadius`,
  `oxygenBubbleRadius`, `oxygenMaxConcentration`, `oxygenBreathableThreshold`,
  `oxygenDiffusionRate`, `oxygenDecayPerStep`, `oxygenSimIntervalTicks`,
  `oxygenMaxActiveCellsPerSource`, `oxygenLeakRange`, `oxygenEvaporateSeconds`, `oxygenSyncRadius`.
- 客户端视觉效果：`oxygenVisualQuality`, `oxygenParticleIntensity`, `oxygenHazeIntensity`,
  `oxygenBoundaryIntensity`, `oxygenDebugLog`.
- 地形改造器：`terraformPlantsEnabled`, `terraformWaterEnabled`, `terraformWaterMaxDepth`,
  `terraformDriftEnabled`, `terraformDriftPerSecond`, `terraformFaunaEnabled`,
  `terraformResourcesEnabled`, `terraformResourceOres`, `terraformMaxColumnsPerTick`,
  `terraformForceLoadChunks`, `terraformMaxForcedChunks`, `terraformPlantChance`,
  `terraformResourceChance`.
- 陨石：`meteorNaturalSpawn`, `meteorAvgIntervalSeconds`, `meteorWarningSeconds`,
  `meteorMinDistance`, `meteorMaxDistance`, `meteorCraterRadius`, `meteorMaxActiveSites`,
  `meteorLootBonusRolls`, `meteorDebugLog`.
- 模板遗留：`logDirtBlock`, `magicNumber`, `magicNumberIntroduction`, `items`,
  `oxygenSealedRoomMax`.

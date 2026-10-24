# Neroland Core

Nerospace is built on **Neroland Core** (mod id `nerolandcore`), the shared foundation library for the
Neroland mod ecosystem. Core is a **required dependency** — it must be installed alongside Nerospace and
loads first. Nerospace 构建在 **Neroland Core**（模组 ID `nerolandcore`）之上，这是 Neroland 模组生态的共享基础库。Core 是**必需依赖**——它必须与 Nerospace 一同安装，并且先于其加载。运行 Nerospace 的整合包和服务器需要两个 jar（对应相同的 Minecraft 版本和加载器）；如果没有兼容的 `nerolandcore` `1.0`–`2.0`，启动器将拒绝启动 Nerospace。

基于 Core 构建意味着 Nerospace 不再重复实现每个 Neroland 模组共享的系统。实际上，这为你带来四点好处。 In practice
that gives you four things.

Core 现在还提供**共享存储方块**——[电池](Battery.md)、[流体储罐](Fluid-Tank.md)、[气体储罐](Gas-Tank.md)、[物品存储器](Item-Store.md)和[垃圾桶](Trash-Can.md)（前四者及其[创造](Creative-Source-Blocks.md)变体）自 `nerolandcore` 1.1.0 起从 Nerospace 迁移到了 Core，因此所有 Neroland 模组都使用同一套端点。它们的合成和行为与以前相同，Nerospace 的[通用管道](Universal-Pipe.md)仍然可以连接它们（仍通过兼容桥接向垃圾桶销毁物品）。 They craft and behave as before, and Nerospace's
[Universal Pipe](Universal-Pipe) still connects to them (still voiding into the Trash Can through the
compat bridge).

## 跨模组的统一电网

现在，每台 Nerospace 发电机、电池、机器和管道都通过 Core 的共享 `nerolandcore:energy` 能力暴露其能量，Nerospace 管道也通过 Core 的查找读取相邻方块。因此，Nerospace 的[通用管道](Universal-Pipe.md)可以将电力直接输送到其他 Neroland 模组的机器中，其他模组的线缆也可以为 Nerospace 机器供电——这是一个连续的网络，而不是在模组边界处中断的独立电网。Nerospace 自身的内部能量仍然完全照旧工作；这只是增加了跨模组的覆盖范围。 So a
Nerospace [Universal Pipe](Universal-Pipe) can carry power straight into another Neroland mod's machine,
and another mod's cable can feed a Nerospace machine — it is one continuous network, not separate grids
that stop at the mod boundary. Nerospace's own internal energy still works exactly as before; this only
adds cross-mod reach.

## 共享的进度里程碑

随着你的进度推进，Nerospace 会悄悄开启 Core 的全生态**进度门槛**，以便其他 Neroland 模组能够根据你在旅程中的位置做出反应：建造你的第一台发电机会开&#x542F;_&#x5DE5;业电力_，第一次发射火箭会开&#x542F;_&#x62B5;达轨道_，用[空间站章程](Station-Charter.md)建立空间站会开&#x542F;_&#x7B2C;一个殖民地_，而抵达遥远的行星或将一个世界改造得生机勃勃会开&#x542F;_&#x6DF1;空_。这是单向的——Nerospace 驱动共享门槛，但仍以[星际指南](Star-Guide.md)把握自身节奏，星际指南依然是你 Nerospace 进度的权威依据。你可以在游戏中用 `/neroland gate list` 查看共享门槛。 This
is one-directional — Nerospace drives the shared gates but still paces itself by the [Star Guide](Star-Guide),
which remains the authority on your Nerospace progress. You can inspect the shared gates in-game with
`/neroland gate list`.

## 共享的陨石材料

在 **[Nerosium研磨机](Nerosium-Grinder.md)** 中研磨 **[陨石岩](Meteor-Rock.md)** 会读取 Core 的全生态**陨石材料注册表**（Core `1.2.0`+）。每个 Neroland 模组都会一次性地将其可研磨材料声明到 Core 中——附带稀有度等级、[进度门槛](#shared-progression-milestones)以及可选的行星绑定——研磨机会根据这份实时汇总按权重随机决定产出。因此，陨石材料池会随着已安装的 Neroland 模组自动扩充，无需修改研磨机。 Every Neroland mod declares its grindable
materials into Core once — with a rarity tier, a [progression gate](#shared-progression-milestones) and an
optional planet binding — and the grinder resolves a weighted-random output from that live aggregate. So
the meteor pool grows automatically with whatever Neroland mods are installed, no grinder changes needed.

Nerospace 是该注册表的第一个使用者：它贡献了自己的原材料和各行星的标志性矿石（Greenxertz 上的 Xertz石英和粗Nerosteel、Cindara 上的 Cindrite、Glacira 上的 Glacite，以及外星碎片／科技废料和奇异的外星核心），并提供 Core &#x7684;_&#x884C;星查&#x627E;_&#x63A5;缝，使解析器知道研磨发生在哪颗行星上（并应用行星偏向倍率）。注册表**只储存物品元数据——不储存玩家数据**；研磨的操作者仅以 UUID 形式用于门槛上下文，从不记录（POPIA/GDPR）。用 `/neroland meteor list` 查看实时材料池。 The registry
stores **only item metadata — no player data**; the grind's operator is used by UUID for gate context
only and is never logged (POPIA/GDPR). Inspect the live pool with `/neroland meteor list`.

## 统一的隐私／数据删除途径

Nerospace 将其以玩家为键的数据注册到 Core 的共享删除钩子上，因此只需一次 `/neroland data eraseme`（或管理员的 `/neroland data erase <uuid>`，或 Core 的不活跃数据保留清理）即可同时清除你的 Nerospace 数据——你的空间站所有权会被解除关联（空间站本身作为共享世界内容保留），你的[货运平台](Cargo-Pad.md)航线会被移除，平台和航班会被解除关联（正在飞行的货物仍会送达），你的氧气和星际指南状态会被重置。为符合 POPIA/GDPR，Nerospace 仅以玩家 UUID 为这些数据的键，从不记录你的身份。完整说明请参阅 `PRIVACY.md`。 In keeping with POPIA/GDPR, Nerospace
keys this data only by player UUID and never logs your identity. See `PRIVACY.md` for the full picture.

## 统一的配置重载命令和共享标签页

Nerospace 的[配置](Configuration.md)现在由 Core 管理：它位于 `config/nerospace.properties`，可通过 `/neroland config reload` 实时重载，游戏平衡倍率由服务器决定（客户端连接时使用服务器的值）。匿名遥测的选择退出仍是每个客户端的个人选择，服务器永远无法覆盖。 The anonymous-telemetry opt-out
stays a personal, per-client choice that a server can never override.

最后，Nerospace 的标志性材料和关键物品也会出现在 Core 的共享 **Neroland** 创造模式标签页中，因此装有多个 Neroland 模组的整合包会显示一个整齐的标签页——而 Nerospace 的完整物品目录仍保留在其专属的 Nerospace 标签页中。

---

另请参阅：[配置](Configuration.md)、[星际指南](Star-Guide.md)、[通用管道](Universal-Pipe.md)、[空间站章程](Station-Charter.md)。

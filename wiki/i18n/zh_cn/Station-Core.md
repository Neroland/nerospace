# 空间站核心

<!-- nerospace:render -->

<p align="right"><img src="images/station_core.png" alt="Station Core" width="150" align="right"></p>
<!-- /nerospace:render -->

玩家建立的空间站的永久锚定方块。

## 概述

Every station founded with a **[Station Charter](Station-Charter)** is built around a Station Core at
its centre. 每座通过\*\*[空间站章程](Station-Charter.md)\*\*建立的空间站都以中心的一个空间站核心为基础建造。核心标记空间站的注册信息，并使整个结构保持持久：只要它还在，空间站就会被列为火箭目的地，并且可以导航前往。

## Obtaining

**Not craftable.** A Station Core is placed automatically when a station is founded (via the charter's
naming screen, or the rocket's _New Station_ dock option). It glows softly and is **unbreakable** — the
station is permanent, so your builds inside it are safe.

## How it works

- **Anchor & persistence:** the Core binds the station to its registry slot and survives restarts and
  chunk unloads. The station **never regenerates** once built — any blocks you place or remove inside
  it are preserved on later visits.
- **Rename:** right-click the Core to open a rename console. \*\*重命名：\*\*右键点击核心打开重命名控制台。新名称会在所有地方更新——火箭对接选择器和空间站着陆台的航行节点。
- **仅限建立者：**只有建立空间站的玩家可以重命名它（建立者的 ID 仅为此项检查而保存在本地）。任何人仍可**飞往**任何空间站。 Anyone can still **fly to** any station.
- **着陆：**飞往该空间站时，你的火箭会降落在**离核心最近的已注册发射台**上（默认是气闸处的 2 级平台，或你在离核心更近处建造的任何平台）。
- \*\*移除空间站：\*\*由于核心是永久的，请使用命令 `/nerospace station remove` 来拆除——站在核心上，或提供 `/nerospace station list` 中的槽位。只有建立者（或创造模式管理员）可以移除它。 Only
  the founder (or a creative admin) may remove it.

## 详细信息

- ID：`nerospace:station_core` · 维度：空间站维度 · 无法破坏

# 公共集成 API

> 本页为机器翻译草稿，仅供参考，以英文版本为准：[English](../../Public-API.md)。

Nerospace 为其他模组提供一个受支持的集成接口：Java 包 `za.co.neroland.nerospace.api`。该包之外的一切都属于内部实现，可能会在不另行通知的情况下变更。

本页面向**模组开发者**。如果你是玩家，这里的内容都无需任何设置——在其他模组使用它之前，该 API 处于休眠状态。

## 稳定性

在同一主版本内，现有的公共类型、方法、记录组件和枚举常量永远不会被移除、重命名或赋予新含义。次版本中可能会添加新成员。请将该 API 作为 `compileOnly` 依赖使用，并将这些类型放在你自己的兼容适配器之后，使你的模组在没有 Nerospace 时仍能运行。

Nerospace 将全部六个加载器构建发布到 GitHub Packages：

```
za.co.neroland.nerospace:nerospace-<loader>-<mc>:<version>
```

## 行星、空间站和航线

- `NerospacePlanets`——行星标识（`PlanetId`）、`currentPlanet(Entity)` 钩子、每颗行星的只读 `PlanetTraits`（默认重力、无空气标志、`Hazard`），以及精确到每个位置的重力。
- `NerospaceStations`——作为不可变 `StationInfo` 记录的空间站目的地。
- `NerospaceRoutes`——维度级航线目录：`RouteEndpoint` 以及带有最低等级、燃料成本和运输时长的有向 `CargoRoute`。

## 货运平台、航线和航班（`nerospace.api.route`）

[货运平台](Cargo-Pad.md)背后的平台级航线 API。通过 `RouteApi.instance()` 获取；它仅在服务器端，且只有接口。

- `RouteApi.padsVisibleTo(server, uuid)`——该玩家可以发货的平台（他们自己的、与他们共享的、公开的平台、他们管理的空间站）。绝不是全服务器的名单。
- `RouteApi.quote(server, origin, destination, manifest, returnEmpty)`——一个 `RouteQuote`：运输刻数、燃料 mB（如有要求，包括空载返程）、跨维度标志、载荷质量。
- `RouteApi.requestFlight(server, requester, FlightRequest)`——从出发平台上停靠且已加注燃料的货运火箭发出一份货物清单；`FlightRequestResult` 包含航班或确切的拒绝原因。新的 `Denial` 常量只会追加：1.4.0 添加了 `CANNOT_PAY`（请求者无法支付服务器可选的发射费或对接费）。请将你不认识的名称映射到一个后备值。
- `RouteApi.flight` / `flightsOwnedBy` / `routesOwnedBy` / `cancel`——查询和管理。
- `RouteEvents.subscribe(listener)`——`onPadRegistered`、`onPadUnregistered`、`onFlightDeparted`、`onFlightArrived`、`onFlightHeld`、`onFlightDropped`。

成本模型如下，以便整合包和其他模组进行推算（`cargoTravelTimeMultiplier` 和 `fuelCostMultiplier` 会在此基础上应用）：

```text
travelTicks = 1200 + (crossDimension ? 2400 : 0) + 1800 × (gOrigin + gDestination) / 2, clamped 600..24000
massUnits   = ceil(items / 64)
fuelMb      = (1000 + 100 × massUnits) × max(0.1, gOrigin)      (+ the same for mass 0 from gDestination on return_empty)
```

`PadHandle`、`RouteHandle` 和 `FlightHandle` 是不可变快照；所有权只能以 `ownedBy(UUID)` 的形式针对你已持有的 UUID 进行查询。

## 环境查询

`NerospaceEnvironment.at(ServerLevel, BlockPos)` 返回一个不可变的 `EnvironmentSnapshot`：

| 字段 | 含义 |
|---|---|
| `loaded` | 区块未加载时为 `false`——此时其他所有字段都是失败即关闭的默认值，而不是读数 |
| `planet` | Nerospace 行星；在地球和任何非 Nerospace 维度中为空 |
| `atmosphere` | `VACUUM`、`PRESSURIZED`、`TERRAFORMING` 或 `BREATHABLE` |
| `hazard` | 该行星的环境危害 |
| `gravity` | 重力系数，`1.0` = 地球正常重力 |
| `oxygen` | 0-15，氧气区域加上任何外部贡献 |
| `terraformStage` | 0-3 |
| `breathable` | 玩家在此处不穿氧气服是否可以呼吸 |

有两个值得了解的行为：

- **它从不强制加载区块。**未加载检查在任何依赖区块的查找之前执行，因此在任意位置的探测都不会让服务器生成地形。
- **阶段 1 就已意味着可呼吸。**那是地形改造器标记该区块的时刻，因此 `TERRAFORMING` 描述的是一个正在进行但尚未达到该状态的区域。

## 行星访问

`NerospaceVisits.hasVisited(player, planet)` 和 `visitedPlanets(server, uuid)` 会按历史记录作答——包括在你的模组安装之前的访问。`PlanetVisitEvents.onVisit(listener)` 会在玩家第一次抵达某颗行星时，在服务器线程上触发一次。

该事件会交给你一个在线的玩家，因为监听器通常需要授予该玩家某些东西。如果你因此持久化了任何以其为键的数据，**那份副本需要由你负责删除**——Nerospace 的删除钩子只覆盖 Nerospace 自己的存储，而不是你的。请向 Neroland Core 的 `PlayerDataErasure` 注册，使单次请求仍能清除所有数据。

## 贡献氧气

`NerospaceOxygen.contribute(level, source, center, radius, strength, durationTicks)` 注册一个有范围、会过期的氧气源，Nerospace 会将其纳入可呼吸性和环境查询的结果。限制：半径 ≤ 64，强度 ≤ 15，持续时间 ≤ 一小时。用相同的来源 ID 重新贡献会替换先前的值，因此自然的“这台机器当前产出 N”的写法是幂等的。`remove(level, source)` 会将其撤回。

贡献必须在过期前刷新。这是有意为之：一台在其区块未加载时被破坏的机器，不能留下永久的免费空气。

## 地形改造叠加层

`NerospaceTerraforming` 应用并回滚可逆的区域叠加层。叠加层会改变 Nerospace 对某个区域的*报告*内容，而不会改写任何一个方块，这正是回滚能够即时且无损的原因。覆盖范围是**水平的**——一个区域像柱子一样从基岩延伸到建筑高度上限，与物理地形改造标记整个区块而非一个气泡的方式一致。每个区域都会记录其下方的物理区块阶段，因此移除叠加层会恢复该基准，而不会抹去真实的进度。

**在服务器安装认领策略之前，所有变更都会被拒绝：**

```java
NerospaceTerraforming.setClaimPolicy((actor, level, request) -> yourClaimMod.mayBuild(actor, request.center()));
```

这是有意设计为失败即关闭的——未经认证的区域环境覆盖就是一种恶意破坏工具。

## 隐私

有两个 API 接口涉及玩家数据，两者都由 Neroland Core 的共享数据删除钩子覆盖：

- **空间站所有权**从不以原始 UUID 的形式暴露——只以按玩家的布尔值 `NerospaceStations.ownedBy` 提供。
- **行星访问**储存一个 UUID 和一组行星 ID。没有时间戳，没有坐标，没有移动轨迹。删除请求会立即删除该行并重写存档数据备份。
- **货运航线**储存平台、航线和航班的所有者 UUID，以及平台访问列表中的 UUID。API 从不返回它们；`padsVisibleTo` 仅限于提出请求的玩家。删除会移除该玩家的航线，匿名化其平台和航班，并将其从访问列表中移除——已在空中的货物仍会送达或装箱。

氧气贡献和地形改造叠加层**完全不储存玩家信息**。它们的来源和区域 ID 由你自行选择，会原样持久化，并且**绝不能编码玩家 UUID 或名称**。

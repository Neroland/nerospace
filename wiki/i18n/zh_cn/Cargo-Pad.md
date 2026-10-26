# 货运平台

The **Cargo Pad** is the endpoint of an automated supply line: a launch-pad plate with a 27-slot hold,
a pipe-fed fuel buffer and a route selector. Build one where goods leave, one where they should land,
put a [Cargo Rocket](Cargo-Rocket) on the first, and cargo flies between them with nobody at either pad.

## 配方

顶部是 Nerosteel锭围绕一个[货运船体](Cargo-Rocket.md)，中间是一个 Nerosium块，两侧各一个[火箭发射台](Rocket-Launch-Pad.md)，底部是一排 Nerosteel。它位于 Greenxertz 材料阶梯上，在 2 级火箭之后。 It sits on the Greenxertz
material ladder, after the Tier 2 rocket.

## 建造平台

A Cargo Pad **is** a launch pad block. 货运平台**本身就是**一个发射台方块。围绕它建造通常的完整 **3×3** 发射台（货运平台可以是方阵中的任意一格）——货运火箭需要 2 级结构，与 2 级载人火箭的门槛相同。空手潜行右键点击平台可查看它构成的等级。 Right-click the pad with an empty hand while sneaking to read the tier it forms.

- 相邻的 **[燃料储罐](Fuel-Tank.md)** 会像为载人火箭加注一样为停靠的货运火箭泵送燃料。你也可以直接通过管道将火箭燃料输入货运平台：其燃料缓存（默认 16,000 mB）只接受火箭燃料，并以 80 mB/刻的速率泵入火箭。 Alternatively pipe rocket fuel straight into the Cargo Pad: its fuel buffer (16,000 mB by
  default) accepts rocket fuel only and is pumped into the rocket at 80 mB/tick.
- **[Universal Pipes](Universal-Pipe)** connect to every face. \*\*[通用管道](Universal-Pipe.md)\*\*可连接每一个面。货舱使用 Neroland Core 的面配置（STORAGE 预设——每个面都可输入／输出），因此管道会在出发地将货物推入，在目的地将送达的货物抽出；基础和高级[管道过滤器](Pipe-Filters-and-Upgrades.md)照常工作。使用[配置器](Configurator.md)可将某个面设为仅输入或仅输出。 Use the [Configurator](Configurator) to make a face input- or output-only.
- 货舱有 27 个槽位；服务器的 `cargoPadSlots` 设置（默认 18）决定其中有多少可用。锁定的槽位在界面中以较暗的颜色显示。
  Locked slots are drawn darker in the GUI.

## 所有权

Placing a Cargo Pad makes you its owner. Only the owner (or anyone, while a pad is unowned) can change
its destination, schedule and flags; anyone may still put items in through the GUI or pipes. Other
players can route cargo **to** your pad only if you mark it **Public** in the GUI. A Name Tag renames
the pad; the name is what other players see in their destination list.

已建立的[轨道空间站](Station-Charter.md)也算作目的地：飞往空间站的航班会降落在放置于空间站着陆台六格范围内的货运平台上。

## 界面

Right-click the pad (or its rocket) with an empty hand.

| 控件          | 含义                                                                                                                                                                      |
| ----------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `<` `>` `x` | Cycle through the pads you may ship to (your own, ones shared with you, public pads, your stations), or clear the destination.       |
| 计划          | **手动**（仅发射按钮）、**装满时发射**（每个可用槽位都有东西），或**每 N 分钟**（`-`/`+` 更改 N）。                                                                                                          |
| 缓存／火箭仪表     | 平台缓存中和停靠火箭中的燃料。                                                                                                                                                         |
| Trip line   | 针对当前货舱内容的报价：火箭必须携带的燃料和飞行时间。                                                                                                                                             |
| 空载返回        | 卸货后，火箭空载飞回本平台，而不是留在目的地。返程燃料在发射时扣除。 The return fuel is charged at launch.                                                                                |
| 公开／私有       | 其他玩家是否可以将货物运往此处。                                                                                                                                                        |
| 发射          | 当有加注了燃料的货运火箭停靠、已设定目的地且货舱不为空时亮起。                                                                                                                                         |
| 状态栏         | The verdict of the last launch attempt, then the state of the last flight (in transit with a countdown, holding, delivered, crated). |

## 飞行、抵达与等待

飞行时间和燃料来自行星注册表（公式参见[公共 API](Public-API.md)）：一次航班至少需要一分钟，跨维度以及在高重力世界之间耗时更长；燃料随载荷和出发地的重力增加。运输途中不会渲染任何东西——航班是一个服务器端计时器，**在服务器重启后依然有效**。 Nothing is rendered in transit — the flight is a server-side
timer that **survives a server restart**.

On arrival the rocket materialises on the destination pad and unloads one stack every two ticks so pipes
keep up. If the destination is not loaded, the flight waits — Nerospace never loads a chunk to deliver.
抵达时，火箭会出现在目的地平台上，并每两刻卸下一组物品，以便管道跟上。如果目的地未加载，航班会等待——Nerospace 从不为了送货而加载区块。如果目的地平台已不存在、上面已停有另一枚火箭，或其货舱已满，航班会**等待**并每 30 秒重试一次（`cargoHoldRetrySeconds`）；等待 30 分钟后（`cargoHoldTimeoutMinutes`），剩余货物会作为**货运箱**物品掉落在目的地位置。货物永远不会被删除。右键点击货运箱即可将其拆开。 Cargo is never deleted. Right-click a crate to unpack it.

Comparator output follows the hold's fill level.

在创造模式世界中，`/nerospace gallery` 会在火箭行的北侧建造一条现成的货运线路（两个已设定航线的平台、一枚加注了燃料的火箭、一个已装货的货舱）——打开平台 A 并按下发射即可观看一次送货。

## 隐私

The pad stores its owner's UUID, and optionally the UUIDs of players the owner shared it with, in the
world save only. `/neroland data eraseme` removes your routes, unlinks your pads (they stay as shared
world content), takes you off every access list and detaches you from your flights — the cargo itself
is still delivered or crated. Nothing here is logged or sent anywhere. See `PRIVACY.md`.

---

另请参阅：[货运火箭](Cargo-Rocket.md)、[火箭发射台](Rocket-Launch-Pad.md)、[燃料储罐](Fuel-Tank.md)、[通用管道](Universal-Pipe.md)、[配置](Configuration.md)。

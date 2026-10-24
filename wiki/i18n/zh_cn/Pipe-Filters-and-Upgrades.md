# 管道过滤器与升级

精细调整什么流向哪里，以及流得多快。

## 管道过滤器

将管道面的**物品层**限制为单一物品。

- **合成**（有序，产出 4 个）：Nerosteel 四角围绕铁栏杆。

- \*\*设定过滤器：\*\*一只手拿着过滤器，另一只手拿着要匹配的物品，然后

  right-click in air. 对空气右键点击。（另一只手为空 = 清除过滤物品。）

- **安装：**手持已配置的过滤器右键点击通用管道的某个**面**——该物品会被**放入该面的过滤器槽位**（类似管道升级），只有匹配的物品会通过该面被抽取或推送。该面上已有的过滤器会弹回给你。 A filter already on the face pops back to you.

- \*\*移除／检查：\*\*打开管道的[配置器](Configurator.md)面板——每个面的过滤器都位于一个真实的槽位中；将鼠标悬停在上面可查看它匹配的内容，将其取出即可清除该面。破坏管道会掉落其已安装的过滤器。 Breaking
  the pipe drops its installed filters.

过滤器会影响提取（抽取面只会抓取匹配的物品）、路线规划（包裹不会前往会拒绝它们的带过滤器的面）以及交付。

**路线规则（过滤器与目的地如何相互作用）：**

- Items enter a pipe network **only through faces set to In** (with the
  [Configurator](Configurator)). 物品**只能通过设为输入的面**（用[配置器](Configurator.md)设定）进入管道网络。**自动**面会交付物品但从不提取物品——因此网络不会把自己目的地箱子里的东西又抽回来。能量、流体和气体的自动面仍会像以前一样抽取。 Energy, fluid and gas Auto
  faces still pull as before.
- **交叉口受控。**只有当离开的面允许物品输出（**输出**／自动）、进入的面允许物品输入（**输入**／自动），并且**两个面的过滤器**都放行时，物品才会穿过管道与管道之间的交叉口。因此，安装在 T 形交叉口分支面上的过滤器准确地控制了什么可以进入该分支——不匹配的物品会绕行或留在原处。 A filter installed on a T-junction's
  branch face therefore controls exactly what may go up that branch — items that don't match
  route around it or stay behind.
- **Filtered faces claim their items first.** An item that matches any filtered face goes to a
  filtered face; unfiltered destinations only receive what no filter claimed. So "cobblestone →
  chest A (whitelist)" really means all the cobblestone lands in A, and the unfiltered overflow
  chest gets everything else.

> **Tip — watch which face you click.** The filter lands on the face you clicked, which is not
> always the face touching the target chest. Hold the Configurator to see the per-face colour
> shading, then check the panel: the filter should sit in the row whose colour matches the shaded
> face pointing at your target. Items with **no accepting face anywhere** wait inside the pipes
> (backpressure — nothing is ever dropped), so a fully filtered line needs at least one
> unfiltered destination for the leftovers.

## 高级管道过滤器

更高等级的过滤器：一条管线、**多种物品类型**，每个面自行决定接收什么——无需平行管线即可实现 BuildCraft 钻石管道式的分拣。

- **合成**（有序，产出 1 个）：一个管道过滤器核心，周围环绕 Nerosteel锭和 Xertz石英（Greenxertz 级材料）。
- \*\*配置：\*\*对空气右键点击打开过滤器界面——
  - **9 ghost slots** (3×3): click with an item on your cursor to set an entry, click with an
    empty cursor to clear it, shift-click items straight from your inventory.
  - **右键点击某个条目**（空光标）可循环切换其匹配模式：精确物品 → 该物品的每个**标签**（例如 `#c:ores` 用一个条目涵盖所有矿石，以金色 `#` 标记）→ 回到精确物品。
  - **白名单／黑名单**切换：只允许这些物品——或允许**除**这些物品之外的所有物品。
  - **精确匹配／忽略数据**切换：是否还必须匹配物品数据组件（NBT）。
- **安装：**右键点击通用管道的某个**面**——该物品（连同其全部配置）会被放入该面的过滤器槽位；之前安装的过滤器会弹回给你。通过[配置器](Configurator.md)面板的面槽位移除或检查它。
  Remove or inspect it via the [Configurator](Configurator) panel's face slots.
- **复制：** **潜行右键点击**管道面，可将其当前过滤器读回手中的物品——无需重新配置即可复制一整面分拣墙。
- **检查：**该物品的**提示框会列出完整配置**（模式行 + 每个条目；标签条目以金色显示）——在你的物品栏中和配置器面板的面槽位中都可以看到。

配置储存在物品上，因此调好的过滤器就是一个**模板**：把它堆放在箱子里、分享给别人，或连续应用到十几个面上。没有条目的过滤器在白名单和黑名单模式下都表现得像没有过滤器（所有东西都能通过）。 A filter with no entries behaves like no filter
(everything passes) in both whitelist and blacklist mode.

**示例——研磨机分流：**从你的研磨机引出一条管道；在熔炉面上应用一个包含可冶炼矿石标签条目的白名单，在旁路箱子面上应用相同条目的**黑名单**。可冶炼的物品进入熔炉，其他所有物品则从旁流过。 Smeltables go to the furnace, everything else flows past.

基础管道过滤器仍然完全照常工作——应用高级过滤器时，它只是替换该面的基础过滤器（反之亦然）。

## 速度升级

- **合成**（有序）：红石 + 金核心，置于 Nerosteel 框架中。
- \*\*安装：\*\*右键点击管段（会被消耗，每段管道最多 **3** 个）。
- 每个都会成倍提高该管段的能量／流体／气体**吞吐量**，并使物品在其中**移动得更快**。

  through it.

## 容量升级

- **合成**（有序）：Xertz石英 + 箱子核心，置于 Nerosteel 框架中。
- \*\*安装：\*\*右键点击管段（最多 **3** 个）。
- 每个都会成倍提高该管段的流体／气体**缓存**，以及可同时**运输中**的物品组数量。

  transit\*\* at once.

## 移除升级

**空手**潜行右键点击管道——所有已安装的升级都会弹出。

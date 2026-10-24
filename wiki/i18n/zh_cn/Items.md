# 物品

Nerospace 非方块物品的参考。（方块有各自的页面——请查看侧边栏。） (Blocks have their own pages — see the sidebar.)

## 材料

| 物品             | 说明                                                                                                     |
| -------------- | ------------------------------------------------------------------------------------------------------ |
| **粗Nerosium**  | 从 [Nerosium矿石](Nerosium-Ore.md) 开采；熔炼／高炉冶炼成 Nerosium锭。                                                 |
| **Nerosium锭**  | 早期核心金属；可合成工具、研磨机以及发射台核心。                                                                               |
| **Nerosium粉**  | [Nerosium研磨机](Nerosium-Grinder.md)的产物；可冶炼成锭（矿石产量翻倍）。                                                   |
| **粗Nerosteel** | 从 Greenxertz 上的 [Nerosteel矿石](Nerosteel-Ore.md) 开采；冶炼成 Nerosteel锭。                                     |
| **Nerosteel锭** | 中期支柱金属：火箭、氧气服、机器、空间站方块。                                                                                |
| **Xertz石英**    | 由 [Xertz石英矿石](Xertz-Quartz-Ore.md) 掉落的宝石；用于升级和地形改造监视器。                                                 |
| **Cindrite**   | 来自 Cindara 上 [Cindrite矿石](Cindrite-Ore.md) 的宝石；3 级地形改造器升级、2 级 + [耐热服](Oxygen-Suit.md)部件、4 级火箭。         |
| **Glacite**    | 来自 Glacira 上 [Glacite矿石](Glacite-Ore.md) 的宝石；[低温服](Oxygen-Suit.md)部件以及[补水模块](Hydration-Module.md)的水循环。 |
| **外星碎片**       | [陨石核心](Meteor-Core.md)的常见战利品；未来扫描仪的原料。                                                                 |
| **外星科技废料**     | Uncommon meteor loot; future upgrade crafting.                                         |
| **外星核心**       | 稀有的陨石战利品；高价值的扫描仪／升级门槛。                                                                                 |

## 工具

- **Nerosium镐**——铁级挖掘，耐久度更高，并有少量攻击加成。合成： Craft:

  3 个Nerosium锭放在两根木棍上方（标准镐形状）。

- **[配置器](Configurator.md)**——管道网络工具：按面 × 按层设定输入／输出模式，

  full configuration panel on sneak-right-click.

- **管道过滤器／高级管道过滤器／速度升级／容量升级**——参见

  [管道过滤器与升级](Pipe-Filters-and-Upgrades.md)。

## 氧气服

A four-piece nerosteel armour set — **Helmet, Chestplate, Leggings, Boots** — that doubles as personal
life support. Wearing the **full set** lets you breathe off-world away from a generator/pad; it draws
from a finite air tank that refills in any breathable zone (so it greatly extends your air rather than
being infinite). Diamond-class protection, repaired with nerosteel.

The suit also refills at **airlocks**: standing within a few blocks of a [Gas Tank](Gas-Tank) (now
provided by [Neroland Core](Neroland-Core)) or [Oxygen Generator](Oxygen-Generator) that holds Oxygen
tops the suit up from that store (draining the gas). A **Tier 2 Oxygen Suit** (each piece = Tier 1 piece + 4 Cindrite) is tougher armour. It does
**not** hold more air: every full suit holds 900 air and refills at the same speed.

2 级氧气服有两种**危害变体**，可保护你免受行星极端环境的影响（未防护的暴露会使氧气消耗变为**四倍**）：用于抵御 Cindara 高温的**耐热服**（2 级部件 + 4 个 Cindrite），以及用于抵御 Glacira 严寒的**低温服**（2 级部件 + 4 个 Glacite）。四件必须全部匹配，防护才会生效。完整页面参见\*\*[氧气服](Oxygen-Suit.md)\*\*。 All four pieces must match for the shield to
engage. See **[Oxygen Suit](Oxygen-Suit)** for the full page.

## 火箭与燃料

- **火箭燃料罐**——可堆叠的燃料单位。合成（无序）：烈焰粉 + 煤炭 + Craft (shapeless): Blaze Powder + Coal +

  Iron Ingot → 2 canisters. Used to fuel rockets/machines and as a crafting core. (Reachable before
  your first launch — no off-world materials needed.)

- **[燃料精炼厂](Fuel-Refinery.md)**——燃料自动化：煤炭 + 烈焰粉 + 电网电力 → 可通过管道输送的

  液态火箭燃料，是手工合成燃料罐的物流级替代方案。

- **火箭燃料桶**——一桶 `rocket_fuel` 流体；可将其倒入火箭或[燃料储罐](Fuel-Tank.md)。

- **火箭（1／2／3／4 级）**——部署到[火箭发射台](Rocket-Launch-Pad.md)上，加注燃料，

  登舱，选择目的地，然后发射（目的地是累积的）：

  - **1 级** → 轨道空间站（以及任何[已建立的空间站](Station-Charter.md)）。

  - **2 级** → + Greenxertz。

  - **Tier 3** → + Cindara. (Needs a 3×3 pad ringed with [Station Wall](Station-Wall) **or** a

    [重型发射综合设施](Launch-Gantry.md)。）

  - **Tier 4** → + Glacira. **4 级** → + Glacira。（**只能**在[重型发射综合设施](Launch-Gantry.md)上部署；

    24,000 mB tank.)
    The in-rocket screen shows a live fuel gauge, a selectable destination trajectory, a station
    selector, and a Launch button, with your inventory laid out in a slot grid below the controls.
    Mods that enlarge the main player inventory (such as _Inventory Extended_ on Fabric) are
    supported automatically — the console grows to show the extra rows.

- **[货运船体](Cargo-Rocket.md)**——一个被一圈铁和 Nerosteel 包围的箱子；货运火箭和

  Cargo Rocket and the [Cargo Pad](Cargo-Pad).

- **[货运火箭](Cargo-Rocket.md)**——一种无人驾驶火箭（2 级火箭 + 货运船体 + 燃料罐，外围

  nerosteel) that deploys only onto a **Cargo Pad** and carries its hold to another pad or station.

- **货运箱**——无法合成：一次未能按时送达的货运航班的货物清单，

  dropped at the destination so nothing is lost. Right-click to unpack.

- **[空间站章程](Station-Charter.md)**——8 个[空间站墙壁](Station-Wall.md)围绕一个

  [空间站地板](Station-Floor.md)；在铁砧中重命名，带上火箭，并在火箭界面中选择
  **FOUND** 节点，即可建立你自己命名的轨道空间站。

## 星际指南

- **Star Guide Book** — Book + Raw Nerosium (shapeless). Right-click to open the

  **[星际指南](Star-Guide.md)**，即交互式进度树（10 个章节，45 个步骤）。

- **[星际指南](Star-Guide.md)**（展台方块）——放置书本，投射下一目标的全息影像。

## 食物与生物掉落物

- **Loper Haunch** — dropped by the Meadow Loper; hearty food.
- **Strutter Drumstick** — dropped by the Ember Strutter; food.
- **漂流兽羊毛**——由绒毛漂流兽掉落；可合成 4 根线。

  牲畜本身参见\*\*[生物](Creatures.md)\*\*。

## 陨石事件

- **Meteor Tracker** — points to the nearest meteor (held action-bar readout: state, heading,

  distance). Craft (shaped):

  ```text
  . N .
  R C R
  . N .
  ```

  `N` = Nerosteel锭 · `R` = 红石 · `C` = 指南针

- **Meteor Caller** — creative-only: right-click a block to call a meteor down onto it.

  完整的世界事件、战利品表和配置参见\*\*[陨石事件](Meteor-Events.md)\*\*。

## 旅行装置（创造模式）

- **Greenxertz导航器**以及**空间站／Greenxertz／Cindara／Glacira 指南针**是

  仅限创造模式的一键旅行辅助工具，用于测试和建造（没有生存配方——火箭才是
  生存模式的路线）。

## 刷怪蛋

所有八种生物都有创造模式刷怪蛋——参见\*\*[生物](Creatures.md)\*\*。

# 太阳能板

<!-- nerospace:render -->

<p align="right"><img src="images/solar_panel.png" alt="Solar Panel" width="150" align="right"></p>
<!-- /nerospace:render -->

Sun-tracking generators that pool with their neighbours into one big solar array. Comes in **three
tiers** — Tier 1 is a single block; Tier 2 and Tier 3 are 2×2 and 3×3 **multiblocks** that you place
from a single item.

## Obtaining

**1 级**（有序）：玻璃面板覆盖在石英／铜电池上，底部是 Nerosteel 外壳——

```text
G G G
Q C Q
N N N
```

`G` = 玻璃 · `Q` = Xertz石英 · `C` = 铜锭 · `N` = Nerosteel锭

**2 级**——四块 1 级太阳能板围绕一个 Nerosium块：

```text
. P .
P N P
. P .
```

`P` = 1 级太阳能板 · `N` = Nerosium块

**3 级**——四块 2 级太阳能板围绕一个金块：

```text
. Q .
Q G Q
. Q .
```

`Q` = 2 级太阳能板 · `G` = 金块

每一级的配方都包含**上一级太阳能板**，因此升级时可以复用你已经造好的东西。

## How it works

- **阳光进，FE 出。**产出随太阳变化：正午最高，到**夜晚降为零**。

  In normal dimensions the panel needs a **clear view of the sky** — anything solid directly above it
  stops generation.

- \*\*天气：\*\*没有固定的雨天或雷暴惩罚。产出取决于天空的暗度： Output follows how dark the sky is:

  产出 × (10 − 天空暗度) / 10。下雨和暴风雨会使天空变暗，因此产出随之下降。 Rain and storms darken the sky, so output drops with them.

- **Nerospace planets** (Greenxertz, Cindara, Glacira, Luna, Mars and Nyxara) follow the planet's own day: a panel

  peaks at that planet's noon, dips at night, on Cindara dims during the eclipse and on Mars falls during a dust storm.
  Nyxara's sun never moves, so a panel there gives a steady ×2.04. Over a full planet day
  every planet gives **at least the old ×2 rate on average** (the peaks are higher to make up for the
  nights), so bases that run at night need a battery. The **Orbital Station** (and founded stations) stays
  a constant ×2. Panels do **not** need a view of the sky on a planet. Server owners can restore the old
  constant ×2 with `solar.clockDriven=false`. Solar is the natural off-world power source.

- 产出和储存随等级缩放（所有数值 × `energyRateMultiplier` 配置）。缓存 The buffer

  从不接受外部推送。

- \*\*自动推送：\*\*太阳能板会向任何与之接触的机器或电池推送电力。

| Tier    | Footprint | Output @ noon | 储存           |
| ------- | --------- | -------------------------- | ------------ |
| **1 级** | 1×1       | 20 FE/t                    | 50,000 FE    |
| **2 级** | 2×2       | 100 FE/t                   | 250,000 FE   |
| **3 级** | 3×3       | 400 FE/t                   | 1,000,000 FE |

Each higher tier is stronger **per area** than tiling the lower one, and carries a much bigger buffer.

### 多方块结构（2 级和 3 级）

- 2 级／3 级太阳能板是一个**放置后会占满其整个 N×N 范围的单一物品**——你需要

  清空那片平地（如果范围内有阻挡，它将无法放置）。

- 它作为**一个整体**运作：一块大型倾斜面板、一个共享缓存，每个外侧面都有能量。

- **Breaking any cell returns the whole panel as one item** — no duplication, and you get your panel

  back wherever you mined it.

### 阵列

- 将同等级的单元**彼此相邻**放置，它们会自动合并为一个**阵列**：

  阵列的储存是**所有单元缓存之和**，其发电量是**所有单元产出之和**。建得越宽，两者就越多——阵列几乎可以是任何尺寸。 Build wider for more of both — arrays can be almost any size.

- \*\*每一面都是输出端口。\*\*一条通用管道（能量层）——或任何机器／电池——接触

  _任&#x610F;_&#x4E00;面，都会从共享阵列池中取电，因此一条管道就能抽取整个阵列的电力。

- \*\*等级永不混合。\*\*并排放置的 1 级阵列和 2 级阵列会保持独立；只&#x6709;_&#x540C;&#x4E00;_&#x7B49;级的单元

  of the _same_ tier pool together.

### 外观

各等级**外观明显不同**——1 级带有绿色点缀，安装在 **T 形支柱**上转动，从东到西追踪太阳；2 级（Nerosium 品红色）和 3 级（金色）是平铺的 N×N 阵列，其面板**白天向上倾斜朝向天空，夜晚折叠平放在外壳上**。所有太阳能板读取相同的世界时间，因此一整片太阳能板会同步运动。 All panels read the same world
time, so a field of them moves in lockstep.

## 详细信息

- ID：`nerospace:solar_panel`（1 级）／`nerospace:solar_panel_t2`／`nerospace:solar_panel_t3`
- 工具：1 级需要铁镐；2 级和 3 级任何镐均可。
- 1 级掉落自身；2／3 级在任何一格被破坏时，为整个多方块结构返还一个物品。
- 根据阵列的电量发出比较器信号。
- 配置：`energyRateMultiplier`（同时缩放产出和储存）。参见\*\*[配置](Configuration.md)\*\*。 See **[Configuration](Configuration)**.

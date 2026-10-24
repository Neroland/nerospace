# 火箭

四种载你飞离地球的载人火箭，它们各自的外观，以及在发射台上和飞行中的表现。燃料数据、发射台尺寸和目的地参见[火箭发射台](Rocket-Launch-Pad.md)；无人货运船体参见[货运火箭](Cargo-Rocket.md)。 For fuel numbers, pad sizes and destinations see [Rocket Launch Pad](Rocket-Launch-Pad);
for the uncrewed freight hull see [Cargo Rocket](Cargo-Rocket).

## 概述

将火箭物品部署到[发射台](Rocket-Launch-Pad.md)上，为其加注燃料，右键点击打开飞行控制台并按下**发射**。引擎点火时你会登舱，几秒钟后抵达目的地发射台。每个等级都仍然可以飞往所有更低等级的目的地。 You board as the engines light and arrive on the destination pad a few
seconds later. Every tier can still fly to every lower tier's destinations.

| Tier    | Reaches      | Pad      | 外观                                                                            |
| ------- | ------------ | -------- | ----------------------------------------------------------------------------- |
| **1 级** | 轨道空间站        | 一个发射台方块  | Red livery, single bell, four raked fins, four landing legs, porthole cockpit |
| **2 级** | + Greenxertz | 3×3      | Nerosium 紫色涂装、两个捆绑式助推器、旋转雷达天线、两片尾翼                                            |
| **3 级** | + Cindara    | 围圈的 3×3  | 金／绿涂装、主喷口周围环绕四个游标发动机、可展开的太阳能翼、扫描天线支臂、全景舱盖、长鼻锥                                 |
| **4 级** | + Glacira    | 重型发射综合设施 | 冰蓝色涂装、宽大的五引擎芯级、四个助推器、栅格翼、重型着陆腿、最高的鼻锥                                          |

每个等级在几何形状和尺寸上都比上一级更大，因此从基地的另一头就能看出进度。

## The cockpit

Every hull has a window band at the rider's eye height, cut out of the hull and glazed from the inside
with tinted glass. Behind it is a lit cabin: a console with a screen under the forward window, a ceiling
lamp, and light strips on the side walls. The rider is seated so their eyes sit exactly on the window
band, so in first person you look out through the glass at the pad falling away; from outside, the
cabin glows through the windows at night.

南侧的**舱门**在无人乘坐时保持打开，并有一架梯子通往发射台，在你发射的那一刻密封。舱门打开时，你可以透过门口直接看到驾驶舱内部。 While it is open you can see straight into the cockpit through the doorway.

## 在发射台上

- 着陆腿稳稳立着；3 级火箭的太阳能翼处于展开状态。
- 导航信标闪烁红光（西）和绿光（东）；鼻锥上有白色频闪灯闪烁。
- 2 级火箭的雷达天线会旋转；3 级和 4 级火箭的侧支臂上装有一个来回扫描的天线。
- 加注了燃料的船体会从外壁排出少量冷蒸汽。

## Launch sequence

1. **点火**——舱门密封，每个喷口下方的尾焰绽放并闪烁（白热的核心，等级颜色的边缘），船体震动，一团烟云在发射台上向外翻滚。太阳能翼和天线支臂收起。 Solar wings and
   the dish boom fold in.
2. **爬升**——着陆腿沿着船体向外并向上收起，发动机喷口摆动，4 级火箭的栅格翼展开。
3. **分级**——爬升超过三分之二后，2 级和 4 级火箭的助推器熄灭并脱落。
4. **抵达**——火箭重新出现在目的地发射台上，着陆腿放下，舱门打开。

船体及其玻璃由世界光照照亮；只有灯、信标和尾焰是全亮度的，因此夜晚停放的火箭是一个带有发光窗户的深色剪影。

## 货运火箭

The [Cargo Rocket](Cargo-Rocket) borrows the Tier 2 hull and livery at a slightly larger scale. Nobody
boards it, so its hatch stays sealed and it carries no ladder.

## 贡献者信息

火箭几何形状、`art/blockbench/entity/` 下的 Blockbench 文件以及纹理图集都由一份规格文件 `tools/rocket_models.py` 生成；动画位于 `RocketModel` 中。编辑 Python 文件，使用 `--multiloader` 运行它（或 `./gradlew genAssets`），然后重新构建。 Edit the
Python, run it with `--multiloader` (or `./gradlew genAssets`) and rebuild.

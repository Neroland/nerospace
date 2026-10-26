# 货运火箭

An **uncrewed** rocket that carries a [Cargo Pad](Cargo-Pad)'s hold to another pad. Nobody boards it,
it has no flight console, and it never decides to launch — the pad it stands on does, by hand or on a
schedule. It borrows the Tier 2 hull and livery (see [Rockets](Rockets)) — with its hatch sealed and no
boarding ladder — and carries 12,000 mB of rocket fuel.

## 货运船体

The crafting intermediate: a chest inside a ring of iron and nerosteel ingots. It is also an ingredient
of the [Cargo Pad](Cargo-Pad).

## 配方

A **Tier 2 Rocket** above a Cargo Hull above a Rocket Fuel Canister, flanked by nerosteel ingots. 一枚 **2 级火箭**放在货运船体上方，货运船体放在火箭燃料罐上方，两侧为 Nerosteel锭。与每个火箭等级一样，它需要先解锁上一级，因此在你抵达 Greenxertz 后货运线路才会开放。

## 使用方法

1. Right-click a **Cargo Pad** with the item. The rocket stands on the Cargo Pad block; a pad that already
   holds a rocket refuses a second one.
2. 为它加注燃料：相邻的[燃料储罐](Fuel-Tank.md)、发射控制器、通过管道注入货运平台缓存的燃料，或手持桶／燃料罐都可以。
3. Load the hold, pick a destination in the pad GUI and press **Launch** — or set a schedule and walk away.

Right-clicking the rocket with an empty hand opens the pad's GUI. Hitting it returns the Cargo Rocket
item. The pad must form a complete 3×3 (Tier 2) for lift-off, exactly as for a Tier 2 crewed rocket.

## 火箭的去向

起飞时火箭会播放上升动画然后消失；航班抵达时，一枚新的火箭会出现在目的地平台上，携带行程结束后剩余的燃料。如果在航线上勾选了**空载返回**，它会在卸货后直接飞回（费用在发射时支付）；否则它会留在目的地平台上，等待装货返程。 With **Return empty** ticked on the
route it flies straight back after unloading (paid for at launch); otherwise it stays on the destination
pad, ready to be loaded for the trip home.

---

另请参阅：[货运平台](Cargo-Pad.md)、[物品](Items.md)、[星际指南](Star-Guide.md)（**货运**章节）。

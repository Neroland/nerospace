# Shaders

Nerospace works with shader packs through **Iris** (Fabric and NeoForge). You do not need a shader mod to
play, and nothing here changes anything when you have none installed.

## What Nerospace does with a shader pack

A shader pack takes over how the sky is drawn. Most packs only know three worlds: the Overworld, the
Nether and the End. Any other world, including every Nerospace world, is treated as the Overworld. That
is why space could look like Earth with shaders on: a blue sky, a sun, and the pack's own clouds over the
Moon.

Nerospace now tells the pack which of its three looks fits each world:

| World | Pack look used |
| --- | --- |
| Orbital Station, Luna, Mars, Nyxara, Europa, Cindara, Glacira | End (dark sky, no pack clouds) |
| Greenxertz | Overworld (it has air and weather of its own) |

If a shader pack supports a Nerospace world by name, the pack's own choice always wins.

## Settings

Both are in `config/nerospace.properties` on your own computer, and both apply straight away.

| Key | Default | Meaning |
| --- | --- | --- |
| `shaders.dimensionCompat` | `auto` | `auto` uses the table above. `off` leaves every world to the pack. |
| `shaders.drawPlanetSky` | `true` | Draw the Nerospace planets, moons and stars through the pack's sky. `false` leaves the sky to the pack. |

See [Configuration](Configuration) for the other keys.

## What to expect

- **Sky colour and clouds.** With `shaders.dimensionCompat = auto`, every world but Greenxertz gets a dark
  sky and no pack clouds.
- **Planets and moons in the sky.** Many popular packs replace everything in the End sky with their own
  effect, and hide or flatten whatever a mod draws there. Under those packs you get the dark sky, but
  Earth or the Moon may not appear, or may appear as plain squares. This is the pack's design and
  Nerospace does not change or patch any shader pack.
- **Europa's Jupiter.** Its cloud bands drift slowly by cross-fading a series of pictures through the normal
  sky drawing; there is no extra shader. On `sky.quality = lite` Jupiter is a still picture.
- **Rocket flames.** The exhaust plume and hull lamps are drawn the way glowing parts of mobs are, which
  shader packs support. The burst of flame and smoke at lift-off is made of ordinary particles.
- **Forge.** There is no complete shader mod for Forge on the supported Minecraft versions. Nerospace's
  shader support simply stays inactive there.

## If you want a different look

- Prefer the pack's own sky everywhere: set `shaders.drawPlanetSky = false`.
- Prefer the pack's Overworld look on every world (the behaviour before 2.4.0): set
  `shaders.dimensionCompat = off`.
- Many packs have an option to show a resource-pack or vanilla sky in the End. Turning that on in the
  pack's own settings usually brings the Nerospace planets back.
- Pack authors: name a Nerospace world in your `dimension.properties` (for example
  `dimension.world1=minecraft:the_end nerospace:luna`) and Nerospace will leave that world to you.

## Square planets

Unrelated to shaders: if you would rather have flat, vanilla-style square suns, moons and planets, set
`sky.bodyStyle = square` (see [Configuration](Configuration)). The squares keep the globes' detailed
surfaces at the same resolution, with no shading towards the edge and no glow around it.

## If something goes wrong

The shader support is optional and switches itself off if the shader mod changes in a way it does not
recognise; the game still starts and plays normally. If a world looks wrong with one pack and fine without
shaders, try `shaders.dimensionCompat = off` and report which world it was. Please do not include
personal details in reports.

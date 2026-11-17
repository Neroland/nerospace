# Gallery images

The CurseForge gallery images in this folder (`nerospace_gallery_<nn>_<slug>.png`, 1920x1080, no overlay) are
captured automatically from the game.

## Run it

```
./gradlew :fabric:26.3:captureGallery
```

(`gradlew.bat` on Windows.) This is a **local** task: it opens a Minecraft window on your desktop, creates a
throwaway creative world with a fixed seed, visits each world, stages the shot, saves a 1920x1080 screenshot here
and closes the game. A full run takes about 2.5 minutes (one game launch covers every world). Leave the window alone while it runs.

It is opt-in. Nothing in the normal build depends on it (`build`, `check`, `assemble` and the publish tasks never
run it), and its code lives in the Fabric 26.3 `gametest` source set (`fabric/src/gametest/`), which is not part of
the released mod jar. Crash reporting is switched off for the capture run's own game folder
(`fabric/versions/26.3/runs/gallery/`).

The images contain no player data: the HUD, hand and chat are hidden, no player names or name tags are in frame, and
the run uses the development placeholder account.

## How a shot works

Shots are listed in `SHOTS` in
`fabric/src/gametest/java/za/co/neroland/nerospace/fabric/gametest/GalleryCaptureClientGameTest.java`, in gallery
order. For each one the test:

1. runs `/nerospace gallery sky <world> <state> here`, which sets that world's clock and weather to a named sky state
   and moves the player to the mod's fixed viewing spot on that world (its centre column);
2. runs the shot's build commands at the player's feet (`~` is the viewing spot), if any;
3. pins the camera at the viewing spot plus the shot's offset, with the shot's yaw and pitch;
4. waits for chunks, raises the render distance for the vista, and saves the frame.

The world seed, viewing spots, clock phases and camera poses are fixed, so every run produces the same frames.

## Add or retune a shot

Edit or add a `Shot(slug, world, state, build, offset, yaw, pitch)` entry:

- `world`: the world name used by `/nerospace gallery sky` (`station`, `luna`, `mars`, `greenxertz`, `cindara`,
  `glacira`, `nyxara`).
- `state`: one of that world's sky states (run `/nerospace gallery sky` in game to list them).
- `build`: commands such as `setblock ~2 ~ ~5 nerospace:fuel_tank`, run at the viewing spot.
- `offset`, `yaw`, `pitch`: the camera. Yaw 90 looks west, -90 east, 180 north, 0 south; positive pitch looks down.

Then run the task again and look at the result.

## Note on skies

The planet sky set pieces (Earth from orbit and Earthrise, the eclipse, aurora, ring arc and similar) are drawn only
by the NeoForge 26.3 build. The Fabric capture shows each world's terrain, fog and sky colours, but not those set
pieces.

## Optional overlay tool

`tools/gen_gallery.py` can add a headline, subline and logo badge to a raw capture (shot list:
`gallery.json`). The current gallery style uses plain screenshots, so it is not part of the capture task.

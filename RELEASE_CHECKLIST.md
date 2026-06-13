# Nerospace — 1.0.0 Release Checklist

Versioning goes **straight to 1.0.0** (no public beta), published to **CurseForge** first, **Modrinth
later**. No deadline — quality-driven; every section below gates the release. This file now lists only
the **remaining** work; completed items and anything already documented in the wiki have been pruned.
Tick a box only after the gradle-MCP build + gametests are green AND the in-game check passed.

---

## 1. Feature completion

- [ ] Advancement-triggered toasts for key milestones — confirm the three milestone toasts (first
      launch, first planet, terraformed) visually in runClient

## 2. Audio

- [ ] Subtitles present for every alias

## 3. Config refactor

- [ ] Sanity-check extreme multiplier values don't break machines/atmosphere (confirm 0.1x/10x in
      runClient; code clamps scaled values to ≥1 and launch cost to tank size)

## 4. Balance

- [ ] Oxygen drain/tank/refill rate tuning from playthrough feel — capacity (not drain) is the lever;
      T1-suit 2.5 min flagged as borderline; confirm in the §5 survival run
- [ ] Fuel economy: costs per launch per tier feel right — confirm "feel" in the §5 survival run

## 5. Verification & testing

### Targeted runClient pass (every pending in-game item)
- [ ] Fuel Tank auto-fuel + 3×3 speed bonus + pumping particles/sound
- [ ] Pad gating messages (3×3, T3 Station Wall ring) + grounding on pad break
- [ ] Oxygen drain/refill/suffocation + sealed rooms + airlock refill
- [ ] O₂ HUD gauge + suit badge + vanilla bubble suppression
- [ ] Suits T1/T2 (tank sizes, mixed-set = T1)
- [ ] Machine GUIs (all themed screens, readouts, slots)
- [ ] Machine cap-feed in-world (hopper + pipe into every machine)
- [ ] Rocket UI (fuel readout, trajectory, destination cycling, launch)
- [ ] Mob idle/ambient + walk animations + glow layers in-game
- [ ] Terraformer + oxygen field end-to-end

### Survival playthrough
- [ ] Full start-to-terraform survival playthrough (no creative shortcuts) — log friction/balance notes

### Multiplayer
- [ ] Dedicated-server pass (`runServer` + 2 clients): oxygen sync, rocket rides, pipe networks,
      GUIs, world join (registry sync), suit HUD per player

### Automated
- [ ] Gametest suite green (`gradlew runGameTestServer`) including tests for all new 1.0 features
- [ ] `ecjCheck` clean
- [ ] `runData` + `build` green via gradle MCP

## 6. Performance (release gate — "it needs to be performant")

- [ ] Stress-test large pipe networks (hundreds of pipes/machines)
- [ ] Stress-test oxygen field: large sealed rooms, multiple players, terraformed areas
- [ ] Spark/profiler pass on a busy dedicated server; fix anything hot
- [ ] Client FPS check around pipe rendering / particles / many mobs

## 7. Distribution & legal

- [ ] `PRIVACY.md` current; Sentry opt-out + POPIA/GDPR scrubbing re-verified on the **release build**
      — REMAINING: the end-to-end release-jar Sentry test (see §10.3)
- [ ] Version set to 1.0.0; `publish.yml` release flow tested end-to-end — REMAINING: add missing
      secrets `CURSE_FORGE_API_TOKEN` + `SENTRY_ORG`, then the publish itself IS the end-to-end test
      (see §10.4 — ⚠️ pushing the gradle.properties bump with secrets in place publishes 1.0.0)
- [ ] Modrinth listing + publish.yml target — REMAINING (human): re-add the `MODRINTH_API_TOKEN`
      secret (it did NOT save — verify with `gh secret list`) + the page fields/gallery
      (see §10.1b); goes live when the first version clears Modrinth review

## 8. Marketing & community

- [ ] Screenshot/gallery set: rockets, launches, planets, machines, creatures, terraforming —
      *human-only; 10-shot list + specs in §10.5*
- [ ] Trailer/showcase video (launch + planets + terraforming) — *human-only; 9-beat script +
      tooling (OBS + DaVinci Resolve) in §10.6*
- [ ] Discord linked from CurseForge + GitHub About — server EXISTS (discord.gg/ArPXvYUzJG) and is
      linked from README/roadmap/description/wiki/issue templates; REMAINING: the CurseForge +
      GitHub About fields (see §10.1a, §10.7)

## 9. Pre-launch

- [ ] Private playtests: a few friends on a dedicated server with the release candidate jar
- [ ] Fix-up pass from playtest feedback
- [ ] Fresh-world + existing-world (datafix/migration) load check on the final jar
- [ ] Final jar built from a tagged commit; gametests + manual smoke test on THAT jar
- [ ] Publish 1.0.0 🚀

## 10. Human-only release steps (external accounts, uploads, media)

Everything here needs **Dario** — external accounts, uploads, media. The in-repo work is done; the
steps below are what to do, and in what order, at release time.

### 10.1a CurseForge project settings

Project: <https://www.curseforge.com/minecraft/mc-mods/nerospace>

- [ ] **Description:** paste the rendered content of `art/curseforge_description.md` (it already
      contains the telemetry blurb, modpack permission, wiki/Discord/issue links).
- [ ] **License:** choose **Custom License**, name it "All Rights Reserved (modpacks allowed — see
      LICENSE)" and point it at <https://github.com/Neroland/nerospace/blob/main/LICENSE>.
- [ ] **External links:** Issues → GitHub issues URL, Wiki →
      <https://github.com/Neroland/nerospace/wiki>, Source → the repo.
- [ ] **Discord:** add `https://discord.gg/ArPXvYUzJG` (project social/Discord field).
- [ ] **Settings → allowed in modpacks:** ensure "Allow project to be added to modpacks" is ON — it
      must match the LICENSE text.
- [ ] **Gallery:** upload the screenshot set (§10.5).

### 10.1b Modrinth project settings

Project: <https://modrinth.com/mod/nerospace> (created 2026-06-12; the first uploaded version goes
through Modrinth moderation review before the page goes public).

- [ ] **Whole project page: automated** — `.github/workflows/modrinth-description.yml` PATCHes
      everything in one run: the body (from `art/modrinth_description.md`), the icon
      (`art/logo/nerospace_logo_400.png`; the 1024px master is over Modrinth's 256 KiB cap), and the
      pinned metadata — title, summary, categories (technology/worldgen/adventure), client+server =
      required, issue/source/wiki/Discord links, and the **Custom license** (`LicenseRef-Custom` →
      the LICENSE on GitHub). Make sure the §7 secrets exist, then trigger it once and
      **resubmit for review**.
- [ ] **Gallery:** same screenshot set as CurseForge (§10.5) — the only manual page item.
- [ ] File uploads themselves are automatic — `publish.yml` targets CurseForge **and** Modrinth in
      the same run.

### 10.2 GitHub repo metadata

- [ ] Repo **About**: description ("Space-progression mod for NeoForge — rockets, oxygen survival,
      player stations, terraforming"), website = the CurseForge page, topics (`minecraft`,
      `neoforge`, `minecraft-mod`, `space`, `terraforming`).
- [ ] **Publish the wiki:** the GitHub wiki is a separate git repo. From a scratch folder, clone
      `https://github.com/Neroland/nerospace.wiki.git`, copy the repo's `wiki/*.md` over it, commit,
      push. (`_Sidebar.md` becomes the sidebar automatically.) Re-do this whenever `wiki/` changes —
      the in-repo `wiki/` folder is the source of truth.
- [ ] **Discussions:** the issue templates link to GitHub Discussions — enable the feature
      (Settings → General → Features) if it isn't already.

### 10.3 Release-jar telemetry verification (§7 PRIVACY sign-off)

On the **final release candidate jar** (not a dev run):

1. Launch a normal client with the jar in `mods/`, create/join a world.
2. `/give @s nerospace:sentry_test`, place it. Chat should confirm the event was sent.
3. Check the Sentry dashboard (environment **production**, release `nerospace@1.0.0`) for the
   synthetic "Sentry test block" event — confirm **no IP, no username, no home-dir paths**.
4. Set `telemetryEnabled = false` in `config/nerospace-common.toml`, reload/restart, place the block
   again — chat must say telemetry is disabled and **nothing** must arrive in Sentry.
5. Re-enable telemetry, delete the block. Done — tick the §7 PRIVACY box's "release build" clause.

### 10.4 Go-live order (the push that publishes)

⚠️ `publish.yml` fires on any push to `main` that touches `gradle.properties`. The version is
already bumped to 1.0.0 in the working tree, so **the publish happens on whatever push lands that
change** — sequence accordingly:

1. Finish §5–§6 + §9 gates (playtests, performance, final jar checks).
2. Add the secrets from §7.
3. If release day isn't 2026-06-11, update the date in `CHANGELOG.md`'s `## [1.0.0]` heading.
4. Commit everything; push to `main`. The workflow: builds → creates the Sentry release → uploads to
   CurseForge **and Modrinth** (as **release**-type files, changelog section as release notes) →
   creates the GitHub release + `v1.0.0` tag.
5. Watch the run (`gh run watch`); CurseForge files sit in moderation briefly, and Modrinth also
   reviews the project itself on its first version.
6. After approval: check the file page renders, then run §10.3 against the *published* jar if you
   didn't already on the identical RC.
7. If a publish half-fails **after** the tag was created: delete the `v1.0.0` tag/release and re-run
   the workflow (documented at the top of `publish.yml`).

### 10.5 Screenshot / gallery shot list

CurseForge gallery: upload **1920×1080** (16:9) PNGs; the first image becomes the card thumbnail —
make it the money shot. Take them fullscreen 1080p+ with HUD hidden (F1) unless the HUD *is* the
subject. Suggested set (≈10):

1. **Hero:** Tier 4 rocket on the Heavy Launch Complex at dusk, gantry beside it, fuel tank line
   visible. (Thumbnail.)
2. Tier 1 rocket lifting off — particles mid-launch.
3. The four rockets side by side on pads (creative lineup; shows per-tier geometry).
4. Greenxertz vista with a Xertz Stalker + Quartz Crawler in frame.
5. Cindara: Cinder Stalker in a lava-lit basin, player in Thermal Suit with HUD badge visible (keep
   HUD on — show "SUIT HEAT").
6. Glacira: Frost Strider against the starfield, player in Cryo Suit.
7. Machine room: grinder + generators + batteries laced with Universal Pipes carrying visible
   items/streams; GUIs open in a second shot if needed.
8. Star Guide GUI open mid-progression (chapters + glowing completed nodes).
9. Terraforming before/after split: barren ground vs Living-stage land with rain, trees, livestock
   (Meadow Loper herd).
10. A founded station: platform + Station Core + rocket parked, nameplate/UI showing the custom
    station name.

Cheapest route: a creative world + `/nerospace gallery` for the lineup shots, a staged terraform
world for 9–10.

### 10.6 Trailer / showcase video outline

Target: **60–90 s**, 1080p60, hosted on **YouTube** (link it on the CurseForge page; CF supports a
featured video URL). Tooling: DaVinci Resolve for the edit; install **OBS Studio** (free) for
capture — game capture at 1080p60; record voice-over separately or use text cards only.

Script (each beat ≈ 6–10 s, cut on the beat of the music):

1. **Cold open** — black, "One ore starts it." → pickaxe breaks nerosium ore.
2. Grinder + pipes montage — dust pours, energy streams pulse, GUIs animate.
3. First launch — Tier 1 on the 3×3 pad, liftoff, cut to the Orbital Station.
4. Worlds montage — Greenxertz pan → Cindara embers + Stalker lunge → Glacira frost vignette
   ("SUIT COLD" flash). Card: "Four worlds. None of them want you alive."
5. Survival beat — sealed base, O₂ HUD draining, airlock refill.
6. Heavy Launch Complex — gantry boarding, Tier 4 launch. Card: "Go heavy."
7. Station founding — anvil rename "Haven-1", FOUND node click, new platform reveal.
8. Terraforming crescendo — timelapse of Rooted → Hydrated → Living, first rain, livestock herd.
   Card: "Make it rain. Literally."
9. Outro — Star Guide tree zooms out to the logo + "Nerospace 1.0 — on CurseForge now" +
   Discord/wiki URLs.

Capture tips: F1 for clean shots, no night-vision gamma, `/gamerule doDaylightCycle false` while
framing; record everything at 1.5× the length you need so Resolve has trim room.

### 10.7 Discord wiring

Server exists: `https://discord.gg/ArPXvYUzJG` (permanent invite, grants the Minecraft role). The
repo/description links are already wired in. Remaining:

- [ ] Add the invite to the CurseForge project (§10.1a) and the GitHub About sidebar.
- [ ] Channels worth having before the announcement: `#announcements`, `#support`, `#bug-reports`
      (pinned link to the GitHub issue forms), `#showcase`, `#modpack-makers`.
- [ ] Pin the wiki + `PRIVACY.md` links in `#support`.
- [ ] Post the 1.0.0 announcement in `#announcements` when the CurseForge file clears moderation.

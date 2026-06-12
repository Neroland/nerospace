# Nerospace 1.0.0 — human-only release runbook

Everything in this file needs **Dario** (external accounts, uploads, media). The in-repo work for
checklist §9–§10 is done; each section below says exactly what to do and in what order. Delete this
file (or keep it as a template) after 1.0.0 ships.

---

## 2. CurseForge project settings

Project: <https://www.curseforge.com/minecraft/mc-mods/nerospace>

- [ ] **Description:** paste the rendered content of `art/curseforge_description.md` (it already
      contains the required telemetry blurb, modpack permission, wiki/Discord/issue links).
- [ ] **License:** in project settings choose **Custom License**, name it
      "All Rights Reserved (modpacks allowed — see LICENSE)" and point it at
      <https://github.com/Neroland/nerospace/blob/main/LICENSE>.
- [ ] **External links:** set Issues → GitHub issues URL, Wiki →
      <https://github.com/Neroland/nerospace/wiki>, Source → the repo.
- [ ] **Discord:** add `https://discord.gg/ArPXvYUzJG` (project social/Discord field).
- [ ] **Settings → allowed in modpacks**: ensure "Allow project to be added to modpacks" (or the
      current equivalent toggle) is ON — it must match the LICENSE text.
- [ ] **Gallery:** upload the screenshot set (section 6).

## 2b. Modrinth project settings

Project: <https://modrinth.com/mod/nerospace> (created 2026-06-12; the first uploaded version
goes through Modrinth moderation review before the page goes public).

- [ ] **Whole project page: automated** — `.github/workflows/modrinth-description.yml` PATCHes
      everything in one run: the body (from `art/modrinth_description.md`), the icon
      (`art/logo/nerospace_logo_400.png`; the 1024px master is over Modrinth's 256 KiB cap),
      and the pinned metadata — title, summary, categories (technology/worldgen/adventure),
      client+server = required, issue/source/wiki/Discord links, and the **Custom license**
      (`LicenseRef-Custom` → the LICENSE on GitHub; this is the field AutoMod's
      "Missing License" check reads). Runs on changes to the sources on `main` or via
      workflow_dispatch; make sure the secrets from §1 exist, then trigger it once and
      **resubmit for review**.
- [ ] **Gallery:** same screenshot set as CurseForge (section 6) — the only manual page item.
- [ ] File uploads themselves are automatic — publish.yml now targets CurseForge **and**
      Modrinth in the same run.

## 3. GitHub repo metadata

- [ ] Repo **About**: description ("Space-progression mod for NeoForge — rockets, oxygen survival,
      player stations, terraforming"), website = the CurseForge page, topics
      (`minecraft`, `neoforge`, `minecraft-mod`, `space`, `terraforming`).
- [ ] **Publish the wiki**: the GitHub wiki is a separate git repo. From a scratch folder:
      ```
      git clone https://github.com/Neroland/nerospace.wiki.git
      copy the repo's wiki/*.md over it, commit, push
      ```
      (`_Sidebar.md` becomes the sidebar automatically.) Re-do this whenever `wiki/` changes —
      the in-repo `wiki/` folder is the source of truth.
- [ ] **Discussions**: the issue templates link to GitHub Discussions — enable the feature
      (Settings → General → Features) if it isn't already.
- [ ] GitHub does not auto-detect custom licenses; the README License section + LICENSE file are
      already in place, nothing else needed.

## 4. Release-jar telemetry verification (§9.4 sign-off)

On the **final release candidate jar** (not a dev run):

1. Launch a normal client with the jar in `mods/`, create/join a world.
2. `/give @s nerospace:sentry_test`, place it. Chat should confirm the event was sent.
3. Check the Sentry dashboard (environment **production**, release `nerospace@1.0.0`) for the
   synthetic "Sentry test block" event. Confirm the event shows **no IP, no username, no home-dir
   paths**.
4. Set `telemetryEnabled = false` in `config/nerospace-common.toml`, reload (or restart), place the
   block again — chat must say telemetry is disabled and **nothing** must arrive in Sentry.
5. Re-enable telemetry, delete the block. Done — tick the §9 PRIVACY box's "release build" clause.

## 5. Go-live order (the push that publishes)

⚠️ `publish.yml` fires on any push to `main` that touches `gradle.properties`. The version is
already bumped to 1.0.0 in the working tree, so **the publish happens on whatever push lands that
change** — sequence accordingly:

1. Finish checklist §6–§8 + §11 gates (playtests, performance, final jar checks).
2. Add the secrets from section 1.
3. If the release day isn't 2026-06-11, update the date in `CHANGELOG.md`'s `## [1.0.0]` heading.
4. Commit everything; push to `main`. The workflow: builds → creates the Sentry release → uploads
   to CurseForge **and Modrinth** (as **release**-type files, with the changelog section as
   release notes) → creates the GitHub release + `v1.0.0` tag.
5. Watch the run (`gh run watch`); CurseForge files sit in moderation for a short while, and
   Modrinth additionally reviews the project itself on its first version.
6. After approval: check the file page renders, then run section 4 against the *published* jar if
   you didn't already on the identical RC.
7. If a publish half-fails **after** the tag was created: delete the `v1.0.0` tag/release and
   re-run the workflow (documented at the top of publish.yml).

## 6. Screenshot / gallery shot list

CurseForge gallery: upload **1920×1080** (16:9) PNGs; the first image becomes the card thumbnail —
make it the money shot. Take them in fullscreen 1080p+ with HUD hidden (F1) unless the HUD *is* the
subject. Suggested set (≈10):

1. **Hero:** Tier 4 rocket on the Heavy Launch Complex at dusk, gantry beside it, fuel tank line
   visible. (Thumbnail.)
2. Tier 1 rocket lifting off — particles mid-launch.
3. The four rockets side by side on pads (creative lineup; shows per-tier geometry).
4. Greenxertz vista with a Xertz Stalker + Quartz Crawler in frame.
5. Cindara: Cinder Stalker in a lava-lit basin, player in Thermal Suit with HUD badge visible
   (keep HUD on for this one — show "SUIT HEAT").
6. Glacira: Frost Strider against the starfield, player in Cryo Suit.
7. Machine room: grinder + generators + batteries laced with Universal Pipes carrying visible
   items/streams, GUIs open in a second shot if needed.
8. Star Guide GUI open mid-progression (chapters + glowing completed nodes).
9. Terraforming before/after split: barren ground vs Living-stage land with rain, trees, and
   livestock (Meadow Loper herd).
10. A founded station: platform + Station Core + rocket parked, nameplate/UI showing the custom
    station name.

Cheapest route to all of these: a creative world + `/nerospace gallery` for the lineup shots, a
staged terraform world for 9–10.

## 7. Trailer / showcase video outline

Target: **60–90 s**, 1080p60, hosted on **YouTube** (link it on the CurseForge page; CF supports a
featured video URL). Tooling you have: DaVinci Resolve for the edit. For capture, install **OBS
Studio** (free) — game capture at 1080p60; record voice-over separately or use text cards only.

Script (each beat ≈ 6–10 s, cut on the beat of the music):

1. **Cold open** — black, the words "One ore starts it." → pickaxe breaks nerosium ore.
2. Grinder + pipes montage — dust pours, energy streams pulse, GUIs animate.
3. First launch — Tier 1 on the 3×3 pad, liftoff, cut to the Orbital Station.
4. Worlds montage — Greenxertz pan → Cindara embers + Stalker lunge → Glacira frost vignette
   ("SUIT COLD" flash). One card: "Four worlds. None of them want you alive."
5. Survival beat — sealed base, O₂ HUD draining, airlock refill.
6. Heavy Launch Complex — gantry boarding, Tier 4 launch. Card: "Go heavy."
7. Station founding — anvil rename "Haven-1", FOUND node click, new platform reveal.
8. Terraforming crescendo — timelapse of Rooted → Hydrated → Living, first rain, livestock herd.
   Card: "Make it rain. Literally."
9. Outro — Star Guide tree zooms out to the logo + "Nerospace 1.0 — on CurseForge now" +
   Discord/wiki URLs.

Capture tips: F1 for clean shots, night-vision-free gamma, `/gamerule doDaylightCycle false` while
framing; record everything at 1.5× the length you need so Resolve has trim room.

## 8. Discord wiring

Server exists: `https://discord.gg/ArPXvYUzJG` (permanent invite, grants the Minecraft role). The
repo/description links are already wired in. Remaining:

- [ ] Add the invite to the CurseForge project (section 2) and the GitHub About sidebar.
- [ ] Channels worth having before the announcement: `#announcements`, `#support`,
      `#bug-reports` (pinned link to the GitHub issue forms), `#showcase`, `#modpack-makers`.
- [ ] Pin the wiki + PRIVACY.md links in `#support`.
- [ ] Post the 1.0.0 announcement in `#announcements` when the CurseForge file clears moderation.

## 9. Deferred (post-1.0, already noted in the checklist)

- JEI/EMI integration when they reach 26.1.

(Modrinth was originally deferred but is now fully wired — see §1 and §2b.)

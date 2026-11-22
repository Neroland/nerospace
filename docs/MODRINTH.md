# Modrinth

Nerospace has a project page at <https://modrinth.com/mod/nerospace>. Publishing a file there is a
manual step. This repository's publish workflow uploads to CurseForge and GitHub only.

A Modrinth release is not valid until both of these are true:

1. **Neroland Core is published on Modrinth** for the same loader and Minecraft versions as the
   Nerospace file. Core is a required mod and must load before Nerospace. There is no Core project
   on Modrinth yet, so a Nerospace file uploaded alone will not start.
2. **That Core project is declared as a required dependency** on the Nerospace version (Modrinth
   `required` dependency, not optional). The in-game manifests already require Core; the Modrinth
   version metadata has to say the same thing or the installer will not fetch it.

Do not add a Modrinth upload job to `.github/workflows/publish.yml` until both are done.

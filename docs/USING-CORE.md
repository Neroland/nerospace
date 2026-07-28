# Neroland Core Dependency

Nerospace `1.0.0-beta.8` requires Neroland Core `1.8.0` or newer within the 1.x API line. Each loader and
Minecraft version consumes its matching artifact:

```text
za.co.neroland.nerolandcore:nerolandcore-<loader>-<mc>:1.8.0
```

Local development resolves Maven Local first. Publish the sibling checkout with
`../neroland-core/gradlew publishToMavenLocal`. CI and fresh clones fall back to the Core GitHub Packages
repository configured in `stonecutter.gradle`, using `GITHUB_ACTOR` / `GITHUB_TOKEN` or Gradle
`gpr.user` / `gpr.key` credentials with `read:packages`.

Core is a separate required mod and is not embedded in Nerospace artifacts.

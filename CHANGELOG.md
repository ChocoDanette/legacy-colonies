# Changelog

Each version is published as a GitHub release with its jar. To release, set `mod_version` in
`gradle.properties`, add its section below, merge to `main`, then run the Release workflow
from the Actions tab (it creates the tag `v<mod_version>`), or push that tag yourself.

## 0.3.0

- Inn: shelter for citizens without a house (4 beds at level 1, 2 more per level, up to level 5). It takes no visitors.
  Its plans `inn1` to `inn5` come from the modpack's structure pack.

## 0.2.1

- The colony campfire can be placed by hand (right click), without the build tool.

## 0.2.0

- Colony campfire: founds the colony and acts as its town hall.
- Invisible builder's post above the campfire: the first citizen is hired as builder.

## 0.1.0

- Empty NeoForge 1.21.1 addon depending on MineColonies 1.1.1403.

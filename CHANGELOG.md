# Changelog

Each version is published as a GitHub release with its jar. To release, set `mod_version` in
`gradle.properties`, add its section below, merge to `main`, then run the Release workflow
from the Actions tab (it creates the tag `v<mod_version>`), or push that tag yourself.

## 0.5.0

- Builder's register (`legacycolonies:register`, 3 straw and a stick): right click a hut with it to validate a building
  of the player's own design. The hut must stand inside a closed, covered room; the room's blocks are checked against
  the rules of the hut's next level. When they pass, the hut reaches that level as if the builder had built it (quests,
  Legacy stages and advancements follow), and the beds, racks and chests of the room are registered. Otherwise the
  chat lists what is missing.
- Rules are data files `data/<namespace>/legacycolonies/building_rules/<name>.json` (hut, then per level: `max_volume`,
  `door`, `light`, and `requires` with block ids or `#tags` and a count). A modpack can replace them. The jar ships the
  inn's level 1: a closed room only.

## 0.4.0

- Materials per building: right click a hut with a TFC log or mud brick block to choose that material. When the
  builder receives the plan, every block of the ash wood family and of the entisol mud bricks is replaced by the same
  block of the chosen material, keeping its orientation. Without a choice the plan is built as drawn.
- The resource list shown before building lists the chosen materials.
- Daubed wattle (`tfc:wattle/unstained` and the stained ones) is requested as a wattle and a daub, plus the dye
  for stained wattle, and the builder places the finished block.
- The builder clears blocks by hand when it has no fitting tool, instead of asking for a pickaxe.
- Sneak and right click the colony campfire with an empty hand to open the builder's stock, where it leaves its
  unused materials.
- The builder fills the ground with TFC dirt: by default the dirt the campfire stands on. Right click the campfire
  with another TFC dirt block to change it.

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

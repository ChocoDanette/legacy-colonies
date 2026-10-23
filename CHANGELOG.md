# Changelog

Each version is published as a GitHub release with its jar. To release, set `mod_version` in
`gradle.properties`, add its section below, merge to `main`, then run the Release workflow
from the Actions tab (it creates the tag `v<mod_version>`), or push that tag yourself.

## 0.6.1

- Who builds a hut is chosen once. Giving the hut to the builder (build or upgrade from its window) makes every level
  come from the builder and its plans: the "Confirm building" button no longer shows for that hut. Confirming a
  building by hand makes every level go through the rules: the build button no longer shows (the confirm button takes
  its place), and the builder refuses to build, upgrade or repair the hut. Breaking the hut forgets the choice. Huts
  built before this version have no choice yet.
- The jar ships the inn's levels 2 and 3: a covered room, openings allowed, with 1 bed for level 2 and 2 beds for
  level 3 (any bed, TFC thatch beds included).

## 0.6.0

- Three new huts of the addon, without workers, to replace MineColonies' own: the forge (`legacycolonies:forge`), the
  stable (`legacycolonies:stable`) and the warehouse (`legacycolonies:warehouse`). Each goes up to level 5. For now they
  look like MineColonies' blacksmith, cowherd and warehouse huts.
- The jar ships the forge's level 1 rules: a covered room, openings allowed, with a TFC forge and an anvil (stone or
  metal, tag `#tfc:anvils`). The stable and the warehouse have no rules yet.
- Rock bricks are a third material per building: plans use andesite bricks (`tfc:rock/bricks/andesite`, its stairs,
  slabs and walls). Right click a hut with a TFC rock brick block to use that rock instead.
- The builder can place TFC firepits, charcoal forges and stone anvils from a plan. A firepit takes a log of the
  building's wood, 3 sticks and 3 straw, and is placed unlit; a forge takes 7 charcoal and is placed cold; a stone anvil
  takes a raw block of its rock.
- The rock of stone anvils is a fourth material per building: plans use dacite. Right click a hut with a raw block of an
  igneous rock (the only ones TFC makes anvils of) to use that rock instead.

## 0.5.0

- Huts are placed without MineColonies' "use the build tool" window. Right click a block with a hut to place it there,
  unbuilt (level 0); break it to get it back. Right click the air with a hut to open the build tool.
- Buildings of the player's own design: build around an unbuilt hut, open its window and press "Confirm building",
  next to the build button. The room around the hut is checked against the rules of the hut's next level. When they
  pass, the hut reaches that level as if the builder had built it (quests, Legacy stages and advancements follow), and
  the beds, racks and chests of the room are registered. Otherwise the chat lists what is missing. The button only
  shows for hut levels that have rules.
- Rules are data files `data/<namespace>/legacycolonies/building_rules/<name>.json` (hut, then per level:
  `max_volume`, `closed`, `door`, `light`, and `requires` with block ids or `#tags` and a count). With
  `"closed": false` the room only needs a roof: doorways and windows without glass are allowed. A modpack can replace
  the rules. The jar ships the inn's level 1: a covered room, openings allowed.

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

# Changelog

Each version is published as a GitHub release with its jar. To release, set `mod_version` in
`gradle.properties`, add its section below, merge to `main`, then run the Release workflow
from the Actions tab (it creates the tag `v<mod_version>`), or push that tag yourself.

## 0.7.0

- Colony needs by era. The modpack tells the addon the era of each colony (`ColonyNeeds.setColonyEra`, called by its
  KubeJS colony script); a colony starts in the primitive age. Each era, in
  `data/<namespace>/legacycolonies/colony_eras/`, sets the most citizens the colony may have, whether each citizen needs
  a bed, the game days between two new citizens, and how many huts of a kind, and up to which level, the colony may build.
  The jar ships three eras: primitive (1 citizen, no bed, inn level 1), copper (4 citizens, one bed each, inn up to
  level 3, 2 houses of level 1) and iron (9 citizens, one bed each, inn up to level 4, 3 houses up to level 2). Later
  eras keep the iron rules until they get their own file.
- New citizens come from outside: while the colony has room in its era (and a free bed when the era needs beds), a
  citizen arrives every 2 game days at the edge of the colony's first claim, and walks in. The first citizen also
  arrives from the edge instead of appearing on the campfire. MineColonies' children are turned off.
- A hut level, or one more hut, that the era does not allow is refused: when placing the hut, when confirming a
  hand-built level, and by the builder.
- New hut: the house (`legacycolonies:house`), one citizen per bed, up to 1 bed at level 1 and 2 beds at level 2.
  Rules for building it by hand: a covered room with a door and 1 bed (level 1), 2 beds (level 2). It looks like
  MineColonies' residence for now.
- The inn and the house count only their real beds, still in place, up to one bed per level above 1 for the inn (none
  at level 1, 2 at level 3, 3 at level 4) and one per level for the house. The inn used to hold 4 citizens at level 1
  whatever its beds. Rules for the inn's level 4: 3 beds.
- The builder only asks for base materials: lumber, brick blocks, mud brick blocks, logs. It crafts the derived blocks
  itself with the player's TFC recipes (stairs, slabs, walls, plank blocks, fences, fence gates, doors, trapdoors,
  chests, barrels): one craft makes a whole batch, for example 8 stairs from 6 brick blocks, and the builder keeps the
  rest in its inventory for the next blocks of that kind. The list of materials of a plan counts whole batches.
- `/legacycolonies needs` shows the era of the colony, its citizens, beds and houses, and when the next citizen arrives.

## 0.6.1

- Who builds a hut is chosen once. Giving the hut to the builder (build or upgrade from its window) makes every level
  come from the builder and its plans: the "Confirm building" button no longer shows for that hut. Confirming a
  building by hand makes every level go through the rules: the build button no longer shows (the confirm button takes
  its place), and the builder refuses to build, upgrade or repair the hut. Breaking the hut forgets the choice. Huts
  built before this version have no choice yet.
- The jar ships the inn's levels 2 and 3: a covered room, openings allowed, with 1 bed for level 2 and 2 beds for
  level 3 (any bed, TFC thatch beds included).
- The builder can place TFC thatch beds from a plan. A thatch bed takes 2 thatch and 1 large raw hide, asked at the
  foot of the bed; both halves are placed at once, and breaking the bed gives the thatch and the hide back.
- The builder keeps filling while its fill slides. TFC dirt at the edge of a drop slides away (TFC landslides) after the
  builder has moved on, leaving a hole in the foundation. Now the slide is played out as soon as the fill is placed (the
  block comes to rest lower down) and the spot is filled again, up to 16 times, until the fill stays in place. Each
  extra fill block comes from the builder's stock; when it runs out, the last fill is left to slide.

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

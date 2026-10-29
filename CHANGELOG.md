# Changelog

Each version is published as a GitHub release with its jar. To release, set `mod_version` in
`gradle.properties`, add its section below, merge to `main`, then run the Release workflow
from the Actions tab (it creates the tag `v<mod_version>`), or push that tag yourself.

## 0.9.1

- No diseases: citizens never get sick, and a citizen already sick is cured. The MineColonies cures (vanilla carrot,
  potato…) do not exist with TFC.
- A hurt citizen no longer leaves work to look for a hospital.
- No mourning: when a citizen dies, the others keep working.
- No complaints from citizens (no bed, hungry, no job, need barracks…). Item requests stay, as the builder asks for its
  materials through them, and so do the builder's own messages. Complaints saved before are dropped when the citizen
  loads.
- No MineColonies hunger: citizens stay full and never look for food. From the iron age, the Legacy rations feed the
  colony.
- Happiness only counts the bed, sleep, the Legacy rations, damage, deaths and quests. The MineColonies needs for
  barracks (security), tavern (social), mystical site, school, food variety, jobs and health are removed.

## 0.9.0

- New hut without a worker: the school (`legacycolonies:school`), with the MineColonies school model. Level 1 needs a
  lectern and 2 bookshelves, level 2 a lectern and 4 bookshelves (TFC wood or vanilla). A colony may build one: level 1
  from the copper age, level 2 from the iron age. The modpack uses its level to let the player choose the traits of the
  heir.

## 0.8.3

- Beds no longer get lost. When a citizen goes to sleep, MineColonies forgets a registered bed that is not in the
  `minecraft:beds` tag, and the TFC thatch bed is not (the modpack now adds it). A bed placed by a player is not
  registered either. A house or an inn with fewer beds than its level allows now looks for the beds inside it (at most
  every 10 seconds) and registers them again.

## 0.8.2

- Giving a Legacy hut to the builder no longer fails. A hut placed with a right click got the path of its plan in the
  background, after its building was created, so the building kept an empty path and MineColonies crashed on the
  builder's work order (`StringIndexOutOfBoundsException` in `WorkOrderBuilding.create`): the forge could not be given
  to the builder. The plan is now found at once, by its file name (`<hut><level>.blueprint` in the Legacy pack).
- A Legacy hut always takes its plans from the Legacy pack, even when it was placed while the colony was in another
  style (an inn placed in Colonial looked for `colonial/null2.blueprint` and could not be upgraded). Huts already
  placed are fixed on their next load or work order.
- When the Legacy pack has no plan for the next level of a hut, the builder refuses with a message instead of doing
  nothing.

## 0.8.1

- No more "Error loading blueprint: ...null1.blueprint" in the log. MineColonies does not know the Legacy huts, so a hut
  without a plan path got `null1.blueprint`. A hut built by hand, the builder's post and the campfire have no plan:
  their prestige, block data and corners no longer load one. A hut for the builder gets the path of its plan in the
  pack back, at its level (this also fixes a hut placed before its plans were in the pack).

## 0.8.0

- Food, from the iron age (`"food": true` in an era file). At the start of each game day, each citizen takes one food
  that is not rotten (TFC decay) from the Firmalife food shelves and hangers in the room of the colony's granaries, and
  is then full. Only shelves and hangers that the climate station finds in a working cellar count; chests and barrels
  are not used. The first rations are taken the day after the colony enters the iron age.
- A citizen without a ration gets unhappy (a little after one day, more after two) and so works more slowly, and the
  players of the colony get a message. No new citizen arrives while a citizen went without a ration, or while the
  granaries could not feed one more.
- New hut: the granary (`legacycolonies:granary`), a Firmalife cellar. Rules for building it by hand: a closed room with
  a door, a climate station and 1 food shelf or hanger (level 1), 2 (level 2), 4 (level 3). The iron age allows one granary, up to level 2; earlier
  ages none. It looks like MineColonies' restaurant for now.
- `/legacycolonies needs` shows the rations in the granaries and why no citizen arrives.

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

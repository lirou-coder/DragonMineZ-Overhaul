# Prestige System

Prestige is Dragon Mine Z: Overhaul's rebirth system. It lets a character break the current level limit in exchange for restarting part of their progression while earning permanent growth.

The system only operates while the Overhaul leveling system is enabled. If the separate **DMZ Prestige addon** is installed, Overhaul Prestige is automatically disabled to prevent the two systems from controlling the same progression.

## Unlocking Prestige

To make the Prestige button appear, you must:

- Reach the level cap for your current Prestige count.
- Complete the final quest of the Overhaul Prestige saga.
- Be below the server's maximum Prestige count.

The button appears in the Statistics page. Confirming is not allowed while you are in a Dragon Mine Z party, so leave the party first.

## What Prestige grants

Every Prestige count permanently increases configurable parts of your progression:

- Race and class stat scaling
- General TP gain
- Transformation mastery gain
- Story quest rewards
- The maximum level available for the next journey

Enemies created by story quests also gain configurable HP, Melee Damage, and Ki Damage. This increase stacks with the selected story difficulty, so later journeys are more rewarding and more dangerous.

The Statistics page shows your current Prestige count. Its tooltip displays the active scale, mastery, and saga difficulty multipliers. Prestige also appears as its own source in the TP multiplier breakdown.

## What is reset

The default rebirth returns base attributes to the merged starting values of your [[Race and Class]], removes ordinary stat bonuses, removes non-passive skills, resets forms and form mastery, clears TP and Dynamic Growth, sets Release to zero, and restarts story progress and difficulty selection.

Race and class passive skills are preserved as part of the character's identity, while racial progression such as accumulated racial bonuses is reset.

Server owners can configure these losses individually. Skills, forms, bonuses, and quests can be preserved, and stat loss can use a percentage instead of returning directly to starting attributes.

## Level caps

The first Prestige uses the configured initial level cap. Each successful Prestige raises that cap toward the global maximum level. The progression is divided across the configured maximum number of Prestiges, and the final Prestige reaches the global cap.

If a per-attribute maximum is enabled, the server may also scale the assignable attribute limit with the current Prestige cap. This prevents a new character from reaching the final-game attribute ceiling before completing the required rebirths.

Prestige is permanent character progression. Read the confirmation screen carefully before accepting.

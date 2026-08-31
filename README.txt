Dragon Mine Z: Overhaul
=======================

Dragon Mine Z: Overhaul is a gameplay addon for Dragon Mine Z. It rebuilds a large part of the player experience around speed, movement, class identity, racial passives, Ki technique building, battle power scaling, and cleaner information in the DMZ menus.

This addon is made for players who want DMZ to feel faster, more specialized, and more RPG-driven without replacing the core Dragon Mine Z progression loop.


Requirements
------------

- Minecraft 1.20.1
- Forge 47+
- Dragon Mine Z 2.1 or newer


Core Gameplay Changes
---------------------

- SKP is presented as SPD. Instead of being focused on strike super damage, it now represents speed.
- SPD affects movement speed and attack speed, while together with other attributes it also affects swiming speed and Flight speed.
- Movement speed is controlled: instead of applying the bonus all the time and having too high speed every time, speed is capped at a max of 200%, and it slowly goes up to it's max speed when running, good for both combat and exploring!
- Very high speed can let players sprint over water or lava surfaces.
- Sprinting no longer drains hunger through vanilla exhaustion.
- Dynamic Growth gives SPD experience for running and perfect evasion, STR/SPD for swimming, RES for staying underwater, and STR instead of SKP from strike attack practice.


Class System
------------

The addon separates race stats from class stats and gives every built-in class a meaningful passive. Servers can tune class stats, names, colors, and race restrictions through generated class config files.

- Warrior: melee and strike hits build Fury stacks. Fury improves stamina regeneration and defense penetration (same as original, but to this mod's vision)
- Berserker: becomes more dangerous at low health, gaining critical chance and critical damage as health drops. (also same as original but to this mod's vision)
- Spiritualist: same as original
- Martial Artist: deals bonus melee, strike, and Ki damage to enemies below half health.
- Enchanter/Cleric: Same as original
- Paladin: same as original
- Tank: same as original
- Speedster: melee and strike hits build Momentum stacks. Momentum increases SPD and adds a melee damage bonus based on speed.
- Duelist: rewards timing and guard pressure with stronger parries, faster redirected Ki blasts, and extra damage/knockback against guard-broken targets.


Racial Rework
-------------

Races have stronger identities and several races use custom active or passive mechanics, some even inspired by Xenoverse race passives!

- Human uses Ki Boosting Body. High Ki improves combat stats, and the racial kit also improves passive Ki recovery.
- Saiyan uses Zenkai. Surviving near-death situations and fully recovering can grant permanent stat growth, with cooldown and diminishing returns.
- Frost Demon uses Dangerously Fast. Lower health increases speed and Ki power, and attacks cost less stamina.
- Namekian keeps its Dragon Mine Z racial identity while fitting into the separated race/class stat system.
- Majin uses Majin Absorption. Select the racial action in the X menu, then use the racial key on a valid target to heal and gain permanent stat bonuses from the target. Death reduces absorption bonuses.
- Bio Android uses Perfect DNA. It combines reduced versions of several racial strengths, including Saiyan Zenkai, Namekian regeneration, Human Ki Boosting Body, and Frost Demon speed/Ki power.


Ki Technique Overhaul
---------------------

Custom Ki attacks have more build options and clearer limits.

- Ki attacks can overcharge up to 400%.
- Charge costs are paid with Ki. Releasing a technique can consume Ki first and then health for part of the missing cost.
- The techniques are separated into Basic, Advanced, and Ultimate categories based on the strength and utility of the technique.
- Equipped techniques are limited to 2 Advanced and 2 Ultimate techniques, you cant just fill all your 8 slots of skills with ultimate attacks!
- Techniques can have third and fourth secondary effects, enabling more then one buff/debuff per ki attack, but also making their tier and cost higher...
- Techniques can have two extra mob effects, with harmful effects for damage techniques and beneficial effects for healing techniques, having even more ways of affecting your allies or foes.
- Small Ball and Medium Ball techniques can be configured to fire multiple projectiles.
- Area techniques can use custom area size controls, being able to cover large areas (DOMAIN EXPANSION IN DRAGON BALL LOL)
- Area techniques can use Both utility, allowing support and harmful effects in one area-style technique. (no, seriously, it really is like domain expansion)
- Extra effects are filtered so cooldowns, transformations, racial passives, fusion states, and other unsafe internal effects cannot be selected.
- Some Ki debuffs can slow mob movement and attack speed.


Combat And Movement
-------------------

- If your flat damage reduction from DMZ makes the incoming damage be zero, it cancels the damage and nothing happens to you (like mr Stan trying to attack Cell :D).
- Defense now applies a Dynamic mitigation based on damage:
> - Incoming Damage that are equal to your defense value reduce the damage by 20% before the DMZ calculation.
> - The bigger the difference between these values, the higher or lower this mitigation is, capped by a configurable value of 70%.
- Parrying preserves the stamina value from before the parry cost is applied.
- Combat flight supports a double dash when the player is flying in combat mode.
- Flight speed scales with SPD, Ki Power, Fly skill level, and movement ramping.
- Apotheosis affixes and gems are supported specifically by Gloves and Wristbands when Apotheosis is installed.


Battle Power And NPC Scaling
----------------------------

- Enemies BP is calculated similar to the player, counting armor points, speed, ranged damage and some other things to determine how generally powerfull the mov is like the player, so you wont get "one-shotted" by an enemy with 10K BP while habing 1 Bilion BP
- By default, this also re-scales Saga entity stats to fit this addon's vision, giving them stats that would equal to their Anime Power Level. This is optional, and can be disabled on configs.
- Since this change makes BP from enemies be VERY LARGE like the player's, their BP now displays like the player's BP using Ki Sense in a more compact way and supporting higher numbers.
- Non-DMZ mobs with battle power support receive a calculated battle power based on health, damage, armor, movement, Ki/projectile power, and compatible mod attributes. (I recomend using the mod "Auto Leveling" for this!)


UI And Quality Of Life
----------------------

- Character stats menus show SPD instead of SKP.
- Statistic descriptions show movement speed, attack speed, swim speed, and flight speed information where relevant.
- Ki Sense labels are adjusted for the battle power rework.
- Metamoru and Potara fusion now change the appearence of the controlling player, giving them temporary cosmetic clothing matching the fusion method used.
- Different-race Fusion Dance is enabled by default.
- DMZ has a list of "helmets that allow hair to display", but no general button to make hair be displayed with helmets. this mod adds a config value that can change that list from a "WHITELIST" to a "BLACKLIST", inverting how it works and basically allowing any helmet not listed there to render hair instead of hiding it!


Server Commands
---------------

- /dmzrevamp resetcooldowns
  Resets this addon's racial and class cooldowns for the command user.

- /dmzrevamp resetcooldowns <targets>
  Resets this addon's racial and class cooldowns for selected players.

Dragon Mine Z's own reload command can reload the class configuration layer used by this addon.


Config Notes
------------

- Main addon config: config/dmzrevamp-common.toml
- Racial configs: config/dmzrevamp/racials
- Class configs: config/dragonminez/classes
- Race configs are prepared as race-only baselines and then merged with class configs at load time.
- Existing generated configs are not automatically migrated when defaults change. If a server wants new defaults, delete the affected generated config files and let them regenerate.

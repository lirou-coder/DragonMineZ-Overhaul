# Racial Strike Abilities

Dragon Mine Z: Overhaul registers special Strike techniques that belong to particular races or biological upgrades. Eligible characters receive missing techniques automatically after character creation and when joining the server.

These techniques are protected character abilities. They cannot be deleted like an ordinary custom technique, but they are removed when a character reset changes the player to an ineligible race and are granted again whenever eligibility returns.

## Android Absorption

Android Absorption is granted to Bio Androids and to Humans who completed the Android upgrade.

The attack restrains its target and deals damage in three absorption hits. Half of the health actually removed by each absorption hit is restored to the user as both HP and Ki. The final part releases the target.

## Sleep Recovery

Sleep Recovery is granted to Majins.

It is a self-targeted recovery technique and does not require an enemy. The Majin becomes stationary during its recovery animation and converts the technique's Ki cost into healing.

## Namekian Regeneration

Namekian Regeneration is granted to Namekians. Bio Androids also receive it when **Sairen's DMZ World** is not installed, allowing that addon to provide its own behavior when present.

The technique does not require a target. On the regeneration moment, it attempts to restore 20% of maximum HP and spends the same amount of Stamina.

If current Stamina is insufficient, all remaining Stamina is consumed and the character heals only that amount. It cannot heal beyond missing HP. The technique uses its own regeneration animation and sounds and has a default cooldown of 60 seconds.

## Configuration

The XP rules, Ki cost, damage multiplier, cast time, and cooldown of these techniques are exposed in **customStrikeAttacks.json** alongside player-created Overhaul Strikes. See [[Custom Strike Attacks]].

Eligibility follows the character's active race or Overhaul racial skill, so compatible custom race configurations can still receive the appropriate technique.

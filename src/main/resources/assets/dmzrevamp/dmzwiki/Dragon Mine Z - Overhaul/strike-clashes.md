# Strike Clashes

Strike Clashes turn two opposing physical techniques into a direct contest. They use the familiar clash meter.

## Starting a Strike Clash

A Strike Clash can begin when a player's Strike Attack reaches an opponent who is attacking back.

- Against another player, both players must be performing Strike Attacks against each other.
- Against a Dragon Mine Z saga mob, the mob must be performing a Combo Attack.

All Strike Attacks are eligible. Unlike [[Ki Clash Overhaul]], there is no list of allowed technique types and Strike Clashes never accept helpers.

## Attack warning

When the configurable Strike Attack delay is enabled, starting a player Strike Attack or a saga mob Combo Attack first creates a short warning period. The attacker is frozen, uses the Ki Charge animation, and displays their aura before the attack begins. The default warning lasts 0.5 seconds.

This warning gives the target a chance to recognize the incoming technique and answer with their own Strike attack.

## During the clash

Press the clash input when the moving indicator enters the good area. Accurate presses earn more momentum, missed presses use the configured reduced efficiency, and unused momentum decays over time.

## Power and Speed

Melee power can increase momentum gain:

- Players contribute their current Melee Damage.
- Mobs contribute their Minecraft Attack Damage attribute.

Speed can widen the good area for the faster participant. Players use their Speed value, which Dragon Mine Z stores internally as Strike Damage. Mobs use Attack Damage for this comparison. A larger good area makes accurate inputs more forgiving, but it never extends beyond the meter.

Both influences and their strength can be configured in **StrikeClashConfigured.json**.

## Winning and losing

The losing fighter's technique is canceled and they receive Stun for two seconds. The winning player immediately restarts the original Strike Attack against the loser without repeating the warning delay.

The winning technique refreshes its damage from the player's current Melee Damage and receives the configured winner damage multiplier. This means a form or Release change that altered Melee Damage during the clash is reflected in the final hit.

The meter speed, timing area, momentum rules, duration, warning delay, and winner multiplier can all be changed by the server and reloaded with **/dmzreload**.

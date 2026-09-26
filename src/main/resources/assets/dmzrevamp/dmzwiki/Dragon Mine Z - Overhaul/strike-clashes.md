# Strike Clashes

Strike Clashes turn two opposing physical techniques into a direct contest. In Dragon Mine Z 2.2 they reuse the base mod's deterministic **ClashMeter**, so their HUD and input timing stay synchronized with the current Beam Clash implementation.

## Starting a Strike Clash

A Strike Clash can begin when a player's Strike Attack reaches an opponent who is attacking back.

- Against another player, both players must be performing Strike Attacks against each other.
- Against a Dragon Mine Z saga mob, the mob must be performing a Combo Attack.

Only an execution that actually captured and locked an opponent is eligible. Techniques such as Deadly Dance, Wolf Fang, and other attacks that can start without a target remain valid when they successfully capture one. An execution whose active Strike has no target cannot start or answer a Strike Clash. Unlike [[Ki Clash Overhaul]], Strike Clashes never accept helpers.

## Attack warning

When the configurable Strike Attack delay is enabled, starting a player Strike Attack or a saga mob Combo Attack first creates a short warning period. The attacker is frozen, uses the Ki Charge animation, and displays their aura before the attack begins. The default warning lasts 0.5 seconds.

This warning gives the target a chance to recognize the incoming technique and answer with their own Strike attack.

## During the clash

Use the same clash input as a Ki Clash. Dragon Mine Z 2.2 generates changing meter cycles from a synchronized seed and grades each press as MISS, GOOD, or PERFECT. The server validates the press time and marker instead of trusting a separate Overhaul-only click packet.

Strike Clash keeps its own momentum gain and decay tuning. Its **meterSpeedMultiplier**, **goodAreaSizeMultiplier**, **perfectAreaFraction**, and **goodMinimumEfficiency** settings now tune the corresponding parts of Dragon Mine Z 2.2's seeded meter. The server synchronizes the resulting parameters with the HUD so input validation cannot drift apart.

## Power and Speed

Melee power can increase momentum gained from a successful input:

- Players contribute their current Melee Damage.
- Mobs contribute their Minecraft Attack Damage attribute.

Speed directly multiplies the randomized good-area width of each participant. Players use their current DMZ Speed calculation; mobs use their movement-speed attribute relative to vanilla player speed. The result is clamped to the available space in the meter and therefore never extends outside the bar. This behavior and its weight are controlled by **goodAreaSpeedInfluence** and **goodAreaSpeedInfluenceMultiplier** in **StrikeClashConfigured.json**.

The melee and speed influences and their strengths can be configured independently.

## Winning and losing

The losing fighter's technique is canceled and they receive Stun for two seconds. The winning player immediately restarts the original Strike Attack against the loser without repeating the warning delay.

The winning technique refreshes its damage from the player's current Melee Damage and receives the configured winner damage multiplier. This means a form or Release change that altered Melee Damage during the clash is reflected in the final hit.

Momentum rules, duration, warning delay, melee influence, and winner multiplier can be changed by the server and reloaded with **/dmzreload**.

# Team Ki Clash

When helpers are enabled, an existing Ki Clash can grow into a team battle. Players and compatible mobs can support either side without creating a separate clash.

## Joining a team

Fire an allowed Ki Attack into one of the two attacks already locked in the clash. Your attack must be launched and must collide with the active clash hitbox.

The Overhaul compares your facing direction with the directions of the two original fighters:

- Looking roughly the same way as one fighter places you on that fighter's team.
- Looking roughly opposite places you on the opposing team.

Each participant receives the normal clash meter and timing speed. A later participant is not treated as a slower spectator: they enter as a full member whose momentum is synchronized with the team.

## Shared momentum

Everyone on the same team works on one momentum value. When any member earns momentum, the updated value is shared with every teammate.

Additional teammates also reduce the team's passive momentum loss. The reduction per helper and its maximum cap are configurable.

## Combined power

Power influence compares the total effective Ki Damage of each team, not only the two original fighters. Each participant contributes their current Ki Damage and the internal damage multiplier of the technique they fired. Charge influence uses the team's attacks as well.

This means a fighter who was winning a one-on-one clash can begin losing when a strong helper joins the other side.

## The winning attack

If a team with helpers wins, the surviving main attack is upgraded before it continues:

- Its damage is refreshed from the winner's current Ki Damage.
- The helpers' refreshed attack damage is added to it.
- The sizes of the helper attacks are combined and multiplied by the configured helper size efficiency.
- That additional size increases both the visual projectile and its hitbox.

Helpers must keep their entities and attacks valid until resolution to contribute to the final combined attack.

For the meter, transformations, overcharge, and allowed attack rules, see [[Ki Clash Overhaul]].

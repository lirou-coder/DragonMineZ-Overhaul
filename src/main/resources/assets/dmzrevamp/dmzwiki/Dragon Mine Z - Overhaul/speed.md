# Speed

Speed is more than running faster in Dragon Mine Z: Overhaul. It is the combat stat that controls how quickly and efficiently your character can act.

In some internal files, Speed is still called **SKP** or **Strike Power** because that is the original Dragon Mine Z stat name. The Overhaul interface presents it as **Speed**.

## What Speed affects

- Ground movement and acceleration
- Swimming speed
- Combat Flight and Search Flight speed
- Basic attack speed
- Ki Attack projectile speed
- Strike Attack dash distance
- Ki and Strike Attack cast time
- Ki and Strike Attack cooldowns

The exact limits and multipliers are controlled by the server configuration. You can inspect your current bonuses in the stat tooltips.

## Different kinds of movement

Not every movement formula uses Speed alone.

- Ground movement is primarily driven by Speed.
- Swimming combines Speed with Melee Damage.
- Flight combines Speed with Ki Damage.
- Basic attack speed rewards Speed but is moderated by Melee Damage, preventing heavy physical power from automatically producing maximum attack speed.

Movement formulas use your underlying stats and transformation multipliers, but ignore temporary DMZ bonus entries such as racial and class stat bonuses. This keeps movement growth predictable even when temporary combat bonuses are active.

Cooldown reduction, cast-time reduction, Ki Attack speed, and Strike Attack dash distance follow a different rule: they can use base-stat bonuses, but they do not become stronger simply because a transformation multiplies Speed.

## Growth and limits

Speed gain is scaled against the server's configured progression limits. By default, movement accelerates gradually and very large bonuses pass through soft caps, so high Speed remains useful without making movement uncontrollable immediately.

When the custom Defense and Speed curve is enabled, utility effects grow quickly at the beginning and more slowly near their maximum:

- 1% of the reference stat grants 20% of the configured effect cap.
- 10% grants 40% of the cap.
- 50% grants 60% of the cap.
- 100% grants the full cap.

For a broader explanation of character attributes, see [[Stats & Attributes]]. For how race and class values combine before these formulas are used, see [[Race and Class]].

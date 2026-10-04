# Archived custom Evasive Strike

This folder preserves the removed Overhaul custom Strike type `Evasive` for a possible future implementation in a dedicated evasive-technique system.

It is intentionally outside every Gradle source set and has no runtime registration. The active mod therefore does not expose, deserialize, create, execute, translate, or render this custom Strike type.

Original behavior:

- enum data: suffix `evasive`, damage `0.2`, cooldown multiplier `1.0`, animation `technique.evasive`, duration `20` ticks, speed `0.0`;
- base creation cooldown: `400` ticks;
- utility: healing/beneficial, with no dash speed or Armor Penetration;
- accepted BUFF secondary effects and BENEFICIAL mob effects applied to the user;
- at tick 6, pushed the locked target away, played the punch sound, emitted cloud particles, and finished at tick 20.

The original animation definitions are preserved alongside this document in `combat.animation.evasive.json` after extraction from the two active animation namespaces.

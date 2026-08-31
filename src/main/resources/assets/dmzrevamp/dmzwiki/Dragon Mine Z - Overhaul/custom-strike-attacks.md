# Custom Strike Attacks

Dragon Mine Z: Overhaul expands the technique creator with fully playable custom Strike Attacks. These techniques use Melee Damage, can participate in [[Strike Clashes]], and are saved and upgraded through the normal Dragon Mine Z technique systems.

## Strike types

The selected type determines the animation, movement pattern, hit sequence, starting damage range, and duration.

- Basic: a multi-position close combat combination.
- Air: launches the target and finishes from above.
- Charge: rushes the target and follows with a finishing blow.
- Meteor Combination: a longer rapid combination based on the Kaioken Attack movement style.
- Fast Punch: a quick physical attack sequence.
- Strong Punch: a shorter, heavier attack.
- Evasive: a defensive recovery technique rather than a damaging Strike.

Damage can be adjusted only within the range supported by the selected type. Changing type also updates the compatible movement and utility options.

## Armor Penetration and effects

Damaging custom Strikes can be created and upgraded with up to 10% Armor Penetration. Evasive techniques cannot use Armor Penetration.

Custom Strikes may carry several secondary stat effects and up to two compatible mob effects. Damaging Strikes accept harmful effects for their target, while Evasive techniques accept beneficial effects for their user.

Adding damage, speed, Armor Penetration, or additional effects raises the derived resource, XP, or cooldown costs. Techniques are classified as Basic, Advanced, or Ultimate from their damage and active effects.

## Server configuration

**customStrikeAttacks.json** follows the general style of Dragon Mine Z's techniques configuration. Entries may target an exact technique ID or one of the built-in Overhaul Strike types.

Each entry can configure:

- Minimum and maximum XP cost
- XP cost and XP gain multipliers
- XP gained per hit and per kill
- Ki cost multiplier
- Damage multiplier
- Additional cast time
- Cooldown in ticks

Race-exclusive techniques also have entries in this file. Missing default entries are restored when the configuration loads, while custom entries are preserved. Run **/dmzreload** after editing it.

For techniques automatically granted by race, see [[Racial Strike Abilities]].

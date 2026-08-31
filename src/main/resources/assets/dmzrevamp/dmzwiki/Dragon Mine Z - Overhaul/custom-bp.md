# Custom BP

Custom BP replaces a simple Battle Power total with a configurable weighted curve. Players and mobs use different stat lists so the number can represent their actual combat tools more accurately.

The settings are stored in **custom_BP.json** and can be reloaded with **/dmzreload**.

## The calculation

Every enabled stat is multiplied by its configured weight. Those weighted values are added together, then passed through this curve:

**Battle Power = reference multiplier multiplied by the weighted total divided by the stats divisor, raised to the configured exponent**

Player Battle Power is finally multiplied by current Release. Mob Battle Power is calculated at full release.

Increasing a stat's weight makes that stat contribute more to BP. Disabling a stat removes it from the total. The divisor controls the scale of the weighted total, the reference multiplier controls the displayed magnitude, and the exponent controls how sharply BP grows at high power.

## Player stats

The default player calculation can include:

- Melee Damage
- Speed, stored internally as Strike Damage
- Maximum Stamina
- Defense
- Maximum HP
- Ki Damage
- Maximum Ki

These are the player's calculated maximum combat values. Current Release is applied after they are combined, so lowering Release lowers visible BP without changing the underlying build.

## Mob stats

The mob calculation can include:

- Maximum HP and Attack Damage
- Armor and Armor Toughness
- Total Protection levels
- Resistance effect levels
- Movement or flying speed
- Ki Damage from DMZ attributes or quest data
- Arrow Damage when its compatible attribute mod is present
- Auto Leveling projectile and explosion damage
- Iron's Spells spell power

Only attributes that exist on the entity and have their corresponding integration available contribute. This prevents a missing optional mod from creating artificial power.

The same calculation is used for compatible DMZ saga entities, quest previews, and synchronized mob BP displays. Very large visible values are compacted with M, B, or T suffixes by Ki Sense.

For how relative BP changes Ki Sense labels, see [[Danger BP]].

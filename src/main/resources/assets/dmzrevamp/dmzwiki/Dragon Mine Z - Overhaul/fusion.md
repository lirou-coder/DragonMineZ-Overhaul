# Fusion

Dragon Mine Z: Overhaul keeps the familiar Metamoran Dance and Potara methods described in [[Fusions]], but changes how the two characters become one fighter.

## Forming the fusion

Both methods still require the normal Dragon Mine Z conditions. Server owners may additionally configure whether different races can use the Metamoran Dance and whether Android characters can fuse.

## Combined character

For every stat enabled in the fusion configuration:

- The controller receives a configurable portion of the partner's base stat.
- The race and class scaling of both players is combined.
- The observer mirrors the controller's current transformation multipliers so both character panels represent the same fused fighter.

All ordinary stat bonuses are also shared. A copy of the partner's bonus is identified as **Fusion followed by the original bonus name**. The original owner's bonus is never removed by this process, and the mirrored copies are cleaned up when the fusion ends.

Class passives are shared too. Actions performed by the controller can build the observer's Combo passive, and Simple, Resource, and Special passive effects can benefit both members. These shared effects are removed when they separate. See [[Custom Class Passives]].

## Shared resources

When fusion completes, the fused pair's HP, Ki, and Stamina are restored to the average percentage the two players had immediately before fusion.

For example, if one player had 100% Ki and the other had 40%, both sides of the fused character begin at 70% Ki. This uses percentages, not raw numbers, so characters with different maximum resources are treated fairly.

While fused, the controller and observer share HP and Ki. Damage received by the controller is reflected on the observer, and Ki spent by the controller is reflected there as well.

## Transformations and fusion time

If transformation time reduction is enabled, entering a stronger form consumes part of the remaining fusion duration. The reduction compares the average STR, Speed, Stamina, Defense, Vitality, PWR, and Energy multipliers of the old and new forms. Stronger jumps cost more time.

The internal timer and the visible Fused effect always use the same remaining duration. Server owners can disable this mechanic or change its efficiency.

Using the Overhaul command to force a fusion to finish applies the normal fusion cooldown to both players. Equipped Potara are consumed so the pair cannot immediately fuse again with the same earrings.

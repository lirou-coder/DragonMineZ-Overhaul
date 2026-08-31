# Adaptive Defense Reworked

Adaptive Defense is the part of Dragon Mine Z combat that changes damage mitigation according to the relationship between an incoming attack and the defender's Defense. Dragon Mine Z: Overhaul can replace the original calculation with a fully configurable curve.

The replacement is controlled by **adaptiveDefenseMoreConfigured.json**. If its enable option is false, the original Dragon Mine Z Adaptive Defense system handles the calculation instead.

## Damage compared with Defense

The Overhaul compares the attack's reference damage with the target's effective Defense after relevant guard-break decay and Armor Penetration are applied.

- When damage and Defense meet at the configured parity ratio, the parity mitigation value is used.
- As Defense becomes stronger relative to damage, mitigation rises toward its configured cap.
- The Defense-to-attack ratio required to reach that cap is configurable.
- As damage becomes stronger relative to Defense, mitigation falls toward zero at the configured zero ratio.

The result is additional mitigation applied after the normal damage calculation, but it can never exceed the configured Adaptive Defense cap.

## Multi-hit techniques

Ki Attacks and Strike Attacks often divide their total damage into several smaller hits. Treating every small hit as an entire weak attack would make Adaptive Defense far stronger than intended.

The Overhaul preserves the total original damage of the technique as the reference for every part of that technique. For example, a Ki Wave divided into five hits is compared as one complete Ki Wave, not as five unrelated weak blasts.

Ki Attacks and Strike Attacks have separate efficiency multipliers. Lowering one reduces Adaptive Defense against that technique family; raising one strengthens it, but mitigation still cannot pass the cap.

## Completely overwhelmed attacks

If Defense is at least the configured threshold times greater than an ordinary incoming hit, the entire hurt event is canceled. This prevents damage, knockback, and the normal Minecraft hurt reaction.

Ki Attacks, Strike Attacks, and saga mob Combo Attacks are exceptions. Their damage may still be reduced to zero by the same comparison, but their attack event is not completely removed. Technique impact logic can therefore continue to function correctly.

Adaptive Defense is different from the normal Defense progression curve shown in the stat menu. The stat curve determines the player's standard Defense reduction, while Adaptive Defense reacts to the relative strength of each incoming attack.

# Custom Class Passives

Any custom class can use Dragon Mine Z: Overhaul's passive engine. Place **Custom Passive: true** inside the class **passive values** object, then select a whole-number **PassiveType**.

Percentage fields use decimal form: **0.2 means 20%**. Selector and timing fields such as PassiveType, Type, Effect, MaxStacks, StackTime, and ResourceType must be whole numbers.

## Simple passives

Set **PassiveType** to 1. A Simple passive is always available and uses **Effect** plus **Value**.

Effect choices are:

1. Melee and Strike defense penetration
2. Ki defense penetration
3. Incoming damage ignored
4. Strike Attack cost reduction
5. Ki Attack cost reduction
6. Strike Attack cooldown reduction
7. Ki Attack cooldown reduction
8. HP regeneration increase
9. Passive and active Ki regeneration increase
10. Stamina regeneration increase

## Combo passives

Set **PassiveType** to 2. Combo passives gain timed stacks when the selected action succeeds.

Use **Type** to choose the action:

1. Melee or Strike Attack hits
2. Ki Attack hits
3. Blocked incoming attacks
4. Parried incoming attacks
5. Perfectly dodged or countered attacks

**MaxStacks** sets the stack limit, **StackTime** is the lifetime of each stack in ticks, and **MaxValue** is the decimal bonus reached at maximum stacks.

## Resource passives

Set **PassiveType** to 3. Resource passives change strength according to a current resource.

- Type 1 watches HP.
- Type 2 watches Ki.
- Type 3 watches Stamina.
- ResourceType 1 reaches full strength at 10% resource or less.
- ResourceType 2 reaches full strength at 90% resource or more.

**Value** is the maximum decimal bonus.

## Combo and Resource effects

Combo and Resource passives share this **Effect** list:

1. Melee and Strike damage
2. Ki damage
3. All outgoing damage
4. Defense
5. Speed
6. HP regeneration
7. Ki regeneration
8. Stamina regeneration
9. Critical damage
10. Critical chance
11. Critical chance and critical damage

## Special passives

Set **PassiveType** to 4. Special passives use **Type** and **Value** to select one predefined mechanic.

1. Add a share of Speed as final Melee and Strike damage.
2. Add a share of Speed as final Ki damage.
3. A successful Strike Attack empowers the next Strike Attack.
4. A successful Ki Attack empowers the next Ki Attack.
5. A successful parry reflects a share of the original damage.
6. Reaching 50% HP or less temporarily exceeds the Potential Unlock release limit.

Special Type 6 sets Release to the character's current Potential Unlock limit multiplied by **1 + Value** for 30 seconds. With Value 0.5, a 100% limit becomes 150%. When the effect ends, Release is set to half of the normal limit, and a 90-second cooldown begins.

## Fusion behavior

During [[Fusion]], both players' custom passives are active for the fused fighter. Controller actions can build either player's Combo stacks, while Simple, Resource, and Special effects are shared between both members. Fusion-only sharing is cleaned up when the fusion ends.

A complete editable reference named **customRacialTutorial.txt** is generated in the classes configuration folder. After changing a class file, run **/dmzreload**.

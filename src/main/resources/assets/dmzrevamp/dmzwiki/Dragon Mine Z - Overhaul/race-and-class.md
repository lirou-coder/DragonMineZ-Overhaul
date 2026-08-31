# Race and Class

Dragon Mine Z: Overhaul separates **Race** and **Class** into two independent parts of character building.

## Race

Your race represents what your character is. It provides the racial portion of starting attributes and stat scaling, racial regeneration values, biological traits, racial skills, and access to race-specific transformations.

Examples include Saiyan racial growth, Majin absorption, and Namekian assimilation. These racial systems belong to the race and do not replace your class passive.

## Class

Your class represents how your character fights. It provides its own starting attributes, stat scaling, regeneration modifiers, TP modifiers, display name and color, and a class passive.

Classes are shared across compatible races unless their configuration restricts them to an exclusive race list. Server owners and modpack authors can add new class JSON files without editing every race file.

## Final starting values

When a race and class are selected, the Overhaul builds the playable combination by adding their values together.

- Final starting stat = race starting stat + class starting stat
- Final stat scaling = race scaling + class scaling
- Regeneration and TP modifiers combine in the same way when both sides provide them
- The class supplies the class passive
- The race continues to supply its racial skill and transformations

For example, choosing a race with high PWR scaling and a class with additional PWR scaling produces a character whose final PWR scaling contains both contributions.

These merged values are what Dragon Mine Z uses for character creation, progression, stat formulas, and Prestige resets. Prestige can then apply its permanent scale multiplier on top without erasing the distinction between race and class.

## Custom classes

Custom classes are loaded from the Dragon Mine Z classes configuration folder. Their display name, description, color, starting values, scaling, race restrictions, and passive can all be defined in JSON. Run **/dmzreload** after editing the files.

For the Overhaul passive format, see [[Custom Class Passives]]. For the original character concepts, see [[Races]] and [[Player Classes]].

# Skill Categories

Dragon Mine Z: Overhaul classifies Ki Attacks and Strike Attacks as **Basic**, **Advanced**, or **Ultimate**. The category represents how many powerful properties are combined in one technique and can limit how many of those techniques fit in your equipped slots.

The category is displayed in the technique creator and in the technique information panel:

- Basic uses a light blue label.
- Advanced uses a gold label.
- Ultimate uses a purple label.

## Active effects

Classification begins by counting every active effect stored on the technique. This can include the primary secondary-stat effect, the additional third and fourth stat effects, and up to two compatible mob effects added by the Overhaul.

Effect intensity and duration increase the technique's creation and upgrade costs, but category promotion uses the number of active effects rather than their individual strength.

## Ki Attack categories

A Ki Attack with no active effects is always Basic, even if it has high damage or fires several projectiles.

Once the Ki Attack has at least one active effect, it earns one tier point for each condition below:

- Its damage multiplier is greater than 1.0.
- It has more than one active effect.
- It has at least three active effects. This is an additional point on top of the previous condition.
- It fires more than one projectile through Multicast.

The final category is:

- 0 tier points: Basic
- 1 tier point: Advanced
- 2 or more tier points: Ultimate

For example, a single-projectile Ki Attack with one effect and a damage multiplier of 1.0 remains Basic. Increasing its damage above 1.0 makes it Advanced. Giving it a second effect as well makes it Ultimate.

## Strike Attack categories

Strike Attacks use a similar system, but Multicast is not part of their classification.

A Strike Attack earns one tier point for each condition below:

- It has at least one active effect and its damage multiplier is greater than 2.0.
- It has more than one active effect.
- It has at least three active effects. This adds another point.

The result uses the same thresholds:

- 0 tier points: Basic
- 1 tier point: Advanced
- 2 or more tier points: Ultimate

A Strike Attack with no effects therefore remains Basic regardless of damage. A Strike with two effects becomes Advanced; adding damage above 2.0 or reaching three effects promotes it to Ultimate.

For the different Overhaul Strike movesets and effect restrictions, see [[Custom Strike Attacks]].

## Equipment limits

When category equipment limits are enabled, Basic techniques have no category limit. Advanced and Ultimate techniques have separate configurable limits.

By default, a player can equip:

- Up to 2 Advanced techniques
- Up to 2 Ultimate techniques

These totals count categorized Ki Attacks and Strike Attacks together across all equipped technique slots. For example, one Advanced Ki Attack and one Advanced Strike Attack already use both default Advanced slots. The Ultimate limit is counted separately and does not consume the Advanced allowance.

Replacing an equipped technique checks the final slot layout, so exchanging one Advanced technique for another does not temporarily exceed the limit. If an equip attempt would pass a category limit, it is canceled and the player receives a message explaining which category has too many equipped skills.

Server owners can disable category limits or change both maximums in **dmzrevamp-common.toml**. Changes become active after **/dmzreload**.

# Danger BP

Danger BP makes the Battle Power label shown by Ki Sense communicate danger before you finish reading the number. The target's BP is compared with your own current sensed BP, then the label changes color and size.

## Danger levels

- Below 75% of your BP: light cyan at normal size.
- From 75% to below 125% of your BP: yellow at 1.2 times normal size.
- From 125% to below 175% of your BP: orange at 1.4 times normal size.
- At 175% of your BP or higher: red at 1.75 times normal size.

Yellow therefore represents a target in roughly the same power range, while orange and red warn that the sensed fighter is considerably stronger.

## Ki Sense only

Danger BP modifies the world label created by **Ki Sense**. It does not recolor or resize the Scouter interface.

The feature uses the BP values already cached by Ki Sense, including values produced by [[Custom BP]]. Hidden or unreadable Battle Power remains hidden.

## Compatibility

If **DMZ Combat Remade** is installed, Danger BP automatically leaves the original label scale and color untouched. This prevents both mods from trying to control the same Ki Sense display.

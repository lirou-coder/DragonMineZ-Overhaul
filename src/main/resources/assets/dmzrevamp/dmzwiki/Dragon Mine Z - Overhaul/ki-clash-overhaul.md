# Ki Clash Overhaul

Dragon Mine Z: Overhaul expands Beam Clash into a clash system for several kinds of launched Ki Attacks, including waves, beams, lasers, medium balls, and giant balls by default.

The allowed attack types are controlled by the server's **KiClashConfigured.json** file. Disk and Barrage attacks are supported by the system but are not enabled in the default list. Shields, area attacks, small balls, and the basic Ki Control blast are also excluded by default.

## Starting a clash

A clash begins when two allowed, already-launched Ki Attacks collide while traveling in sufficiently opposing directions.

If one attack is enormously stronger than the other, the server can cancel the clash before it starts and destroy the weaker attack. This prevents a tiny blast from stopping an overwhelmingly stronger technique.

## Winning the meter

Use the clash input when the moving indicator enters the good area. Accurate timing gives full momentum, while missing the good area gives only the configured fraction. Momentum also decays over time, and the clash has a configurable maximum duration.

The Overhaul keeps the clash window and camera active until the dispute is resolved. Players are held in place during the clash so projectile types that normally permit movement cannot be used to escape it.

## PWR and Ki Damage influence

When Ki Damage influence is enabled, the side with greater effective Ki power gains additional momentum. For players, this starts from the current Ki Damage produced by PWR. For mobs, the system can use their Ki Blast Damage or story-configured Ki Damage.

The comparison also includes the technique's own damage multiplier. A powerful technique therefore contributes more than a weak technique fired by the same character.

Overcharge can influence the comparison separately. A technique fired at 200% charge is treated as more powerful than the same technique at 100% charge. If both Ki Damage and overcharge influence are active, they work together.

The weaker side never receives less than the normal base momentum gain. Power advantages improve the stronger side instead of making the controls unresponsive for the weaker one.

## Transforming during a clash

When enabled by the server, both instant transformations and charged transformations can be used during a clash. Ki charging is also available. The clash animation remains visible while hair, color, release, and form state change.

Ki Damage is checked again every time momentum is earned. Transforming or raising your release in the middle of the struggle can therefore change which side has the power advantage immediately. The winning attack's damage is refreshed from the winner's current Ki Damage when the clash ends.

The loser is briefly immobilized and prevented from using attacks or evasive abilities, giving the winning projectile a fair opportunity to connect.

For clashes with more than two fighters, see [[Team Ki Clash]].

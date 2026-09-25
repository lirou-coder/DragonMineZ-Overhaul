# Ki Clash Overhaul

Dragon Mine Z: Overhaul expands Dragon Mine Z 2.2's Ki Clash system to several kinds of launched Ki Attacks, including waves, beams, lasers, medium balls, and giant balls by default.

The allowed attack types are controlled by the server's **KiClashConfigured.json** file. Disk and Barrage attacks are supported by the Overhaul but are not enabled in the default list. Shields, area attacks, small balls, and the basic Ki Control blast are excluded by default.

## Starting a clash

A clash begins when two allowed, already-launched Ki Attacks collide while traveling in sufficiently opposing directions.

The Overhaul upgrades configured attacks to full clash participants without erasing Dragon Mine Z 2.2's native **MINOR** role. Native minor Ki attacks can therefore still be shattered by a major clashing attack.

If one attack is enormously stronger than the other, the server can cancel the clash before it starts and destroy the weaker attack. This prevents a tiny blast from stopping an overwhelmingly stronger technique.

## Dragon Mine Z 2.2 Clash Meter

The timing minigame itself is now the native Dragon Mine Z 2.2 **ClashMeter**. Each participant receives a deterministic meter seed. The server and client independently reproduce the same changing cycles, timing windows, direction, and marker position.

A press can be graded **MISS**, **GOOD**, or **PERFECT**. The server validates the submitted press time and marker, accounts for latency, allows only one contribution per cycle, and applies Dragon Mine Z's anti-bot consistency checks. Because this timing simulation must stay identical on both sides, the Overhaul no longer replaces the meter speed or good-area shape.

The Overhaul still controls its own momentum multiplier, momentum decay, win thresholds, maximum clash duration, team rules, and power influences through **KiClashConfigured.json**.

## PWR and Ki Damage influence

When Ki Damage influence is enabled, the side with greater effective Ki power gains additional momentum from successful meter inputs. For players, this starts from the current Ki Damage produced by PWR. For mobs, the system can use their Ki Blast Damage or story-configured Ki Damage.

The comparison also includes the technique's own damage multiplier. A powerful technique therefore contributes more than a weak technique fired by the same character.

Overcharge can influence the comparison separately. A technique fired at 200% charge is treated as more powerful than the same technique at 100% charge. If both Ki Damage and overcharge influence are active, they work together.

The weaker side never receives less than the normal base momentum gain. Power advantages improve the stronger side instead of making the controls unresponsive for the weaker one.

## Transforming during a clash

When enabled by the server, both instant transformations and charged transformations can be used during a clash. Ki charging is also available. The clash animation remains visible while hair, color, release, and form state change.

Ki Damage is checked again whenever momentum is earned. Transforming or raising your release in the middle of the struggle can therefore change which side has the power advantage immediately. The winning attack's damage is refreshed from the winner's current Ki Damage when the clash ends.

Dragon Mine Z 2.2 applies its native post-clash exhaustion to the loser: the loser receives the base mod's Stun window while the winning projectile is released to continue forward. The Overhaul does not add a second independent immobilization timer, but it keeps packet guards during that native Stun window for actions whose base packet does not check Stun itself.

For clashes with more than two fighters, see [[Team Ki Clash]].

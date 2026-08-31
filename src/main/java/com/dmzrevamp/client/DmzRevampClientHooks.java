package com.dmzrevamp.client;

public final class DmzRevampClientHooks {
    private static volatile int localFlightMovementTicks;

    // Static hook state is shared by client mixins and event handlers, so this class should not be instantiated.
    private DmzRevampClientHooks() {
    }

    // Older code paths still call this; disabling boost now just clears the local flight ramp.
    public static void setFlightBoostModeEnabled(boolean enabled) {
        if (!enabled) {
            localFlightMovementTicks = 0;
        }
    }

    // Reserved compatibility hook for older flight-boost toggles that used to play a client sound.
    public static void playToggleSound(boolean enabled) {
    }

    // Returns how long the client has been providing flight input for the local ramp calculation.
    public static int getLocalFlightMovementTicks() {
        return localFlightMovementTicks;
    }

    // Reports whether the local ramp is still active while input is fading out.
    public static boolean canKeepLocalFlightRamp(long gameTime, int resetDelayTicks) {
        return localFlightMovementTicks > 0;
    }

    // Advances the local flight ramp while moving and decays it when the player stops.
    public static void tickLocalFlightMovement(boolean moving, int rampTicks, int resetDelayTicks, long gameTime) {
        if (moving) {
            localFlightMovementTicks = Math.min(rampTicks, localFlightMovementTicks + 1);
            return;
        }

        int decayStep = Math.max(1, (int) Math.ceil(rampTicks / (double) Math.max(1, resetDelayTicks)));
        if (localFlightMovementTicks <= 0) {
            localFlightMovementTicks = 0;
        } else {
            localFlightMovementTicks = Math.max(0, localFlightMovementTicks - decayStep);
        }
    }
}

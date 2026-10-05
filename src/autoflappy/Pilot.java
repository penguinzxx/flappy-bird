package autoflappy;

// Decides when to flap. It learns how high a flap goes, how fast flappy falls, and how long
// this computer takes to react while playing, so it works on fast and slow machines alike.
// Positions are rows inside the game area (bigger is lower); times are in milliseconds.
class Pilot {

    static final double FIRST_DELAY_GUESS = 60;      // click until flappy visibly moves
    static final double FIRST_JUMP_GUESS = 0.08;     // flap height, as a fraction of the game height
    static final double FIRST_GRAVITY_GUESS = 0.0015;
    static final double GAP_MARGIN = 0.08;           // keep this fraction of the gap clear at each edge
    static final long MIN_FLAP_INTERVAL = 100;

    private final double gameHeight;

    // Speed in rows per millisecond, positive when falling; tracked from flappy's bottom edge,
    // which is the solid body and doesn't wobble with the wings
    double velocity = 0;
    double gravity = FIRST_GRAVITY_GUESS;
    double jump;
    double delay = FIRST_DELAY_GUESS;

    private double lastBottom;
    private long lastSeen = -1;
    private double lastInterval = 0;

    private long lastFlap = Long.MIN_VALUE / 2;
    private double flapBottom;
    private double highest;
    private boolean measuringJump = false;
    private boolean waitingForLift = false;

    Pilot(double gameHeight) {
        this.gameHeight = gameHeight;
        this.jump = gameHeight * FIRST_JUMP_GUESS;
    }

    // Call with every sighting of flappy
    void see(long now, double top, double bottom) {
        if (lastSeen >= 0 && now > lastSeen) {
            double interval = now - lastSeen;
            double previous = velocity;
            velocity = 0.3 * velocity + 0.7 * (bottom - lastBottom) / interval;

            // Falling freely: speeding up shows how strong gravity is
            if (!waitingForLift && previous > 0 && velocity > previous) {
                double measured = (velocity - previous) / interval;
                if (measured < 0.01) {
                    gravity = 0.9 * gravity + 0.1 * measured;
                }
            }

            // First sign of rising since the click: that's how slow this computer is
            if (waitingForLift && bottom < lastBottom - 1) {
                double measured = now - lastFlap;
                if (measured < 500) {
                    delay = 0.7 * delay + 0.3 * measured;
                }
                waitingForLift = false;
            }
            lastInterval = interval;
        }
        lastBottom = bottom;
        lastSeen = now;

        // Once it starts falling again after a flap, the flap's height is known
        if (measuringJump) {
            highest = Math.min(highest, bottom);
            if (!waitingForLift && velocity > 0) {
                double measured = flapBottom - highest;
                if (measured > 2 && measured < gameHeight * 0.4) {
                    // Guessing too low sends flappy into the top pipe, so follow bigger
                    // measurements at once and smaller ones slowly
                    jump = measured > jump ? measured : 0.9 * jump + 0.1 * measured;
                }
                measuringJump = false;
            }
        }
    }

    // Call after clicking
    void flapped(long now, double bottom) {
        lastFlap = now;
        flapBottom = bottom;
        highest = bottom;
        measuringJump = true;
        waitingForLift = true;
    }

    // nextGap is the gap of the pipe after the current one, or null if it isn't in sight
    boolean shouldFlap(long now, double top, double bottom, double gapTop, double gapBottom, int[] nextGap,
                       double aim, double floor) {
        // Let a flap take effect before flapping again; double flaps shoot into the top pipe
        long sinceFlap = now - lastFlap;
        if (sinceFlap < MIN_FLAP_INTERVAL || (waitingForLift && sinceFlap < 3 * delay + 100) || velocity < 0) {
            return false;
        }

        // Where it will be by the time a flap takes effect. The speed is an average over the last
        // screenshot interval, so it is half an interval out of date.
        double ahead = delay + lastInterval / 2;
        double drop = velocity * ahead + 0.5 * gravity * ahead * ahead;
        double nextTop = top + drop;
        double nextBottom = bottom + drop;
        double margin = Math.max(5, (gapBottom - gapTop) * GAP_MARGIN);

        // About to touch the bottom pipe or the ground
        if (nextBottom > gapBottom - margin || nextBottom > floor) {
            return true;
        }
        // A flap now would carry it into the top pipe
        if (nextTop - jump < gapTop + margin) {
            return false;
        }
        // Otherwise flap so it bobs up and down around the aim line: falling to
        // aimLine + jump / 2, then flapping back up to aimLine - jump / 2
        return (nextTop + nextBottom) / 2 > aimLine(gapTop, gapBottom, nextGap, aim, bottom - top) + jump / 2;
    }

    // Where the middle of flappy should bob around, kept far enough from both edges of the gap
    // that a whole bob (one flap up, then the fall back down) fits inside it.
    // Flapping up is quick but falling takes time, so if the next gap is lower, start sinking early.
    double aimLine(double gapTop, double gapBottom, int[] nextGap, double aim, double flappyHeight) {
        double wanted = gapBottom - (gapBottom - gapTop) * aim;
        if (nextGap != null) {
            wanted = Math.max(wanted, nextGap[1] - (nextGap[1] - nextGap[0]) * aim);
        }
        double margin = Math.max(5, (gapBottom - gapTop) * GAP_MARGIN);
        double highestAllowed = gapTop + margin + (jump + flappyHeight) / 2;
        double lowestAllowed = gapBottom - margin - (jump + flappyHeight) / 2;
        if (highestAllowed > lowestAllowed) {
            return (gapTop + gapBottom) / 2;
        }
        return Math.max(highestAllowed, Math.min(lowestAllowed, wanted));
    }
}

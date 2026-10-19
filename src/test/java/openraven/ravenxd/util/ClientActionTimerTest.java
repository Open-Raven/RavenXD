package openraven.ravenxd.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientActionTimerTest {
    @Test void cancelledCastNeverRunsAfterSessionChange() {
        ClientActionTimer timer = new ClientActionTimer();
        int[] casts = {0};
        timer.schedule(100, 200, () -> casts[0]++);
        timer.tick(299);
        assertEquals(0, casts[0]);
        timer.cancel();
        timer.tick(10000);
        assertEquals(0, casts[0]);
    }

    @Test void reactivationReplacesOldCast() {
        ClientActionTimer timer = new ClientActionTimer();
        int[] casts = {0};
        timer.schedule(0, 10, () -> fail("Stale action ran"));
        timer.schedule(5, 20, () -> casts[0]++);
        timer.tick(10);
        assertEquals(0, casts[0]);
        timer.tick(25);
        timer.tick(100);
        assertEquals(1, casts[0]);
    }

    @Test void switchbackScheduledByCastSurvivesUntilItsOwnDeadline() {
        ClientActionTimer timer = new ClientActionTimer();
        int[] switches = {0};
        timer.schedule(0, 10, () -> timer.schedule(10, 20, () -> switches[0]++));
        timer.tick(10);
        timer.tick(29);
        assertEquals(0, switches[0]);
        timer.tick(30);
        timer.tick(31);
        assertEquals(1, switches[0]);
    }
}

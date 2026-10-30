package za.co.neroland.nerospace.rocket;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Only the player who flies may press Launch: nobody can launch (and charge) a player seated in the rocket. */
class LaunchSeatTest {

    @Test
    void seatRule() {
        assertTrue(LaunchSeat.mayLaunch(true, false));   // empty rocket: the presser boards
        assertTrue(LaunchSeat.mayLaunch(false, true));   // the seated rider presses Launch
        assertFalse(LaunchSeat.mayLaunch(false, false)); // someone else is seated
    }

    @Test
    void boardAndLaunchChecksTheSeatOnTheServer() throws Exception {
        String launch = PodArrivalTest.body(PodArrivalTest.source("RocketEntity.java"),
                "public boolean boardAndLaunch(ServerPlayer player)");
        int seat = launch.indexOf("LaunchSeat.mayLaunch(");
        assertTrue(seat >= 0 && seat < launch.indexOf("startLaunch()"), "the seat must be checked before launch");
    }
}

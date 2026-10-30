package za.co.neroland.nerospace.rocket;

/**
 * Who may press Launch on a rocket, from the rocket console or a Launch Controller. Checked on the server: the
 * player pressing Launch must be the one who flies. An empty rocket boards the presser; a rocket with someone
 * already seated launches only for that rider, so nobody can be sent off (and charged) by another player.
 */
public final class LaunchSeat {

    private LaunchSeat() {
    }

    /** {@code true} when the seat is empty (the presser boards) or the presser is the seated rider. */
    public static boolean mayLaunch(boolean seatEmpty, boolean presserIsRider) {
        return seatEmpty || presserIsRider;
    }
}

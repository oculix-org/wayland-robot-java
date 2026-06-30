package org.oculix.wayland.robot;

/**
 * Entry point for the library — creates a {@link WaylandRobot} appropriate to
 * the current session.
 *
 * <p>Currently picks {@link PortalRobot} on any non-empty desktop session.
 * Future versions will add a {@code LibeiRobot} for compositors that expose
 * {@code libei} directly, and a probe for compositor-specific protocols
 * (wlroots) when the portal is absent.
 */
public final class RobotFactory {

    private RobotFactory() {
        // utility
    }

    /**
     * Create a robot for the current desktop session.
     *
     * @throws WaylandRobotException if the environment does not look like a
     *     desktop session, or the chosen backend fails to initialise.
     */
    public static WaylandRobot create() {
        String sessionType = System.getenv("XDG_SESSION_TYPE");
        if (sessionType == null || sessionType.isEmpty()) {
            throw new WaylandRobotException("XDG_SESSION_TYPE is not set — "
                    + "this library is intended for use inside a Wayland or X11 session.");
        }
        // For now we go straight to the portal regardless of session type;
        // the portal works on both X11 (via XWayland) and pure Wayland.
        return new PortalRobot();
    }

    /**
     * @return {@code true} if the runtime environment looks suitable for the
     *     portal backend (i.e. a Linux desktop session is active).
     */
    public static boolean isSupported() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("linux")) {
            return false;
        }
        String sessionType = System.getenv("XDG_SESSION_TYPE");
        return sessionType != null && !sessionType.isEmpty();
    }
}

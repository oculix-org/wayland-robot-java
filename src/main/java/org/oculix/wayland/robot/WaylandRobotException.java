package org.oculix.wayland.robot;

/**
 * Thrown when the Wayland Robot cannot fulfill an operation — typically a DBus
 * connection failure, a portal consent denial, an unsupported session type, or
 * a compositor that does not implement the requested portal interface.
 */
public class WaylandRobotException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public WaylandRobotException(String message) {
        super(message);
    }

    public WaylandRobotException(String message, Throwable cause) {
        super(message, cause);
    }
}

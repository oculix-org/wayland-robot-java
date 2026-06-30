package org.oculix.wayland.robot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class WaylandRobotExceptionTest {

    @Test
    void carriesMessage() {
        WaylandRobotException ex = new WaylandRobotException("boom");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void carriesMessageAndCause() {
        Throwable root = new IllegalStateException("dbus missing");
        WaylandRobotException ex = new WaylandRobotException("boom", root);
        assertEquals("boom", ex.getMessage());
        assertSame(root, ex.getCause());
    }
}

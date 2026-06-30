package org.oculix.wayland.robot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Pure-contract tests for the {@link WaylandRobot} interface — verifies that
 * a custom implementation can be written and that the method shapes line up
 * with what callers (Sikuli-style scripts) expect.
 *
 * <p>The real {@link PortalRobot} requires a DBus session bus and an
 * {@code xdg-desktop-portal} implementation, neither of which are available
 * on a typical CI runner, so they are excluded from this test class.
 */
class WaylandRobotContractTest {

    @Test
    void interfaceCanBeImplemented() {
        RecordingRobot bot = new RecordingRobot();
        bot.mouseMove(100, 200);
        bot.mousePress(0x10);
        bot.mouseRelease(0x10);
        bot.mouseWheel(3);
        bot.keyPress(65);
        bot.keyRelease(65);
        bot.delay(0);
        bot.close();

        assertEquals(List.of(
                "mouseMove(100,200)",
                "mousePress(16)",
                "mouseRelease(16)",
                "mouseWheel(3)",
                "keyPress(65)",
                "keyRelease(65)",
                "delay(0)",
                "close()"
        ), bot.calls);
        assertTrue(bot.closed);
    }

    private static final class RecordingRobot implements WaylandRobot {
        final List<String> calls = new ArrayList<>();
        boolean closed;

        @Override public void mouseMove(int x, int y)    { calls.add("mouseMove(" + x + "," + y + ")"); }
        @Override public void mousePress(int b)          { calls.add("mousePress(" + b + ")"); }
        @Override public void mouseRelease(int b)        { calls.add("mouseRelease(" + b + ")"); }
        @Override public void mouseWheel(int w)          { calls.add("mouseWheel(" + w + ")"); }
        @Override public void keyPress(int k)            { calls.add("keyPress(" + k + ")"); }
        @Override public void keyRelease(int k)          { calls.add("keyRelease(" + k + ")"); }
        @Override public void delay(int ms)              { calls.add("delay(" + ms + ")"); }
        @Override public void close()                    { calls.add("close()"); closed = true; }
    }
}

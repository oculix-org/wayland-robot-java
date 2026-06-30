package org.oculix.wayland.robot;

import java.io.Closeable;

/**
 * A drop-in replacement for {@code java.awt.Robot} that works on Wayland
 * sessions where {@code java.awt.Robot} silently fails (see
 * <a href="https://bugs.openjdk.org/browse/JDK-8280983">JDK-8280983</a>).
 *
 * <p>The public surface mirrors {@code java.awt.Robot}'s input methods so that
 * existing code paths can switch with minimal change. Acquire an instance via
 * {@link RobotFactory#create()}.
 *
 * <p>An instance holds a session with the underlying Wayland compositor —
 * {@code xdg-desktop-portal} {@code RemoteDesktop} today, {@code libei} planned —
 * and must be closed when no longer needed.
 *
 * <p>The first instantiation on a given desktop will trigger a one-time user
 * consent dialog (the portal popup). Subsequent calls within the same desktop
 * session reuse the granted handle.
 *
 * @see RobotFactory
 */
public interface WaylandRobot extends Closeable {

    /**
     * Move the mouse pointer to absolute screen coordinates.
     *
     * @param x absolute X in screen pixels
     * @param y absolute Y in screen pixels
     */
    void mouseMove(int x, int y);

    /**
     * Press one or more mouse buttons.
     *
     * @param buttons bitmask of {@code java.awt.event.InputEvent.BUTTON*_DOWN_MASK}
     */
    void mousePress(int buttons);

    /**
     * Release one or more mouse buttons (same bitmask as {@link #mousePress(int)}).
     */
    void mouseRelease(int buttons);

    /**
     * Scroll the mouse wheel.
     *
     * @param wheelAmt number of notches; positive scrolls down
     */
    void mouseWheel(int wheelAmt);

    /**
     * Press a keyboard key by AWT {@code java.awt.event.KeyEvent.VK_*} code.
     */
    void keyPress(int keycode);

    /**
     * Release a keyboard key by AWT {@code java.awt.event.KeyEvent.VK_*} code.
     */
    void keyRelease(int keycode);

    /**
     * Sleep the current thread for the given number of milliseconds.
     */
    void delay(int ms);

    /**
     * Release the underlying Wayland session and resources. Idempotent.
     */
    @Override
    void close();
}

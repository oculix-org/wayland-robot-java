/**
 * A drop-in {@code java.awt.Robot} replacement for Wayland sessions.
 *
 * <p>Bridges the JVM to the sanctioned Wayland input synthesis channels —
 * {@code xdg-desktop-portal} {@code RemoteDesktop} today, {@code libei}
 * planned — so that input synthesis works on pure Wayland (GNOME, KDE,
 * wlroots-based compositors) where {@code java.awt.Robot} silently fails
 * because of <a href="https://bugs.openjdk.org/browse/JDK-8280983">JDK-8280983</a>.
 *
 * <p>Entry point: {@link org.oculix.wayland.robot.RobotFactory}.
 */
package org.oculix.wayland.robot;

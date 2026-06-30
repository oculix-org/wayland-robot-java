package org.oculix.wayland.robot;

import java.util.Map;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

/**
 * DBus client binding for {@code org.freedesktop.portal.RemoteDesktop}.
 *
 * <p>The session lifecycle ({@code CreateSession}, {@code SelectDevices},
 * {@code Start}) returns a {@code Request} object path that resolves via a
 * signal on {@link RequestInterface}. The input-synthesis methods
 * ({@code NotifyPointerMotion}, {@code NotifyPointerButton},
 * {@code NotifyKeyboardKeycode}, {@code NotifyPointerAxis}) are
 * fire-and-forget on an established session.
 *
 * <p>Spec: <a href="https://flatpak.github.io/xdg-desktop-portal/docs/doc-org.freedesktop.portal.RemoteDesktop.html">
 * portal.RemoteDesktop</a>.
 */
@DBusInterfaceName("org.freedesktop.portal.RemoteDesktop")
public interface RemoteDesktopInterface extends DBusInterface {

    DBusPath CreateSession(Map<String, Variant<?>> options);

    DBusPath SelectDevices(DBusPath session, Map<String, Variant<?>> options);

    DBusPath Start(DBusPath session, String parentWindow, Map<String, Variant<?>> options);

    void NotifyPointerMotion(DBusPath session, Map<String, Variant<?>> options, double dx, double dy);

    void NotifyPointerMotionAbsolute(DBusPath session, Map<String, Variant<?>> options, UInt32 stream, double x, double y);

    void NotifyPointerButton(DBusPath session, Map<String, Variant<?>> options, int button, UInt32 state);

    void NotifyPointerAxis(DBusPath session, Map<String, Variant<?>> options, double dx, double dy);

    void NotifyPointerAxisDiscrete(DBusPath session, Map<String, Variant<?>> options, UInt32 axis, int steps);

    void NotifyKeyboardKeycode(DBusPath session, Map<String, Variant<?>> options, int keycode, UInt32 state);

    void NotifyKeyboardKeysym(DBusPath session, Map<String, Variant<?>> options, int keysym, UInt32 state);
}

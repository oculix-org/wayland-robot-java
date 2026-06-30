package org.oculix.wayland.robot;

import java.util.Map;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

/**
 * DBus client binding for {@code org.freedesktop.portal.Request} — the
 * universal portal callback channel. Every portal session method
 * ({@code CreateSession}, {@code SelectDevices}, {@code Start}) returns a
 * {@code Request} object path, and the result arrives later as a
 * {@link Response} signal on that path.
 *
 * <p>Spec: <a href="https://flatpak.github.io/xdg-desktop-portal/docs/doc-org.freedesktop.portal.Request.html">
 * portal.Request</a>.
 */
@DBusInterfaceName("org.freedesktop.portal.Request")
public interface RequestInterface extends DBusInterface {

    void Close();

    /**
     * Signal emitted on the {@code Request} path when the operation completes.
     * Response code {@code 0} = success, {@code 1} = user cancelled,
     * {@code 2} = other error. {@code results} carries the portal-specific
     * return values (for example {@code session_handle} on
     * {@code CreateSession}).
     */
    class Response extends DBusSignal {
        private final UInt32 response;
        private final Map<String, Variant<?>> results;

        public Response(String objectPath, UInt32 response, Map<String, Variant<?>> results) throws DBusException {
            super(objectPath, response, results);
            this.response = response;
            this.results = results;
        }

        public UInt32 getResponse() {
            return response;
        }

        public Map<String, Variant<?>> getResults() {
            return results;
        }
    }
}

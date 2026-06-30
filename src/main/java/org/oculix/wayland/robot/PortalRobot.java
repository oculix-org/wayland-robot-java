package org.oculix.wayland.robot;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link WaylandRobot} implementation backed by {@code xdg-desktop-portal}'s
 * {@code RemoteDesktop} interface, over DBus.
 *
 * <p>On construction this class:
 * <ol>
 *   <li>opens the user session DBus,
 *   <li>obtains a proxy on {@code /org/freedesktop/portal/desktop},
 *   <li>negotiates a {@code RemoteDesktop} session — which triggers the
 *       portal consent dialog on first use.
 * </ol>
 *
 * <p>Input methods translate AWT-style codes (the bitmasks of
 * {@link InputEvent} and the {@link KeyEvent} {@code VK_*} integers) into
 * the evdev codes the portal expects. The translation table is minimal
 * for now (mouse buttons + a small subset of keys) and will grow as the
 * library matures.
 *
 * <p>This class is thread-safe for individual method calls; concurrent use
 * across threads is supported by the underlying DBus connection, but the
 * portal session itself is single-owner.
 */
public final class PortalRobot implements WaylandRobot {

    private static final Logger LOG = LoggerFactory.getLogger(PortalRobot.class);

    private static final String PORTAL_SERVICE = "org.freedesktop.portal.Desktop";
    private static final String PORTAL_PATH = "/org/freedesktop/portal/desktop";

    /** {@code RemoteDesktop} {@code DeviceType} bitmask values, per the portal spec. */
    private static final int DEVICE_KEYBOARD = 1;
    private static final int DEVICE_POINTER = 2;
    private static final int DEVICE_TOUCHSCREEN = 4;

    /** Default consent dialog timeout — generous; the user may take their time. */
    private static final long CONSENT_TIMEOUT_SECONDS = 120;

    private final DBusConnection conn;
    private final RemoteDesktopInterface portal;
    private final DBusPath session;

    private volatile boolean closed;

    /**
     * Open a portal session synchronously. The constructor blocks until the
     * user dismisses the consent dialog (success or cancel) or the timeout
     * elapses.
     *
     * @throws WaylandRobotException if DBus is unavailable, the portal is not
     *     present, or the user denies consent.
     */
    public PortalRobot() {
        try {
            this.conn = DBusConnectionBuilder.forSessionBus().build();
            this.portal = conn.getRemoteObject(PORTAL_SERVICE, PORTAL_PATH, RemoteDesktopInterface.class);
            this.session = negotiateSession();
        } catch (Exception e) {
            throw new WaylandRobotException("Failed to initialize portal session", e);
        }
    }

    @Override
    public void mouseMove(int x, int y) {
        ensureOpen();
        try {
            portal.NotifyPointerMotionAbsolute(session, Map.of(), new UInt32(0), x, y);
        } catch (Exception e) {
            throw new WaylandRobotException("NotifyPointerMotionAbsolute failed", e);
        }
    }

    @Override
    public void mousePress(int buttons) {
        sendMouseButtons(buttons, true);
    }

    @Override
    public void mouseRelease(int buttons) {
        sendMouseButtons(buttons, false);
    }

    @Override
    public void mouseWheel(int wheelAmt) {
        ensureOpen();
        try {
            portal.NotifyPointerAxisDiscrete(session, Map.of(), new UInt32(0), wheelAmt);
        } catch (Exception e) {
            throw new WaylandRobotException("NotifyPointerAxisDiscrete failed", e);
        }
    }

    @Override
    public void keyPress(int keycode) {
        sendKey(keycode, true);
    }

    @Override
    public void keyRelease(int keycode) {
        sendKey(keycode, false);
    }

    @Override
    public void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        try {
            conn.disconnect();
        } catch (Exception e) {
            LOG.warn("DBus disconnect raised; the session may already be torn down", e);
        }
    }

    // --- internals -----------------------------------------------------

    private void ensureOpen() {
        if (closed) {
            throw new WaylandRobotException("PortalRobot is closed");
        }
    }

    private void sendMouseButtons(int buttons, boolean press) {
        ensureOpen();
        UInt32 state = new UInt32(press ? 1 : 0);
        try {
            if ((buttons & InputEvent.BUTTON1_DOWN_MASK) != 0) {
                portal.NotifyPointerButton(session, Map.of(), EvdevButton.LEFT, state);
            }
            if ((buttons & InputEvent.BUTTON2_DOWN_MASK) != 0) {
                portal.NotifyPointerButton(session, Map.of(), EvdevButton.MIDDLE, state);
            }
            if ((buttons & InputEvent.BUTTON3_DOWN_MASK) != 0) {
                portal.NotifyPointerButton(session, Map.of(), EvdevButton.RIGHT, state);
            }
        } catch (Exception e) {
            throw new WaylandRobotException("NotifyPointerButton failed", e);
        }
    }

    private void sendKey(int awtKeyCode, boolean press) {
        ensureOpen();
        int evdev = EvdevKey.fromAwt(awtKeyCode);
        if (evdev < 0) {
            throw new WaylandRobotException("Unsupported key code: " + awtKeyCode);
        }
        try {
            portal.NotifyKeyboardKeycode(session, Map.of(), evdev, new UInt32(press ? 1 : 0));
        } catch (Exception e) {
            throw new WaylandRobotException("NotifyKeyboardKeycode failed", e);
        }
    }

    private DBusPath negotiateSession() throws Exception {
        // 1. CreateSession — handle + session token must be unique per call.
        Map<String, Variant<?>> createOpts = new HashMap<>();
        String sessionToken = "wayrobot_" + UUID.randomUUID().toString().replace("-", "");
        createOpts.put("session_handle_token", new Variant<>(sessionToken));
        createOpts.put("handle_token", new Variant<>("wr_" + UUID.randomUUID().toString().replace("-", "")));
        Map<String, Variant<?>> createResult = awaitRequest(portal.CreateSession(createOpts));
        Variant<?> sessionHandleVariant = createResult.get("session_handle");
        if (sessionHandleVariant == null) {
            throw new WaylandRobotException("CreateSession returned no session_handle");
        }
        DBusPath sessionPath = new DBusPath((String) sessionHandleVariant.getValue());

        // 2. SelectDevices — keyboard + pointer.
        Map<String, Variant<?>> selectOpts = new HashMap<>();
        selectOpts.put("types", new Variant<>(new UInt32(DEVICE_KEYBOARD | DEVICE_POINTER)));
        selectOpts.put("handle_token", new Variant<>("wr_" + UUID.randomUUID().toString().replace("-", "")));
        awaitRequest(portal.SelectDevices(sessionPath, selectOpts));

        // 3. Start — this is the call that triggers the user consent dialog.
        Map<String, Variant<?>> startOpts = new HashMap<>();
        startOpts.put("handle_token", new Variant<>("wr_" + UUID.randomUUID().toString().replace("-", "")));
        awaitRequest(portal.Start(sessionPath, "", startOpts));

        return sessionPath;
    }

    private Map<String, Variant<?>> awaitRequest(DBusPath requestPath) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<RequestInterface.Response> ref = new AtomicReference<>();

        var handler = (org.freedesktop.dbus.interfaces.DBusSigHandler<RequestInterface.Response>) sig -> {
            if (sig.getPath().equals(requestPath.getPath())) {
                ref.set(sig);
                latch.countDown();
            }
        };
        conn.addSigHandler(RequestInterface.Response.class, handler);
        try {
            if (!latch.await(CONSENT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new WaylandRobotException("Portal request timed out after "
                        + CONSENT_TIMEOUT_SECONDS + "s");
            }
        } finally {
            conn.removeSigHandler(RequestInterface.Response.class, handler);
        }

        RequestInterface.Response resp = ref.get();
        int code = (int) resp.getResponse().longValue();
        if (code != 0) {
            throw new WaylandRobotException("Portal request denied (response=" + code + ")");
        }
        return resp.getResults();
    }

    /**
     * evdev mouse button codes (from {@code linux/input-event-codes.h}). The
     * portal wants raw evdev integers, not AWT button numbers.
     */
    private static final class EvdevButton {
        static final int LEFT = 0x110;
        static final int RIGHT = 0x111;
        static final int MIDDLE = 0x112;
    }

    /**
     * Minimal AWT {@code VK_*} → evdev keycode translation, covering the
     * usual suspects. The full map will grow with the library; for now this
     * is enough to validate that the wiring works end-to-end.
     *
     * <p>Reference: {@code /usr/include/linux/input-event-codes.h}.
     */
    private static final class EvdevKey {
        static int fromAwt(int vk) {
            switch (vk) {
                case KeyEvent.VK_ESCAPE: return 1;
                case KeyEvent.VK_1: return 2;
                case KeyEvent.VK_2: return 3;
                case KeyEvent.VK_3: return 4;
                case KeyEvent.VK_4: return 5;
                case KeyEvent.VK_5: return 6;
                case KeyEvent.VK_6: return 7;
                case KeyEvent.VK_7: return 8;
                case KeyEvent.VK_8: return 9;
                case KeyEvent.VK_9: return 10;
                case KeyEvent.VK_0: return 11;
                case KeyEvent.VK_MINUS: return 12;
                case KeyEvent.VK_EQUALS: return 13;
                case KeyEvent.VK_BACK_SPACE: return 14;
                case KeyEvent.VK_TAB: return 15;
                case KeyEvent.VK_Q: return 16;
                case KeyEvent.VK_W: return 17;
                case KeyEvent.VK_E: return 18;
                case KeyEvent.VK_R: return 19;
                case KeyEvent.VK_T: return 20;
                case KeyEvent.VK_Y: return 21;
                case KeyEvent.VK_U: return 22;
                case KeyEvent.VK_I: return 23;
                case KeyEvent.VK_O: return 24;
                case KeyEvent.VK_P: return 25;
                case KeyEvent.VK_ENTER: return 28;
                case KeyEvent.VK_CONTROL: return 29;
                case KeyEvent.VK_A: return 30;
                case KeyEvent.VK_S: return 31;
                case KeyEvent.VK_D: return 32;
                case KeyEvent.VK_F: return 33;
                case KeyEvent.VK_G: return 34;
                case KeyEvent.VK_H: return 35;
                case KeyEvent.VK_J: return 36;
                case KeyEvent.VK_K: return 37;
                case KeyEvent.VK_L: return 38;
                case KeyEvent.VK_SHIFT: return 42;
                case KeyEvent.VK_Z: return 44;
                case KeyEvent.VK_X: return 45;
                case KeyEvent.VK_C: return 46;
                case KeyEvent.VK_V: return 47;
                case KeyEvent.VK_B: return 48;
                case KeyEvent.VK_N: return 49;
                case KeyEvent.VK_M: return 50;
                case KeyEvent.VK_SPACE: return 57;
                case KeyEvent.VK_LEFT: return 105;
                case KeyEvent.VK_RIGHT: return 106;
                case KeyEvent.VK_UP: return 103;
                case KeyEvent.VK_DOWN: return 108;
                default: return -1;
            }
        }
    }
}

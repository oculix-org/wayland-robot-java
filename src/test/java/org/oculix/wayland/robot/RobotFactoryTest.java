package org.oculix.wayland.robot;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class RobotFactoryTest {

    @Test
    void isSupportedReturnsFalseOnNonLinuxOs() {
        String savedOsName = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 11");
        try {
            assertFalse(RobotFactory.isSupported(),
                    "isSupported() must return false on non-Linux OS");
        } finally {
            if (savedOsName != null) {
                System.setProperty("os.name", savedOsName);
            }
        }
    }
}

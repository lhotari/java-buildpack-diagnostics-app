package io.github.lhotari.jbpdiagnostics

import java.lang.management.ManagementFactory

/**
 * Created by lari on 13/03/15.
 */
class DiagUtils {
    public static int getCurrentProcessId() {
        ManagementFactory.getRuntimeMXBean().getName().split("@")[0] as int
    }

    public static boolean isMacOs() {
        System.properties['os.name'] =~ /Mac/
    }
}

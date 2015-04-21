package io.github.lhotari.jbpdiagnostics;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;

import java.io.File;

public class MallocInfo {
    static native void malloc_stats();
    static native Pointer fopen(String filename, String mode);
    static native int fflush(Pointer fd);
    static native int fclose(Pointer fd);
    static native int malloc_info(int options, Pointer fd);

    static {
        Native.register(Platform.C_LIBRARY_NAME);
    }

    public static void writeMallocInfo(File file) {
        Pointer fd = fopen(file.getAbsolutePath(), "w");
        try {
            malloc_info(0, fd);
            fflush(fd);
        } finally {
            fclose(fd);
        }
    }

    public static void mallocStats() {
        malloc_stats();
    }
}

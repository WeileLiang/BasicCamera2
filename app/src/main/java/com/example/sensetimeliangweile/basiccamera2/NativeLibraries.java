package com.example.sensetimeliangweile.basiccamera2;

public final class NativeLibraries {
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    public static native String stringFromJNI();
}

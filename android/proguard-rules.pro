-keep class org.qtproject.qt.android.** { *; }

-keep class org.asterium.asterium.NativeBridge {
    private static native void nativeSend(int, java.lang.String, java.lang.String);
    static void onEngineReady();
    static void onSnapshot(java.lang.String);
    static void onReply(int, java.lang.String);
}

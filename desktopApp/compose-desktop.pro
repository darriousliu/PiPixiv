-ignorewarnings
-keepattributes SourceFile,LineNumberTable,SourceDebugExtension
-allowaccessmodification
-repackageclasses

-keep class de.jensklingenberg.ktorfit.** { *; }
-keepclassmembers class de.jensklingenberg.ktorfit.** { *; }

-keep class com.mrl.pixiv.common.network.ApiClient
-keep class com.mrl.pixiv.common.network.AuthClient
-keep class com.mrl.pixiv.common.network.ImageClient

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers interface com.ctrip.flight.mmkv.MMKVInternalLog { *; }

# androidx sqlite
-keep class androidx.sqlite.SQLiteException
-keep class androidx.sqlite.driver.bundled.** { *; }

# JNA
-keep class com.sun.jna.* { *; }
-keep class * extends com.sun.jna.* { *; }
-keepclassmembers class * extends com.sun.jna.* { public *; }

# FileKit's XDG portal uses D-Bus proxy interfaces. Their methods are wire
# protocol names, and dbus-java rejects interfaces repackaged into the root.
-keep interface * extends org.freedesktop.dbus.interfaces.DBusInterface {
    public <methods>;
}
-keep interface org.freedesktop.dbus.interfaces.DBusInterface {
    public <methods>;
}
# FileKit locates these public signal handlers by name, including inherited ones.
-keepclassmembers class org.freedesktop.dbus.connections.** {
    java.lang.AutoCloseable addGenericSigHandler(org.freedesktop.dbus.matchrules.DBusMatchRule, org.freedesktop.dbus.interfaces.DBusSigHandler);
    java.lang.AutoCloseable addSigHandler(org.freedesktop.dbus.matchrules.DBusMatchRule, org.freedesktop.dbus.interfaces.DBusSigHandler);
}
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,Signature

-assumevalues public class androidx.compose.runtime.ComposeRuntimeFlags {
    static boolean isLinkBufferComposerEnabled return true;
}

# -printmapping mappings-desktop-currentOS.txt

-printconfiguration build/compose/binaries/main-release/proguard/configuration.txt
-printmapping build/compose/binaries/main-release/proguard/mapping.txt
-printseeds build/compose/binaries/main-release/proguard/seeds.txt
-printusage build/compose/binaries/main-release/proguard/usage.txt
# Conscrypt 反射读取此策略；丢失该方法会导致 ECH 未启用。
-keep class org.conscrypt.** { *; }
-keep,allowoptimization class com.mrl.pixiv.common.network.PixivEchTrustManager {
    public org.conscrypt.NetworkSecurityPolicy getNetworkSecurityPolicy();
}

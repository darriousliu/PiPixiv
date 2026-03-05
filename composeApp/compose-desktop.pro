-ignorewarnings
-keepattributes LineNumberTable
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

# -printmapping mappings-desktop-currentOS.txt

-printconfiguration build/compose/binaries/main-release/proguard/configuration.txt
-printmapping build/compose/binaries/main-release/proguard/mapping.txt
-printseeds build/compose/binaries/main-release/proguard/seeds.txt
-printusage build/compose/binaries/main-release/proguard/usage.txt
# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Jetpack Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase

# Room Database & SQLite Reflection
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# App Data Models & Question Entities
-keep class com.nhyle.hyperionmaritime.data.model.** { *; }
-keepclassmembers class com.nhyle.hyperionmaritime.data.model.** { *; }
-keep enum com.nhyle.hyperionmaritime.** { *; }

# Serialization & Licensing Objects
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve Licensing Key, Security Tokens & Question Bank Models
-keep class com.nhyle.hyperionmaritime.** { *; }

# Automated Debug Log Stripping (Remove logging overhead in production)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# Jetpack Compose Rules
-keep class androidx.compose.runtime.** { *; }

# Google Play Core Billing & Entitlement Cache
-keep class com.android.billingclient.api.** { *; }
-keep interface com.android.billingclient.api.** { *; }
-keep class com.nhyle.hyperionmaritime.data.billing.** { *; }
-keepclassmembers class com.nhyle.hyperionmaritime.data.billing.** { *; }
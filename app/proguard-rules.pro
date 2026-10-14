# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Room Database & SQLite Reflection
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Data Models, Serialization & Licensing Objects
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
-keep class com.hyperion.maritime.data.** { *; }
-keep class com.hyperion.maritime.model.** { *; }
-keep class com.example.data.** { *; }
-keep class com.example.model.** { *; }
-keep class com.example.data.model.** { *; }
-keep class com.example.data.local.** { *; }

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
-keep class com.example.billing.** { *; }
-keep class com.example.data.billing.** { *; }
-keepclassmembers class com.example.data.billing.TrackEntitlements { *; }
-keepclassmembers class com.example.data.billing.PlayProductItem { *; }
-keepclassmembers class com.example.data.billing.ActivePlanInfo { *; }

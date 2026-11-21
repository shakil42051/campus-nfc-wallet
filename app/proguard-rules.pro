# Campus NFC Wallet ProGuard / R8 Rules

# Keep Room database, entities, and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class com.example.data.local.** { *; }
-keep class com.example.attendance.data.** { *; }

# Keep data models used with JSON serialization
-keep class com.example.data.model.** { *; }
-keep class com.example.backup.model.** { *; }

# Keep BiometricPrompt callbacks
-keepclassmembers class * extends androidx.biometric.BiometricPrompt$AuthenticationCallback {
    public *;
}

# Preserve line numbers for crash diagnostics while obfuscating filenames
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

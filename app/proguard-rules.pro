# ─── ProGuard Rules for AI Coding Mentor ────────────────────────────────

# ── Retrofit ────────────────────────────────────────────────────────────
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# ── OkHttp ──────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ── Gson ────────────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep our data model classes (used by Gson serialization)
-keep class com.euphoria.aimentor.data.model.** { *; }

# ── Firebase ────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ── ML Kit ──────────────────────────────────────────────────────────────
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ── CameraX ─────────────────────────────────────────────────────────────
-keep class androidx.camera.** { *; }

# ── Compose ─────────────────────────────────────────────────────────────
-dontwarn androidx.compose.**

# ── Keep API service interfaces ─────────────────────────────────────────
-keep,allowobfuscation interface com.euphoria.aimentor.data.api.BackendApiService

# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Release shrink compatibility -------------------------------------------
# Enabling minify/shrink triggers R8 warnings for optional classes that are
# only referenced for annotation processing or via reflection by dependencies.
# These classes are not required at runtime, so we suppress the warnings using
# the rules generated under app/build/outputs/mapping/release/missing_rules.txt.
-dontwarn com.google.auto.value.AutoValue
-dontwarn com.google.crypto.tink.subtle.Ed25519Sign$KeyPair
-dontwarn com.google.crypto.tink.subtle.Ed25519Sign
-dontwarn com.google.crypto.tink.subtle.Ed25519Verify
-dontwarn com.google.crypto.tink.subtle.X25519
-dontwarn com.google.crypto.tink.subtle.XChaCha20Poly1305
-dontwarn edu.umd.cs.findbugs.annotations.NonNull
-dontwarn edu.umd.cs.findbugs.annotations.Nullable
-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings
-dontwarn net.jcip.annotations.GuardedBy
-dontwarn net.jcip.annotations.Immutable
-dontwarn net.jcip.annotations.ThreadSafe

# --- Retrofit 3 / Moshi -----------------------------------------------------
# Retrofit 3.0.0 (Preview) still relies on reflection for interface methods.
# Keep service interfaces and retain method annotations.
-keep class com.lurenjia534.skydrivex.data.remote.** { *; }
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Moshi generates JsonAdapter classes at compile time; keep them and any
# @JsonClass annotated models to avoid being stripped by R8.
-keep @com.squareup.moshi.JsonClass class * { *; }
-if class * extends com.squareup.moshi.JsonAdapter
-keep class <1> { *; }
-if class * extends com.squareup.moshi.JsonAdapter$Factory
-keep class <1> { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.FromJson <methods>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.ToJson <methods>;
}

# Our DTO packages are consumed via Moshi adapters – keep them intact.
-keep class com.lurenjia534.skydrivex.data.model.** { *; }

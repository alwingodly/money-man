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

# Keep readable crash stack traces while still obfuscating class names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- kotlinx.serialization ----
# Room/Hilt/Compose ship their own consumer ProGuard rules, but kotlinx.serialization
# relies on generated $$serializer classes that R8 would otherwise strip/rename, which
# would break the JSON backup/restore feature at runtime.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Keep the generated serializers and the Companion.serializer() lookups for our @Serializable models.
-keepclassmembers class com.alwin.moneymanager.** {
    *** Companion;
}
-keepclasseswithmembers class com.alwin.moneymanager.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.alwin.moneymanager.**$$serializer { *; }
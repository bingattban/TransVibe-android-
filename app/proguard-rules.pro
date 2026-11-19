# TransVibe R8 / ProGuard Rules
-keep class com.transvibe.app.nativebridge.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.k2fsa.sherpa.onnx.** { *; }
-keep class com.google.mlkit.** { *; }
-keep class com.transvibe.app.domain.** { *; }
-keep class com.transvibe.app.persistence.** { *; }

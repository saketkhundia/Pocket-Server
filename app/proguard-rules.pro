# Keep model / serialization classes
-keep class com.saketkhundia.pocketserver.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
# Ktor / CIO
-dontwarn io.netty.**
# Ktor references JVM-only management APIs (IntellijIdeaDebugDetector) absent on Android
-dontwarn java.lang.management.**
# ZXing
-dontwarn com.google.zxing.**

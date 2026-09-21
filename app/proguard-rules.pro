# Keep model / serialization classes
-keep class com.saketkhundia.pocketserver.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
# Ktor / CIO
-dontwarn io.netty.**
# JmDNS (mDNS responder — instantiated directly, no reflection, but keep it
# intact for release builds; slf4j-api is its tiny logging facade)
-keep class javax.jmdns.** { *; }
-dontwarn javax.jmdns.**
-dontwarn org.slf4j.**
# Ktor references JVM-only management APIs (IntellijIdeaDebugDetector) absent on Android
-dontwarn java.lang.management.**
# ZXing
-dontwarn com.google.zxing.**

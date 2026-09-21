# Keep model / serialization classes
-keep class com.saketkhundia.pocketserver.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
# Ktor / CIO
-dontwarn io.netty.**
# ZXing
-dontwarn com.google.zxing.**

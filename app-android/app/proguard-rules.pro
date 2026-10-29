# 쉼표 릴리스 R8 규칙 (docs/release.md). Hilt·Room·Retrofit·kotlinx.serialization은 각 라이브러리가 consumer 규칙을 같이 준다.

# Pleos SDK: aar 안 클래스 이름으로 바인딩하므로 그대로 둔다.
-keep class ai.pleos.** { *; }
-dontwarn ai.pleos.**

# AAOS android.car (uses-library, 기기에만 있음)
-dontwarn android.car.**

# kotlinx.serialization: @Serializable 클래스의 serializer() 유지
-keepclassmembers class com.emop.pause.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepattributes *Annotation*, InnerClasses, Signature

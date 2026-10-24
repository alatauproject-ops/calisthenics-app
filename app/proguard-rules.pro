-keepattributes *Annotation*, InnerClasses
-keep class com.edward.calisthenics.data.** { *; }
-keepclassmembers class com.edward.calisthenics.data.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

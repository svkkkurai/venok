-keep class com.google.gson.** { *; }
-keep class org.jaudiotagger.** { *; }

-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

-keepattributes AnnotationDefault,RuntimeVisibleAnnotations
-keep class com.google.gson.reflect.TypeToken { <fields>; }
-keepclassmembers class **$TypeAdapterFactory { <fields>; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.sakkkurai.venokUpdates.UpdateConfig { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
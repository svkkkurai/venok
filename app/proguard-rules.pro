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


-keep class com.sakkkurai.venok.tools.Updater$UpdateConfig { *; }
-keepclassmembers class com.sakkkurai.venok.tools.Updater$UpdateConfig {
    public *;
    private *;
}

-keep class java.util.Map { *; }
-keep class java.util.HashMap { *; }
-keep class java.util.AbstractMap { *; } # На случай, если g4.a — это AbstractMap
-keepclassmembers class java.util.HashMap {
    public *;
    private *;
}
-keepclassmembers class java.util.Map {
    public *;
    private *;
}

-keep class com.google.gson.** { *; }
-keepclassmembers class com.google.gson.** { *; }


-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes AnnotationDefault
-keepattributes RuntimeVisibleAnnotations

-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}


-keep class java.util.** { *; }
-keepclassmembers class java.util.** { *; }

-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.TypeAdapter { *; }

-dontoptimize
-dontshrink

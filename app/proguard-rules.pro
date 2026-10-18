# Entry points the framework reaches by name.
-keep class com.wae.statussaver.WaeApp { *; }
-keep class com.wae.statussaver.MainActivity { *; }
-keep class com.wae.statussaver.service.** { *; }

# Coroutines internals.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-dontwarn org.jetbrains.annotations.**

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

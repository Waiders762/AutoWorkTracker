-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

-dontwarn kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers @kotlinx.serialization.Serializable class com.autoworktracker.data.backup.** {
    <fields>;
    *** Companion;
}
-keep class com.autoworktracker.data.backup.** { *; }

-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

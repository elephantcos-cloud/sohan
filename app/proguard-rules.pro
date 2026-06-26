# dadb — ADB TCP library
-keep class dadb.** { *; }
-dontwarn dadb.**

# AIDL stubs — client apps depend on these exact class names
-keep class com.shohan.sohan.ISohanService { *; }
-keep class com.shohan.sohan.ISohanService$Stub { *; }
-keep class com.shohan.sohan.ISohanService$Stub$Proxy { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# Kotlin serialization (if used in future)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

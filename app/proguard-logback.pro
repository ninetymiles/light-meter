
# For logback-android and SLF4J
# Ref: https://github.com/tony19/logback-android#proguard
-keep class ch.qos.** { *; }
-keep class org.slf4j.** { *; }
-keepattributes *Annotation*
-dontwarn ch.qos.logback.core.net.*
-dontwarn org.slf4j.**

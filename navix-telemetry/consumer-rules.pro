# Navix Telemetry — consumer ProGuard rules

# Keep all NavEventExporter implementations (user-provided subclasses)
-keep class * implements io.navix.telemetry.NavEventExporter { *; }

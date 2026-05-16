# Navix Runtime — consumer ProGuard rules
# These rules are automatically applied to projects that depend on navix-runtime.

# Keep all Route implementations (serialized by Kotlin Serialization)
-keep @kotlinx.serialization.Serializable class * implements io.navix.contracts.Route { *; }

# Keep NavixRouteRegistry implementations (referenced by class name at startup)
-keep class * implements io.navix.runtime.NavixRouteRegistry { *; }

# Keep generated DeepLinkHandler implementations
-keep class io.navix.generated.*DeepLinkHandler { *; }

# Keep generated NavixRouteRegistry implementations
-keep class io.navix.generated.*NavixRouteRegistry { *; }

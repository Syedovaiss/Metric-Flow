# MetricFlow Consumer ProGuard Rules
# These rules are applied to consumers of this library

# Keep crash monitoring classes
-keep class com.ovais.metric_flow.domain.CrashMonitorer { *; }
-keep class com.ovais.metric_flow.domain.DefaultCrashMonitorer { *; }

# Keep network observer classes
-keep class com.ovais.metric_flow.domain.NetworkObserver { *; }
-keep class com.ovais.metric_flow.domain.OkHttpNetworkObserver { *; }
-keep class com.ovais.metric_flow.domain.KtorNetworkObserver { *; }

# Keep MetricFlow core classes
-keep class com.ovais.metric_flow.core.MetricFlow { *; }
-keep class com.ovais.metric_flow.core.MetricFlowImpl { *; }

# Keep data classes
-keep class com.ovais.metric_flow.data.PerformanceConfig { *; }
-keep class com.ovais.metric_flow.data.NetworkClientType { *; }

# Keep reflection-based classes used by Timber
-keep class com.ovais.metric_flow.util.MetricLoggingTree { *; }

# Keep line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable


# MetricFlow SDK

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-blue.svg)](https://developer.android.com/about/versions/7.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-purple.svg)](https://kotlinlang.org/)

A comprehensive, production-ready Android performance monitoring SDK that tracks crashes, app startup, frame rates, memory usage, network calls, battery status, connectivity, and device information.

## 🚀 Features

### Performance Monitoring
- **🚨 Crash Monitoring** - Automatic crash detection with optional screenshot capture and ANR detection
- **⚡ App Startup Tracking** - Measure app cold start time accurately
- **🎬 Frame Rate Monitoring** - Track FPS and detect dropped frames in real-time
- **💾 Memory Tracking** - Monitor memory usage with configurable sampling intervals and heap dump support
- **🌐 Network Monitoring** - Track network requests across all major Android networking libraries
- **📝 Log Collection** - Capture and filter logcat output

### System Monitoring
- **🔋 Battery Monitoring** - Track battery level, charging status, health, and temperature
- **📡 Connectivity Monitoring** - Monitor WiFi, Cellular, Ethernet, and VPN connections
- **📱 Device Information** - Collect comprehensive device and app information

### SDK Highlights
- ✅ **Thread-safe** - All operations are thread-safe and production-ready
- ✅ **Memory leak prevention** - Proper resource cleanup and lifecycle management
- ✅ **Comprehensive error handling** - Graceful degradation with detailed error logging
- ✅ **Zero dependencies on app code** - Works automatically after initialization
- ✅ **Highly configurable** - Enable/disable features individually
- ✅ **Network library agnostic** - Supports OkHttp, Retrofit, Ktor, HttpURLConnection, and Volley

## 📦 Installation

### Gradle (Kotlin DSL)

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.ovais.metric_flow:metric-flow:1.0.0")
}
```

### Gradle (Groovy)

Add to your `build.gradle`:

```groovy
dependencies {
    implementation 'com.ovais.metric_flow:metric-flow:1.0.0'
}
```

## 🎯 Quick Start

### Basic Setup

```kotlin
class MyApp : Application() {
    private val metricFlow by lazy { MetricFlowFactory.create() }

    override fun onCreate() {
        super.onCreate()
        metricFlow.initialize(
            application = this,
            config = performanceConfig {
                enableCrashMonitoring = true
                enableAppStartupTracker = true
                enableFrameObservation = true
                enableMemoryTracker = true
                enableNetworkObserver = true
            }
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        metricFlow.release()
    }
}
```

### With Network Type Selection

```kotlin
metricFlow.initialize(
    application = this,
    config = performanceConfig { /* ... */ },
    networkType = NetworkClientType.Auto  // Auto-detects network type
)
```

## ⚙️ Configuration

### PerformanceConfig

All features can be individually enabled/disabled:

```kotlin
val config = performanceConfig {
    // Feature toggles
    enableCrashMonitoring = true
    enableAppStartupTracker = true
    enableFrameObservation = true
    enableMemoryTracker = true
    enableScreenshotCapture = true
    enableNetworkObserver = true
    enableLogsCapture = true
    enableBatteryMonitoring = true
    enableConnectivityMonitoring = true
    enableDeviceInfoCollection = true
    
    // Memory tracking configuration
    memorySampleIntervalMs = 5_000L  // Sample every 5 seconds
    memoryLowMemoryTrimThresholdKb = 50 * 1024  // 50 MB threshold
    memoryEnableHeapDumpOnLowMemory = false
    memoryLogSamples = true
}
```

### Network Type Options

```kotlin
NetworkClientType.OkHttp           // For OkHttp clients
NetworkClientType.Retrofit          // For Retrofit (uses OkHttp)
NetworkClientType.Ktor              // For Ktor clients
NetworkClientType.HttpURLConnection // For HttpURLConnection
NetworkClientType.Volley            // For Volley
NetworkClientType.Auto              // Auto-detect (defaults to OkHttp/Retrofit)
```

## 🌐 Network Monitoring Setup

Network monitoring requires manual integration of interceptors/plugins. The SDK provides helpers for each library:

### OkHttp

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(OkHttpNetworkObserver.createInterceptor())
    .build()
```

### Retrofit

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(RetrofitNetworkObserver.createInterceptor())
    .build()

val retrofit = Retrofit.Builder()
    .baseUrl("https://api.example.com/")
    .client(okHttpClient)
    .build()
```

### Ktor

```kotlin
val httpClient = HttpClient {
    install(KtorNetworkObserver.MetricFlowPlugin)
}
```

### HttpURLConnection

```kotlin
val url = URL("https://api.example.com/data")
val connection = url.openConnection() as HttpURLConnection
val monitor = HttpURLConnectionObserver.createMonitor(connection)
monitor.setMethod("GET")
connection.connect()
monitor.recordResponse(connection.responseCode)
```

### Volley

```kotlin
val startTime = System.nanoTime()
val request = StringRequest(Request.Method.GET, url,
    { response ->
        VolleyNetworkObserver.recordSuccess(url, "GET", startTime, 200)
        // handle response
    },
    { error ->
        VolleyNetworkObserver.recordError(url, "GET", startTime, error)
        // handle error
    }
)
requestQueue.add(request)
```

## 📚 API Reference

### MetricFlowFactory

```kotlin
object MetricFlowFactory {
    @JvmStatic
    fun create(): MetricFlow
}
```

### MetricFlow Interface

```kotlin
interface MetricFlow {
    fun initialize(application: Application, config: PerformanceConfig)
    fun initialize(application: Application, config: PerformanceConfig, networkType: NetworkClientType)
    fun release()
}
```

### Network Observers

#### OkHttpNetworkObserver

```kotlin
object OkHttpNetworkObserver {
    fun createInterceptor(): Interceptor
    fun install()
    fun uninstall()
}
```

#### RetrofitNetworkObserver

```kotlin
object RetrofitNetworkObserver {
    fun createInterceptor(): Interceptor
    fun install()
    fun uninstall()
}
```

#### KtorNetworkObserver

```kotlin
object KtorNetworkObserver {
    val MetricFlowPlugin: ClientPlugin<*, *>
    fun install()
    fun uninstall()
}
```

#### HttpURLConnectionObserver

```kotlin
object HttpURLConnectionObserver {
    fun createMonitor(connection: HttpURLConnection): HttpURLConnectionMonitor
    fun wrap(connection: HttpURLConnection, block: (HttpURLConnection) -> T): T
    fun install()
    fun uninstall()
}
```

#### VolleyNetworkObserver

```kotlin
object VolleyNetworkObserver {
    fun recordSuccess(url: String, method: String, startNs: Long, statusCode: Int)
    fun recordError(url: String, method: String, startNs: Long, error: Exception)
    fun install()
    fun uninstall()
}
```

### DeviceInfoCollector

```kotlin
object DeviceInfoCollector {
    fun collect(context: Context): DeviceInfo
    fun logDeviceInfo(context: Context)
}

data class DeviceInfo(
    val deviceModel: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdkVersion: Int,
    val appVersion: String?,
    val appVersionCode: Long?,
    val totalMemory: Long,
    val availableMemory: Long,
    val cpuCores: Int,
    val screenResolution: String
)
```

## 🔧 Advanced Usage

### Custom Memory Tracking Listener

```kotlin
MemoryTracker.install(
    application = application,
    config = MemoryTracker.Config(
        sampleIntervalMs = 10_000L,
        enableHeapDumpOnLowMemory = true,
        heapDumpDirectory = File(cacheDir, "heapdumps")
    ),
    listener = object : MemoryTracker.Listener {
        override fun onSample(snapshot: MemoryTracker.MemorySnapshot) {
            // Custom handling
        }
        
        override fun onLowMemory(trimLevel: Int, snapshot: MemoryTracker.MemorySnapshot) {
            // Handle low memory
        }
        
        override fun onHeapDumpSaved(path: String) {
            // Process heap dump
        }
    }
)
```

### Activity Lifecycle Integration

```kotlin
val activityProvider = DefaultActivityProvider()
activityProvider.install(application)

// Get current activity
val currentActivity = activityProvider.currentActivity

// Listen for activity resume
activityProvider.addOnActivityResumedListener { activity ->
    // Handle activity resume
}
```

## 📋 Requirements

- **Min SDK:** 24 (Android 7.0)
- **Compile SDK:** 36
- **Kotlin:** 1.9.0+
- **Java:** 11+

## 🔒 Permissions

The SDK requires the following permissions (automatically merged):

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

Optional (for LogCat collection - requires root on production devices):
```xml
<uses-permission android:name="android.permission.READ_LOGS" />
```

## 🛡️ ProGuard/R8

The SDK includes consumer ProGuard rules. If you're using R8/ProGuard, ensure your library's `build.gradle.kts` includes:

```kotlin
android {
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}
```

## 🐛 Troubleshooting

### Network monitoring not working

Network monitoring requires manual integration. Make sure you've added the interceptor/plugin to your HTTP client.

### LogCat collection not working

LogCat collection requires `READ_LOGS` permission, which typically requires root access on production devices. This is expected behavior.

### Memory tracking not sampling

Check that:
1. `enableMemoryTracker` is set to `true`
2. `memorySampleIntervalMs` is greater than 0
3. The app has sufficient permissions

### Initialization errors

Ensure:
1. `Application` instance is not null
2. Configuration values are valid (intervals > 0, thresholds >= 0)
3. SDK is initialized on the main thread

## 📝 Logging

The SDK uses Timber for logging. All logs are tagged with `"PerfMon"` for easy filtering:

```bash
adb logcat -s PerfMon
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Built with [Timber](https://github.com/JakeWharton/timber) for logging
- Network monitoring support for OkHttp, Retrofit, Ktor, and Volley
- Inspired by best practices from Firebase Performance Monitoring and Sentry

## 📞 Support

For issues, questions, or contributions, please open an issue on GitHub.

---

**Made with ❤️ for the Android community**


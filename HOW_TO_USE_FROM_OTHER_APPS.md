# Using Sohan from your app

## 1. Copy the AIDL file
Copy `ISohanService.aidl` into your project at:
`app/src/main/aidl/com/shohan/sohan/ISohanService.aidl`

## 2. Add to build.gradle
```groovy
android {
    buildFeatures { aidl true }
}
```

## 3. Bind to SohanService
```kotlin
class MyActivity : AppCompatActivity() {

    private var sohan: ISohanService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            sohan = ISohanService.Stub.asInterface(binder)

            // Check permission first
            if (sohan?.hasPermission() == false) {
                sohan?.requestPermission()   // Shows dialog in Sohan
                // Retry after user grants permission
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) { sohan = null }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent("com.shohan.sohan.SERVICE").apply {
            setPackage("com.shohan.sohan")
        }
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }

    // Run a shell command
    fun example() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val output = sohan?.shell("pm list packages -3")
                // Use output on main thread
            } catch (e: SecurityException) {
                // Not authorized yet — call requestPermission()
                sohan?.requestPermission()
            } catch (e: RemoteException) {
                // ADB disconnected — open Sohan to reconnect
            }
        }
    }
}
```

## Available methods
| Method | Description |
|---|---|
| `getVersion()` | Returns service version |
| `isAdbConnected()` | True if ADB session is alive |
| `shell(cmd)` | Run a shell command, returns stdout |
| `requestPermission()` | Show permission dialog in Sohan |
| `hasPermission()` | Check if your app is authorized |
| `forceStop(pkg)` | Force-stop an app |
| `clearCache(pkg)` | Clear app cache only |
| `uninstall(pkg)` | Uninstall an app |

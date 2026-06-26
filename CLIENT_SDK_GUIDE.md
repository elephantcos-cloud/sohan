# Sohan Client SDK — ব্যবহারের গাইড

## অন্য অ্যাপে Sohan যোগ করার নিয়ম

### ধাপ ১ — `sohan-client` ফোল্ডার copy করো
`sohan-client/` ফোল্ডারটি তোমার project-এর root-এ রাখো।

### ধাপ ২ — settings.gradle-এ যোগ করো
```groovy
include ':sohan-client'
```

### ধাপ ৩ — app/build.gradle-এ dependency যোগ করো
```groovy
dependencies {
    implementation project(':sohan-client')
}
```

---

## সহজ ব্যবহার

### Activity-তে
```kotlin
class MyActivity : AppCompatActivity() {

    private val sohan by lazy { SohanClient(this) }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            // Sohan installed না থাকলে
            if (!SohanInstallChecker.isInstalled(this@MyActivity)) {
                SohanInstallChecker.openInstallPage(this@MyActivity)
                return@launch
            }

            val result = sohan.connect()

            when (result) {
                is SohanResult.Success -> {
                    // Connected! এখন shell command চালাও
                    val output = sohan.shell("pm list packages -3")
                    Log.d("Sohan", output.getOrNull() ?: "failed")
                }
                is SohanResult.Error -> {
                    when (result.error) {
                        SohanError.NOT_AUTHORIZED -> {
                            // Permission নেই — dialog দেখাও
                            sohan.requestPermission()
                            Toast.makeText(this, "Open Sohan and tap Allow", Toast.LENGTH_LONG).show()
                        }
                        SohanError.NOT_INSTALLED -> {
                            SohanInstallChecker.openInstallPage(this)
                        }
                        else -> Log.e("Sohan", result.message)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        sohan.disconnect()
    }
}
```

### Lifecycle Observer দিয়ে (আরো সহজ)
```kotlin
val sohan = SohanLifecycleObserver(this) { result ->
    if (result.isSuccess) {
        lifecycleScope.launch {
            sohan.client.clearCache("com.example.app")
            sohan.client.forceStop("com.example.app")
        }
    }
}
lifecycle.addObserver(sohan)
```

### ViewModel থেকে
```kotlin
class MyViewModel(app: Application) : SohanViewModel(app) {
    fun clearMyCache() = viewModelScope.launch {
        val r = clearCache("com.myapp.package")
        r.fold(
            onSuccess = { /* সফল */ },
            onError   = { msg, _ -> /* ব্যর্থ: $msg */ }
        )
    }
}
```

---

## সব available method

| Method | Return | বিবরণ |
|---|---|---|
| `connect()` | `SohanResult<Int>` | Sohan-এ bind করো |
| `disconnect()` | `Unit` | Unbind করো |
| `isConnected` | `Boolean` | Bind আছে কিনা |
| `isAdbConnected()` | `Boolean` | ADB session আছে কিনা |
| `hasPermission()` | `Boolean` | এই app authorized কিনা |
| `requestPermission()` | `Unit` | Sohan-এ dialog দেখাও |
| `shell(cmd)` | `SohanResult<String>` | Shell command চালাও |
| `clearCache(pkg)` | `SohanResult<String>` | Cache clear করো |
| `forceStop(pkg)` | `SohanResult<Unit>` | Force stop করো |
| `uninstall(pkg)` | `SohanResult<String>` | Uninstall করো |

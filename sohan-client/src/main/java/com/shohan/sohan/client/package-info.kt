/**
 * # sohan-client
 *
 * A lightweight SDK for using the Sohan Wireless ADB bridge from any Android app.
 *
 * ## Quick start
 *
 * ### 1. Add the module to your project
 * Copy the entire `sohan-client/` folder into your project root, then:
 *
 * settings.gradle:
 * ```groovy
 * include ':sohan-client'
 * ```
 *
 * app/build.gradle:
 * ```groovy
 * dependencies {
 *     implementation project(':sohan-client')
 * }
 * ```
 *
 * ### 2. Check if Sohan is installed
 * ```kotlin
 * if (!SohanInstallChecker.isInstalled(context)) {
 *     SohanInstallChecker.openInstallPage(context)
 *     return
 * }
 * ```
 *
 * ### 3a. Lifecycle-aware usage (recommended)
 * ```kotlin
 * val sohan = SohanLifecycleObserver(this) { result ->
 *     if (result.isSuccess) {
 *         lifecycleScope.launch {
 *             val out = sohan.client.shell("id")
 *             Log.d("TAG", out.getOrNull() ?: "error")
 *         }
 *     }
 * }
 * lifecycle.addObserver(sohan)
 * ```
 *
 * ### 3b. Manual usage
 * ```kotlin
 * val client = SohanClient(context)
 * client.connect()
 * val result = client.shell("pm list packages -3")
 * result.fold(
 *     onSuccess = { output -> println(output) },
 *     onError   = { msg, err -> println("Error $err: $msg") }
 * )
 * client.disconnect()
 * ```
 *
 * ### 3c. ViewModel-based usage
 * ```kotlin
 * class MyViewModel(app: Application) : SohanViewModel(app) {
 *     fun doSomething() = viewModelScope.launch {
 *         val r = shell("dumpsys battery")
 *         // use r.getOrNull()
 *     }
 * }
 * ```
 *
 * ## Permission flow
 * If the user has not yet authorized your app in Sohan:
 * 1. Your `shell()` call returns `SohanResult.Error(error = NOT_AUTHORIZED)`
 * 2. Call `client.requestPermission()` — Sohan shows a dialog to the user
 * 3. User taps Allow → retry your call
 */
package com.shohan.sohan.client

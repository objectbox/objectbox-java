package io.objectbox.objectbox_sync_flutter_libs

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.PluginRegistry

/**
 * Handles requesting Android runtime permissions required for Mesh Sync.
 */
internal class MeshSyncPermissions(private val applicationContext: Context) :
    PluginRegistry.RequestPermissionsResultListener {
  private val pendingCallbacks = mutableListOf<() -> Unit>()
  private var activity: Activity? = null
  private var activityBinding: ActivityPluginBinding? = null

  fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activityBinding = binding
    activity = binding.activity
    binding.addRequestPermissionsResultListener(this)
  }

  fun onDetachedFromActivity() {
    activityBinding?.removeRequestPermissionsResultListener(this)
    activityBinding = null
    activity = null
  }

  fun requestIfMissing(callback: () -> Unit) {
    val missingPermissions = missingRuntimePermissions()
    if (missingPermissions.isEmpty()) {
      callback()
      return
    }

    val currentActivity = activity
    if (currentActivity == null) {
      Log.w(
          logTag,
          "Android Mesh Sync runtime permissions are missing, but no Activity is attached")
      callback()
      return
    }

    pendingCallbacks += callback
    if (pendingCallbacks.size > 1) return

    currentActivity.requestPermissions(
        missingPermissions.toTypedArray(), meshPermissionsRequestCode)
  }

  override fun onRequestPermissionsResult(
      requestCode: Int,
      permissions: Array<out String>,
      grantResults: IntArray
  ): Boolean {
    if (requestCode != meshPermissionsRequestCode) return false

    val callbacks = pendingCallbacks.toList()
    pendingCallbacks.clear()
    callbacks.forEach { it() }
    return true
  }

  private fun missingRuntimePermissions(): List<String> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return emptyList()

    return runtimePermissions()
        .filter { applicationContext.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
  }

  private fun runtimePermissions(): List<String> {
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      permissions += Manifest.permission.BLUETOOTH_ADVERTISE
      permissions += Manifest.permission.BLUETOOTH_CONNECT
      permissions += Manifest.permission.BLUETOOTH_SCAN
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      permissions += Manifest.permission.NEARBY_WIFI_DEVICES
    }
    return permissions
  }

  private companion object {
    const val logTag = "ObjectBoxSyncFlutterLibsPlugin"
    const val meshPermissionsRequestCode = 0x0B09
  }
}

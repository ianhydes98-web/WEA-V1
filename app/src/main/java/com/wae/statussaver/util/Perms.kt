package com.wae.statussaver.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

object Perms {

    const val PREFS = "wae_prefs"
    const val KEY_AUTO_SAVE = "auto_save_enabled"

    fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED

    fun runtime(): Array<String> {
        val out = ArrayList<String>(4)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            out += Manifest.permission.READ_MEDIA_IMAGES
            out += Manifest.permission.READ_MEDIA_VIDEO
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                out += Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            }
        } else {
            out += Manifest.permission.READ_EXTERNAL_STORAGE
        }
        out += Manifest.permission.POST_NOTIFICATIONS
        return out.toTypedArray()
    }

    fun hasMediaAccess(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            granted(context, Manifest.permission.READ_MEDIA_IMAGES) ||
                granted(context, Manifest.permission.READ_MEDIA_VIDEO) ||
                hasPartialMediaGrant(context)
        } else {
            granted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    fun hasPartialMediaGrant(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            granted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) &&
            !granted(context, Manifest.permission.READ_MEDIA_IMAGES)

    fun hasNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            granted(context, Manifest.permission.POST_NOTIFICATIONS)

    fun hasAllFilesAccess(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()

    /** True only when the legacy layout exists and the permission-free one does not. */
    fun needsAllFilesAccess(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            !hasAllFilesAccess() &&
            StatusDiscovery.legacyOnly()

    fun allFilesIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            .setData(Uri.fromParts("package", context.packageName, null))

    fun ignoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun batteryIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.fromParts("package", context.packageName, null))

    fun batteryListIntent(): Intent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
}

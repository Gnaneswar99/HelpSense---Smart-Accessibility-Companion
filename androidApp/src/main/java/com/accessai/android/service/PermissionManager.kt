package com.accessai.android.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Centralized permission management for AccessAI.
 * Checks and tracks which permissions are needed per module.
 */
object PermissionManager {

    /**
     * Permissions required for the Vision module (camera).
     */
    val visionPermissions: List<String> = listOf(
        Manifest.permission.CAMERA
    )

    /**
     * Permissions required for the Audio module (microphone).
     */
    val audioPermissions: List<String> = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        // Android 13+ requires POST_NOTIFICATIONS for foreground service alerts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Permissions required for the Navigation module (location + camera).
     */
    val navigationPermissions: List<String> = listOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    /**
     * All permissions the app might need.
     */
    val allPermissions: List<String> = buildList {
        addAll(visionPermissions)
        addAll(audioPermissions)
        addAll(navigationPermissions)
    }.distinct()

    /**
     * Check if a specific permission is granted.
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if all permissions in a list are granted.
     */
    fun areAllGranted(context: Context, permissions: List<String>): Boolean {
        return permissions.all { isPermissionGranted(context, it) }
    }

    /**
     * Get the list of permissions that still need to be requested.
     */
    fun getMissingPermissions(context: Context, permissions: List<String>): List<String> {
        return permissions.filter { !isPermissionGranted(context, it) }
    }
}

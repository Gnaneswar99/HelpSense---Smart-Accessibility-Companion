package com.accessai.core.util

import android.os.Build

actual class PlatformContext

actual fun getPlatformName(): String = "Android"

actual fun getPlatformVersion(): String = "Android ${Build.VERSION.SDK_INT}"

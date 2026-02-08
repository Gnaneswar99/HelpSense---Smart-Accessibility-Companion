package com.accessai.core.util

/**
 * Platform-specific declarations.
 * Each platform (Android/iOS) provides its own implementation.
 */
expect class PlatformContext

expect fun getPlatformName(): String

expect fun getPlatformVersion(): String

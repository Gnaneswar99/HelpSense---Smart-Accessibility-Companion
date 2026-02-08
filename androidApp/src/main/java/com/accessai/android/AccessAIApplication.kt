package com.accessai.android

import android.app.Application
import com.accessai.android.di.androidModule
import com.accessai.android.ml.TFLiteModelManager
import com.accessai.core.di.getSharedModules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * AccessAI Application class.
 * Initializes Koin DI and pre-loads ML models for faster first inference.
 */
class AccessAIApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@AccessAIApplication)
            modules(getSharedModules() + androidModule)
        }

        // Pre-initialize ML engine (checks GPU availability)
        applicationScope.launch {
            val modelManager: TFLiteModelManager by inject()
            modelManager.initialize()
        }
    }
}

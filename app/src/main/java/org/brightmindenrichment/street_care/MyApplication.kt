// MyApplication.kt

package org.brightmindenrichment.street_care

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import org.brightmindenrichment.data.local.EventsDatabase
import org.brightmindenrichment.street_care.notification.NotificationWorker
import org.brightmindenrichment.street_care.notification.NotificationWorkerFactory
import javax.inject.Inject

@HiltAndroidApp
class MyApplication() : Application(), Configuration.Provider {

    @Inject
    lateinit var notificationWorkerFactory: NotificationWorkerFactory

     override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
//         WorkManager.initialize(this)
    }

    override val workManagerConfiguration: Configuration get() = Configuration.Builder()
        .setMinimumLoggingLevel(Log.DEBUG)
        .setWorkerFactory(notificationWorkerFactory)
        .build()

//    fun getWorkManagerConfiguration() =
//        Configuration.Builder()
//            .setMinimumLoggingLevel(Log.DEBUG)
//            .setWorkerFactory(notificationWorkerFactory)
//            .build()

}
/*
class NotificationWorkerFactory @Inject constructor(
    private val eventsDatabase: EventsDatabase
):  WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker = NotificationWorker(
        eventsDatabase,
        appContext,
        workerParameters,
    )
}

 */

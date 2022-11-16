package com.example.android.wearable.wear.common

import android.app.Application
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.core.DataStore
import com.example.android.wearable.wear.wearnotifications.proto.NotificationsProto

abstract class SampleNotificationApplication: Application() {
    private lateinit var intentBuilder: IntentBuilder
    lateinit var notificationCentre: NotificationCentre
    private lateinit var notificationsDataStore: DataStore<NotificationsProto.PostedNotifications>
    lateinit var notificationManager: NotificationManagerCompat

    override fun onCreate() {
        super.onCreate()

        notificationManager = NotificationManagerCompat.from(this)
        notificationsDataStore = postedNotificationsStore
        intentBuilder = createIntentBuilder()
        notificationCentre = NotificationCentre(this, notificationManager, notificationsDataStore, intentBuilder)

        notificationCentre.createChannels()
    }

    abstract fun createIntentBuilder(): IntentBuilder
}

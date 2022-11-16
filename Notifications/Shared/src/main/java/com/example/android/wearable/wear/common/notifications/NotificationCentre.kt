package com.example.android.wearable.wear.common.notifications

import android.app.Notification
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.core.DataStore
import com.example.android.wearable.wear.common.navigation.IntentBuilder
import com.example.android.wearable.wear.wearnotifications.proto.NotificationsProto.PostedNotifications
import com.example.android.wearable.wear.wearnotifications.proto.NotificationsProto.TextNotification
import com.example.android.wearable.wear.wearnotifications.proto.postedNotification

class NotificationCentre(
    val context: Context,
    val notificationManager: NotificationManagerCompat,
    val postedNotificationsDataStore: DataStore<PostedNotifications>,
    intentBuilder: IntentBuilder
) {
    val textNotificationRenderer = TextNotificationRenderer(context, intentBuilder)

    fun createChannels() {
        textNotificationRenderer.createNotificationChannel()
    }

    suspend fun postTextNotification(textNotification: TextNotification): Int {
        var id: Int = -1

        postedNotificationsDataStore.updateData {
            id = it.lastId + 1
            it.toBuilder()
                .addNotification(postedNotification {
                    this.id = id
                    this.text = textNotification
                })
                .setLastId(id)
                .build()
        }

        val notification = textNotificationRenderer.createNotification(id, textNotification)

        postNotification(id, notification)

        return id
    }

    suspend fun clearNotification(id: Int) {
        postedNotificationsDataStore.updateData {
            val index = it.notificationList.indexOfFirst { notification ->
                notification.id == id
            }

            if (index >= 0) {
                it.toBuilder()
                    .removeNotification(index)
                    .build()
            } else {
                it
            }
        }

        notificationManager.cancel(id)
    }

    private fun postNotification(id: Int, notification: Notification) {
        try {
            notificationManager.notify(id, notification)
        } catch (se: SecurityException) {
            // TODO show snackbar
            Log.e("MainViewModel", "Unable to post notification", se)
        }
    }
}

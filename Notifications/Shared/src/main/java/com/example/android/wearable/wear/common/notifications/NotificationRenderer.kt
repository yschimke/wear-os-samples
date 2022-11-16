package com.example.android.wearable.wear.common.notifications

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.android.wearable.wear.common.navigation.IntentBuilder
import com.example.android.wearable.wear.common.mock.MockDatabase

abstract class NotificationRenderer<T>(
    val context: Context,
    val intentBuilder: IntentBuilder
) {
    abstract fun createNotificationChannel()
    abstract fun createNotification(id: Int, notificationData: T): Notification

    fun createNotificationChannel(
        mockNotificationData: MockDatabase.MockNotificationData
    ) {
        // NotificationChannels are required for Notifications on O (API 26) and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // The id of the channel.
            val channelId = mockNotificationData.channelId

            // The user-visible name of the channel.
            val channelName = mockNotificationData.channelName
            // The user-visible description of the channel.
            val channelDescription = mockNotificationData.channelDescription
            val channelImportance = mockNotificationData.channelImportance
            val channelEnableVibrate = mockNotificationData.isChannelEnableVibrate
            val channelLockscreenVisibility = mockNotificationData.channelLockscreenVisibility

            // Initializes NotificationChannel.
            @SuppressLint("WrongConstant")
            val notificationChannel = NotificationChannel(channelId, channelName, channelImportance)
            notificationChannel.description = channelDescription
            notificationChannel.enableVibration(channelEnableVibrate)
            notificationChannel.lockscreenVisibility = channelLockscreenVisibility

            // Adds NotificationChannel to system. Attempting to create an existing notification
            // channel with its original values performs no operation, so it's safe to perform the
            // below sequence.
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}

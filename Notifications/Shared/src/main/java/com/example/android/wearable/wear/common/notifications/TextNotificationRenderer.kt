package com.example.android.wearable.wear.common.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.android.wearable.wear.common.navigation.IntentBuilder
import com.example.android.wearable.wear.wearnotifications.common.R
import com.example.android.wearable.wear.wearnotifications.proto.NotificationsProto.TextNotification

class TextNotificationRenderer(
    context: Context,
    intentBuilder: IntentBuilder,
    notificationManagerCompat: NotificationManagerCompat
) : NotificationRenderer<TextNotification>(context, intentBuilder, notificationManagerCompat) {
    override val channelId = "big_text"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun buildNotificationChannel(): NotificationChannel = NotificationChannel(
        channelId,
        context.getString(R.string.text_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = context.getString(R.string.text_channel_description)
        enableVibration(false)
        lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
    }

    override fun buildNotification(id: Int, notificationData: TextNotification): Notification =
        NotificationCompat.Builder(context, channelId)
            .apply {
                val bigTextStyle =
                    NotificationCompat.BigTextStyle()
                        // Overrides ContentText in the big form of the template.
                        .bigText(notificationData.body)
                        // Overrides ContentTitle in the big form of the template.
                        .setBigContentTitle(notificationData.title)
                        // Summary line after the detail section in the big form of the template
                        // Note: To improve readability, don't overload the user with info. If Summary Text
                        // doesn't add critical information, you should skip it.
                        .setSummaryText(notificationData.summary)

                setStyle(bigTextStyle)
                setContentTitle(notificationData.title)
                setContentText(notificationData.summary)
                setSmallIcon(R.drawable.ic_launcher)
                setLargeIcon(
                    BitmapFactory.decodeResource(
                        context.resources,
                        R.drawable.ic_alarm_white_48dp
                    )
                )
                setDefaults(NotificationCompat.DEFAULT_ALL)
                color = ContextCompat.getColor(context, R.color.colorPrimary)

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    priority = NotificationCompat.PRIORITY_DEFAULT
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setCategory(Notification.CATEGORY_REMINDER)
                }
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                setDeleteIntent(
                    PendingIntent.getService(
                        context,
                        0,
                        intentBuilder.dismissIntent(id),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )

                setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        0,
                        intentBuilder.textScreenIntent(id),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
            .build()
}

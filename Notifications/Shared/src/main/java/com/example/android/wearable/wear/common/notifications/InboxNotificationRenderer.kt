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
import com.example.android.wearable.wear.wearnotifications.proto.NotificationsProto.InboxNotification

class InboxNotificationRenderer(
    context: Context,
    intentBuilder: IntentBuilder,
    notificationManagerCompat: NotificationManagerCompat
) : NotificationRenderer<InboxNotification>(context, intentBuilder, notificationManagerCompat) {
    override val channelId = "inbox"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun buildNotificationChannel(): NotificationChannel {
        return NotificationChannel(
            this.channelId,
            context.getString(R.string.inbox_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.inbox_channel_description)
            enableVibration(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
        }
    }

    override fun buildNotification(id: Int, notificationData: InboxNotification): Notification =
        NotificationCompat.Builder(
            context, channelId
        ).apply {
            val inboxStyle =
                NotificationCompat.InboxStyle()
                    .setBigContentTitle(notificationData.bigContent)
                    .setSummaryText(notificationData.summary)

            // Add each summary line of the new emails, you can add up to 5.
            for (email in notificationData.emailList.takeLast(5)) {
                val sender = email.participantList.firstOrNull()?.name ?: "No-one"
                inboxStyle.addLine("$sender - ${email.summary}")
            }

            // INBOX_STYLE sets title and content.
            setStyle(inboxStyle)
            setContentTitle(notificationData.title)
            setContentText(notificationData.summary)
            setSmallIcon(R.drawable.ic_launcher)
            setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_person_black_48dp
                )
            )
            setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    intentBuilder.inboxScreenIntent(id),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            setDefaults(NotificationCompat.DEFAULT_ALL)
            // Set primary color (important for Wear 2.0 Notifications).
            color = ContextCompat.getColor(context, R.color.colorPrimary)
            // Sets large number at the right-hand side of the notification for Wear 1.+.
            setSubText(notificationData.emailCount.toString())
            setCategory(Notification.CATEGORY_EMAIL)
            // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
            // 'importance' which is set in the NotificationChannel. The integers representing
            // 'priority' are different from 'importance', so make sure you don't mix them.
            priority = NotificationCompat.PRIORITY_DEFAULT
            // Sets lock-screen visibility for 25 and below. For 26 and above, lock screen
            // visibility is set in the NotificationChannel.
            setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            // Notifies system that the main launch intent is an Activity.
            extend(
                NotificationCompat.WearableExtender()
                    .setHintContentIntentLaunchesActivity(true)
            )

            // If the phone is in "Do not disturb mode, the user will still be notified if
            // the sender(s) is starred as a favorite.
            for (contact in notificationData.emailList.flatMap { it.participantList }) {
                addPerson(contact.name)
//                addPerson(Person.Builder()
//                    .setName(contact.name)
//                    .build()
//                )
            }
        }.build()
}

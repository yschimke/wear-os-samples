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
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.example.android.wearable.wear.common.navigation.IntentBuilder
import com.example.android.wearable.wear.wearnotifications.common.R
import com.example.android.wearable.wear.wearnotifications.proto.NotificationsProto.PictureNotification

class PictureNotificationRenderer(
    context: Context,
    intentBuilder: IntentBuilder,
    notificationManagerCompat: NotificationManagerCompat
) : NotificationRenderer<PictureNotification>(context, intentBuilder, notificationManagerCompat) {
    override val channelId = "picture"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun buildNotificationChannel(): NotificationChannel = NotificationChannel(
        channelId,
        context.getString(R.string.picture_channel_name),
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = context.getString(R.string.picture_channel_description)
        enableVibration(true)
        lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
    }

    override fun buildNotification(id: Int, notificationData: PictureNotification): Notification =
        NotificationCompat.Builder(
            context, channelId
        ).apply {
            val bigPictureStyle =
                NotificationCompat.BigPictureStyle() // Provides the bitmap for the BigPicture notification.
                    .bigPicture(
                        BitmapFactory.decodeResource(
                            context.resources,
                            R.drawable.earth
                        )
                    ) // Overrides ContentTitle in the big form of the template.
                    .setBigContentTitle(notificationData.body) // Summary line after the detail section in the big form of the template.
                    .setSummaryText(notificationData.summary)

            setStyle(bigPictureStyle)
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
                    intentBuilder.pictureScreenIntent(id),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            setDefaults(NotificationCompat.DEFAULT_ALL)
            // Set primary color (important for Wear 2.0 Notifications).
            color = ContextCompat.getColor(context, R.color.colorPrimary)
            setSubText(1.toString())

            addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_reply_white_18dp,
                    context.getString(R.string.reply_label),
                    PendingIntent.getService(
                        context,
                        0,
                        intentBuilder.pictureReplyIntent(id),
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
                    .addRemoteInput(
                        RemoteInput.Builder(NotificationReplyService.EXTRA_COMMENT)
                            .setLabel(context.getString(R.string.reply_label))
                            // List of quick response choices for any wearables paired with the phone.
                            .setChoices(notificationData.postRepliesList.toTypedArray())
                            .build()
                    ) // Add WearableExtender to enable inline actions.
                    .extend(
                        NotificationCompat.Action.WearableExtender()
                            .setHintDisplayActionInline(true)
                            .setHintLaunchesActivity(false)
                    )
                    .build()
            )

            setCategory(Notification.CATEGORY_SOCIAL)
            // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
            // 'importance' which is set in the NotificationChannel. The integers representing
            // 'priority' are different from 'importance', so make sure you don't mix them.
            priority = NotificationCompat.PRIORITY_HIGH
            // Sets lock-screen visibility for 25 and below. For 26 and above, lock screen
            // visibility is set in the NotificationChannel.
            setVisibility(NotificationCompat.VISIBILITY_PRIVATE) // Notifies system that the main launch intent is an Activity.
            extend(
                NotificationCompat.WearableExtender()
                    .setHintContentIntentLaunchesActivity(true)
            )

            // Updates active Notification
            if (notificationData.commentList.isNotEmpty()) {
                // Adds a line and comment below content in Notification
                setRemoteInputHistory(notificationData.commentList.toTypedArray())
                    .build()
            }

            // If the phone is in "Do not disturb mode, the user will still be notified if
            // the sender(s) is starred as a favorite.
            for (contact in notificationData.participantList) {
                addPerson(contact.name)
            }
        }.build()
}

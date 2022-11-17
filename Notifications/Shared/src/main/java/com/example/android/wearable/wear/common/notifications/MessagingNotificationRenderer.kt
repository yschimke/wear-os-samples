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
import com.example.android.wearable.wear.common.domain.toPerson
import com.example.android.wearable.wear.common.handlers.NotificationReplyService
import com.example.android.wearable.wear.common.navigation.IntentBuilder
import com.example.android.wearable.wear.wearnotifications.common.R
import com.example.android.wearable.wear.wearnotifications.proto.NotificationsProto.MessagingNotification

class MessagingNotificationRenderer(
    context: Context,
    intentBuilder: IntentBuilder,
    notificationManagerCompat: NotificationManagerCompat
) : NotificationRenderer<MessagingNotification>(context, intentBuilder, notificationManagerCompat) {
    override val channelId = "messaging"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun buildNotificationChannel(): NotificationChannel = NotificationChannel(
        channelId,
        context.getString(R.string.messaging_channel_name),
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = context.getString(R.string.messaging_channel_description)
        enableVibration(true)
        lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
    }

    override fun buildNotification(id: Int, notificationData: MessagingNotification): Notification =
        NotificationCompat.Builder(
            context, channelId
        ).apply {
            val messagingStyle = NotificationCompat.MessagingStyle(notificationData.me.toPerson())
                .setConversationTitle(notificationData.title)

            for (message in notificationData.messageList) {
                messagingStyle.addMessage(
                    message.text,
                    message.timestamp,
                    message.sender.toPerson()
                )
            }
            messagingStyle.isGroupConversation = notificationData.isGroupConversation

            val replyLabel = context.getString(R.string.reply_label)

            // Enable action to appear inline on Wear 2.0 (24+). This means it will appear over the
            // lower portion of the Notification for easy action (only possible for one action).
            val inlineActionForWear2 = NotificationCompat.Action.WearableExtender()
                .setHintDisplayActionInline(true)
                .setHintLaunchesActivity(false)
            val replyAction = NotificationCompat.Action.Builder(
                R.drawable.ic_reply_white_18dp,
                replyLabel,
                PendingIntent.getService(
                    context,
                    0,
                    intentBuilder.messagingReplyIntent(id),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            )
                .addRemoteInput(
                    RemoteInput.Builder(NotificationReplyService.EXTRA_REPLY)
                        .setLabel(replyLabel) // Use machine learning to create responses based on previous messages.
                        .setChoices(notificationData.postRepliesList.toTypedArray())
                        .build()
                )

                // Informs system we aren't bringing up our own custom UI for a reply
                // action.
                .setShowsUserInterface(false) // Allows system to generate replies by context of conversation.
                .setAllowGeneratedReplies(true) // Add WearableExtender to enable inline actions.
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                .extend(inlineActionForWear2)
                .build()

            setStyle(messagingStyle)
            setContentTitle(notificationData.title)
            setContentText(notificationData.body)
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
                    intentBuilder.messagingScreenIntent(id),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            setDefaults(NotificationCompat.DEFAULT_ALL) // Set primary color (important for Wear 2.0 Notifications).
            setColor(
                ContextCompat.getColor(
                    context,
                    R.color.colorPrimary
                )
            )
            // Number of new notifications for API <24 (Wear 1.+) devices.
            setSubText(notificationData.messageCount.toString())
            addAction(replyAction)
            setCategory(Notification.CATEGORY_MESSAGE)
            // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
            // 'importance' which is set in the NotificationChannel. The integers representing
            // 'priority' are different from 'importance', so make sure you don't mix them.
            setPriority(NotificationCompat.PRIORITY_HIGH)

                // Sets lock-screen visibility for 25 and below. For 26 and above, lock screen
                // visibility is set in the NotificationChannel.
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)

            // If the phone is in "Do not disturb" mode, the user may still be notified if the
            // sender(s) are in a group allowed through "Do not disturb" by the user.
            for (person in notificationData.messageList.map { it.sender }) {
                addPerson(person.toPerson())
            }
        }.build()
}

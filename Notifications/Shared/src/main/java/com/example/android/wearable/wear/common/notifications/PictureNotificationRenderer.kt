package com.example.android.wearable.wear.common.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.example.android.wearable.wear.common.mock.MockDatabase
import com.example.android.wearable.wear.common.navigation.IntentBuilder
import com.example.android.wearable.wear.common.util.NotificationUtil
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

    override fun buildNotification(id: Int, notificationData: PictureNotification): Notification {
        // 0. Get your data (everything unique per Notification).
        val bigPictureStyleSocialAppData = MockDatabase.bigPictureStyleData

        // 1. Build the BIG_PICTURE_STYLE.
        val bigPictureStyle =
            NotificationCompat.BigPictureStyle() // Provides the bitmap for the BigPicture notification.
                .bigPicture(
                    BitmapFactory.decodeResource(
                        context.resources,
                        bigPictureStyleSocialAppData.bigImage
                    )
                ) // Overrides ContentTitle in the big form of the template.
                .setBigContentTitle(bigPictureStyleSocialAppData.bigContentTitle) // Summary line after the detail section in the big form of the template.
                .setSummaryText(bigPictureStyleSocialAppData.summaryText)

        // 2. Create/Retrieve Notification Channel for O and beyond devices (26+).
        val notificationChannelId =
            NotificationUtil.createNotificationChannel(context, bigPictureStyleSocialAppData)!!

        // 3. Set up main Intent for notification.

        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            intentBuilder.pictureScreenIntent(id),
            PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Set up a RemoteInput Action, so users can input (keyboard, drawing, voice) directly
        // from the notification without entering the app.

        // Create the RemoteInput.
        val replyLabel = context.getString(R.string.reply_label)
        val remoteInput = RemoteInput.Builder(NotificationReplyService.EXTRA_COMMENT)
            .setLabel(replyLabel) // List of quick response choices for any wearables paired with the phone.
            .setChoices(bigPictureStyleSocialAppData.possiblePostResponses)
            .build()

        // Create PendingIntent for service that handles input.
        val replyActionPendingIntent =
            PendingIntent.getService(
                context,
                0,
                intentBuilder.pictureReplyIntent(id),
                PendingIntent.FLAG_UPDATE_CURRENT
            )

        // Enable action to appear inline on Wear 2.0 (24+). This means it will appear over the
        // lower portion of the Notification for easy action (only possible for one action).
        val inlineActionForWear2 = NotificationCompat.Action.WearableExtender()
            .setHintDisplayActionInline(true)
            .setHintLaunchesActivity(false)
        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_reply_white_18dp,
            replyLabel,
            replyActionPendingIntent
        )
            .addRemoteInput(remoteInput) // Add WearableExtender to enable inline actions.
            .extend(inlineActionForWear2)
            .build()

        // 5. Build and issue the notification

        // Because we want this to be a new notification (not updating a previous notification), we
        // create a new Builder. Later, we use the same global builder to get back the notification
        // we built here for a comment on the post.

        // Notification Channel Id is ignored for Android pre O (26).
        val notificationCompatBuilder = NotificationCompat.Builder(
            context, notificationChannelId
        )
        notificationCompatBuilder // BIG_PICTURE_STYLE sets title and content.
            .setStyle(bigPictureStyle)
            .setContentTitle(bigPictureStyleSocialAppData.contentTitle)
            .setContentText(bigPictureStyleSocialAppData.contentText)
            .setSmallIcon(R.drawable.ic_launcher)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_person_black_48dp
                )
            )
            .setContentIntent(mainPendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Set primary color (important for Wear 2.0 Notifications).
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setSubText(1.toString())
            .addAction(replyAction)
            .setCategory(Notification.CATEGORY_SOCIAL) // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
            // 'importance' which is set in the NotificationChannel. The integers representing
            // 'priority' are different from 'importance', so make sure you don't mix them.
            .setPriority(bigPictureStyleSocialAppData.priority) // Sets lock-screen visibility for 25 and below. For 26 and above, lock screen
            // visibility is set in the NotificationChannel.
            .setVisibility(bigPictureStyleSocialAppData.channelLockscreenVisibility) // Notifies system that the main launch intent is an Activity.
            .extend(
                NotificationCompat.WearableExtender()
                    .setHintContentIntentLaunchesActivity(true)
            )

        // Updates active Notification
        if (notificationData.commentList.isNotEmpty()) {
            notificationCompatBuilder // Adds a line and comment below content in Notification
                .setRemoteInputHistory(notificationData.commentList.toTypedArray())
                .build()
        }

        // If the phone is in "Do not disturb mode, the user will still be notified if
        // the sender(s) is starred as a favorite.
        for (name in bigPictureStyleSocialAppData.participants) {
            notificationCompatBuilder.addPerson(name)
        }

        return notificationCompatBuilder.build()
    }
}

package com.example.android.wearable.wear.common.notifications

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.android.wearable.wear.common.navigation.IntentBuilder
import com.example.android.wearable.wear.common.mock.MockDatabase
import com.example.android.wearable.wear.common.util.NotificationUtil
import com.example.android.wearable.wear.wearnotifications.common.R
import com.example.android.wearable.wear.wearnotifications.proto.NotificationsProto.TextNotification

class TextNotificationRenderer(
    context: Context,
    intentBuilder: IntentBuilder,
    notificationManagerCompat: NotificationManagerCompat
) : NotificationRenderer<TextNotification>(context, intentBuilder, notificationManagerCompat) {
    override fun createNotificationChannel() {
        // NotificationChannels are required for Notifications on O (API 26) and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // The id of the channel.

            // The user-visible name of the channel.
            val channelName = context.getString(R.string.text_channel_name)
            // The user-visible description of the channel.
            val channelDescription = context.getString(R.string.text_channel_description)

            // Initializes NotificationChannel.
            @SuppressLint("WrongConstant")
            val notificationChannel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            notificationChannel.description = channelDescription
            notificationChannel.enableVibration(false)
            notificationChannel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC

            // Adds NotificationChannel to system. Attempting to create an existing notification
            // channel with its original values performs no operation, so it's safe to perform the
            // below sequence.
            notificationManagerCompat.createNotificationChannel(notificationChannel)
        }
    }

    override fun createNotification(id: Int, notificationData: TextNotification): Notification {
        // Main steps for building a BIG_TEXT_STYLE notification:
        //      0. Get your data
        //      1. Create/Retrieve Notification Channel for O and beyond devices (26+)
        //      2. Build the BIG_TEXT_STYLE
        //      3. Set up main Intent for notification
        //      4. Create additional Actions for the Notification
        //      5. Build and issue the notification

        // 0. Get your data (everything unique per Notification).
        val bigTextStyleReminderAppData = MockDatabase.bigTextStyleData

        // 2. Build the BIG_TEXT_STYLE
        val bigTextStyle =
            NotificationCompat.BigTextStyle() // Overrides ContentText in the big form of the template.
                .bigText(notificationData.body) // Overrides ContentTitle in the big form of the template.
                .setBigContentTitle(notificationData.title) // Summary line after the detail section in the big form of the template
                // Note: To improve readability, don't overload the user with info. If Summary Text
                // doesn't add critical information, you should skip it.
                .setSummaryText(notificationData.summary)


        // 3. Set up main Intent for notification.
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            intentBuilder.textScreenIntent(id),
            PendingIntent.FLAG_IMMUTABLE
        )


        // 4. Create additional Actions (Intents) for the Notification.

        // Dismiss Action.
        val dismissPendingIntent = PendingIntent.getService(
            context,
            0,
            intentBuilder.dismissIntent(id),
            PendingIntent.FLAG_IMMUTABLE
        )


        // 5. Build and issue the notification.

        // Because we want context to be a new notification (not updating a previous notification), we
        // create a new Builder. Later, we use the same global builder to get back the notification
        // we built here for the snooze action, that is, canceling the notification and relaunching
        // it several seconds later.

        // Notification Channel Id is ignored for Android pre O (26).
        val notificationCompatBuilder = NotificationCompat.Builder(context, channelId)
        notificationCompatBuilder // BIG_TEXT_STYLE sets title and content.
            .setStyle(bigTextStyle)
            .setContentTitle(bigTextStyleReminderAppData.contentTitle)
            .setContentText(bigTextStyleReminderAppData.contentText)
            .setSmallIcon(R.drawable.ic_launcher)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_alarm_white_48dp
                )
            )
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Set primary color (important for Wear 2.0 Notifications).
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .apply {
                // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
                // 'importance' which is set in the NotificationChannel. The integers representing
                // 'priority' are different from 'importance', so make sure you don't mix them.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    // Sets lock-screen visibility for 25 and below. For 26 and above, lock screen
                    priority = bigTextStyleReminderAppData.priority
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setCategory(Notification.CATEGORY_REMINDER)
                }
            }
            // visibility is set in the NotificationChannel.
            .setVisibility(bigTextStyleReminderAppData.channelLockscreenVisibility) // Adds additional actions specified above.
            .setDeleteIntent(dismissPendingIntent)

        /* REPLICATE_NOTIFICATION_STYLE_CODE:
         * You can replicate Notification Style functionality on Wear 2.0 (24+) by not setting the
         * main content intent, that is, skipping the call setContentIntent(). However, you need to
         * still allow the user to open the native Wear app from the Notification itself, so you
         * add an action to launch the app.
         */

        // Enables launching app in Wear 2.0 while keeping the old Notification Style behavior.
        val mainAction = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher,
            "Open",
            mainPendingIntent
        )
            .build()
        notificationCompatBuilder.addAction(mainAction)

        return notificationCompatBuilder.build()
    }

    companion object {
        val channelId = "big_text"
    }
}

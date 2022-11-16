package com.example.android.wearable.wear.wearnotifications

import android.app.Activity
import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.wearable.wear.common.mock.MockDatabase
import com.example.android.wearable.wear.common.util.NotificationUtil
import com.example.android.wearable.wear.wearnotifications.handlers.BigPictureSocialIntentService
import com.example.android.wearable.wear.wearnotifications.handlers.MessagingIntentService
import com.example.android.wearable.wear.wearnotifications.main.StandaloneMainActivity
import com.example.android.wearable.wear.wearnotifications.proto.textNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val notificationCentre = (application as WearNotificationApplication).notificationCentre
    private val mNotificationManagerCompat = (application as WearNotificationApplication).notificationManager

    val uiState: StateFlow<MainUiState> = MutableStateFlow(
        MainUiState(
            mNotificationManagerCompat.areNotificationsEnabled()
        )
    )

    /*
     * Generates a BIG_TEXT_STYLE Notification that supports both Wear 1.+ and Wear 2.0.
     *
     * IMPORTANT NOTE:
     * This method includes extra code to replicate Notification Styles behavior from Wear 1.+ and
     * phones on Wear 2.0, i.e., the notification expands on click. To see the specific code in the
     * method, search for "REPLICATE_NOTIFICATION_STYLE_CODE".
     *
     * Notification Styles behave slightly different on Wear 2.0 when they are launched by a
     * native/local Wear app, i.e., they will NOT expand when the user taps them but will instead
     * take the user directly into the local app for the richest experience. In contrast, a bridged
     * Notification launched from the phone will expand with the style details (whether there is a
     * local app or not).
     *
     * If you want to see the new behavior, please review the generateBigPictureStyleNotification()
     * and generateMessagingStyleNotification() methods.
     */
    fun generateBigTextStyleNotification(activity: Activity) {
        viewModelScope.launch {
            notificationCentre.postTextNotification(textNotification {
                this.text =
                    "... feed the dogs before you leave for work, and check the garage to make sure the door is closed."
            })

            // Close app to demonstrate notification in steam.
            activity.finish()
        }
    }

    private fun postNotification(id: Int, notification: Notification) {
        try {
            mNotificationManagerCompat.notify(id, notification)
        } catch (se: SecurityException) {
            // TODO show snackbar
            Log.e("MainViewModel", "Unable to post notification", se)
        }
    }

    /*
     * Generates a BIG_PICTURE_STYLE Notification that supports both Wear 1.+ and Wear 2.0.
     *
     * This example Notification is a social post. It allows updating the notification with
     * comments/responses via RemoteInput and the BigPictureSocialIntentService on 24+ (N+) and
     * Wear devices.
     *
     * IMPORTANT NOTE:
     * Notification Styles behave slightly different on Wear 2.0 when they are launched by a
     * native/local Wear app, i.e., they will NOT expand when the user taps them but will instead
     * take the user directly into the local app for the richest experience. In contrast, a bridged
     * Notification launched from the phone will expand with the style details (whether there is a
     * local app or not).
     *
     * If you want to enable an action on your Notification without launching the app, you can do so
     * with the setHintDisplayActionInline() feature (shown below), but this only allows one action.
     *
     * If you wish to replicate the original experience of a bridged notification, please review the
     * generateBigTextStyleNotification() method above to see how.
     */
    fun generateBigPictureStyleNotification(activity: Activity) {
        Log.d(StandaloneMainActivity.TAG, "generateBigPictureStyleNotification()")

        // Main steps for building a BIG_PICTURE_STYLE notification:
        //      0. Get your data
        //      1. Create/Retrieve Notification Channel for O and beyond devices (26+)
        //      2. Build the BIG_PICTURE_STYLE
        //      3. Set up main Intent for notification
        //      4. Set up RemoteInput, so users can input (keyboard and voice) from notification
        //      5. Build and issue the notification

        // 0. Get your data (everything unique per Notification).
        val bigPictureStyleSocialAppData = MockDatabase.bigPictureStyleData

        // 1. Build the BIG_PICTURE_STYLE.
        val bigPictureStyle =
            NotificationCompat.BigPictureStyle() // Provides the bitmap for the BigPicture notification.
                .bigPicture(
                    BitmapFactory.decodeResource(
                        activity.resources,
                        bigPictureStyleSocialAppData.bigImage
                    )
                ) // Overrides ContentTitle in the big form of the template.
                .setBigContentTitle(bigPictureStyleSocialAppData.bigContentTitle) // Summary line after the detail section in the big form of the template.
                .setSummaryText(bigPictureStyleSocialAppData.summaryText)

        // 2. Create/Retrieve Notification Channel for O and beyond devices (26+).
        val notificationChannelId =
            NotificationUtil.createNotificationChannel(activity, bigPictureStyleSocialAppData)!!

        // 3. Set up main Intent for notification.
        val mainIntent = Intent(
            Intent.ACTION_VIEW,
            "$DeepLinkPrefix/picture?id=$StandaloneMainActivity.NOTIFICATION_ID".toUri()
        )
        val mainPendingIntent = PendingIntent.getActivity(
            activity,
            0,
            mainIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Set up a RemoteInput Action, so users can input (keyboard, drawing, voice) directly
        // from the notification without entering the app.

        // Create the RemoteInput.
        val replyLabel = activity.getString(R.string.reply_label)
        val remoteInput = RemoteInput.Builder(BigPictureSocialIntentService.EXTRA_COMMENT)
            .setLabel(replyLabel) // List of quick response choices for any wearables paired with the phone.
            .setChoices(bigPictureStyleSocialAppData.possiblePostResponses)
            .build()

        // Create PendingIntent for service that handles input.
        val replyIntent = Intent(activity, BigPictureSocialIntentService::class.java)
        replyIntent.action = BigPictureSocialIntentService.ACTION_COMMENT
        val replyActionPendingIntent =
            PendingIntent.getService(activity, 0, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT)

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
            getApplication(), notificationChannelId
        )
        notificationCompatBuilder // BIG_PICTURE_STYLE sets title and content.
            .setStyle(bigPictureStyle)
            .setContentTitle(bigPictureStyleSocialAppData.contentTitle)
            .setContentText(bigPictureStyleSocialAppData.contentText)
            .setSmallIcon(R.drawable.ic_launcher)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    activity.resources,
                    R.drawable.ic_person_black_48dp
                )
            )
            .setContentIntent(mainPendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Set primary color (important for Wear 2.0 Notifications).
            .setColor(ContextCompat.getColor(getApplication(), R.color.colorPrimary))
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

        // If the phone is in "Do not disturb mode, the user will still be notified if
        // the sender(s) is starred as a favorite.
        for (name in bigPictureStyleSocialAppData.participants) {
            notificationCompatBuilder.addPerson(name)
        }
        val notification = notificationCompatBuilder.build()

        postNotification(StandaloneMainActivity.NOTIFICATION_ID, notification)

        // Close app to demonstrate notification in steam.
        activity.finish()
    }

    /*
     * Generates a INBOX_STYLE Notification that supports both Wear 1.+ and Wear 2.0.
     */
    fun generateInboxStyleNotification(activity: Activity) {
        Log.d(StandaloneMainActivity.TAG, "generateInboxStyleNotification()")


        // Main steps for building a INBOX_STYLE notification:
        //      0. Get your data
        //      1. Create/Retrieve Notification Channel for O and beyond devices (26+)
        //      2. Build the INBOX_STYLE
        //      3. Set up main Intent for notification
        //      4. Build and issue the notification

        // 0. Get your data (everything unique per Notification).
        val inboxStyleEmailAppData = MockDatabase.inboxStyleData

        // 1. Create/Retrieve Notification Channel for O and beyond devices (26+).
        val notificationChannelId =
            NotificationUtil.createNotificationChannel(activity, inboxStyleEmailAppData)!!

        // 2. Build the INBOX_STYLE
        val inboxStyle =
            NotificationCompat.InboxStyle() // This title is slightly different than regular title, since I know INBOX_STYLE is
                // available.
                .setBigContentTitle(inboxStyleEmailAppData.bigContentTitle)
                .setSummaryText(inboxStyleEmailAppData.summaryText)

        // Add each summary line of the new emails, you can add up to 5.
        for (summary in inboxStyleEmailAppData.individualEmailSummary) {
            inboxStyle.addLine(summary)
        }

        // 3. Set up main Intent for notification.
        val mainIntent = Intent(
            Intent.ACTION_VIEW,
            "$DeepLinkPrefix/inbox?id=$StandaloneMainActivity.NOTIFICATION_ID".toUri()
        )
        val mainPendingIntent = PendingIntent.getActivity(
            activity,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 4. Build and issue the notification.

        // Because we want this to be a new notification (not updating a previous notification), we
        // create a new Builder. However, we don't need to update this notification later, so we
        // will not need to set a global builder for access to the notification later.

        // Notification Channel Id is ignored for Android pre O (26).
        val notificationCompatBuilder = NotificationCompat.Builder(
            getApplication(), notificationChannelId
        )
        notificationCompatBuilder // INBOX_STYLE sets title and content.
            .setStyle(inboxStyle)
            .setContentTitle(inboxStyleEmailAppData.contentTitle)
            .setContentText(inboxStyleEmailAppData.contentText)
            .setSmallIcon(R.drawable.ic_launcher)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    activity.resources,
                    R.drawable.ic_person_black_48dp
                )
            )
            .setContentIntent(mainPendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Set primary color (important for Wear 2.0 Notifications).
            .setColor(
                ContextCompat.getColor(
                    getApplication(),
                    R.color.colorPrimary
                )
            ) // Sets large number at the right-hand side of the notification for Wear 1.+.
            .setSubText(inboxStyleEmailAppData.numberOfNewEmails.toString())
            .setCategory(Notification.CATEGORY_EMAIL) // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
            // 'importance' which is set in the NotificationChannel. The integers representing
            // 'priority' are different from 'importance', so make sure you don't mix them.
            .setPriority(inboxStyleEmailAppData.priority) // Sets lock-screen visibility for 25 and below. For 26 and above, lock screen
            // visibility is set in the NotificationChannel.
            .setVisibility(inboxStyleEmailAppData.channelLockscreenVisibility) // Notifies system that the main launch intent is an Activity.
            .extend(
                NotificationCompat.WearableExtender()
                    .setHintContentIntentLaunchesActivity(true)
            )

        // If the phone is in "Do not disturb mode, the user will still be notified if
        // the sender(s) is starred as a favorite.
        for (name in inboxStyleEmailAppData.participants) {
            notificationCompatBuilder.addPerson(name)
        }
        val notification = notificationCompatBuilder.build()

        postNotification(StandaloneMainActivity.NOTIFICATION_ID, notification)

        // Close app to demonstrate notification in steam.
        activity.finish()
    }

    /*
     * Generates a MESSAGING_STYLE Notification that supports both Wear 1.+ and Wear 2.0. For
     * devices on API level 24 (Wear 2.0) and after, displays MESSAGING_STYLE. Otherwise, displays
     * a basic BIG_TEXT_STYLE.
     *
     * IMPORTANT NOTE:
     * Notification Styles behave slightly different on Wear 2.0 when they are launched by a
     * native/local Wear app, i.e., they will NOT expand when the user taps them but will instead
     * take the user directly into the local app for the richest experience. In contrast, a bridged
     * Notification launched from the phone will expand with the style details (whether there is a
     * local app or not).
     *
     * If you want to enable an action on your Notification without launching the app, you can do so
     * with the setHintDisplayActionInline() feature (shown below), but this only allows one action.
     *
     * If you wish to replicate the original experience of a bridged notification, please review the
     * generateBigTextStyleNotification() method above to see how.
     */
    fun generateMessagingStyleNotification(activity: Activity) {
        Log.d(StandaloneMainActivity.TAG, "generateMessagingStyleNotification()")

        // Main steps for building a MESSAGING_STYLE notification:
        //      0. Get your data
        //      1. Create/Retrieve Notification Channel for O and beyond devices (26+)
        //      2. Build the MESSAGING_STYLE
        //      3. Set up main Intent for notification
        //      4. Set up RemoteInput (users can input directly from notification)
        //      5. Build and issue the notification

        // 0. Get your data (everything unique per Notification).
        val messagingStyleCommsAppData = MockDatabase.getMessagingStyleData(getApplication())

        // 1. Create/Retrieve Notification Channel for O and beyond devices (26+).
        val notificationChannelId =
            NotificationUtil.createNotificationChannel(activity, messagingStyleCommsAppData)!!

        // 2. Build the Notification.Style (MESSAGING_STYLE).
        val contentTitle = messagingStyleCommsAppData.contentTitle
        val messagingStyle = NotificationCompat.MessagingStyle(messagingStyleCommsAppData.me) /*
                         * <p>This API's behavior was changed in SDK version
                         * {@link Build.VERSION_CODES#P}. If your application's target version is
                         * less than {@link Build.VERSION_CODES#P}, setting a conversation title to
                         * a non-null value will make {@link #isGroupConversation()} return
                         * {@code true} and passing {@code null} will make it return {@code false}.
                         * This behavior can be overridden by calling
                         * {@link #setGroupConversation(boolean)} regardless of SDK version.
                         * In {@code P} and above, this method does not affect group conversation
                         * settings.
                         *
                         * In our case, we use the same title.
                         */
            .setConversationTitle(contentTitle)

        // Adds all Messages.
        // Note: Messages include the text, timestamp, and sender.
        for (message in messagingStyleCommsAppData.messages) {
            messagingStyle.addMessage(message)
        }
        messagingStyle.isGroupConversation = messagingStyleCommsAppData.isGroupConversation

        // 3. Set up main Intent for notification.
        val notifyIntent = Intent(
            Intent.ACTION_VIEW,
            "$DeepLinkPrefix/messaging?id=$StandaloneMainActivity.NOTIFICATION_ID".toUri()
        )
        val mainPendingIntent = PendingIntent.getActivity(
            activity,
            0,
            notifyIntent,
            PendingIntent.FLAG_IMMUTABLE
        )


        // 4. Set up a RemoteInput Action, so users can input (keyboard, drawing, voice) directly
        // from the notification without entering the app.

        // Create the RemoteInput specifying this key.
        val replyLabel = activity.getString(R.string.reply_label)
        val remoteInput = RemoteInput.Builder(MessagingIntentService.EXTRA_REPLY)
            .setLabel(replyLabel) // Use machine learning to create responses based on previous messages.
            .setChoices(messagingStyleCommsAppData.replyChoicesBasedOnLastMessage)
            .build()

        // Create PendingIntent for service that handles input.
        val replyIntent = Intent(activity, MessagingIntentService::class.java)
        replyIntent.action = MessagingIntentService.ACTION_REPLY
        val replyActionPendingIntent =
            PendingIntent.getService(activity, 0, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT)

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
            .addRemoteInput(remoteInput) // Informs system we aren't bringing up our own custom UI for a reply
            // action.
            .setShowsUserInterface(false) // Allows system to generate replies by context of conversation.
            .setAllowGeneratedReplies(true) // Add WearableExtender to enable inline actions.
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .extend(inlineActionForWear2)
            .build()


        // 5. Build and issue the notification.

        // Because we want this to be a new notification (not updating current notification), we
        // create a new Builder. Later, we update this same notification, so we need to save this
        // Builder globally (as outlined earlier).

        // Notification Channel Id is ignored for Android pre O (26).
        val notificationCompatBuilder = NotificationCompat.Builder(
            getApplication(), notificationChannelId
        )
        notificationCompatBuilder // MESSAGING_STYLE sets title and content for Wear 1.+ and Wear 2.0 devices.
            .setStyle(messagingStyle)
            .setContentTitle(contentTitle)
            .setContentText(messagingStyleCommsAppData.contentText)
            .setSmallIcon(R.drawable.ic_launcher)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    activity.resources,
                    R.drawable.ic_person_black_48dp
                )
            )
            .setContentIntent(mainPendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Set primary color (important for Wear 2.0 Notifications).
            .setColor(
                ContextCompat.getColor(
                    getApplication(),
                    R.color.colorPrimary
                )
            ) // Number of new notifications for API <24 (Wear 1.+) devices.
            .setSubText(messagingStyleCommsAppData.numberOfNewMessages.toString())
            .addAction(replyAction)
            .setCategory(Notification.CATEGORY_MESSAGE) // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
            // 'importance' which is set in the NotificationChannel. The integers representing
            // 'priority' are different from 'importance', so make sure you don't mix them.
            .setPriority(messagingStyleCommsAppData.priority) // Sets lock-screen visibility for 25 and below. For 26 and above, lock screen
            // visibility is set in the NotificationChannel.
            .setVisibility(messagingStyleCommsAppData.channelLockscreenVisibility)

        // If the phone is in "Do not disturb" mode, the user may still be notified if the
        // sender(s) are in a group allowed through "Do not disturb" by the user.
        for (person in messagingStyleCommsAppData.participants) {
            notificationCompatBuilder.addPerson(person.uri)
        }
        val notification = notificationCompatBuilder.build()

        postNotification(StandaloneMainActivity.NOTIFICATION_ID, notification)

        // Close app to demonstrate notification in steam.
        activity.finish()
    }
}

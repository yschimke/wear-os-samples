/*
Copyright 2016 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.example.android.wearable.wear.wearnotifications.handlers

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.android.wearable.wear.common.mock.MockDatabase
import com.example.android.wearable.wear.wearnotifications.DeepLinkPrefix
import com.example.android.wearable.wear.wearnotifications.R
import com.example.android.wearable.wear.wearnotifications.main.StandaloneMainActivity

/**
 * Asynchronously handles updating social app posts (and active Notification) with comments from
 * user. Notification for social app use BigPictureStyle.
 */
class BigPictureSocialIntentService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onHandleIntent(): $intent")
        if (intent != null) {
            if (ACTION_COMMENT == intent.action) {
                handleActionComment(getMessage(intent))
            }
        }
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Handles action for adding a comment from the notification.
     */
    private fun handleActionComment(comment: CharSequence?) {
        Log.d(TAG, "handleActionComment(): $comment")
        if (comment != null) {

            // TODO: Asynchronously save your message to Database and servers.

            /*
             * You have two options for updating your notification (this class uses approach #2):
             *
             *  1. Use a new NotificationCompatBuilder to create the Notification. This approach
             *  requires you to get *ALL* the information that existed in the previous
             *  Notification (and updates) and pass it to the builder. This is the approach used in
             *  the MainActivity.
             *
             *  2. Use the original NotificationCompatBuilder to create the Notification. This
             *  approach requires you to store a reference to the original builder. The benefit is
             *  you only need the new/updated information. In our case, the comment from the user
             *  regarding the post (which we already have here).
             *
             *  IMPORTANT NOTE: You shouldn't save/modify the resulting Notification object using
             *  its member variables and/or legacy APIs. If you want to retain anything from update
             *  to update, retain the Builder as option 2 outlines.
             */

            val notificationCompatBuilder = recreateBuilderWithBigPictureStyle()

            // Updates active Notification
            val updatedNotification =
                notificationCompatBuilder // Adds a line and comment below content in Notification
                    .setRemoteInputHistory(arrayOf(comment))
                    .build()

            // Pushes out the updated Notification
            val notificationManagerCompat = NotificationManagerCompat.from(
                applicationContext
            )
            try {
                notificationManagerCompat.notify(
                    StandaloneMainActivity.NOTIFICATION_ID,
                    updatedNotification
                )
            } catch (se: SecurityException) {
                Log.e(
                    TAG,
                    "Unable to post notification from notification service",
                    se
                )
            }
        }
    }

    /*
     * Extracts CharSequence created from the RemoteInput associated with the Notification.
     */
    private fun getMessage(intent: Intent): CharSequence? {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        return remoteInput?.getCharSequence(EXTRA_COMMENT)
    }

    /*
     * This recreates the notification from the persistent state in case the app process was killed.
     * It is basically the same code for creating the Notification from StandaloneMainActivity.
     */
    private fun recreateBuilderWithBigPictureStyle(): NotificationCompat.Builder {

        // Main steps for building a BIG_PICTURE_STYLE notification (for more detailed comments on
        // building this notification, check StandaloneMainActivity.java):
        //      0. Get your data
        //      1. Retrieve Notification Channel for O and beyond devices (26+)
        //      2. Build the BIG_PICTURE_STYLE
        //      3. Set up main Intent for notification
        //      4. Set up RemoteInput, so users can input (keyboard and voice) from notification
        //      5. Build and issue the notification

        // 0. Get your data (everything unique per Notification)
        val bigPictureStyleSocialAppData = MockDatabase.getBigPictureStyleData()

        // 1. Retrieve Notification Channel for O and beyond devices (26+). We don't need to create
        //    the NotificationChannel, since it was created the first time this Notification was
        //    created.
        val notificationChannelId = bigPictureStyleSocialAppData.channelId


        // 2. Build the BIG_PICTURE_STYLE.
        val bigPictureStyle = NotificationCompat.BigPictureStyle()
            .bigPicture(
                BitmapFactory.decodeResource(
                    resources,
                    bigPictureStyleSocialAppData.bigImage
                )
            )
            .setBigContentTitle(bigPictureStyleSocialAppData.bigContentTitle)
            .setSummaryText(bigPictureStyleSocialAppData.summaryText)

        // 3. Set up main Intent for notification.
        val mainIntent = Intent(Intent.ACTION_VIEW, "$DeepLinkPrefix/picture?id=$StandaloneMainActivity.NOTIFICATION_ID".toUri())
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Set up a RemoteInput Action, so users can input (keyboard, drawing, voice) directly
        // from the notification without entering the app.
        val replyLabel = getString(R.string.reply_label)
        val remoteInput = RemoteInput.Builder(EXTRA_COMMENT)
            .setLabel(replyLabel)
            .setChoices(bigPictureStyleSocialAppData.possiblePostResponses)
            .build()
        val replyIntent = Intent(this, BigPictureSocialIntentService::class.java)
        replyIntent.action = ACTION_COMMENT
        val replyActionPendingIntent = PendingIntent.getService(
            this,
            0,
            replyIntent,
            PendingIntent.FLAG_MUTABLE
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

        // 5. Build and issue the notification.

        // Notification Channel Id is ignored for Android pre O (26).
        val notificationCompatBuilder = NotificationCompat.Builder(
            applicationContext, notificationChannelId
        )
        notificationCompatBuilder
            .setStyle(bigPictureStyle)
            .setContentTitle(bigPictureStyleSocialAppData.contentTitle)
            .setContentText(bigPictureStyleSocialAppData.contentText)
            .setSmallIcon(R.drawable.ic_launcher)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    resources,
                    R.drawable.ic_person_black_48dp
                )
            )
            .setContentIntent(mainPendingIntent)
            .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
            .setSubText(Integer.toString(1))
            .addAction(replyAction)
            .setCategory(Notification.CATEGORY_SOCIAL)
            .setPriority(bigPictureStyleSocialAppData.priority)
            .setVisibility(bigPictureStyleSocialAppData.channelLockscreenVisibility)
            .extend(
                NotificationCompat.WearableExtender()
                    .setHintContentIntentLaunchesActivity(true)
            )
        for (name in bigPictureStyleSocialAppData.participants) {
            notificationCompatBuilder.addPerson(name)
        }
        return notificationCompatBuilder
    }

    companion object {
        private const val TAG = "BigPictureService"
        const val ACTION_COMMENT =
            "com.example.android.wearable.wear.wearnotifications.handlers.action.COMMENT"
        const val EXTRA_COMMENT =
            "com.example.android.wearable.wear.wearnotifications.handlers.extra.COMMENT"
    }
}

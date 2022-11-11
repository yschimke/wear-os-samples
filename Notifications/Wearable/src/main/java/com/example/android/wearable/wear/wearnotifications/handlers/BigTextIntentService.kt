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

import android.app.IntentService
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.android.wearable.wear.common.mock.MockDatabase
import com.example.android.wearable.wear.wearnotifications.GlobalNotificationBuilder
import com.example.android.wearable.wear.wearnotifications.R
import com.example.android.wearable.wear.wearnotifications.StandaloneMainActivity
import java.util.concurrent.TimeUnit

/**
 * Asynchronously handles snooze and dismiss actions for reminder app (and active Notification).
 * Notification for for reminder app uses BigTextStyle.
 */
class BigTextIntentService : IntentService("BigTextIntentService") {
    override fun onHandleIntent(intent: Intent?) {
        Log.d(TAG, "onHandleIntent(): $intent")
        if (intent != null) {
            val action = intent.action
            if (ACTION_DISMISS == action) {
                handleActionDismiss()
            } else if (ACTION_SNOOZE == action) {
                handleActionSnooze()
            }
        }
    }

    /**
     * Handles action Dismiss in the provided background thread.
     */
    private fun handleActionDismiss() {
        Log.d(TAG, "handleActionDismiss()")
        val notificationManagerCompat = NotificationManagerCompat.from(
            applicationContext
        )
        notificationManagerCompat.cancel(StandaloneMainActivity.Companion.NOTIFICATION_ID)
    }

    /**
     * Handles action Snooze in the provided background thread.
     */
    private fun handleActionSnooze() {
        Log.d(TAG, "handleActionSnooze()")

        // You could use NotificationManager.getActiveNotifications() if you are targeting SDK 23
        // and above, but we are targeting devices with lower SDK API numbers, so we saved the
        // builder globally and get the notification back to recreate it later.
        var notificationCompatBuilder =
            GlobalNotificationBuilder.notificationCompatBuilderInstance

        // Recreate builder from persistent state if app process is killed
        if (notificationCompatBuilder == null) {
            // Note: New builder set globally in the method
            notificationCompatBuilder = recreateBuilderWithBigTextStyle()
        }
        val notification: Notification
        notification = notificationCompatBuilder.build()
        if (notification != null) {
            val notificationManagerCompat = NotificationManagerCompat.from(
                applicationContext
            )
            notificationManagerCompat.cancel(StandaloneMainActivity.Companion.NOTIFICATION_ID)
            try {
                Thread.sleep(SNOOZE_TIME)
            } catch (ex: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            notificationManagerCompat.notify(
                StandaloneMainActivity.Companion.NOTIFICATION_ID,
                notification
            )
        }
    }

    /*
     * This recreates the notification from the persistent state in case the app process was killed.
     * It is basically the same code for creating the Notification from StandaloneMainActivity.
     */
    private fun recreateBuilderWithBigTextStyle(): NotificationCompat.Builder {

        // Main steps for building a BIG_TEXT_STYLE notification (for more detailed comments on
        // building this notification, check StandaloneMainActivity.java):
        //      0. Get your data
        //      1. Retrieve Notification Channel for O and beyond devices (26+)
        //      2. Build the BIG_TEXT_STYLE
        //      3. Set up main Intent for notification
        //      4. Create additional Actions for the Notification
        //      5. Build and issue the notification

        // 0. Get your data (everything unique per Notification).
        val bigTextStyleReminderAppData = MockDatabase.getBigTextStyleData()

        // 1. Retrieve Notification Channel for O and beyond devices (26+). We don't need to create
        //    the NotificationChannel, since it was created the first time this Notification was
        //    created.
        val notificationChannelId = bigTextStyleReminderAppData.channelId

        // 2. Build the BIG_TEXT_STYLE.
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(bigTextStyleReminderAppData.bigText)
            .setBigContentTitle(bigTextStyleReminderAppData.bigContentTitle)
            .setSummaryText(bigTextStyleReminderAppData.summaryText)


        // 3. Set up main Intent for notification.
        val mainIntent = Intent(this, BigTextMainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )


        // 4. Create additional Actions (Intents) for the Notification.

        // Snooze Action.
        val snoozeIntent = Intent(this, BigTextIntentService::class.java)
        snoozeIntent.action =
            ACTION_SNOOZE
        val snoozePendingIntent = PendingIntent.getService(this, 0, snoozeIntent, 0)
        val snoozeAction = NotificationCompat.Action.Builder(
            R.drawable.ic_alarm_white_48dp,
            "Snooze",
            snoozePendingIntent
        )
            .build()

        // Dismiss Action
        val dismissIntent = Intent(this, BigTextIntentService::class.java)
        dismissIntent.action =
            ACTION_DISMISS
        val dismissPendingIntent = PendingIntent.getService(this, 0, dismissIntent, 0)
        val dismissAction = NotificationCompat.Action.Builder(
            R.drawable.ic_cancel_white_48dp,
            "Dismiss",
            dismissPendingIntent
        )
            .build()


        // 5. Build and issue the notification.

        // Notification Channel Id is ignored for Android pre O (26).
        val notificationCompatBuilder = NotificationCompat.Builder(
            applicationContext, notificationChannelId
        )
        GlobalNotificationBuilder.notificationCompatBuilderInstance = notificationCompatBuilder
        notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(bigTextStyleReminderAppData.contentTitle)
            .setContentText(bigTextStyleReminderAppData.contentText)
            .setSmallIcon(R.drawable.ic_launcher)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    resources,
                    R.drawable.ic_alarm_white_48dp
                )
            )
            .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setPriority(bigTextStyleReminderAppData.priority)
            .setVisibility(bigTextStyleReminderAppData.channelLockscreenVisibility)
            .addAction(snoozeAction)
            .addAction(dismissAction)

        /* REPLICATE_NOTIFICATION_STYLE_CODE:
         * You can replicate Notification Style functionality on Wear 2.0 (24+) by not setting the
         * main content intent, that is, skipping the call setContentIntent(). However, you need to
         * still allow the user to open the native Wear app from the Notification itself, so you
         * add an action to launch the app.
         */if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            // Enables launching app in Wear 2.0 while keeping the old Notification Style behavior.
            val mainAction = NotificationCompat.Action.Builder(
                R.drawable.ic_launcher,
                "Open",
                mainPendingIntent
            )
                .build()
            notificationCompatBuilder.addAction(mainAction)
        } else {
            // Wear 1.+ still functions the same, so we set the main content intent.
            notificationCompatBuilder.setContentIntent(mainPendingIntent)
        }
        return notificationCompatBuilder
    }

    companion object {
        private const val TAG = "BigTextService"
        const val ACTION_DISMISS =
            "com.example.android.wearable.wear.wearnotifications.handlers.action.DISMISS"
        const val ACTION_SNOOZE =
            "com.example.android.wearable.wear.wearnotifications.handlers.action.SNOOZE"
        private val SNOOZE_TIME = TimeUnit.SECONDS.toMillis(5)
    }
}

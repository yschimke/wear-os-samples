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
package com.example.android.wearable.wear.common

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.runBlocking

/**
 * Asynchronously handles snooze and dismiss actions for reminder app (and active Notification).
 * Notification for for reminder app uses BigTextStyle.
 */
class CoreNotificationActionsService : Service() {
    private lateinit var notificationCentre: NotificationCentre

    override fun onCreate() {
        super.onCreate()

        notificationCentre = (application as SampleNotificationApplication).notificationCentre
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> handleActionDismiss(intent)
        }
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Handles action Dismiss in the provided background thread.
     */
    private fun handleActionDismiss(intent: Intent) {
        runBlocking {
            notificationCentre.clearNotification(intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1))
        }
    }

    companion object {
        const val ACTION_DISMISS =
            "com.example.android.wearable.wear.wearnotifications.handlers.action.DISMISS"

        @SuppressLint("InlinedApi")
        const val EXTRA_NOTIFICATION_ID = Notification.EXTRA_NOTIFICATION_ID
    }
}

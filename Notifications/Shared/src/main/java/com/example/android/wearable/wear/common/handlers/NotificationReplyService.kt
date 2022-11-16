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
package com.example.android.wearable.wear.common.handlers

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.RemoteInput
import com.example.android.wearable.wear.common.components.SampleNotificationApplication
import com.example.android.wearable.wear.common.notifications.NotificationCentre
import kotlinx.coroutines.runBlocking

/**
 * Asynchronously handles snooze and dismiss actions for reminder app (and active Notification).
 * Notification for for reminder app uses BigTextStyle.
 */
abstract class NotificationReplyService : Service() {
    lateinit var notificationCentre: NotificationCentre

    override fun onCreate() {
        super.onCreate()

        notificationCentre = (application as SampleNotificationApplication).notificationCentre
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_REPLY -> handleActionComment(intent)
        }
        return START_REDELIVER_INTENT
    }

    private fun getComment(intent: Intent): String {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        return remoteInput?.getString(EXTRA_REPLY) ?: "Missing"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun handleActionComment(intent: Intent) {
        runBlocking {
            handleActionComment(
                intent.getIntExtra(CoreNotificationActionsService.EXTRA_NOTIFICATION_ID, -1),
                getComment(intent)
            )
        }
    }

    abstract suspend fun handleActionComment(id: Int, comment: String)

    companion object {
        const val ACTION_REPLY =
            "com.example.android.wearable.wear.wearnotifications.handlers.action.REPLY"

        const val EXTRA_REPLY =
            "com.example.android.wearable.wear.wearnotifications.handlers.extra.REPLY"
    }
}

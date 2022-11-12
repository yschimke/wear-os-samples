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

import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material.Text
import com.example.android.wearable.wear.wearnotifications.R
import com.example.android.wearable.wear.wearnotifications.main.StandaloneMainActivity

/**
 * Template class meant to include functionality for your Messaging App. (This project's main focus
 * is on Notification Styles.)
 */
class MessagingMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(id = R.string.main_text_activity_messaging_main))
            }
        }

        // Cancel Notification
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(StandaloneMainActivity.NOTIFICATION_ID)

        // TODO: Handle and display message/conversation from your database

        // NOTE: You can retrieve the EXTRA_REMOTE_INPUT_DRAFT sent by the system when a user
        // inadvertently closes a messaging notification to pre-populate the reply text field so
        // the user can finish their reply.
    }

    companion object {
        private const val TAG = "MessagingMainActivity"
    }
}

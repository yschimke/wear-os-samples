/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.wearable.wear.wearnotifications

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.app.NotificationCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.ambient.AmbientModeSupport
import com.example.android.wearable.wear.wearnotifications.SettingsLauncher.openNotificationSettingsForApp

/**
 * Demonstrates best practice for [NotificationCompat] Notifications created by local
 * standalone Wear apps. All [NotificationCompat] examples use
 * [NotificationCompat.Style].
 */
class StandaloneComposeActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enables Ambient mode.
        AmbientModeSupport.attach(this)

        setContent {
            val viewModel = viewModel<MainViewModel>()
            WearNotificationApp(
                bigPictureClick = { viewModel.generateBigPictureStyleNotification(this) },
                inboxClick = { viewModel.generateInboxStyleNotification(this) },
                bigTextClick = { viewModel.generateBigTextStyleNotification(this) },
                messagingClick = { viewModel.generateMessagingStyleNotification(this) },
                launchSettings = { openNotificationSettingsForApp() }
            )
        }
    }
}

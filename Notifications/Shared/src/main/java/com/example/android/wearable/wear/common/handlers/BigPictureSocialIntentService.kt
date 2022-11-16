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

import androidx.datastore.core.DataStore
import com.example.android.wearable.wear.common.components.SampleNotificationApplication
import com.example.android.wearable.wear.common.datastore.postedNotificationsStore
import com.example.android.wearable.wear.common.handlers.NotificationReplyService
import com.example.android.wearable.wear.wearnotifications.proto.NotificationsProto

/**
 * Asynchronously handles updating social app posts (and active Notification) with comments from
 * user. Notification for social app use BigPictureStyle.
 */
class BigPictureSocialIntentService : NotificationReplyService() {
    private lateinit var postedNotificationsStore: DataStore<NotificationsProto.PostedNotifications>

    override fun onCreate() {
        super.onCreate()

        postedNotificationsStore = (application as SampleNotificationApplication).postedNotificationsStore
    }

    override suspend fun handleActionComment(id: Int, comment: String) {
        notificationCentre.updatePictureNotification(id) {
            this.comment.add(comment)
        }
    }
}

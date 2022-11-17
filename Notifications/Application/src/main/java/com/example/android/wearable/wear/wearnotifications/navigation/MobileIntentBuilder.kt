package com.example.android.wearable.wear.wearnotifications.navigation

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.example.android.wearable.wear.common.handlers.BigPictureSocialIntentService
import com.example.android.wearable.wear.common.handlers.CoreNotificationActionsService
import com.example.android.wearable.wear.common.handlers.MessagingIntentService
import com.example.android.wearable.wear.common.handlers.NotificationReplyService
import com.example.android.wearable.wear.common.navigation.IntentBuilder
import com.example.android.wearable.wear.wearnotifications.DeepLinkPrefix

class MobileIntentBuilder(
    val context: Context
) : IntentBuilder {
    override fun dismissIntent(id: Int): Intent =
        Intent(context, CoreNotificationActionsService::class.java).apply {
            action = CoreNotificationActionsService.ACTION_DISMISS
            putExtra(CoreNotificationActionsService.EXTRA_NOTIFICATION_ID, id)
        }

    override fun textScreenIntent(id: Int): Intent = Intent(
        Intent.ACTION_VIEW,
        "$DeepLinkPrefix/text?id=$id".toUri()
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    override fun inboxScreenIntent(id: Int): Intent = Intent(
        Intent.ACTION_VIEW,
        "$DeepLinkPrefix/inbox?id=$id".toUri()
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    override fun pictureScreenIntent(id: Int): Intent = Intent(
        Intent.ACTION_VIEW,
        "$DeepLinkPrefix/picture?id=$id".toUri()
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    override fun pictureReplyIntent(id: Int): Intent =
        Intent(
            context, BigPictureSocialIntentService::class.java
        ).apply {
            action = NotificationReplyService.ACTION_REPLY
            putExtra(CoreNotificationActionsService.EXTRA_NOTIFICATION_ID, id)
        }

    override fun messagingScreenIntent(id: Int): Intent = Intent(
        Intent.ACTION_VIEW,
        "$DeepLinkPrefix/messaging?id=$id".toUri()
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    override fun messagingReplyIntent(id: Int): Intent =
        Intent(context, MessagingIntentService::class.java).apply {
            action = NotificationReplyService.ACTION_REPLY
            putExtra(CoreNotificationActionsService.EXTRA_NOTIFICATION_ID, id)
        }
}

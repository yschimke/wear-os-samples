package com.example.android.wearable.wear.wearnotifications.navigation

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.example.android.wearable.wear.common.navigation.IntentBuilder
import com.example.android.wearable.wear.common.notifications.CoreNotificationActionsService
import com.example.android.wearable.wear.wearnotifications.DeepLinkPrefix
import com.example.android.wearable.wear.wearnotifications.handlers.BigPictureSocialIntentService

class WearIntentBuilder(
    val context: Context
): IntentBuilder {
    override fun dismissIntent(id: Int): Intent =
        Intent(context, CoreNotificationActionsService::class.java).apply {
            action = CoreNotificationActionsService.ACTION_DISMISS
        }

    override fun textScreenIntent(id: Int): Intent = Intent(
        Intent.ACTION_VIEW,
        "$DeepLinkPrefix/text?id=$id".toUri()
    )

    override fun inboxScreenIntent(id: Int): Intent = Intent(
        Intent.ACTION_VIEW,
        "$DeepLinkPrefix/inbox?id=$id".toUri()
    )

    override fun pictureScreenIntent(id: Int): Intent = Intent(
        Intent.ACTION_VIEW,
        "$DeepLinkPrefix/picture?id=$id".toUri()
    )

    override fun pictureReplyIntent(id: Int): Intent {
        return Intent(context, BigPictureSocialIntentService::class.java).apply {
            action = BigPictureSocialIntentService.ACTION_COMMENT
        }
    }
}

package com.example.android.wearable.wear.wearnotifications.navigation

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.example.android.wearable.wear.common.navigation.IntentBuilder
import com.example.android.wearable.wear.common.notifications.CoreNotificationActionsService
import com.example.android.wearable.wear.wearnotifications.DeepLinkPrefix

class WearIntentBuilder(
    val context: Context
): IntentBuilder {
    override fun dismissIntent(id: Int): Intent {
        val dismissIntent = Intent(context, CoreNotificationActionsService::class.java)
        dismissIntent.action = CoreNotificationActionsService.ACTION_DISMISS
        return dismissIntent
    }

    override fun textScreenIntent(id: Int): Intent = Intent(
        Intent.ACTION_VIEW,
        "$DeepLinkPrefix/text?id=$id".toUri()
    )

    override fun inboxScreenIntent(id: Int): Intent = Intent(
        Intent.ACTION_VIEW,
        "$DeepLinkPrefix/inbox?id=$id".toUri()
    )
}

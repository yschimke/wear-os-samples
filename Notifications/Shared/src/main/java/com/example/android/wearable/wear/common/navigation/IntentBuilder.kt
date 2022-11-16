package com.example.android.wearable.wear.common.navigation

import android.content.Intent

interface IntentBuilder {
    fun dismissIntent(id: Int): Intent
    fun textScreenIntent(id: Int): Intent
    fun inboxScreenIntent(id: Int): Intent
    fun pictureScreenIntent(id: Int): Intent
    fun pictureReplyIntent(id: Int): Intent
    fun messagingScreenIntent(id: Int): Intent
    fun messagingReplyIntent(id: Int): Intent
}

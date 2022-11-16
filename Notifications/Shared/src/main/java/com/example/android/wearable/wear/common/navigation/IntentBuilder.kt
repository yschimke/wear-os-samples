package com.example.android.wearable.wear.common.navigation

import android.content.Intent

interface IntentBuilder {
    fun dismissIntent(id: Int): Intent
    fun textScreenIntent(id: Int): Intent
    fun inboxScreenIntent(id: Int): Intent
}

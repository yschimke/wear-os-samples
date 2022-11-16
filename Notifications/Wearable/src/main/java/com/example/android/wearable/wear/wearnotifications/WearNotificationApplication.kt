package com.example.android.wearable.wear.wearnotifications

import com.example.android.wearable.wear.common.IntentBuilder
import com.example.android.wearable.wear.common.SampleNotificationApplication
import com.example.android.wearable.wear.wearnotifications.handlers.WearIntentBuilder

class WearNotificationApplication: SampleNotificationApplication() {
    override fun createIntentBuilder(): IntentBuilder = WearIntentBuilder(this)
}

package com.example.android.wearable.wear.wearnotifications

import com.example.android.wearable.wear.common.components.SampleNotificationApplication
import com.example.android.wearable.wear.common.navigation.IntentBuilder
import com.example.android.wearable.wear.wearnotifications.navigation.MobileIntentBuilder

class MobileNotificationApplication: SampleNotificationApplication() {
    override fun createIntentBuilder(): IntentBuilder = MobileIntentBuilder(this)
}

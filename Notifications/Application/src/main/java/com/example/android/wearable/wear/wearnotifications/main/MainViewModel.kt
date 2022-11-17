package com.example.android.wearable.wear.wearnotifications.main

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.wearable.wear.common.domain.Me
import com.example.android.wearable.wear.common.domain.TestData.sampleInboxNotification
import com.example.android.wearable.wear.common.domain.TestData.sampleMessagingNotification
import com.example.android.wearable.wear.common.domain.TestData.samplePictureNotification
import com.example.android.wearable.wear.common.domain.TestData.sampleTextNotification
import com.example.android.wearable.wear.common.domain.resourceToUri
import com.example.android.wearable.wear.wearnotifications.MobileNotificationApplication
import com.example.android.wearable.wear.wearnotifications.common.R
import com.example.android.wearable.wear.wearnotifications.proto.contact
import com.example.android.wearable.wear.wearnotifications.proto.email
import com.example.android.wearable.wear.wearnotifications.proto.image
import com.example.android.wearable.wear.wearnotifications.proto.inboxNotification
import com.example.android.wearable.wear.wearnotifications.proto.message
import com.example.android.wearable.wear.wearnotifications.proto.messagingNotification
import com.example.android.wearable.wear.wearnotifications.proto.pictureNotification
import com.example.android.wearable.wear.wearnotifications.proto.textNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val notificationCentre = (application as MobileNotificationApplication).notificationCentre
    private val mNotificationManagerCompat =
        (application as MobileNotificationApplication).notificationManager

    val uiState: StateFlow<MainUiState> = MutableStateFlow(
        MainUiState(
            mNotificationManagerCompat.areNotificationsEnabled()
        )
    )

    fun generateBigTextStyleNotification(activity: Activity) {
        viewModelScope.launch {
            notificationCentre.postTextNotification(sampleTextNotification())
        }
    }

    fun generateBigPictureStyleNotification(activity: Activity) {
        viewModelScope.launch {
            notificationCentre.postPictureNotification(samplePictureNotification())
        }
    }

    fun generateInboxStyleNotification(activity: Activity) {
        viewModelScope.launch {
            notificationCentre.postInboxNotification(sampleInboxNotification())
        }
    }

    fun generateMessagingStyleNotification(activity: Activity) {
        viewModelScope.launch {
            notificationCentre.postMessagingNotification(sampleMessagingNotification(activity))
        }
    }
}

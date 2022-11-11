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
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.core.app.NotificationCompat
import androidx.fragment.app.FragmentActivity
import androidx.wear.ambient.AmbientModeSupport
import androidx.wear.ambient.AmbientModeSupport.AmbientCallback
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import com.example.android.wearable.wear.wearnotifications.SettingsLauncher.openNotificationSettingsForApp
import com.google.android.material.snackbar.Snackbar

/**
 * Demonstrates best practice for [NotificationCompat] Notifications created by local
 * standalone Wear apps. All [NotificationCompat] examples use
 * [NotificationCompat.Style].
 */
class StandaloneMainActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {

    // Needed for {@link SnackBar} to alert users when {@link Notification} are disabled for app.
    private lateinit var mMainFrameLayout: FrameLayout
    private lateinit var mWearableRecyclerView: WearableRecyclerView
    private lateinit var mCustomRecyclerAdapter: CustomRecyclerAdapter

    val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate()")
        setContentView(R.layout.activity_main)

        // Enables Ambient mode.
        AmbientModeSupport.attach(this)

        mMainFrameLayout = findViewById(R.id.mainFrameLayout)
        mWearableRecyclerView = findViewById(R.id.recycler_view)

        // Aligns the first and last items on the list vertically centered on the screen.
        mWearableRecyclerView.isEdgeItemsCenteringEnabled = true

        // Customizes scrolling so items farther away form center are smaller.
        val scalingScrollLayoutCallback = ScalingScrollLayoutCallback()
        mWearableRecyclerView.layoutManager =
            WearableLinearLayoutManager(this, scalingScrollLayoutCallback)

        // Improves performance because we know changes in content do not change the layout size of
        // the RecyclerView.
        mWearableRecyclerView.setHasFixedSize(true)

        // Specifies an adapter (see also next example).
        mCustomRecyclerAdapter = CustomRecyclerAdapter(
            NOTIFICATION_STYLES,  // Controller passes selected data from the Adapter out to this Activity to trigger
            // updates in the UI/Notifications.
            Controller(this)
        )
        mWearableRecyclerView.adapter = mCustomRecyclerAdapter
    }

    // Called by WearableRecyclerView when an item is selected (check onCreate() for
    // initialization).
    fun itemSelected(data: String?) {
        Log.d(TAG, "itemSelected()")
        val areNotificationsEnabled = viewModel.uiState.value.areNotificationsEnabled

        // If notifications are disabled, allow user to enable.
        if (!areNotificationsEnabled) {
            // Because the user took an action to create a notification, we create a prompt to let
            // the user re-enable notifications for this application again.
            val snackbar = Snackbar
                .make(
                    mMainFrameLayout,
                    "",  // Not enough space for both text and action text.
                    Snackbar.LENGTH_LONG
                )
                .setAction("Enable Notifications") { // Links to this app's notification settings.
                    openNotificationSettingsForApp()
                }
            snackbar.show()
            return
        }
        when (data) {
            BIG_TEXT_STYLE -> viewModel.generateBigTextStyleNotification(this)
            BIG_PICTURE_STYLE -> viewModel.generateBigPictureStyleNotification(this)
            INBOX_STYLE -> viewModel.generateInboxStyleNotification(this)
            MESSAGING_STYLE -> viewModel.generateMessagingStyleNotification(this)
            else -> {}
        }
    }

    override fun getAmbientCallback(): AmbientCallback {
        return MyAmbientCallback()
    }

    private inner class MyAmbientCallback : AmbientCallback() {
        /** Prepares the UI for ambient mode.  */
        override fun onEnterAmbient(ambientDetails: Bundle) {
            super.onEnterAmbient(ambientDetails)
            Log.d(TAG, "onEnterAmbient() $ambientDetails")
            // In our case, the assets are already in black and white, so we don't update UI.
        }

        /** Restores the UI to active (non-ambient) mode.  */
        override fun onExitAmbient() {
            super.onExitAmbient()
            Log.d(TAG, "onExitAmbient()")
            // In our case, the assets are already in black and white, so we don't update UI.
        }
    }

    companion object {
        internal const val TAG = "StandaloneMainActivity"
        const val NOTIFICATION_ID = 888

        /*
     * Used to represent each major {@link NotificationCompat.Style} in the
     * {@link WearableRecyclerView}. These constants are also used in a switch statement when one
     * of the items is selected to create the appropriate {@link Notification}.
     */
        private const val BIG_TEXT_STYLE = "BIG_TEXT_STYLE"
        private const val BIG_PICTURE_STYLE = "BIG_PICTURE_STYLE"
        private const val INBOX_STYLE = "INBOX_STYLE"
        private const val MESSAGING_STYLE = "MESSAGING_STYLE"

        /*
    Collection of major {@link NotificationCompat.Style} to create {@link CustomRecyclerAdapter}
    for {@link WearableRecyclerView}.
    */
        private val NOTIFICATION_STYLES =
            arrayOf(BIG_TEXT_STYLE, BIG_PICTURE_STYLE, INBOX_STYLE, MESSAGING_STYLE)
    }
}

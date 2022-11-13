package com.example.android.wearable.wear.wearnotifications

import android.content.Context
import android.content.Intent

object SettingsLauncher {
    /**
     * Helper method for the SnackBar action, i.e., if the user has this application's notifications
     * disabled, this opens up the dialog to turn them back on after the user requests a
     * Notification launch.
     *
     * IMPORTANT NOTE: You should not do this action unless the user takes an action to see your
     * Notifications like this sample demonstrates. Spamming users to re-enable your notifications
     * is a bad idea.
     */
    fun Context.openNotificationSettingsForApp() {
        // Links to this app's notification settings
        val intent = Intent()
        intent.action = "com.google.android.clockwork.settings.APP_NOTIFICATIONS"
        intent.putExtra("app_pkg", packageName)
        startActivity(intent)
    }
}

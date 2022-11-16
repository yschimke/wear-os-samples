package com.example.android.wearable.wear.wearnotifications.navigation

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
    @JvmStatic
    fun openNotificationSettingsForApp(context: Context) {
        // Links to this app's notification settings.

        // Links to this app's notification settings.
        val intent = Intent()
        intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
        intent.putExtra("app_package", context.packageName)
        intent.putExtra("app_uid", context.applicationInfo.uid)

        // for Android 8 and above

        // for Android 8 and above
        intent.putExtra("android.provider.extra.APP_PACKAGE", context.packageName)

        context.startActivity(intent)
    }
}

package com.example.android.wearable.wear.wearnotifications.main

object StandaloneMainActivity {
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

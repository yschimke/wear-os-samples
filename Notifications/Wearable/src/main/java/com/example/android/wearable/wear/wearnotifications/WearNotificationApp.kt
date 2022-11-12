package com.example.android.wearable.wear.wearnotifications

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun WearNotificationApp(
    bigPictureClick: () -> Unit,
    inboxClick: () -> Unit,
    bigTextClick: () -> Unit,
    messagingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WearMainScreen(
        modifier = modifier,
        bigPictureClick = bigPictureClick,
        inboxClick = inboxClick,
        bigTextClick = bigTextClick,
        messagingClick = messagingClick
    )
}

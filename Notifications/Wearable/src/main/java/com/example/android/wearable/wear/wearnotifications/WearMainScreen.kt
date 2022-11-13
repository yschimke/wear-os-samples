package com.example.android.wearable.wear.wearnotifications

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text

@Composable
fun WearMainScreen(
    bigPictureClick: () -> Unit,
    inboxClick: () -> Unit,
    bigTextClick: () -> Unit,
    messagingClick: () -> Unit,
    launchSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScalingLazyColumn(modifier = modifier) {
        item {
            ListHeader {
                Text(stringResource(id = R.string.floating_text))
            }
        }
        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = stringResource(id = R.string.big_picture_chip))
                },
                onClick = bigPictureClick
            )
        }
        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = stringResource(id = R.string.messaging_chip))
                },
                onClick = messagingClick
            )
        }
        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = stringResource(id = R.string.inbox_chip))
                },
                onClick = inboxClick
            )
        }
        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = stringResource(id = R.string.big_text_chip))
                },
                onClick = bigTextClick
            )
        }
        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = stringResource(id = R.string.settings_chip))
                },
                onClick = launchSettings
            )
        }
    }
}

@file:OptIn(ExperimentalMaterialApi::class)

package com.example.android.wearable.wear.wearnotifications.main

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Chip
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.android.wearable.wear.wearnotifications.R

@Composable
fun MobileMainScreen(
    bigPictureClick: () -> Unit,
    inboxClick: () -> Unit,
    bigTextClick: () -> Unit,
    messagingClick: () -> Unit,
    launchSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        state = rememberLazyListState(),
    ) {
        item {
                Text(stringResource(id = R.string.floating_text))
        }
        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = bigPictureClick
            ) {
                Text(text = stringResource(id = R.string.big_picture_chip))
            }
        }
        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = messagingClick
            ) {
                Text(text = stringResource(id = R.string.messaging_chip))
            }
        }
        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = inboxClick
            ) {
                Text(text = stringResource(id = R.string.inbox_chip))
            }
        }
        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = bigTextClick
            ) {
                Text(text = stringResource(id = R.string.big_text_chip))
            }
        }
        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = launchSettings
            ) {
                Text(text = stringResource(id = R.string.settings_chip))
            }
        }
    }
}

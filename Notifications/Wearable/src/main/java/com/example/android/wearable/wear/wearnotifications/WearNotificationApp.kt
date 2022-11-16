package com.example.android.wearable.wear.wearnotifications

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.wear.compose.material.ScalingLazyListState
import com.example.android.wearable.wear.wearnotifications.handlers.BigPictureMainScreen
import com.example.android.wearable.wear.wearnotifications.handlers.BigTextMainScreen
import com.example.android.wearable.wear.wearnotifications.handlers.InboxMainScreen
import com.example.android.wearable.wear.wearnotifications.handlers.MessagingMainScreen
import com.google.android.horologist.compose.navscaffold.WearNavScaffold
import com.google.android.horologist.compose.navscaffold.scalingLazyColumnComposable
import com.google.android.horologist.compose.navscaffold.wearNavComposable

@Composable
fun WearNotificationApp(
    navController: NavHostController,
    bigPictureClick: () -> Unit,
    inboxClick: () -> Unit,
    bigTextClick: () -> Unit,
    messagingClick: () -> Unit,
    launchSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WearNavScaffold(startDestination = "main", navController = navController) {
        scalingLazyColumnComposable(
            route = "main",
            scrollStateBuilder = {
                ScalingLazyListState()
            }
        ) {
            WearMainScreen(
                bigPictureClick = bigPictureClick,
                inboxClick = inboxClick,
                bigTextClick = bigTextClick,
                messagingClick = messagingClick,
                launchSettings = launchSettings,
                modifier = modifier,
                state = it.scrollableState
            )
        }
        wearNavComposable(
            route = "picture?id={id}",
            arguments = listOf(navArgument("id") {
                type = NavType.StringType
            }),
            deepLinks = listOf(navDeepLink {
                uriPattern = "$DeepLinkPrefix/picture?id={id}"
            })
        ) { _, _ ->
            BigPictureMainScreen()
        }
        wearNavComposable(
            route = "text?id={id}",
            arguments = listOf(navArgument("id") {
                type = NavType.StringType
            }),
            deepLinks = listOf(navDeepLink {
                uriPattern = "$DeepLinkPrefix/text?id={id}"
            })
        ) { _, _ ->
            BigTextMainScreen()
        }
        wearNavComposable(
            route = "inbox?id={id}",
            arguments = listOf(navArgument("id") {
                type = NavType.StringType
            }),
            deepLinks = listOf(navDeepLink {
                uriPattern = "$DeepLinkPrefix/inbox?id={id}"
            })
        ) { _, _ ->
            InboxMainScreen()
        }
        wearNavComposable(
            route = "messaging?id={id}",
            arguments = listOf(navArgument("id") {
                type = NavType.StringType
            }),
            deepLinks = listOf(navDeepLink {
                uriPattern = "$DeepLinkPrefix/messaging?id={id}"
            })
        ) { _, _ ->
            MessagingMainScreen()
        }
    }
}

val DeepLinkPrefix = "notifsample://notifsample"

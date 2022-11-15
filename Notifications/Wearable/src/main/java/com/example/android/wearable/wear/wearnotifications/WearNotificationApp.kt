package com.example.android.wearable.wear.wearnotifications

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
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
                modifier = modifier,
                bigPictureClick = bigPictureClick,
                inboxClick = inboxClick,
                bigTextClick = bigTextClick,
                messagingClick = messagingClick,
                launchSettings = launchSettings
            )
        }
        wearNavComposable(
            route = "picture?id={id}",
            arguments = listOf(navArgument("id") {
                type = NavType.StringType
            }),
            deepLinks = listOf(navDeepLink {
                uriPattern = "$deepLinkPrefix/picture?id={id}"
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
                uriPattern = "$deepLinkPrefix/text?id={id}"
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
                uriPattern = "$deepLinkPrefix/inbox?id={id}"
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
                uriPattern = "$deepLinkPrefix/messaging?id={id}"
            })
        ) { _, _ ->
            MessagingMainScreen()
        }
    }
}

val deepLinkPrefix = "notifsample://notifsample"

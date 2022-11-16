package com.example.android.wearable.wear.wearnotifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.android.wearable.wear.wearnotifications.main.MobileMainScreen
import com.example.android.wearable.wear.wearnotifications.screens.BigPictureMainScreen
import com.example.android.wearable.wear.wearnotifications.screens.BigTextMainScreen
import com.example.android.wearable.wear.wearnotifications.screens.InboxMainScreen
import com.example.android.wearable.wear.wearnotifications.screens.MessagingMainScreen

@Composable
fun WearNotificationApp(
    navController: NavHostController,
    bigPictureClick: () -> Unit,
    inboxClick: () -> Unit,
    bigTextClick: () -> Unit,
    messagingClick: () -> Unit,
    launchSettings: () -> Unit,
    dismissHandler: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(startDestination = "main", navController = navController) {
        composable(
            route = "main",
        ) {
            MobileMainScreen(
                bigPictureClick = bigPictureClick,
                inboxClick = inboxClick,
                bigTextClick = bigTextClick,
                messagingClick = messagingClick,
                launchSettings = launchSettings,
                modifier = modifier,
            )
        }
        composable(
            route = "picture?id={id}",
            arguments = listOf(navArgument("id") {
                type = NavType.IntType
            }),
            deepLinks = listOf(navDeepLink {
                uriPattern = "$DeepLinkPrefix/picture?id={id}"
            })
        ) {
            val id = it.arguments?.getInt("id")

            BigPictureMainScreen()

            LaunchedEffect(id) {
                if (id != null) {
                    dismissHandler(id)
                }
            }
        }
        composable(
            route = "text?id={id}",
            arguments = listOf(navArgument("id") {
                type = NavType.IntType
            }),
            deepLinks = listOf(navDeepLink {
                uriPattern = "$DeepLinkPrefix/text?id={id}"
            })
        ) {
            val id = it.arguments?.getInt("id")

            BigTextMainScreen()

            LaunchedEffect(id) {
                if (id != null) {
                    dismissHandler(id)
                }
            }
        }
        composable(
            route = "inbox?id={id}",
            arguments = listOf(navArgument("id") {
                type = NavType.IntType
            }),
            deepLinks = listOf(navDeepLink {
                uriPattern = "$DeepLinkPrefix/inbox?id={id}"
            })
        ) {
            val id = it.arguments?.getInt("id")

            InboxMainScreen()

            LaunchedEffect(id) {
                if (id != null) {
                    dismissHandler(id)
                }
            }
        }
        composable(
            route = "messaging?id={id}",
            arguments = listOf(navArgument("id") {
                type = NavType.IntType
            }),
            deepLinks = listOf(navDeepLink {
                uriPattern = "$DeepLinkPrefix/messaging?id={id}"
            })
        ) {
            val id = it.arguments?.getInt("id")

            MessagingMainScreen()

            LaunchedEffect(id) {
                if (id != null) {
                    dismissHandler(id)
                }
            }
        }
    }
}

val DeepLinkPrefix = "notifsample://notifsample"

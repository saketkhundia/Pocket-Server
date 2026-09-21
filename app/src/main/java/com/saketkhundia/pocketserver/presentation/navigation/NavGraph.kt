package com.saketkhundia.pocketserver.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.saketkhundia.pocketserver.presentation.activity.ActivityScreen
import com.saketkhundia.pocketserver.presentation.developer.DeveloperScreen
import com.saketkhundia.pocketserver.presentation.files.FilesScreen
import com.saketkhundia.pocketserver.presentation.glass.GlassTab
import com.saketkhundia.pocketserver.presentation.glass.LiquidGlassBottomNavigation
import com.saketkhundia.pocketserver.presentation.home.HomeScreen
import com.saketkhundia.pocketserver.presentation.logs.LogsScreen
import com.saketkhundia.pocketserver.presentation.media.MediaScreen
import com.saketkhundia.pocketserver.presentation.media.PhotosScreen
import com.saketkhundia.pocketserver.presentation.motion.PsMotion
import com.saketkhundia.pocketserver.presentation.motion.backIn
import com.saketkhundia.pocketserver.presentation.motion.backOut
import com.saketkhundia.pocketserver.presentation.motion.forwardIn
import com.saketkhundia.pocketserver.presentation.motion.forwardOut
import com.saketkhundia.pocketserver.presentation.motion.tabIn
import com.saketkhundia.pocketserver.presentation.motion.tabOut
import com.saketkhundia.pocketserver.presentation.onboarding.OnboardingScreen
import com.saketkhundia.pocketserver.presentation.qr.QrScreen
import com.saketkhundia.pocketserver.presentation.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Files : Screen("files")
    data object Activity : Screen("activity")
    data object Settings : Screen("settings")
    // Secondary (no bottom dock)
    data object Photos : Screen("photos")
    data object Media : Screen("media")
    data object Logs : Screen("logs")
    data object Developer : Screen("developer")
    data object Onboarding : Screen("onboarding")
    data object Qr : Screen("qr")
}

private data class TopLevel(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

private val TopLevels = listOf(
    TopLevel(Screen.Home, "Home", Icons.Outlined.Home),
    TopLevel(Screen.Files, "Files", Icons.Outlined.Folder),
    TopLevel(Screen.Activity, "Activity", Icons.AutoMirrored.Outlined.ShowChart),
    TopLevel(Screen.Settings, "Settings", Icons.Outlined.Settings)
)

private val TopRoutes = TopLevels.map { it.screen.route }.toSet()

private fun topIndex(route: String?): Int = TopLevels.indexOfFirst { it.screen.route == route }

/**
 * Single path for ALL top-level switches (dock tabs + header shortcuts).
 *
 * Previously the dock used popUpTo/launchSingleTop/restoreState but the Home
 * header shortcuts (settings gear, activity bell, files cards) used plain
 * navigate(). That pushed a second copy of the top destination instead of
 * switching to it, so the back stack became [home, settings] / [home, activity]
 * with no saved Home state to restore. Tapping Home in the dock then popped
 * with restoreState=true but there was nothing consistent to restore, and
 * repeated switches stacked duplicates — Home felt dead.
 */
private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        // Pop up to Home (not graph start: after onboarding the graph start
        // is still Onboarding, which is no longer in the back stack).
        popUpTo(Screen.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppNavGraph(navController: NavHostController, startRoute: String) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showDock = currentRoute in TopRoutes

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showDock) {
                // ONE source of truth: currentRoute from NavController.
                LiquidGlassBottomNavigation(
                    tabs = TopLevels.map { GlassTab(it.screen.route, it.label, it.icon) },
                    currentRoute = currentRoute,
                    onSelect = { route -> navController.navigateTopLevel(route) }
                )
            }
        }
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(inner),
            // Tab ↔ tab: subtle fade + tiny vertical lift (feels instant).
            // Drill-in (top → secondary): content slides slightly left.
            // Back (secondary → top): content slides slightly right.
            // All 200ms — almost instantaneous, never dramatic.
            enterTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
                when {
                    from in TopRoutes && to in TopRoutes -> tabIn()
                    to in TopRoutes -> backIn()
                    else -> forwardIn()
                }
            },
            exitTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
                when {
                    from in TopRoutes && to in TopRoutes -> tabOut()
                    from in TopRoutes -> forwardOut()
                    else -> backOut()
                }
            },
            popEnterTransition = {
                val to = targetState.destination.route
                if (to in TopRoutes) backIn() else tabIn()
            },
            popExitTransition = {
                val from = initialState.destination.route
                if (from in TopRoutes) tabOut() else backOut()
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onOpenQr = { navController.navigate(Screen.Qr.route) },
                    onOpenFiles = { navController.navigateTopLevel(Screen.Files.route) },
                    onOpenActivity = { navController.navigateTopLevel(Screen.Activity.route) },
                    onOpenSettings = { navController.navigateTopLevel(Screen.Settings.route) },
                    onOpenPhotos = { navController.navigate(Screen.Photos.route) },
                    onOpenMedia = { navController.navigate(Screen.Media.route) }
                )
            }
            composable(Screen.Files.route) {
                FilesScreen()
            }
            composable(Screen.Activity.route) {
                ActivityScreen(onOpenLogs = { navController.navigate(Screen.Logs.route) })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onOpenLogs = { navController.navigate(Screen.Logs.route) },
                    onOpenDeveloper = { navController.navigate(Screen.Developer.route) },
                    onOpenFiles = { navController.navigateTopLevel(Screen.Files.route) }
                )
            }
            composable(Screen.Photos.route) {
                PhotosScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Media.route) {
                MediaScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Logs.route) {
                LogsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Developer.route) {
                DeveloperScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Qr.route) {
                QrScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(onDone = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                })
            }
        }
    }
}

@Composable
fun rememberAppNavController(): NavHostController = rememberNavController()

/** Whether [route] participates in the bottom dock. */
fun isTopLevel(route: String?): Boolean = route in TopRoutes

/** Tab order for directional slide (Home→Files slides left, reverse slides right). */
fun tabOrder(route: String?): Int = topIndex(route)

package com.saketkhundia.pocketserver.presentation.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.saketkhundia.pocketserver.presentation.activity.ActivityScreen
import com.saketkhundia.pocketserver.presentation.developer.DeveloperScreen
import com.saketkhundia.pocketserver.presentation.files.FilesScreen
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
import com.saketkhundia.pocketserver.presentation.theme.LocalPsExtra
import com.saketkhundia.pocketserver.presentation.theme.PsRadius

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
                FloatingDock(
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

/**
 * Floating glass dock with a *moving* active capsule.
 *
 * A single pill indicator glides behind the tabs instead of
 * disappearing/reappearing. The pill is GPU-translated (translationX, no
 * layout per frame) on a 180ms tween so rapid tab switches restart cleanly
 * instead of queuing spring oscillations. Icons scale subtly (GPU layer),
 * labels fade. Tapping the active tab is a no-op.
 */
@Composable
private fun FloatingDock(
    currentRoute: String?,
    onSelect: (String) -> Unit
) {
    val extra = LocalPsExtra.current
    val selectedIndex = topIndex(currentRoute).takeIf { it >= 0 } ?: 0
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .shadow(18.dp, RoundedCornerShape(PsRadius.dock), ambientColor = Color.Black.copy(alpha = 0.6f))
                .clip(RoundedCornerShape(PsRadius.dock))
                .background(extra.glass)
                .border(1.dp, extra.subtleBorder, RoundedCornerShape(PsRadius.dock))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            val slots = TopLevels.size
            // BoxWithConstraints scope maxWidth ALREADY excludes this layout's own
            // padding modifier — it is the exact content width. (Subtracting the
            // 20dp padding again was the half-pill bug: slots came out narrow and
            // the error compounded per tab index.) Tabs use weight(1f) with NO
            // inter-item arrangement, so tab i owns exactly [i * slot, (i+1) * slot].
            val slotWidth: Dp = maxWidth / slots
            val slotPx = with(density) { slotWidth.toPx() }
            val targetPx = slotPx * selectedIndex

            // 180ms tween on a GPU layer: settles fast, restarts from the current
            // position on rapid taps (no spring queue, no layout thrash).
            val pillX by animateFloatAsState(
                targetValue = targetPx,
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                label = "dockPillX"
            )

            // Overlay fills the Row's exact size (Row determines Box size;
            // matchParentSize children don't affect measurement). The pill fills
            // the full tab height and glides via translationX — zero layout work.
            Box(modifier = Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(slotWidth)
                        .fillMaxHeight()
                        .graphicsLayer { translationX = pillX }
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopLevels.forEachIndexed { index, top ->
                    val selected = index == selectedIndex
                    DockTab(
                        label = top.label,
                        icon = top.icon,
                        selected = selected,
                        onClick = { if (!selected) onSelect(top.screen.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DockTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // GPU-layer transforms only: scale + alpha. Fast tweens (no bounce) so
    // rapid switches never stack oscillations — each retap restarts cleanly.
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "dockIconScale"
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.75f,
        animationSpec = tween(PsMotion.Fast),
        label = "dockLabelAlpha"
    )
    val interaction = remember { MutableInteractionSource() }
    // Immediate press feedback: the tap visibly lands within ~1 frame, long
    // before the 180ms transition completes. Without this (indication = null),
    // taps felt "dead" until navigation caught up. GPU scale, no layout.
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "dockPress"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(23.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = (if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                .copy(alpha = labelAlpha),
            modifier = Modifier.graphicsLayer { alpha = labelAlpha }
        )
    }
}

// Unused legacy helper kept out of the composition path.
@Suppress("unused")
private fun dockTabCount(): Int = TopLevels.size

@Composable
fun rememberAppNavController(): NavHostController = rememberNavController()

/** Whether [route] participates in the bottom dock. */
fun isTopLevel(route: String?): Boolean = route in TopRoutes

/** Tab order for directional slide (Home→Files slides left, reverse slides right). */
fun tabOrder(route: String?): Int = topIndex(route)

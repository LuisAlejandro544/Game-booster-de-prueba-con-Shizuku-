package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Destinos de navegación disponibles en la aplicación Game Booster.
 *
 * Cada pantalla cumple una función específica siguiendo el principio de diseño modular
 * y una navegación clara e intuitiva para el usuario.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Shizuku : Screen(
        route = "shizuku",
        title = "Shizuku",
        selectedIcon = Icons.Filled.Bolt,
        unselectedIcon = Icons.Outlined.Bolt
    )

    object Booster : Screen(
        route = "booster",
        title = "Booster",
        selectedIcon = Icons.Filled.SportsEsports,
        unselectedIcon = Icons.Outlined.SportsEsports
    )

    object Notifications : Screen(
        route = "notifications",
        title = "No Molestar",
        selectedIcon = Icons.Filled.NotificationsOff,
        unselectedIcon = Icons.Outlined.NotificationsOff
    )

    object Guide : Screen(
        route = "guide",
        title = "Guía Móvil",
        selectedIcon = Icons.Filled.HelpOutline,
        unselectedIcon = Icons.Outlined.HelpOutline
    )
}

val navItems = listOf(
    Screen.Shizuku,
    Screen.Booster,
    Screen.Notifications,
    Screen.Guide
)

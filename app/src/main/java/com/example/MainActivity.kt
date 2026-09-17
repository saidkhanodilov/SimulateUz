package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FlightTelemetry
import com.example.model.RocketDesign
import com.example.model.RocketPresets
import com.example.ui.screens.*
import com.example.ui.theme.*

enum class NavScreen(val label: String, val icon: ImageVector) {
    LAUNCH("3D Launch", Icons.Default.RocketLaunch),
    ADVISOR("Cost & Mass", Icons.Default.PriceCheck),
    SATELLITES("Satellites", Icons.Default.Public),
    REPORTS("Reports", Icons.Default.Assessment),
    ASSEMBLY("Assembly", Icons.Default.Build)
}

class MainActivity : ComponentActivity() {
    private var initialScreen = NavScreen.LAUNCH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming web link deep link (e.g. https://simulateuz.app/advisor or simulateuz://satellites)
        handleDeepLink(intent?.data)

        setContent {
            MyApplicationTheme {
                SimulateUzApp(initialScreen = initialScreen)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent.data)
    }

    private fun handleDeepLink(uri: Uri?) {
        if (uri == null) return
        val path = uri.path?.lowercase() ?: ""
        val host = uri.host?.lowercase() ?: ""

        initialScreen = when {
            path.contains("advisor") || path.contains("cost") || path.contains("payload") -> NavScreen.ADVISOR
            path.contains("sat") || path.contains("orbit") || path.contains("collision") -> NavScreen.SATELLITES
            path.contains("report") || path.contains("history") || path.contains("log") -> NavScreen.REPORTS
            path.contains("assembly") || path.contains("build") -> NavScreen.ASSEMBLY
            else -> NavScreen.LAUNCH
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulateUzApp(initialScreen: NavScreen = NavScreen.LAUNCH) {
    var currentScreen by remember { mutableStateOf(initialScreen) }
    var activeRocket by remember { mutableStateOf(RocketPresets.FALCON_9) }
    var latestTelemetry by remember { mutableStateOf(FlightTelemetry()) }
    var showCreatorDialog by remember { mutableStateOf(false) }

    if (showCreatorDialog) {
        AlertDialog(
            onDismissRequest = { showCreatorDialog = false },
            confirmButton = {
                Button(
                    onClick = { showCreatorDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIce, contentColor = SpaceBackground)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = AccentGold, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SimulateUz Mission Control", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "LEAD ARCHITECT & CREATOR",
                        color = AccentGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "SAIDMUXAMMADXON ODILOV",
                        color = AccentIce,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Divider(color = SpaceCardBorder, modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "SimulateUz provides full 3D rocket flight mechanics, orbital force integration, satellite conjunction analysis, payload cost optimization, and mission flight reporting.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            },
            containerColor = SpaceSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBackground),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Rocket,
                                contentDescription = null,
                                tint = AccentIce,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SIMULATEUZ",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Creator Badge Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SpaceSurfaceVariant,
                            border = BorderStroke(1.dp, SpaceCardBorder),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable { showCreatorDialog = true }
                                .testTag("creator_badge_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Creator: SAIDMUXAMMADXON ODILOV",
                                    color = AccentGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpaceSurface,
                    titleContentColor = TextPrimary
                ),
                modifier = Modifier.testTag("app_top_bar")
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SpaceSurface,
                contentColor = AccentIce,
                tonalElevation = 6.dp,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                NavScreen.values().forEach { screen ->
                    val selected = currentScreen == screen
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = screen.label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SpaceBackground,
                            selectedTextColor = AccentIce,
                            indicatorColor = AccentIce,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            NavScreen.LAUNCH -> {
                LaunchSimulatorScreen(
                    currentRocket = activeRocket,
                    onOpenReports = { tel ->
                        latestTelemetry = tel
                        currentScreen = NavScreen.REPORTS
                    },
                    onOpenAdvisor = {
                        currentScreen = NavScreen.ADVISOR
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavScreen.ADVISOR -> {
                PayloadCostAdvisorScreen(
                    onSelectVehicleForLaunch = { chosenRocket ->
                        activeRocket = chosenRocket
                        currentScreen = NavScreen.LAUNCH
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavScreen.SATELLITES -> {
                SatelliteTrackerScreen(
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavScreen.REPORTS -> {
                FlightReportsHistoryScreen(
                    currentTelemetry = latestTelemetry,
                    currentRocket = activeRocket,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavScreen.ASSEMBLY -> {
                RocketAssemblyScreen(
                    currentRocket = activeRocket,
                    onSaveRocketDesign = { design ->
                        activeRocket = design
                        currentScreen = NavScreen.LAUNCH
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

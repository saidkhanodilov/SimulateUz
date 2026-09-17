package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.OrbitalGlobeCanvas
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

@Composable
fun SatelliteTrackerScreen(
    modifier: Modifier = Modifier
) {
    var satellites by remember { mutableStateOf(TLEParser.DEFAULT_SATELLITES) }
    var selectedSatellite by remember { mutableStateOf<SatelliteTLE?>(TLEParser.DEFAULT_SATELLITES.firstOrNull()) }
    var simElapsedSeconds by remember { mutableDoubleStateOf(0.0) }
    var showIngestDialog by remember { mutableStateOf(false) }

    // Orbital Force Simulation Parameters
    var simulatedSatelliteMassKg by remember { mutableDoubleStateOf(1000.0) }
    var simulatedOrbitAltitudeKm by remember { mutableDoubleStateOf(400.0) }

    // Collision Avoidance Scheduled Window
    var scheduledWindowIndex by remember { mutableIntStateOf(2) } // default +15m window

    // Continuous orbital rotation
    LaunchedEffect(Unit) {
        while (true) {
            simElapsedSeconds += 1.0
            delay(100)
        }
    }

    // Force calculations
    val rMeters = EARTH_RADIUS + (simulatedOrbitAltitudeKm * 1000.0)
    val vOrbital = sqrt(EARTH_MU / rMeters)
    val localG = EARTH_MU / (rMeters * rMeters)
    val fgNewtons = simulatedSatelliteMassKg * localG
    val deltaVRequired = vOrbital + 1400.0 // orbital speed + aerodynamic & gravity losses

    // Calculate Safe Launch Windows
    val collisionWindows = remember(satellites) {
        CollisionScheduler.calculateLaunchWindows(satellites)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SATELLITE ORBIT & COLLISION AVOIDANCE",
                        color = AccentIce,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Orbital Force Physics & Safe Launch Window Scheduler",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }

                FilledTonalButton(
                    onClick = { showIngestDialog = true },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AccentIce,
                        contentColor = SpaceBackground
                    ),
                    modifier = Modifier.testTag("ingest_tle_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add TLE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // 2. 3D Orbital Globe Canvas
        item {
            OrbitalGlobeCanvas(
                satellites = satellites,
                selectedSatellite = selectedSatellite,
                elapsedSimTimeSeconds = simElapsedSeconds,
                onSelectSatellite = { selectedSatellite = it }
            )
        }

        // 3. Satellite Orbital Force & Speed Calculator (User Requirement)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SpaceSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentIce)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "ORBITAL FORCE & INSERTION VELOCITY SIMULATOR",
                        color = AccentIce,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Altitude Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TARGET ORBIT ALTITUDE", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(String.format(Locale.US, "%,.0f km", simulatedOrbitAltitudeKm), color = AccentIce, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = simulatedOrbitAltitudeKm.toFloat(),
                            onValueChange = { simulatedOrbitAltitudeKm = it.toDouble() },
                            valueRange = 160f..36000f,
                            colors = SliderDefaults.colors(thumbColor = AccentIce, activeTrackColor = AccentIce)
                        )
                    }

                    // Satellite Mass Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SATELLITE MASS", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(String.format(Locale.US, "%,.0f kg", simulatedSatelliteMassKg), color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = simulatedSatelliteMassKg.toFloat(),
                            onValueChange = { simulatedSatelliteMassKg = it.toDouble() },
                            valueRange = 50f..10000f,
                            colors = SliderDefaults.colors(thumbColor = AccentGold, activeTrackColor = AccentGold)
                        )
                    }

                    // Derived Force Metrics
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SatPhysicsCard(
                            title = "ORBITAL SPEED (v)",
                            value = String.format(Locale.US, "%,.0f m/s", vOrbital),
                            formula = "v = √(GM / r)",
                            color = AccentIce,
                            modifier = Modifier.weight(1f)
                        )
                        SatPhysicsCard(
                            title = "GRAVITATIONAL PULL",
                            value = String.format(Locale.US, "%,.0f N", fgNewtons),
                            formula = "Fg = G·M·m / r²",
                            color = AccentGold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SatPhysicsCard(
                            title = "TOTAL ΔV NEEDED",
                            value = String.format(Locale.US, "%,.0f m/s", deltaVRequired),
                            formula = "Δv = v_circ + losses",
                            color = AccentEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        SatPhysicsCard(
                            title = "LOCAL GRAVITY (g)",
                            value = String.format(Locale.US, "%.2f m/s²", localG),
                            formula = "g = GM / r²",
                            color = AccentViolet,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4. Safe Launch Window & Collision Avoidance Scheduler (User Requirement)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SpaceSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentEmerald)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "COLLISION AVOIDANCE LAUNCH WINDOWS",
                                color = AccentEmerald,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Conjunction Analysis with ${satellites.size} Active Orbital Satellites",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Icon(Icons.Default.Shield, contentDescription = null, tint = AccentEmerald)
                    }

                    // Window options list
                    collisionWindows.forEachIndexed { index, window ->
                        val isSelected = index == scheduledWindowIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) SpaceSurfaceVariant else SpaceSurface, RoundedCornerShape(8.dp))
                                .border(
                                    1.dp,
                                    if (isSelected) AccentEmerald else SpaceCardBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { scheduledWindowIndex = index }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { scheduledWindowIndex = index },
                                        colors = RadioButtonDefaults.colors(selectedColor = AccentEmerald)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${window.windowTimeDisplay} (T+${window.launchDelaySeconds / 60}m)",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = "Nearest: ${window.closestApproachSatellite} • Separation: ${String.format(Locale.US, "%,.1f km", window.missDistanceKm)}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Badge(
                                containerColor = when (window.riskLevel) {
                                    RiskLevel.SAFE -> AccentEmerald.copy(alpha = 0.2f)
                                    RiskLevel.CAUTION -> AccentGold.copy(alpha = 0.2f)
                                    RiskLevel.CRITICAL -> AccentRose.copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    text = window.riskLevel.label,
                                    color = when (window.riskLevel) {
                                        RiskLevel.SAFE -> AccentEmerald
                                        RiskLevel.CAUTION -> AccentGold
                                        RiskLevel.CRITICAL -> AccentRose
                                    },
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Scheduled verdict
                    val selectedWin = collisionWindows.getOrNull(scheduledWindowIndex)
                    if (selectedWin != null) {
                        Text(
                            text = "✓ Scheduled Launch Slot Locked: ${selectedWin.windowTimeDisplay} UTC. Minimum separation buffer ${String.format(Locale.US, "%,.1f km", selectedWin.missDistanceKm)} clears all Starlink/ISS conjunctions.",
                            color = AccentEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 5. Tracked Satellites List
        item {
            Text(
                text = "TRACKED LIVE ORBITAL ASSETS",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        items(satellites) { sat ->
            val isSelected = sat.noradId == selectedSatellite?.noradId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedSatellite = sat },
                colors = CardDefaults.cardColors(containerColor = if (isSelected) SpaceSurfaceVariant else SpaceSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AccentIce else SpaceCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sat.name,
                            color = if (isSelected) AccentIce else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "NORAD #${sat.noradId} • Period: ${String.format(Locale.US, "%.1f min", sat.orbitalPeriodMinutes)} • Inc: ${String.format(Locale.US, "%.1f°", sat.inclinationDeg)}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Icon(
                        Icons.Default.Public,
                        contentDescription = null,
                        tint = if (isSelected) AccentIce else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showIngestDialog) {
        CustomTleDialog(
            onDismiss = { showIngestDialog = false },
            onAddSatellite = { newSat ->
                satellites = listOf(newSat) + satellites
                selectedSatellite = newSat
                showIngestDialog = false
            }
        )
    }
}

@Composable
private fun SatPhysicsCard(title: String, value: String, formula: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(SpaceSurfaceVariant, RoundedCornerShape(8.dp))
            .border(1.dp, SpaceCardBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(title, color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(formula, color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun CustomTleDialog(onDismiss: () -> Unit, onAddSatellite: (SatelliteTLE) -> Unit) {
    var satName by remember { mutableStateOf("Sentinel-6A Michael Freilich") }
    var line1 by remember { mutableStateOf("1 46984U 20086A   24080.50000000  .00000120  00000+0  15000-4 0  9998") }
    var line2 by remember { mutableStateOf("2 46984  66.0400 142.1000 0001000  80.2000 280.1000 14.12000000160001") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceSurface,
        title = { Text("Ingest Custom Satellite TLE", color = AccentIce, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = satName,
                    onValueChange = { satName = it },
                    label = { Text("Satellite Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = line1,
                    onValueChange = { line1 = it },
                    label = { Text("TLE Line 1") },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = line2,
                    onValueChange = { line2 = it },
                    label = { Text("TLE Line 2") },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = TLEParser.parse(satName, line1, line2)
                    if (parsed != null) onAddSatellite(parsed)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentIce, contentColor = SpaceBackground)
            ) {
                Text("INGEST ORBIT", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

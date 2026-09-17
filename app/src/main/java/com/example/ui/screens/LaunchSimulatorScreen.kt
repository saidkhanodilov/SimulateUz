package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.RocketCanvas3D
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun LaunchSimulatorScreen(
    currentRocket: RocketDesign,
    onOpenReports: (FlightTelemetry) -> Unit,
    onOpenAdvisor: () -> Unit,
    modifier: Modifier = Modifier
) {
    var telemetry by remember { mutableStateOf(FlightTelemetry()) }
    var isSimulating by remember { mutableStateOf(false) }
    var timeWarp by remember { mutableIntStateOf(1) }
    var autoGravityTurn by remember { mutableStateOf(true) }
    var flightLogs by remember { mutableStateOf(listOf("T-00:00: Pre-launch checks verified. Vehicle fueled.")) }

    var trajectoryPoints by remember { mutableStateOf(listOf<Offset>()) }

    // Simulation loop running at 60 FPS
    LaunchedEffect(isSimulating, timeWarp, autoGravityTurn, currentRocket) {
        val dt = 1.0 / 60.0
        while (isSimulating) {
            val stepDt = dt * timeWarp

            val activeStage = if (!telemetry.isSeparated && currentRocket.stages.isNotEmpty()) {
                currentRocket.stages[0]
            } else if (currentRocket.stages.size > 1) {
                currentRocket.stages[1]
            } else {
                currentRocket.stages.firstOrNull() ?: RocketPresets.FALCON_9.stages[0]
            }

            val targetPitch = if (autoGravityTurn) {
                val alt = telemetry.altitudeKm
                when {
                    alt < 1.2 -> 90.0
                    alt < 75.0 -> (90.0 - ((alt - 1.2) / 73.8) * 78.0).coerceIn(12.0, 90.0)
                    else -> 10.0
                }
            } else {
                telemetry.pitchDeg
            }

            val updatedPitchState = telemetry.copy(pitchDeg = targetPitch)

            val nextState = OrbitalIntegrator.rk4Step(
                current = updatedPitchState,
                dt = stepDt,
                thrustMaxNewtons = activeStage.thrustNewtons,
                ispSeconds = activeStage.ispSeconds,
                diameterMeters = currentRocket.diameterMeters
            )

            if (telemetry.timeSeconds < 0.1 && nextState.timeSeconds >= 0.1) {
                flightLogs = listOf("T+00:01: LIFTOFF! Engine ignition confirmed.") + flightLogs
            }
            if (telemetry.machNumber < 1.0 && nextState.machNumber >= 1.0) {
                flightLogs = listOf(String.format(Locale.US, "T+%.0fs: Transonic Mach 1.0 boundary cleared.", nextState.timeSeconds)) + flightLogs
            }
            if (telemetry.currentDynamicPressurePa < 28000 && nextState.currentDynamicPressurePa >= 28000) {
                flightLogs = listOf(String.format(Locale.US, "T+%.0fs: MAX-Q reached (%,.0f Pa aerodynamic load).", nextState.timeSeconds, nextState.maxQPressurePa)) + flightLogs
            }
            if (telemetry.altitudeKm < 100.0 && nextState.altitudeKm >= 100.0) {
                flightLogs = listOf(String.format(Locale.US, "T+%.0fs: KÁRMÁN LINE! Outer space entry (100 km).", nextState.timeSeconds)) + flightLogs
            }

            telemetry = nextState

            if (telemetry.timeSeconds % 2.0 < stepDt * 2.0) {
                val ptX = (170.0 + (telemetry.position.x / 1000.0) * 0.05).toFloat()
                val ptY = (280.0 - (telemetry.altitudeKm) * 1.5).toFloat()
                trajectoryPoints = (trajectoryPoints + Offset(ptX, ptY)).takeLast(40)
            }

            delay(16)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "3D LAUNCH FLIGHT COCKPIT",
                        color = AccentIce,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Creator: SAIDMUXAMMADXON ODILOV • ${currentRocket.name}",
                        color = AccentGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Payload: ${String.format(Locale.US, "%,.0f kg", currentRocket.payloadMassKg)}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = onOpenAdvisor,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SpaceSurfaceVariant,
                            contentColor = AccentGold
                        ),
                        modifier = Modifier.testTag("payload_advisor_quick_btn")
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cost", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = { onOpenReports(telemetry) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SpaceSurfaceVariant,
                            contentColor = AccentIce
                        ),
                        modifier = Modifier.testTag("flight_reports_button")
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Report", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. True 3D Rocket Flight Arena
        item {
            RocketCanvas3D(
                telemetry = telemetry,
                trajectoryHistory = trajectoryPoints
            )
        }

        // 3. Telemetry HUD
        item {
            FlightHudGrid(telemetry = telemetry, currentRocket = currentRocket)
        }

        // 4. Primary Launch & Staging Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SpaceSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "PROPULSION & STAGING CONTROLS",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Throttle Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ENGINE THROTTLE", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(String.format(Locale.US, "%.0f%%", telemetry.throttlePercent * 100), color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = telemetry.throttlePercent.toFloat(),
                            onValueChange = { telemetry = telemetry.copy(throttlePercent = it.toDouble()) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = AccentGold,
                                activeTrackColor = AccentGold,
                                inactiveTrackColor = SpaceSurfaceVariant
                            ),
                            modifier = Modifier.testTag("throttle_slider")
                        )
                    }

                    // Pitch Angle Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PITCH GIMBAL ANGLE", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(String.format(Locale.US, "%.1f°", telemetry.pitchDeg), color = AccentIce, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = telemetry.pitchDeg.toFloat(),
                            onValueChange = {
                                autoGravityTurn = false
                                telemetry = telemetry.copy(pitchDeg = it.toDouble())
                            },
                            valueRange = 0f..90f,
                            colors = SliderDefaults.colors(
                                thumbColor = AccentIce,
                                activeTrackColor = AccentIce,
                                inactiveTrackColor = SpaceSurfaceVariant
                            ),
                            modifier = Modifier.testTag("pitch_slider")
                        )
                    }

                    // Auto Gravity Turn Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto Gravity Turn Program", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Autonomously tilts stack along flight vector", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = autoGravityTurn,
                            onCheckedChange = { autoGravityTurn = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SpaceBackground,
                                checkedTrackColor = AccentIce
                            ),
                            modifier = Modifier.testTag("gravity_turn_switch")
                        )
                    }

                    // Action Buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { isSimulating = !isSimulating },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("launch_toggle_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSimulating) AccentRose else AccentEmerald,
                                contentColor = SpaceBackground
                            )
                        ) {
                            Icon(
                                if (isSimulating) Icons.Default.Pause else Icons.Default.RocketLaunch,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isSimulating) "HOLD FLIGHT" else "IGNITE & LAUNCH", fontWeight = FontWeight.Bold)
                        }

                        // Staging Separation Button
                        Button(
                            onClick = {
                                if (!telemetry.isSeparated && currentRocket.stages.size > 1) {
                                    val s2 = currentRocket.stages[1]
                                    telemetry = telemetry.copy(
                                        isSeparated = true,
                                        currentStageIndex = 1,
                                        currentDryMassKg = s2.dryMassKg + currentRocket.payloadMassKg,
                                        currentFuelKg = s2.fuelMassKg
                                    )
                                    flightLogs = listOf(
                                        String.format(
                                            Locale.US,
                                            "T+%.0fs: STAGE 1 SEPARATION & STAGE 2 VACUUM IGNITION!",
                                            telemetry.timeSeconds
                                        )
                                    ) + flightLogs
                                }
                            },
                            enabled = !telemetry.isSeparated && currentRocket.stages.size > 1,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGold,
                                contentColor = SpaceBackground
                            ),
                            modifier = Modifier.testTag("stage_separation_button")
                        ) {
                            Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("STAGE SEP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Time Warp & Reset Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1, 2, 5, 10).forEach { warp ->
                                FilterChip(
                                    selected = timeWarp == warp,
                                    onClick = { timeWarp = warp },
                                    label = { Text("${warp}x", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentIce,
                                        selectedLabelColor = SpaceBackground
                                    )
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                isSimulating = false
                                val s1 = currentRocket.stages.firstOrNull() ?: RocketPresets.FALCON_9.stages[0]
                                telemetry = FlightTelemetry(
                                    currentDryMassKg = s1.dryMassKg + (if (currentRocket.stages.size > 1) currentRocket.stages[1].totalMassKg else 0.0) + currentRocket.payloadMassKg,
                                    currentFuelKg = s1.fuelMassKg
                                )
                                trajectoryPoints = emptyList()
                                flightLogs = listOf("Launchpad reset. Vehicle re-stacked.")
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            modifier = Modifier.testTag("reset_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RESET", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 5. Mission Flight Log Terminal
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SpaceSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FLIGHT EVENT TELEMETRY LOG",
                            color = AccentEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = String.format(Locale.US, "T+%.1fs", telemetry.timeSeconds),
                            color = AccentIce,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF060910), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        flightLogs.take(5).forEach { log ->
                            Text(
                                text = "› $log",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlightHudGrid(telemetry: FlightTelemetry, currentRocket: RocketDesign) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SpaceSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "FLIGHT DYNAMICS HUD",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HudBox(
                    title = "ALTITUDE",
                    value = String.format(Locale.US, "%.2f km", telemetry.altitudeKm),
                    subtitle = if (telemetry.altitudeKm > 100) "LEO Space" else "Atmosphere",
                    color = AccentIce,
                    modifier = Modifier.weight(1f)
                )
                HudBox(
                    title = "VELOCITY",
                    value = String.format(Locale.US, "%.0f m/s", telemetry.speedMetersPerSec),
                    subtitle = String.format(Locale.US, "Mach %.2f", telemetry.machNumber),
                    color = AccentEmerald,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HudBox(
                    title = "DYNAMIC PRESS (q)",
                    value = String.format(Locale.US, "%.0f Pa", telemetry.currentDynamicPressurePa),
                    subtitle = String.format(Locale.US, "Max: %,.0f Pa", telemetry.maxQPressurePa),
                    color = AccentGold,
                    modifier = Modifier.weight(1f)
                )
                HudBox(
                    title = "ACCELERATION",
                    value = String.format(Locale.US, "%.2f G", telemetry.gForce),
                    subtitle = String.format(Locale.US, "Mass: %,.0f kg", telemetry.totalMassKg),
                    color = AccentViolet,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HudBox(
                    title = "EST. APOAPSIS",
                    value = String.format(Locale.US, "%.1f km", telemetry.apoapsisKm),
                    subtitle = "Target: 300 km",
                    color = AccentIce,
                    modifier = Modifier.weight(1f)
                )
                HudBox(
                    title = "FUEL REMAINING",
                    value = String.format(Locale.US, "%,.0f kg", telemetry.currentFuelKg),
                    subtitle = if (telemetry.currentFuelKg > 0) "Thrusting" else "Depleted",
                    color = if (telemetry.currentFuelKg > 0) AccentEmerald else AccentRose,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HudBox(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(SpaceSurfaceVariant, RoundedCornerShape(8.dp))
            .border(1.dp, SpaceCardBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(title, color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(subtitle, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

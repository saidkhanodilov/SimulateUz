package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun RocketAssemblyScreen(
    currentRocket: RocketDesign,
    onSaveRocketDesign: (RocketDesign) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPresetName by remember { mutableStateOf(currentRocket.name) }

    // Editable rocket properties
    var payloadMassKg by remember { mutableDoubleStateOf(currentRocket.payloadMassKg) }

    // Stage 1 properties
    var s1DryMass by remember { mutableDoubleStateOf(currentRocket.stages.getOrNull(0)?.dryMassKg ?: 22000.0) }
    var s1FuelMass by remember { mutableDoubleStateOf(currentRocket.stages.getOrNull(0)?.fuelMassKg ?: 418000.0) }
    var s1ThrustKn by remember { mutableDoubleStateOf((currentRocket.stages.getOrNull(0)?.thrustNewtons ?: 7600000.0) / 1000.0) }
    var s1Isp by remember { mutableDoubleStateOf(currentRocket.stages.getOrNull(0)?.ispSeconds ?: 311.0) }
    var s1EngineName by remember { mutableStateOf(currentRocket.stages.getOrNull(0)?.engineName ?: "9x Merlin 1D") }

    // Stage 2 properties
    var s2DryMass by remember { mutableDoubleStateOf(currentRocket.stages.getOrNull(1)?.dryMassKg ?: 4000.0) }
    var s2FuelMass by remember { mutableDoubleStateOf(currentRocket.stages.getOrNull(1)?.fuelMassKg ?: 92000.0) }
    var s2ThrustKn by remember { mutableDoubleStateOf((currentRocket.stages.getOrNull(1)?.thrustNewtons ?: 981000.0) / 1000.0) }
    var s2Isp by remember { mutableDoubleStateOf(currentRocket.stages.getOrNull(1)?.ispSeconds ?: 348.0) }

    // Dynamically computed design
    val assembledStages = listOf(
        RocketStage(
            stageName = "Stage 1 Booster",
            dryMassKg = s1DryMass,
            fuelMassKg = s1FuelMass,
            thrustNewtons = s1ThrustKn * 1000.0,
            ispSeconds = s1Isp,
            engineName = s1EngineName
        ),
        RocketStage(
            stageName = "Stage 2 Vacuum",
            dryMassKg = s2DryMass,
            fuelMassKg = s2FuelMass,
            thrustNewtons = s2ThrustKn * 1000.0,
            ispSeconds = s2Isp,
            engineName = "Vacuum Engine"
        )
    )

    val customDesign = RocketDesign(
        name = selectedPresetName,
        stages = assembledStages,
        payloadMassKg = payloadMassKg
    )

    // Mathematical calculations
    val s2DeltaV = assembledStages[1].computeDeltaV(payloadMassKg)
    val s1PayloadEquivalent = assembledStages[1].totalMassKg + payloadMassKg
    val s1DeltaV = assembledStages[0].computeDeltaV(s1PayloadEquivalent)
    val totalDeltaV = s1DeltaV + s2DeltaV

    val liftoffTwr = (s1ThrustKn * 1000.0) / (customDesign.totalLiftOffMass * STANDARD_G0)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "ROCKET ASSEMBLY & TSIOLKOVSKY MATH",
                    color = CyanPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Multistage Propulsive Mechanics & Delta-V Budgeting",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Vehicle Preset Selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SpaceSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "SELECT VEHICLE ARCHITECTURE PRESET",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RocketPresets.ALL_PRESETS.forEach { preset ->
                            FilterChip(
                                selected = selectedPresetName == preset.name,
                                onClick = {
                                    selectedPresetName = preset.name
                                    payloadMassKg = preset.payloadMassKg
                                    val s1 = preset.stages[0]
                                    s1DryMass = s1.dryMassKg
                                    s1FuelMass = s1.fuelMassKg
                                    s1ThrustKn = s1.thrustNewtons / 1000.0
                                    s1Isp = s1.ispSeconds
                                    s1EngineName = s1.engineName

                                    if (preset.stages.size > 1) {
                                        val s2 = preset.stages[1]
                                        s2DryMass = s2.dryMassKg
                                        s2FuelMass = s2.fuelMassKg
                                        s2ThrustKn = s2.thrustNewtons / 1000.0
                                        s2Isp = s2.ispSeconds
                                    }
                                    onSaveRocketDesign(preset)
                                },
                                label = { Text(preset.name.split(" ")[0], fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimary,
                                    selectedLabelColor = SpaceBackground
                                )
                            )
                        }
                    }
                }
            }
        }

        // Delta-V & TWR Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SpaceSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL VEHICLE DELTA-V (Δv)",
                            color = CyanPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = String.format(Locale.US, "%,.0f m/s", totalDeltaV),
                            color = NeonGreenTelemetry,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricBox(
                            label = "LIFTOFF TWR",
                            value = String.format(Locale.US, "%.2f", liftoffTwr),
                            status = if (liftoffTwr > 1.2) "Flight Ready" else "Underpowered (<1.2)",
                            color = if (liftoffTwr > 1.2) NeonGreenTelemetry else AlertRed,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBox(
                            label = "TOTAL WET MASS",
                            value = String.format(Locale.US, "%,.0f t", customDesign.totalLiftOffMass / 1000.0),
                            status = String.format(Locale.US, "Dry: %,.0f t", (s1DryMass + s2DryMass + payloadMassKg) / 1000.0),
                            color = AmberThrust,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Target Orbit Capability Meter
                    OrbitCapabilityIndicator(totalDeltaV = totalDeltaV)
                }
            }
        }

        // Tsiolkovsky Educational Calculus Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SpaceSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "THE TSIOLKOVSKY ROCKET EQUATION",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "Δv = Isp · g₀ · ln(m_initial / m_final)",
                        color = AmberThrust,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "Calculus Derivation:\n" +
                                "Newton's Second Law with variable mass: F = dp/dt. Conservation of momentum gives:\n" +
                                "m · dv = -v_exhaust · dm\n" +
                                "Integrating both sides: ∫ dv = -g₀·Isp · ∫ (1/m) dm\n" +
                                "Yields: Δv = Isp · g₀ · [ln(m₀) - ln(mf)] = Isp · g₀ · ln(m₀ / mf)\n\n" +
                                "• Stage 1 Δv: ${String.format(Locale.US, "%,.0f", s1DeltaV)} m/s (Atmosphere & Boost)\n" +
                                "• Stage 2 Δv: ${String.format(Locale.US, "%,.0f", s2DeltaV)} m/s (Vacuum Orbital Insertion)",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Stage 1 Configuration Controls
        item {
            StageConfigCard(
                stageTitle = "STAGE 1 (BOOSTER)",
                fuelMass = s1FuelMass,
                onFuelChange = { s1FuelMass = it },
                dryMass = s1DryMass,
                onDryMassChange = { s1DryMass = it },
                thrustKn = s1ThrustKn,
                onThrustChange = { s1ThrustKn = it },
                isp = s1Isp,
                onIspChange = { s1Isp = it },
                deltaV = s1DeltaV
            )
        }

        // Stage 2 Configuration Controls
        item {
            StageConfigCard(
                stageTitle = "STAGE 2 (VACUUM UPPER STAGE)",
                fuelMass = s2FuelMass,
                onFuelChange = { s2FuelMass = it },
                dryMass = s2DryMass,
                onDryMassChange = { s2DryMass = it },
                thrustKn = s2ThrustKn,
                onThrustChange = { s2ThrustKn = it },
                isp = s2Isp,
                onIspChange = { s2Isp = it },
                deltaV = s2DeltaV
            )
        }

        // Payload Mass Slider
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SpaceSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("PAYLOAD MASS (SATELLITE / CREW)", color = TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text(String.format(Locale.US, "%,.0f kg", payloadMassKg), color = CyanPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = payloadMassKg.toFloat(),
                        onValueChange = { payloadMassKg = it.toDouble() },
                        valueRange = 0f..50000f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanPrimary,
                            activeTrackColor = CyanPrimary,
                            inactiveTrackColor = SpaceSurfaceVariant
                        )
                    )
                }
            }
        }

        // Apply Design Button
        item {
            Button(
                onClick = { onSaveRocketDesign(customDesign) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_rocket_design_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = SpaceBackground
                )
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("APPLY TO LAUNCH SIMULATOR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MetricBox(label: String, value: String, status: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(SpaceSurfaceVariant, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(label, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(status, color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun OrbitCapabilityIndicator(totalDeltaV: Double) {
    val leoThreshold = 9300.0
    val gtoThreshold = 11800.0

    val progress = (totalDeltaV / 13000.0).coerceIn(0.0, 1.0).toFloat()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("MISSION ORBITAL CAPABILITY", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text(
                text = when {
                    totalDeltaV >= gtoThreshold -> "GTO / Lunar Transfer Ready 🌕"
                    totalDeltaV >= leoThreshold -> "LEO Orbit Insertion Ready 🌍"
                    else -> "Suborbital / Insufficient Δv ⚠️"
                },
                color = if (totalDeltaV >= leoThreshold) NeonGreenTelemetry else AlertRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = if (totalDeltaV >= leoThreshold) NeonGreenTelemetry else AmberThrust,
            trackColor = SpaceSurfaceVariant
        )
    }
}

@Composable
private fun StageConfigCard(
    stageTitle: String,
    fuelMass: Double,
    onFuelChange: (Double) -> Unit,
    dryMass: Double,
    onDryMassChange: (Double) -> Unit,
    thrustKn: Double,
    onThrustChange: (Double) -> Unit,
    isp: Double,
    onIspChange: (Double) -> Unit,
    deltaV: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SpaceSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stageTitle, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(String.format(Locale.US, "Δv: %,.0f m/s", deltaV), color = AmberThrust, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            // Fuel Mass
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Propellant (Fuel) Mass", color = TextSecondary, fontSize = 11.sp)
                    Text(String.format(Locale.US, "%,.0f kg", fuelMass), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = fuelMass.toFloat(),
                    onValueChange = { onFuelChange(it.toDouble()) },
                    valueRange = 1000f..500000f,
                    colors = SliderDefaults.colors(thumbColor = AmberThrust, activeTrackColor = AmberThrust)
                )
            }

            // Dry Mass
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Structural Dry Mass", color = TextSecondary, fontSize = 11.sp)
                    Text(String.format(Locale.US, "%,.0f kg", dryMass), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = dryMass.toFloat(),
                    onValueChange = { onDryMassChange(it.toDouble()) },
                    valueRange = 500f..50000f,
                    colors = SliderDefaults.colors(thumbColor = CyanPrimary, activeTrackColor = CyanPrimary)
                )
            }

            // Thrust
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Engine Thrust", color = TextSecondary, fontSize = 11.sp)
                    Text(String.format(Locale.US, "%,.0f kN", thrustKn), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = thrustKn.toFloat(),
                    onValueChange = { onThrustChange(it.toDouble()) },
                    valueRange = 50f..10000f,
                    colors = SliderDefaults.colors(thumbColor = AmberThrust, activeTrackColor = AmberThrust)
                )
            }

            // Specific Impulse (Isp)
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Specific Impulse (Isp)", color = TextSecondary, fontSize = 11.sp)
                    Text(String.format(Locale.US, "%.0f seconds", isp), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = isp.toFloat(),
                    onValueChange = { onIspChange(it.toDouble()) },
                    valueRange = 200f..480f,
                    colors = SliderDefaults.colors(thumbColor = NeonGreenTelemetry, activeTrackColor = NeonGreenTelemetry)
                )
            }
        }
    }
}

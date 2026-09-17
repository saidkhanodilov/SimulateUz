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
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@Composable
fun PayloadCostAdvisorScreen(
    onSelectVehicleForLaunch: (RocketDesign) -> Unit,
    modifier: Modifier = Modifier
) {
    var payloadWeightKg by remember { mutableDoubleStateOf(5200.0) }
    var selectedOrbit by remember { mutableStateOf(TargetOrbit.LEO) }

    val evaluation = remember(payloadWeightKg, selectedOrbit) {
        RocketMarketDatabase.evaluatePayload(payloadWeightKg, selectedOrbit)
    }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }
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
            Column {
                Text(
                    text = "PAYLOAD & LAUNCH COST ADVISOR",
                    color = AccentIce,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Commercial Mass Evaluation & Vehicle Recommendation Engine",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // 2. Payload Mass Configurator Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SpaceSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INTENDED PAYLOAD MASS",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = String.format(Locale.US, "%,.0f kg", payloadWeightKg),
                            color = AccentGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Mass Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Pair("CubeSat", 50.0),
                            Pair("SmallSat", 300.0),
                            Pair("Commsat", 5500.0),
                            Pair("Starlink", 16000.0),
                            Pair("Module", 45000.0)
                        ).forEach { (label, mass) ->
                            FilterChip(
                                selected = abs(payloadWeightKg - mass) < 1.0,
                                onClick = { payloadWeightKg = mass },
                                label = { Text(label, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentGold,
                                    selectedLabelColor = SpaceBackground
                                )
                            )
                        }
                    }

                    // Slider
                    Slider(
                        value = payloadWeightKg.toFloat(),
                        onValueChange = { payloadWeightKg = it.toDouble() },
                        valueRange = 10f..65000f,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentGold,
                            activeTrackColor = AccentGold,
                            inactiveTrackColor = SpaceSurfaceVariant
                        ),
                        modifier = Modifier.testTag("payload_mass_slider")
                    )

                    // Target Orbit Selector
                    Text(
                        text = "DESTINATION ORBIT",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TargetOrbit.values().take(3).forEach { orbit ->
                            FilterChip(
                                selected = selectedOrbit == orbit,
                                onClick = { selectedOrbit = orbit },
                                label = { Text(orbit.title.split(" ")[0], fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentIce,
                                    selectedLabelColor = SpaceBackground
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TargetOrbit.values().drop(3).forEach { orbit ->
                            FilterChip(
                                selected = selectedOrbit == orbit,
                                onClick = { selectedOrbit = orbit },
                                label = { Text(orbit.title.split(" ")[0], fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentIce,
                                    selectedLabelColor = SpaceBackground
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Recommended Rocket & Financial Breakdown Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SpaceSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "RECOMMENDED LAUNCH VEHICLE",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = evaluation.recommendedVehicle.name,
                                color = AccentGold,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Operator: ${evaluation.recommendedVehicle.provider}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "ESTIMATED BUDGET",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (evaluation.estimatedCostUsd > 0) currencyFormatter.format(evaluation.estimatedCostUsd) else "N/A",
                                color = AccentEmerald,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (evaluation.costPerKg > 0) String.format(Locale.US, "$%,.0f / kg", evaluation.costPerKg) else "",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Feasibility Note
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SpaceSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "ℹ️ ${evaluation.feasibilityNote}",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    // Orbital Force & Physics Calculations
                    Text(
                        text = "ORBITAL FORCE & ENERGY REQUIREMENTS",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdvisorMetric(
                            label = "ORBITAL SPEED (v)",
                            value = String.format(Locale.US, "%,.0f m/s", evaluation.requiredSpeedMps),
                            subtitle = "Circular orbital velocity",
                            modifier = Modifier.weight(1f)
                        )
                        AdvisorMetric(
                            label = "GRAVITY FORCE (Fg)",
                            value = String.format(Locale.US, "%,.0f N", evaluation.requiredForceNewtons),
                            subtitle = "Downward orbital pull",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Button to load into 3D Simulator
                    Button(
                        onClick = {
                            val presetToUse = when (evaluation.recommendedVehicle.id) {
                                "electron" -> RocketPresets.ELECTRON
                                "starship" -> RocketPresets.STARSHIP
                                else -> RocketPresets.FALCON_9
                            }
                            val updatedRocket = presetToUse.copy(payloadMassKg = payloadWeightKg)
                            onSelectVehicleForLaunch(updatedRocket)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("load_recommended_rocket_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentIce,
                            contentColor = SpaceBackground
                        )
                    ) {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LOAD THIS VEHICLE INTO 3D LAUNCH SIMULATOR", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. Commercial Market Comparison Table
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SpaceSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "GLOBAL LAUNCH VEHICLE COMPARISON",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    RocketMarketDatabase.ALL_VEHICLES.forEach { vehicle ->
                        val capacity = vehicle.getCapacityForOrbit(selectedOrbit)
                        val cost = vehicle.calculateCost(payloadWeightKg, selectedOrbit)
                        val isCapable = cost > 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SpaceSurfaceVariant, RoundedCornerShape(8.dp))
                                .border(1.dp, if (vehicle.id == evaluation.recommendedVehicle.id) AccentGold else SpaceCardBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = vehicle.name,
                                    color = if (vehicle.id == evaluation.recommendedVehicle.id) AccentGold else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${vehicle.provider} • Max Orbit Payload: ${String.format(Locale.US, "%,.0f kg", capacity)}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isCapable) currencyFormatter.format(cost) else "OVERWEIGHT",
                                    color = if (isCapable) AccentEmerald else AccentRose,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (isCapable) "Status: Fit" else "Exceeds Cap",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvisorMetric(label: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(SpaceSurfaceVariant, RoundedCornerShape(8.dp))
            .border(1.dp, SpaceCardBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(label, color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = AccentIce, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(subtitle, color = TextSecondary, fontSize = 10.sp)
        }
    }
}

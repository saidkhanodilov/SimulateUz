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
import com.example.model.FlightTelemetry
import com.example.model.MissionHistoryRepository
import com.example.model.MissionReport
import com.example.model.RocketDesign
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FlightReportsHistoryScreen(
    currentTelemetry: FlightTelemetry,
    currentRocket: RocketDesign,
    modifier: Modifier = Modifier
) {
    var reports by remember { mutableStateOf(MissionHistoryRepository.getAllReports()) }
    var selectedReport by remember { mutableStateOf<MissionReport?>(reports.firstOrNull()) }
    var filterSuccessOnly by remember { mutableStateOf(false) }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }
    }

    val displayedReports = remember(reports, filterSuccessOnly) {
        if (filterSuccessOnly) reports.filter { it.isOrbitAchieved } else reports
    }

    // High level stats
    val totalMissions = reports.size
    val successfulMissions = reports.count { it.isOrbitAchieved }
    val successRate = if (totalMissions > 0) (successfulMissions * 100) / totalMissions else 0
    val totalPayloadMassTons = reports.sumOf { it.payloadKg } / 1000.0

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
                        text = "FLIGHT REPORTS & MISSION LOGS",
                        color = AccentIce,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Post-Flight Telemetry Records & Historical Archive",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }

                // Log Current Flight Button
                FilledTonalButton(
                    onClick = {
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                        val nowStr = sdf.format(Date()) + " UTC"
                        val isOrbit = currentTelemetry.altitudeKm > 150.0 && currentTelemetry.speedMetersPerSec > 7000.0

                        val newReport = MissionReport(
                            id = "SIM-${System.currentTimeMillis() % 100000}",
                            missionName = "${currentRocket.name} Live Run",
                            dateFormatted = nowStr,
                            rocketName = currentRocket.name,
                            payloadKg = currentRocket.payloadMassKg,
                            maxAltitudeKm = currentTelemetry.altitudeKm,
                            maxVelocityMps = currentTelemetry.speedMetersPerSec,
                            maxDynamicPressurePa = currentTelemetry.maxQPressurePa,
                            isOrbitAchieved = isOrbit,
                            estimatedCostUsd = if (currentRocket.stages.isNotEmpty()) 67000000.0 else 7500000.0,
                            flightDurationSec = currentTelemetry.timeSeconds,
                            flightEvents = listOf(
                                "T+00:00: Liftoff initiated",
                                "T+${String.format("%.0f", currentTelemetry.timeSeconds * 0.2)}s: Max-Q passed (%,.0f Pa)".format(currentTelemetry.maxQPressurePa),
                                "T+${String.format("%.0f", currentTelemetry.timeSeconds * 0.5)}s: Stage 1 cutoff & staging",
                                "T+${String.format("%.0f", currentTelemetry.timeSeconds)}s: Mission completed at %.1f km altitude".format(currentTelemetry.altitudeKm)
                            )
                        )

                        MissionHistoryRepository.addReport(newReport)
                        reports = MissionHistoryRepository.getAllReports()
                        selectedReport = newReport
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AccentEmerald,
                        contentColor = SpaceBackground
                    ),
                    modifier = Modifier.testTag("save_current_flight_report_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Flight", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // Creator Credit Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SpaceSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SYSTEM ARCHITECT & CREATOR",
                            color = AccentGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "SAIDMUXAMMADXON ODILOV",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 2. Mission Statistics Overview Cards
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SpaceSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "FLEET LAUNCH PERFORMANCE STATS",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReportStatBox("TOTAL LAUNCHES", "$totalMissions Missions", AccentIce, Modifier.weight(1f))
                        ReportStatBox("SUCCESS RATE", "$successRate%", if (successRate > 80) AccentEmerald else AccentGold, Modifier.weight(1f))
                        ReportStatBox("PAYLOAD ORBITED", String.format(Locale.US, "%.1f Tons", totalPayloadMassTons), AccentGold, Modifier.weight(1f))
                    }
                }
            }
        }

        // 3. Selected Report Deep Breakdown
        if (selectedReport != null) {
            val rep = selectedReport!!
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SpaceSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (rep.isOrbitAchieved) AccentEmerald else AccentGold)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = rep.missionName.uppercase(),
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${rep.id} • ${rep.dateFormatted}",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Badge(
                                containerColor = if (rep.isOrbitAchieved) AccentEmerald.copy(alpha = 0.2f) else AccentGold.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (rep.isOrbitAchieved) "ORBIT CONFIRMED" else "SUBORBITAL TEST",
                                    color = if (rep.isOrbitAchieved) AccentEmerald else AccentGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReportMetricCell("VEHICLE", rep.rocketName, AccentIce, Modifier.weight(1f))
                            ReportMetricCell("PAYLOAD", String.format(Locale.US, "%,.0f kg", rep.payloadKg), AccentGold, Modifier.weight(1f))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReportMetricCell("PEAK ALTITUDE", String.format(Locale.US, "%.1f km", rep.maxAltitudeKm), AccentIce, Modifier.weight(1f))
                            ReportMetricCell("BURNOUT SPEED", String.format(Locale.US, "%,.0f m/s", rep.maxVelocityMps), AccentEmerald, Modifier.weight(1f))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReportMetricCell("MAX-Q PRESSURE", String.format(Locale.US, "%,.0f Pa", rep.maxDynamicPressurePa), AccentRose, Modifier.weight(1f))
                            ReportMetricCell("MISSION BUDGET", currencyFormatter.format(rep.estimatedCostUsd), AccentGold, Modifier.weight(1f))
                        }

                        // Flight Event Timeline
                        Text(
                            text = "FLIGHT EVENT LOG CHRONOLOGY",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF070B12), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rep.flightEvents.forEach { ev ->
                                Text(
                                    text = "› $ev",
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

        // 4. Filter Chip Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTORICAL MISSION LOGS",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                FilterChip(
                    selected = filterSuccessOnly,
                    onClick = { filterSuccessOnly = !filterSuccessOnly },
                    label = { Text("Orbital Success Only", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentEmerald,
                        selectedLabelColor = SpaceBackground
                    )
                )
            }
        }

        // 5. Reports List
        items(displayedReports) { report ->
            val isSelected = report.id == selectedReport?.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedReport = report },
                colors = CardDefaults.cardColors(containerColor = if (isSelected) SpaceSurfaceVariant else SpaceSurface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) AccentIce else SpaceCardBorder
                )
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
                            text = report.missionName,
                            color = if (isSelected) AccentIce else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${report.dateFormatted} • ${report.rocketName} • Alt: ${String.format(Locale.US, "%.0f km", report.maxAltitudeKm)}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Badge(
                        containerColor = if (report.isOrbitAchieved) AccentEmerald.copy(alpha = 0.2f) else AccentGold.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (report.isOrbitAchieved) "SUCCESS" else "TEST",
                            color = if (report.isOrbitAchieved) AccentEmerald else AccentGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportStatBox(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(SpaceSurfaceVariant, RoundedCornerShape(8.dp))
            .border(1.dp, SpaceCardBorder, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(title, color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun ReportMetricCell(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(SpaceSurfaceVariant, RoundedCornerShape(8.dp))
            .border(1.dp, SpaceCardBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(label, color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

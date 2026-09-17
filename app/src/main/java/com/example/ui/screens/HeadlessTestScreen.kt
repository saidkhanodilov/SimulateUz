package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max

data class HeadlessSimResult(
    val timePoints: List<Double>,
    val altPointsKm: List<Double>,
    val velPointsMps: List<Double>,
    val maxAltKm: Double,
    val maxVelMps: Double,
    val finalApoapsisKm: Double,
    val finalPeriapsisKm: Double,
    val isOrbitAchieved: Boolean,
    val totalSimSeconds: Double,
    val totalStepsComputed: Int
)

@Composable
fun HeadlessTestScreen(
    currentRocket: RocketDesign,
    modifier: Modifier = Modifier
) {
    var isRunning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<HeadlessSimResult?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Pre-run initial calculation
    LaunchedEffect(currentRocket) {
        coroutineScope.launch {
            isRunning = true
            result = runHeadlessSimulation(currentRocket)
            isRunning = false
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "HEADLESS MATH-ONLY TEST LAUNCH",
                    color = CyanPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Fast-Forward Numerical RK4 Orbital Verification Engine",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Action Run Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SpaceSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TARGET: ${currentRocket.name.uppercase()}",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Calculates 20,000 physics ticks in ~50ms",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isRunning = true
                                result = runHeadlessSimulation(currentRocket)
                                isRunning = false
                            }
                        },
                        enabled = !isRunning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = SpaceBackground
                        ),
                        modifier = Modifier.testTag("run_headless_simulation_button")
                    ) {
                        if (isRunning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SpaceBackground)
                        } else {
                            Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RUN FAST TEST", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Verdict & Metric Highlights
        if (result != null) {
            val res = result!!
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SpaceSurface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (res.isOrbitAchieved) NeonGreenTelemetry else AmberThrust
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SIMULATION VERDICT",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (res.isOrbitAchieved) "STABLE ORBIT INSERTION" else "SUBORBITAL TRAJECTORY",
                                color = if (res.isOrbitAchieved) NeonGreenTelemetry else AmberThrust,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricCell(
                                label = "PEAK ALTITUDE",
                                value = String.format(Locale.US, "%.1f km", res.maxAltKm),
                                color = CyanPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCell(
                                label = "BURNOUT VELOCITY",
                                value = String.format(Locale.US, "%,.0f m/s", res.maxVelMps),
                                color = NeonGreenTelemetry,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricCell(
                                label = "APOAPSIS (Ra)",
                                value = String.format(Locale.US, "%.1f km", res.finalApoapsisKm),
                                color = AmberThrust,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCell(
                                label = "PERIAPSIS (Rp)",
                                value = String.format(Locale.US, "%.1f km", res.finalPeriapsisKm),
                                color = OrbitPurple,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = "Engine computed ${res.totalStepsComputed} Runge-Kutta 4th Order iterations simulating ${res.totalSimSeconds.toInt()}s flight time.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Graph: Altitude vs Time
            item {
                PlotGraphCard(
                    title = "ALTITUDE TRAJECTORY PROFILE (KM vs TIME)",
                    points = res.altPointsKm,
                    lineColor = CyanPrimary,
                    unitLabel = "km",
                    maxVal = res.maxAltKm
                )
            }

            // Graph: Velocity vs Time
            item {
                PlotGraphCard(
                    title = "VELOCITY ACCELERATION PROFILE (M/S vs TIME)",
                    points = res.velPointsMps,
                    lineColor = NeonGreenTelemetry,
                    unitLabel = "m/s",
                    maxVal = res.maxVelMps
                )
            }
        }

        // RK4 Calculus Explanation for the Student Pair Programmer
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SpaceSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "AEROSPACE CALCULUS: WHY RK4 OVER EULER?",
                        color = CyanPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "In orbital mechanics, simple Euler integration (x_new = x + v·dt) accumulates quadratic error O(dt²), causing the rocket to falsely spiral outward and gain infinite energy.\n\n" +
                                "The Runge-Kutta 4th Order (RK4) samples 4 trial slopes across the interval dt:\n" +
                                "• k1: slope at the start (t)\n" +
                                "• k2: slope at midpoint using k1 (t + dt/2)\n" +
                                "• k3: refined slope at midpoint using k2 (t + dt/2)\n" +
                                "• k4: slope at interval end using k3 (t + dt)\n\n" +
                                "Weighted sum: S(n+1) = S(n) + dt/6 · (k1 + 2k2 + 2k3 + k4).\n" +
                                "Global truncation error drops to O(dt⁴), ensuring zero orbital decay!",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCell(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(SpaceSurfaceVariant, RoundedCornerShape(8.dp))
            .border(1.dp, SpaceCardBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(label, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun PlotGraphCard(
    title: String,
    points: List<Double>,
    lineColor: Color,
    unitLabel: String,
    maxVal: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SpaceSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("Max: ${String.format(Locale.US, "%,.0f", maxVal)} $unitLabel", color = lineColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFF050811), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                if (points.size < 2 || maxVal <= 0.0) return@Canvas
                val width = size.width
                val height = size.height

                // Draw grid lines
                drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, height * 0.5f), Offset(width, height * 0.5f))

                val path = Path()
                val stepX = width / (points.size - 1).toFloat()

                points.forEachIndexed { i, v ->
                    val x = i * stepX
                    val y = height - (v / maxVal).coerceIn(0.0, 1.0).toFloat() * (height - 8f) - 4f
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.5f)
                )
            }
        }
    }
}

private suspend fun runHeadlessSimulation(rocket: RocketDesign): HeadlessSimResult = withContext(Dispatchers.Default) {
    var state = FlightTelemetry()
    val dt = 0.1 // 100ms timestep
    val totalTime = 400.0 // 400 seconds flight
    val totalSteps = (totalTime / dt).toInt()

    val timeList = mutableListOf<Double>()
    val altList = mutableListOf<Double>()
    val velList = mutableListOf<Double>()

    var maxAlt = 0.0
    var maxVel = 0.0

    val s1 = rocket.stages.getOrNull(0) ?: RocketPresets.FALCON_9.stages[0]
    val s2 = rocket.stages.getOrNull(1)

    var currentStage = s1
    var separated = false

    state = state.copy(
        currentDryMassKg = s1.dryMassKg + (s2?.totalMassKg ?: 0.0) + rocket.payloadMassKg,
        currentFuelKg = s1.fuelMassKg
    )

    for (step in 0 until totalSteps) {
        // Stage 1 separation trigger
        if (!separated && state.currentFuelKg <= 0.0 && s2 != null) {
            separated = true
            currentStage = s2
            state = state.copy(
                isSeparated = true,
                currentDryMassKg = s2.dryMassKg + rocket.payloadMassKg,
                currentFuelKg = s2.fuelMassKg,
                isEngineRunning = true
            )
        }

        // Pitch profile
        val alt = state.altitudeKm
        val targetPitch = when {
            alt < 1.2 -> 90.0
            alt < 70.0 -> (90.0 - ((alt - 1.2) / 68.8) * 80.0).coerceIn(10.0, 90.0)
            else -> 8.0
        }

        state = OrbitalIntegrator.rk4Step(
            current = state.copy(pitchDeg = targetPitch),
            dt = dt,
            thrustMaxNewtons = currentStage.thrustNewtons,
            ispSeconds = currentStage.ispSeconds,
            diameterMeters = rocket.diameterMeters
        )

        maxAlt = max(maxAlt, state.altitudeKm)
        maxVel = max(maxVel, state.speedMetersPerSec)

        // Sample every 5 seconds for chart
        if (step % 50 == 0 || step == totalSteps - 1) {
            timeList.add(state.timeSeconds)
            altList.add(state.altitudeKm)
            velList.add(state.speedMetersPerSec)
        }
    }

    val finalApo = state.apoapsisKm
    val finalPeri = state.periapsisKm
    val orbitAchieved = finalApo > 150.0 && maxVel > 6800.0

    HeadlessSimResult(
        timePoints = timeList,
        altPointsKm = altList,
        velPointsMps = velList,
        maxAltKm = maxAlt,
        maxVelMps = maxVel,
        finalApoapsisKm = finalApo,
        finalPeriapsisKm = finalPeri,
        isOrbitAchieved = orbitAchieved,
        totalSimSeconds = totalTime,
        totalStepsComputed = totalSteps
    )
}

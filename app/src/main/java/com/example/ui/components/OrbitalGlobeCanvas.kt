package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SatelliteCategory
import com.example.model.SatelliteTLE
import com.example.model.Vector3
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun OrbitalGlobeCanvas(
    satellites: List<SatelliteTLE>,
    selectedSatellite: SatelliteTLE?,
    elapsedSimTimeSeconds: Double,
    onSelectSatellite: (SatelliteTLE) -> Unit,
    modifier: Modifier = Modifier
) {
    // 3D rotation angles controllable via touch drag
    var rotationYaw by remember { mutableFloatStateOf(35f) }
    var rotationPitch by remember { mutableFloatStateOf(20f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .background(SpaceBackground, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    rotationYaw = (rotationYaw + dragAmount.x * 0.4f) % 360f
                    rotationPitch = (rotationPitch - dragAmount.y * 0.3f).coerceIn(-75f, 75f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val center = Offset(width * 0.5f, height * 0.5f)
            val earthRadiusPx = min(width, height) * 0.28f

            // 1. Draw Space & Star Background
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0F1A30), SpaceBackground),
                    center = center,
                    radius = earthRadiusPx * 2.5f
                )
            )

            // 2. Draw 3D Earth Globe
            val earthBrush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF2E7D32), // Green land
                    Color(0xFF1565C0), // Blue ocean
                    Color(0xFF0D47A1),
                    Color(0xFF051B38)
                ),
                center = center - Offset(earthRadiusPx * 0.3f, earthRadiusPx * 0.3f),
                radius = earthRadiusPx * 1.3f
            )

            // Atmospheric limb halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x6600E5FF), Color.Transparent),
                    center = center,
                    radius = earthRadiusPx * 1.15f
                ),
                radius = earthRadiusPx * 1.15f,
                center = center
            )

            // Earth body
            drawCircle(
                brush = earthBrush,
                radius = earthRadiusPx,
                center = center
            )

            // 3. Draw Latitude & Longitude grid lines on Earth
            val radPitch = Math.toRadians(rotationPitch.toDouble())
            val radYaw = Math.toRadians(rotationYaw.toDouble())

            drawEarthGridLines(center, earthRadiusPx, radPitch, radYaw)

            // 4. Draw Satellite Orbits and Positions
            val scaleMetersToPx = earthRadiusPx / 6371000.0 // Earth radius scale

            for (sat in satellites) {
                val isSelected = sat.noradId == selectedSatellite?.noradId
                val orbitColor = when (sat.category) {
                    SatelliteCategory.SPACE_STATION -> CyanPrimary
                    SatelliteCategory.STARLINK -> AmberThrust
                    SatelliteCategory.SCIENCE -> OrbitPurple
                    SatelliteCategory.NAVIGATION -> NeonGreenTelemetry
                    SatelliteCategory.WEATHER -> CyanAccent
                }

                // Draw orbital ellipse path in 3D
                drawOrbitalTrack(
                    sat = sat,
                    center = center,
                    scale = scaleMetersToPx,
                    radPitch = radPitch,
                    radYaw = radYaw,
                    color = if (isSelected) orbitColor else orbitColor.copy(alpha = 0.35f),
                    strokeWidth = if (isSelected) 3.5f else 1.8f
                )

                // Current satellite 3D position
                val posEci = sat.computePosition(elapsedSimTimeSeconds)
                val proj2D = project3Dto2D(posEci, center, scaleMetersToPx, radPitch, radYaw)

                // Behind Earth occlusion check:
                val zDepth = calculateZDepth(posEci, radPitch, radYaw)
                val isOccluded = zDepth < 0 && (proj2D - center).getDistance() < earthRadiusPx

                val alpha = if (isOccluded) 0.25f else 1.0f

                // Draw satellite marker
                drawCircle(
                    color = if (isSelected) Color.White else orbitColor.copy(alpha = alpha),
                    radius = if (isSelected) 7f else 4.5f,
                    center = proj2D
                )
                if (isSelected) {
                    drawCircle(
                        color = orbitColor,
                        radius = 12f,
                        center = proj2D,
                        style = Stroke(width = 2f)
                    )
                }
            }
        }

        // Overlay drag help tip
        Text(
            text = "↻ Drag to rotate 3D Earth view",
            color = TextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEarthGridLines(
    center: Offset,
    radius: Float,
    pitch: Double,
    yaw: Double
) {
    // Equator line projection
    val equatorPath = Path()
    var first = true
    for (deg in 0..360 step 10) {
        val rad = Math.toRadians(deg.toDouble())
        val x = cos(rad) * radius
        val y = 0.0
        val z = sin(rad) * radius

        val pt = rotateAndProject(x, y, z, center, pitch, yaw)
        if (pt.second > 0) { // Front face
            if (first) {
                equatorPath.moveTo(pt.first.x, pt.first.y)
                first = false
            } else {
                equatorPath.lineTo(pt.first.x, pt.first.y)
            }
        } else {
            first = true
        }
    }
    drawPath(
        path = equatorPath,
        color = Color.White.copy(alpha = 0.18f),
        style = Stroke(width = 1.5f)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOrbitalTrack(
    sat: SatelliteTLE,
    center: Offset,
    scale: Double,
    radPitch: Double,
    radYaw: Double,
    color: Color,
    strokeWidth: Float
) {
    val path = Path()
    var started = false
    val periodSec = sat.orbitalPeriodMinutes * 60.0
    val stepCount = 64
    val dt = periodSec / stepCount

    for (i in 0..stepCount) {
        val t = i * dt
        val pos = sat.computePosition(t)
        val proj = project3Dto2D(pos, center, scale, radPitch, radYaw)
        if (!started) {
            path.moveTo(proj.x, proj.y)
            started = true
        } else {
            path.lineTo(proj.x, proj.y)
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth)
    )
}

private fun project3Dto2D(
    pos: Vector3,
    center: Offset,
    scale: Double,
    pitch: Double,
    yaw: Double
): Offset {
    val x = pos.x * scale
    val y = pos.y * scale
    val z = pos.z * scale
    val res = rotateAndProject(x, y, z, center, pitch, yaw)
    return res.first
}

private fun calculateZDepth(pos: Vector3, pitch: Double, yaw: Double): Double {
    // 3D rotation along yaw (Y axis) then pitch (X axis)
    val cosY = cos(yaw)
    val sinY = sin(yaw)
    val cosP = cos(pitch)
    val sinP = sin(pitch)

    val z1 = -pos.x * sinY + pos.z * cosY
    val y1 = pos.y
    val z2 = y1 * sinP + z1 * cosP
    return z2
}

private fun rotateAndProject(
    x: Double,
    y: Double,
    z: Double,
    center: Offset,
    pitch: Double,
    yaw: Double
): Pair<Offset, Double> {
    // Rotate Yaw around Y
    val cosY = cos(yaw)
    val sinY = sin(yaw)
    val x1 = x * cosY + z * sinY
    val y1 = y
    val z1 = -x * sinY + z * cosY

    // Rotate Pitch around X
    val cosP = cos(pitch)
    val sinP = sin(pitch)
    val x2 = x1
    val y2 = y1 * cosP - z1 * sinP
    val z2 = y1 * sinP + z1 * cosP

    // 2D projection on screen (orthographic with center offset)
    val screenX = center.x + x2.toFloat()
    val screenY = center.y - y2.toFloat() // Invert Y for screen coordinates
    return Pair(Offset(screenX, screenY), z2)
}

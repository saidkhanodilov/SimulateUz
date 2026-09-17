package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FlightTelemetry
import com.example.ui.theme.*
import kotlin.math.*
import kotlin.random.Random

/**
 * 3D Interactive Rocket Flight Arena with camera orbit controls & 3D perspective projection.
 */
@Composable
fun RocketCanvas3D(
    telemetry: FlightTelemetry,
    modifier: Modifier = Modifier,
    trajectoryHistory: List<Offset> = emptyList()
) {
    // 3D Camera Orbit angles (yaw and pitch in degrees)
    var cameraYaw by remember { mutableFloatStateOf(25f) }
    var cameraPitch by remember { mutableFloatStateOf(10f) }
    var cameraZoom by remember { mutableFloatStateOf(1.0f) }

    // Staging separation physical distance animation
    var stageSeparationOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(telemetry.isSeparated) {
        if (telemetry.isSeparated) {
            // Animate booster falling behind
            while (stageSeparationOffset < 180f) {
                stageSeparationOffset += 3.5f
                kotlinx.coroutines.delay(16)
            }
        } else {
            stageSeparationOffset = 0f
        }
    }

    // Flame turbulence flicker
    val infiniteTransition = rememberInfiniteTransition(label = "flame_turbulence")
    val flicker by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(90, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(350.dp)
            .background(SpaceBackground, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    cameraYaw = (cameraYaw + dragAmount.x * 0.5f) % 360f
                    cameraPitch = (cameraPitch - dragAmount.y * 0.4f).coerceIn(-65f, 65f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val center = Offset(width * 0.5f, height * 0.55f)

            // 1. Draw Space, Atmosphere, and Curved Earth Limb
            draw3DAtmosphere(telemetry.altitudeKm, width, height)
            draw3DStars(width, height, telemetry.altitudeKm, cameraYaw)
            draw3DEarthHorizon(telemetry.altitudeKm, width, height, cameraPitch)

            // 2. Trajectory trace
            drawTrajectory(trajectoryHistory)

            // 3. Render 3D Rocket Stack
            render3DRocket(
                telemetry = telemetry,
                center = center,
                cameraYaw = cameraYaw,
                cameraPitch = cameraPitch,
                cameraZoom = cameraZoom,
                separationOffset = stageSeparationOffset,
                flicker = flicker
            )
        }

        // HUD Telemetry Overlays
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OverlayBadge("ALT", String.format("%.1f km", telemetry.altitudeKm), AccentIce)
            OverlayBadge("VEL", String.format("%.0f m/s", telemetry.speedMetersPerSec), AccentEmerald)
            OverlayBadge("STAGE", if (telemetry.isSeparated) "S2 (VAC)" else "S1 (BOOST)", AccentGold)
        }

        // Camera control helper & reset button
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "3D CAM: ${cameraYaw.toInt()}° / ${cameraPitch.toInt()}° (drag to rotate)",
                color = TextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            IconButton(
                onClick = {
                    cameraYaw = 25f
                    cameraPitch = 10f
                    cameraZoom = 1.0f
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.RestartAlt,
                    contentDescription = "Reset Camera",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun OverlayBadge(label: String, value: String, valueColor: Color) {
    Box(
        modifier = Modifier
            .background(SpaceSurface.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label ",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = value,
                color = valueColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun DrawScope.draw3DAtmosphere(altitudeKm: Double, width: Float, height: Float) {
    val spaceRatio = (altitudeKm / 90.0).coerceIn(0.0, 1.0).toFloat()
    val topColor = Color(0xFF060910)
    val bottomColor = lerp(Color(0xFF0D2847), Color(0xFF090D14), spaceRatio)

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(topColor, bottomColor),
            startY = 0f,
            endY = height
        )
    )
}

private fun DrawScope.draw3DStars(width: Float, height: Float, altitudeKm: Double, cameraYaw: Float) {
    val alpha = (altitudeKm / 35.0).coerceIn(0.2, 0.95).toFloat()
    val random = Random(1337)
    val yawOffset = (cameraYaw * 1.8f) % width

    for (i in 0..80) {
        var x = (random.nextFloat() * width + yawOffset) % width
        if (x < 0) x += width
        val y = random.nextFloat() * height * 0.75f
        val radius = random.nextFloat() * 1.4f + 0.6f
        val starAlpha = (random.nextFloat() * 0.7f + 0.3f) * alpha

        drawCircle(
            color = Color.White.copy(alpha = starAlpha),
            radius = radius,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.draw3DEarthHorizon(altitudeKm: Double, width: Float, height: Float, cameraPitch: Float) {
    val horizonY = height * (0.82f + (altitudeKm.toFloat() / 300f) * 0.4f + (cameraPitch / 100f) * 0.3f)
    val curveRadius = width * 1.9f
    val centerHorizon = Offset(width * 0.5f, horizonY + curveRadius)

    // Atmospheric halo glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(EarthAtmosphere, Color.Transparent),
            center = centerHorizon,
            radius = curveRadius + 22f
        ),
        radius = curveRadius + 22f,
        center = centerHorizon
    )

    // Earth curved body
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1E3A2F), Color(0xFF0E1E2E), Color(0xFF060D17)),
            startY = horizonY,
            endY = height
        ),
        radius = curveRadius,
        center = centerHorizon
    )
}

private fun DrawScope.drawTrajectory(trajectoryHistory: List<Offset>) {
    if (trajectoryHistory.size < 2) return
    val path = Path()
    trajectoryHistory.forEachIndexed { index, pt ->
        if (index == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
    }
    drawPath(
        path = path,
        color = AccentIce.copy(alpha = 0.4f),
        style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f)))
    )
}

/**
 * High-fidelity 3D Rocket Projection with 3D lighting, cylindrical perspective,
 * fins, vacuum plume, and physical stage separation.
 */
private fun DrawScope.render3DRocket(
    telemetry: FlightTelemetry,
    center: Offset,
    cameraYaw: Float,
    cameraPitch: Float,
    cameraZoom: Float,
    separationOffset: Float,
    flicker: Float
) {
    val scale = cameraZoom * 1.15f
    val rocketRadius = 14f * scale
    val stage1Height = 85f * scale
    val stage2Height = 55f * scale
    val noseconeHeight = 35f * scale

    // Rocket pitch tilt in 3D: (90 = vertical up)
    val pitchRad = Math.toRadians((90.0 - telemetry.pitchDeg)).toFloat()
    val yawRad = Math.toRadians(cameraYaw.toDouble()).toFloat()
    val camPitchRad = Math.toRadians(cameraPitch.toDouble()).toFloat()

    // 3D rotation transform function for a point in rocket-local space (x, y, z)
    fun project3D(rx: Float, ry: Float, rz: Float, yWorldOffset: Float = 0f): Offset {
        // 1. Rocket pitch tilt around Z
        val x1 = rx * cos(pitchRad) - ry * sin(pitchRad)
        val y1 = rx * sin(pitchRad) + ry * cos(pitchRad) + yWorldOffset
        val z1 = rz

        // 2. Camera Yaw around Y
        val x2 = x1 * cos(yawRad) + z1 * sin(yawRad)
        val y2 = y1
        val z2 = -x1 * sin(yawRad) + z1 * cos(yawRad)

        // 3. Camera Pitch around X
        val x3 = x2
        val y3 = y2 * cos(camPitchRad) - z2 * sin(camPitchRad)
        val z3 = y2 * sin(camPitchRad) + z2 * cos(camPitchRad)

        // Perspective foreshortening
        val perspectiveDist = 700f
        val factor = perspectiveDist / (perspectiveDist + z3)

        return Offset(center.x + x3 * factor, center.y + y3 * factor)
    }

    // A. Draw Stage 2 + Nosecone (Upper Section)
    val s2BaseY = 0f
    val s2TopY = -stage2Height
    val noseTipY = -(stage2Height + noseconeHeight)

    // Nosecone in 3D
    val noseTip = project3D(0f, noseTipY, 0f)
    val noseConeSlices = 10
    for (i in 0 until noseConeSlices) {
        val a1 = (i * 2 * Math.PI / noseConeSlices).toFloat()
        val a2 = ((i + 1) * 2 * Math.PI / noseConeSlices).toFloat()

        val p1 = project3D(cos(a1) * rocketRadius, s2TopY, sin(a1) * rocketRadius)
        val p2 = project3D(cos(a2) * rocketRadius, s2TopY, sin(a2) * rocketRadius)

        // 3D Directional Lighting Calculation
        val lightAngle = cos(a1 - yawRad + 0.6f)
        val shade = (0.55f + 0.45f * lightAngle).coerceIn(0.15f, 1.0f)
        val noseColor = Color(
            (0xEE * shade).toInt(),
            (0xF2 * shade).toInt(),
            (0xF6 * shade).toInt()
        )

        val conePath = Path().apply {
            moveTo(noseTip.x, noseTip.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            close()
        }
        drawPath(conePath, color = noseColor, style = Fill)
    }

    // Stage 2 Cylinder
    draw3DCylinder(
        radius = rocketRadius,
        topY = s2TopY,
        bottomY = s2BaseY,
        yawRad = yawRad,
        project = { x, y, z -> project3D(x, y, z) },
        baseColor = Color(0xFFF1F5F9)
    )

    // B. Draw Stage 1 (Booster) - separated downwards if staging active
    val s1YOffset = if (telemetry.isSeparated) separationOffset else 0f
    val s1TopY = 4f
    val s1BaseY = stage1Height

    draw3DCylinder(
        radius = rocketRadius,
        topY = s1TopY,
        bottomY = s1BaseY,
        yawRad = yawRad,
        project = { x, y, z -> project3D(x, y, z, yWorldOffset = s1YOffset) },
        baseColor = Color(0xFFE2E8F0)
    )

    // 3D Grid Fins on Booster
    val finAngles = listOf(0f, 90f, 180f, 270f)
    for (deg in finAngles) {
        val rad = Math.toRadians(deg.toDouble()).toFloat()
        val finBase1 = project3D(cos(rad) * rocketRadius, s1TopY + 10f, sin(rad) * rocketRadius, s1YOffset)
        val finTip = project3D(cos(rad) * (rocketRadius + 14f * scale), s1TopY + 16f, sin(rad) * (rocketRadius + 14f * scale), s1YOffset)
        val finBase2 = project3D(cos(rad) * rocketRadius, s1TopY + 22f, sin(rad) * rocketRadius, s1YOffset)

        val finPath = Path().apply {
            moveTo(finBase1.x, finBase1.y)
            lineTo(finTip.x, finTip.y)
            lineTo(finBase2.x, finBase2.y)
            close()
        }
        drawPath(finPath, color = Color(0xFF334155), style = Fill)
    }

    // C. 3D Exhaust Plume & Vacuum Expansion
    val isFiring = telemetry.isEngineRunning && telemetry.currentFuelKg > 0
    if (isFiring) {
        val activeY = if (telemetry.isSeparated) s2BaseY else (s1BaseY + s1YOffset)
        val activeRadius = if (telemetry.isSeparated) rocketRadius * 0.6f else rocketRadius

        // In vacuum (>45 km), plume blooms laterally
        val vacuumBloom = (telemetry.altitudeKm / 45.0).coerceIn(1.0, 2.8).toFloat()
        val plumeLength = (75f * scale + 40f * telemetry.throttlePercent.toFloat()) * flicker
        val plumeBaseRadius = activeRadius * 1.3f * vacuumBloom * flicker

        val flameTip = project3D(0f, activeY + plumeLength, 0f)
        val flameBaseLeft = project3D(-plumeBaseRadius, activeY, 0f)
        val flameBaseRight = project3D(plumeBaseRadius, activeY, 0f)

        val flamePath = Path().apply {
            moveTo(flameBaseLeft.x, flameBaseLeft.y)
            quadraticTo(
                (flameBaseLeft.x + flameTip.x) * 0.5f - 10f, (flameBaseLeft.y + flameTip.y) * 0.5f,
                flameTip.x, flameTip.y
            )
            quadraticTo(
                (flameBaseRight.x + flameTip.x) * 0.5f + 10f, (flameBaseRight.y + flameTip.y) * 0.5f,
                flameBaseRight.x, flameBaseRight.y
            )
            close()
        }

        val flameGradient = Brush.linearGradient(
            colors = listOf(PlumeStart, PlumeCore, PlumeOuter.copy(alpha = 0.3f), Color.Transparent),
            start = Offset(center.x, flameBaseLeft.y),
            end = Offset(flameTip.x, flameTip.y)
        )
        drawPath(flamePath, brush = flameGradient)

        // Shock diamonds
        val shock1 = project3D(0f, activeY + 18f * scale, 0f)
        drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 4f * scale * flicker, center = shock1)
    }

    // Cold Gas separation thrusters when staging happens
    if (telemetry.isSeparated && separationOffset < 90f) {
        val puffLeft = project3D(-rocketRadius - 12f, s1TopY + s1YOffset, 0f)
        val puffRight = project3D(rocketRadius + 12f, s1TopY + s1YOffset, 0f)
        drawCircle(color = Color.White.copy(alpha = 0.6f), radius = 6f, center = puffLeft)
        drawCircle(color = Color.White.copy(alpha = 0.6f), radius = 6f, center = puffRight)
    }

    // D. 3D Max-Q Shockwave condensation disc in 3D perspective
    if (telemetry.currentDynamicPressurePa > 26000.0 && telemetry.speedMetersPerSec > 260.0) {
        val shockY = s2TopY + 20f
        val shockRingPath = Path()
        var first = true
        val shockRadius = rocketRadius * 2.8f
        for (deg in 0..360 step 20) {
            val rad = Math.toRadians(deg.toDouble()).toFloat()
            val pt = project3D(cos(rad) * shockRadius, shockY, sin(rad) * shockRadius)
            if (first) {
                shockRingPath.moveTo(pt.x, pt.y)
                first = false
            } else {
                shockRingPath.lineTo(pt.x, pt.y)
            }
        }
        shockRingPath.close()
        drawPath(
            path = shockRingPath,
            color = Color.White.copy(alpha = 0.45f),
            style = Stroke(width = 3f)
        )
    }
}

private fun DrawScope.draw3DCylinder(
    radius: Float,
    topY: Float,
    bottomY: Float,
    yawRad: Float,
    project: (Float, Float, Float) -> Offset,
    baseColor: Color
) {
    val slices = 12
    for (i in 0 until slices) {
        val a1 = (i * 2 * Math.PI / slices).toFloat()
        val a2 = ((i + 1) * 2 * Math.PI / slices).toFloat()

        val pTop1 = project(cos(a1) * radius, topY, sin(a1) * radius)
        val pTop2 = project(cos(a2) * radius, topY, sin(a2) * radius)
        val pBot1 = project(cos(a1) * radius, bottomY, sin(a1) * radius)
        val pBot2 = project(cos(a2) * radius, bottomY, sin(a2) * radius)

        // Directional illumination
        val lightNormal = cos(a1 - yawRad + 0.6f)
        val shade = (0.50f + 0.50f * lightNormal).coerceIn(0.12f, 1.0f)
        val panelColor = Color(
            (baseColor.red * shade).coerceIn(0f, 1f),
            (baseColor.green * shade).coerceIn(0f, 1f),
            (baseColor.blue * shade).coerceIn(0f, 1f),
            1f
        )

        val quadPath = Path().apply {
            moveTo(pTop1.x, pTop1.y)
            lineTo(pTop2.x, pTop2.y)
            lineTo(pBot2.x, pBot2.y)
            lineTo(pBot1.x, pBot1.y)
            close()
        }
        drawPath(quadPath, color = panelColor, style = Fill)
    }
}

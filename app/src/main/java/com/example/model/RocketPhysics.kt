package com.example.model

import kotlin.math.*

/**
 * 3D Cartesian Vector representation for orbital & trajectory kinematics.
 */
data class Vector3(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0
) {
    operator fun plus(o: Vector3) = Vector3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vector3) = Vector3(x - o.x, y - o.y, z - o.z)
    operator fun times(scalar: Double) = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Double) = Vector3(x / scalar, y / scalar, z / scalar)

    fun magnitude(): Double = sqrt(x * x + y * y + z * z)
    fun normalized(): Vector3 {
        val m = magnitude()
        return if (m < 1e-12) Vector3(0.0, 0.0, 0.0) else this / m
    }
}

/**
 * Rocket stage specification for multi-stage vehicle configurations.
 */
data class RocketStage(
    val stageName: String,
    val dryMassKg: Double,
    val fuelMassKg: Double,
    val thrustNewtons: Double,
    val ispSeconds: Double,
    val engineName: String,
    val engineCount: Int = 1
) {
    val totalMassKg: Double get() = dryMassKg + fuelMassKg
    val burnTimeSeconds: Double get() = if (thrustNewtons > 0 && ispSeconds > 0) {
        (fuelMassKg * STANDARD_G0 * ispSeconds) / thrustNewtons
    } else 0.0

    // Tsiolkovsky Delta-v: delta_v = Isp * g0 * ln(m0 / mf)
    fun computeDeltaV(payloadMassKg: Double = 0.0): Double {
        val m0 = totalMassKg + payloadMassKg
        val mf = dryMassKg + payloadMassKg
        if (m0 <= 0 || mf <= 0 || m0 <= mf) return 0.0
        return ispSeconds * STANDARD_G0 * ln(m0 / mf)
    }

    // Thrust-to-weight ratio at liftoff: TWR = F / (m0 * g0)
    fun computeTWR(payloadMassKg: Double = 0.0): Double {
        val totalWeight = (totalMassKg + payloadMassKg) * STANDARD_G0
        return if (totalWeight > 0) thrustNewtons / totalWeight else 0.0
    }
}

/**
 * Complete vehicle architecture.
 */
data class RocketDesign(
    val name: String,
    val stages: List<RocketStage>,
    val payloadMassKg: Double,
    val diameterMeters: Double = 3.7
) {
    val totalLiftOffMass: Double get() = stages.sumOf { it.totalMassKg } + payloadMassKg
    val totalDeltaV: Double get() {
        var totalDv = 0.0
        var currentPayload = payloadMassKg
        for (i in stages.indices.reversed()) {
            val stage = stages[i]
            val dv = stage.computeDeltaV(currentPayload)
            totalDv += dv
            currentPayload += stage.totalMassKg
        }
        return totalDv
    }
    val liftoffTWR: Double get() = if (stages.isNotEmpty()) {
        val firstStageThrust = stages[0].thrustNewtons
        val totalWeight = totalLiftOffMass * STANDARD_G0
        firstStageThrust / totalWeight
    } else 0.0
}

/**
 * Standard constants for Earth orbit mechanics.
 */
const val G_CONSTANT = 6.67430e-11
const val EARTH_MASS = 5.9722e24
const val EARTH_RADIUS = 6371000.0 // 6,371 km
const val EARTH_MU = G_CONSTANT * EARTH_MASS
const val STANDARD_G0 = 9.80665

/**
 * Real-time simulation flight state.
 */
data class FlightTelemetry(
    val timeSeconds: Double = 0.0,
    val position: Vector3 = Vector3(0.0, EARTH_RADIUS, 0.0),
    val velocity: Vector3 = Vector3(0.0, 0.0, 0.0),
    val acceleration: Vector3 = Vector3(0.0, 0.0, 0.0),
    val currentStageIndex: Int = 0,
    val currentFuelKg: Double = 395000.0,
    val currentDryMassKg: Double = 25000.0,
    val throttlePercent: Double = 1.0, // 0.0 to 1.0
    val pitchDeg: Double = 90.0,       // 90 = vertical up, 0 = horizontal downrange
    val isEngineRunning: Boolean = true,
    val isSeparated: Boolean = false,
    val maxQPressurePa: Double = 0.0,
    val currentDynamicPressurePa: Double = 0.0
) {
    val altitudeMeters: Double get() = max(0.0, position.magnitude() - EARTH_RADIUS)
    val altitudeKm: Double get() = altitudeMeters / 1000.0
    val speedMetersPerSec: Double get() = velocity.magnitude()
    val machNumber: Double get() = speedMetersPerSec / 340.0
    val totalMassKg: Double get() = currentDryMassKg + currentFuelKg
    val gForce: Double get() = (acceleration.magnitude()) / STANDARD_G0

    // Specific orbital energy: epsilon = v^2/2 - mu/r
    val rMagnitude: Double get() = position.magnitude()
    val specificEnergy: Double get() = (speedMetersPerSec * speedMetersPerSec) / 2.0 - (EARTH_MU / rMagnitude)
    val semiMajorAxisKm: Double get() {
        val eps = specificEnergy
        return if (abs(eps) > 1e-6) (-EARTH_MU / (2.0 * eps)) / 1000.0 else 0.0
    }
    val apoapsisKm: Double get() {
        val a = semiMajorAxisKm * 1000.0
        val v = speedMetersPerSec
        val r = rMagnitude
        val h = r * v * sin(Math.toRadians(pitchDeg))
        val term = 1.0 + (2.0 * specificEnergy * h * h) / (EARTH_MU * EARTH_MU)
        val ecc = if (term >= 0) sqrt(term) else 0.0
        val ra = a * (1.0 + ecc)
        return max(0.0, (ra - EARTH_RADIUS) / 1000.0)
    }
    val periapsisKm: Double get() {
        val a = semiMajorAxisKm * 1000.0
        val v = speedMetersPerSec
        val r = rMagnitude
        val h = r * v * sin(Math.toRadians(pitchDeg))
        val term = 1.0 + (2.0 * specificEnergy * h * h) / (EARTH_MU * EARTH_MU)
        val ecc = if (term >= 0) sqrt(term) else 0.0
        val rp = a * (1.0 - ecc)
        return max(0.0, (rp - EARTH_RADIUS) / 1000.0)
    }
}

/**
 * High-performance Runge-Kutta 4th Order numerical integrator in Kotlin.
 * Computes exact Newton gravity, atmospheric drag, rocket thrust, and mass flow rate.
 */
object OrbitalIntegrator {

    fun computeGravity(pos: Vector3): Vector3 {
        val r = pos.magnitude()
        if (r < 1.0) return Vector3(0.0, 0.0, 0.0)
        val factor = -EARTH_MU / (r * r * r)
        return pos * factor
    }

    fun computeAtmosphericDensity(altitudeMeters: Double): Double {
        if (altitudeMeters < 0.0) return 1.225
        if (altitudeMeters > 130000.0) return 0.0
        val scaleHeight = 8500.0
        return 1.225 * exp(-altitudeMeters / scaleHeight)
    }

    fun computeDerivatives(
        state: FlightTelemetry,
        thrustMaxNewtons: Double,
        ispSeconds: Double,
        diameterMeters: Double
    ): Triple<Vector3, Vector3, Double> {
        val totalMass = state.totalMassKg
        if (totalMass <= 0) return Triple(Vector3(), Vector3(), 0.0)

        // 1. Gravity
        val aGrav = computeGravity(state.position)

        // 2. Thrust vectoring based on pitch deg
        val effectiveThrust = if (state.isEngineRunning && state.currentFuelKg > 0) {
            thrustMaxNewtons * state.throttlePercent
        } else 0.0

        val pitchRad = Math.toRadians(state.pitchDeg)
        // Vector along local pitch (radial up * sin(pitch) + tangential downrange * cos(pitch))
        val radialUp = state.position.normalized()
        val eastDir = Vector3(1.0, 0.0, 0.0) // simplified eastward vector
        val thrustDir = (radialUp * sin(pitchRad) + eastDir * cos(pitchRad)).normalized()
        val aThrust = thrustDir * (effectiveThrust / totalMass)

        // 3. Aerodynamic drag
        val alt = state.altitudeMeters
        val rho = computeAtmosphericDensity(alt)
        val v = state.speedMetersPerSec
        val q = 0.5 * rho * v * v
        val area = Math.PI * (diameterMeters / 2.0).pow(2)
        val cd = 0.3
        val dragForceMag = q * cd * area
        val aDrag = if (v > 1e-3) state.velocity.normalized() * (-dragForceMag / totalMass) else Vector3()

        val totalAcc = aGrav + aThrust + aDrag
        val massFlowRate = if (state.isEngineRunning && state.currentFuelKg > 0 && ispSeconds > 0) {
            -(effectiveThrust / (STANDARD_G0 * ispSeconds))
        } else 0.0

        return Triple(state.velocity, totalAcc, massFlowRate)
    }

    fun rk4Step(
        current: FlightTelemetry,
        dt: Double,
        thrustMaxNewtons: Double,
        ispSeconds: Double,
        diameterMeters: Double
    ): FlightTelemetry {
        if (dt <= 0.0) return current

        // k1
        val (v1, a1, m1) = computeDerivatives(current, thrustMaxNewtons, ispSeconds, diameterMeters)

        // k2
        val k2State = current.copy(
            position = current.position + v1 * (0.5 * dt),
            velocity = current.velocity + a1 * (0.5 * dt),
            currentFuelKg = max(0.0, current.currentFuelKg + m1 * (0.5 * dt))
        )
        val (v2, a2, m2) = computeDerivatives(k2State, thrustMaxNewtons, ispSeconds, diameterMeters)

        // k3
        val k3State = current.copy(
            position = current.position + v2 * (0.5 * dt),
            velocity = current.velocity + a2 * (0.5 * dt),
            currentFuelKg = max(0.0, current.currentFuelKg + m2 * (0.5 * dt))
        )
        val (v3, a3, m3) = computeDerivatives(k3State, thrustMaxNewtons, ispSeconds, diameterMeters)

        // k4
        val k4State = current.copy(
            position = current.position + v3 * dt,
            velocity = current.velocity + a3 * dt,
            currentFuelKg = max(0.0, current.currentFuelKg + m3 * dt)
        )
        val (v4, a4, m4) = computeDerivatives(k4State, thrustMaxNewtons, ispSeconds, diameterMeters)

        val dPos = (v1 + v2 * 2.0 + v3 * 2.0 + v4) * (dt / 6.0)
        val dVel = (a1 + a2 * 2.0 + a3 * 2.0 + a4) * (dt / 6.0)
        val dFuel = (m1 + m2 * 2.0 + m3 * 2.0 + m4) * (dt / 6.0)

        var nextFuel = max(0.0, current.currentFuelKg + dFuel)
        var engineOn = current.isEngineRunning
        if (nextFuel <= 0.0) {
            nextFuel = 0.0
            engineOn = false
        }

        var nextPos = current.position + dPos
        var nextVel = current.velocity + dVel

        // Ground clamping
        if (nextPos.magnitude() < EARTH_RADIUS) {
            nextPos = nextPos.normalized() * EARTH_RADIUS
            if (nextVel.y < 0) nextVel = Vector3(nextVel.x, 0.0, nextVel.z)
        }

        val alt = max(0.0, nextPos.magnitude() - EARTH_RADIUS)
        val rho = computeAtmosphericDensity(alt)
        val dynamicP = 0.5 * rho * nextVel.magnitude().pow(2)
        val maxQ = max(current.maxQPressurePa, dynamicP)

        return current.copy(
            timeSeconds = current.timeSeconds + dt,
            position = nextPos,
            velocity = nextVel,
            acceleration = a1, // instant acceleration
            currentFuelKg = nextFuel,
            isEngineRunning = engineOn,
            currentDynamicPressurePa = dynamicP,
            maxQPressurePa = maxQ
        )
    }
}

/**
 * Aerospace Presets for Rocket Assembly & Simulation.
 */
object RocketPresets {
    val FALCON_9 = RocketDesign(
        name = "Falcon 9 (Block 5)",
        stages = listOf(
            RocketStage(
                stageName = "Stage 1 Booster",
                dryMassKg = 22200.0,
                fuelMassKg = 418000.0,
                thrustNewtons = 7607000.0, // 9 Merlin 1D sea-level
                ispSeconds = 311.0,
                engineName = "9x Merlin 1D",
                engineCount = 9
            ),
            RocketStage(
                stageName = "Stage 2 Vacuum",
                dryMassKg = 4000.0,
                fuelMassKg = 92670.0,
                thrustNewtons = 981000.0, // 1 Merlin 1D Vac
                ispSeconds = 348.0,
                engineName = "1x Merlin 1D Vac",
                engineCount = 1
            )
        ),
        payloadMassKg = 15600.0, // Starlink or LEO payload
        diameterMeters = 3.7
    )

    val STARSHIP = RocketDesign(
        name = "Starship + Super Heavy",
        stages = listOf(
            RocketStage(
                stageName = "Super Heavy Booster",
                dryMassKg = 200000.0,
                fuelMassKg = 3400000.0,
                thrustNewtons = 74000000.0, // 33 Raptor 3 engines
                ispSeconds = 330.0,
                engineName = "33x Raptor 3",
                engineCount = 33
            ),
            RocketStage(
                stageName = "Starship Upper Stage",
                dryMassKg = 100000.0,
                fuelMassKg = 1200000.0,
                thrustNewtons = 15000000.0, // 6 Raptors (3 Sea, 3 Vac)
                ispSeconds = 380.0,
                engineName = "6x Raptor 3 Vac/Sea",
                engineCount = 6
            )
        ),
        payloadMassKg = 100000.0,
        diameterMeters = 9.0
    )

    val ELECTRON = RocketDesign(
        name = "Rocket Lab Electron",
        stages = listOf(
            RocketStage(
                stageName = "Stage 1 Booster",
                dryMassKg = 950.0,
                fuelMassKg = 9200.0,
                thrustNewtons = 224000.0, // 9 Rutherford
                ispSeconds = 311.0,
                engineName = "9x Rutherford Electric",
                engineCount = 9
            ),
            RocketStage(
                stageName = "Stage 2 Upper",
                dryMassKg = 250.0,
                fuelMassKg = 2150.0,
                thrustNewtons = 25800.0, // 1 Rutherford Vac
                ispSeconds = 343.0,
                engineName = "1x Rutherford Vac",
                engineCount = 1
            )
        ),
        payloadMassKg = 300.0,
        diameterMeters = 1.2
    )

    val SATURN_V = RocketDesign(
        name = "Apollo Saturn V",
        stages = listOf(
            RocketStage(
                stageName = "S-IC First Stage",
                dryMassKg = 130000.0,
                fuelMassKg = 2169000.0,
                thrustNewtons = 35100000.0, // 5 F-1 engines
                ispSeconds = 263.0,
                engineName = "5x F-1",
                engineCount = 5
            ),
            RocketStage(
                stageName = "S-II Second Stage",
                dryMassKg = 36000.0,
                fuelMassKg = 450000.0,
                thrustNewtons = 5141000.0, // 5 J-2 engines
                ispSeconds = 421.0,
                engineName = "5x J-2",
                engineCount = 5
            ),
            RocketStage(
                stageName = "S-IVB Third Stage",
                dryMassKg = 11000.0,
                fuelMassKg = 107000.0,
                thrustNewtons = 1033000.0, // 1 J-2
                ispSeconds = 421.0,
                engineName = "1x J-2",
                engineCount = 1
            )
        ),
        payloadMassKg = 48600.0, // Apollo CSM + LM
        diameterMeters = 10.1
    )

    val ALL_PRESETS = listOf(FALCON_9, STARSHIP, ELECTRON, SATURN_V)
}

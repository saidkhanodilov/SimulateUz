package com.example.model

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

/**
 * Global Rocket Launch Vehicle database for payload evaluation & cost calculation.
 */
data class LaunchVehicleOption(
    val id: String,
    val name: String,
    val provider: String,
    val leoCapacityKg: Double,
    val gtoCapacityKg: Double,
    val baseCostUsd: Double,
    val rideshareCostPerKg: Double = 5000.0,
    val reliabilityPercent: Double = 98.0,
    val diameterMeters: Double = 3.7,
    val description: String
) {
    fun getCapacityForOrbit(orbit: TargetOrbit): Double = when (orbit) {
        TargetOrbit.LEO -> leoCapacityKg
        TargetOrbit.SSO -> leoCapacityKg * 0.88
        TargetOrbit.MEO -> leoCapacityKg * 0.45
        TargetOrbit.GTO -> gtoCapacityKg
        TargetOrbit.TLI -> gtoCapacityKg * 0.75
    }

    fun calculateCost(payloadKg: Double, orbit: TargetOrbit): Double {
        val capacity = getCapacityForOrbit(orbit)
        if (payloadKg > capacity) return -1.0 // Exceeds capacity

        // If payload is small (< 500kg) and rocket supports rideshare, calculate rideshare price
        return if (payloadKg <= 500.0 && leoCapacityKg > 5000.0) {
            max(250000.0, payloadKg * rideshareCostPerKg)
        } else {
            baseCostUsd
        }
    }
}

enum class TargetOrbit(val title: String, val altitudeKm: Double, val requiredDeltaVMps: Double) {
    LEO("Low Earth Orbit (LEO)", 300.0, 9300.0),
    SSO("Sun-Synchronous Polar (SSO)", 600.0, 9600.0),
    MEO("Medium Earth Orbit (MEO)", 20200.0, 11200.0),
    GTO("Geostationary Transfer (GTO)", 35786.0, 11800.0),
    TLI("Trans-Lunar Injection (Moon)", 384400.0, 12600.0)
}

data class PayloadEvaluation(
    val payloadKg: Double,
    val targetOrbit: TargetOrbit,
    val recommendedVehicle: LaunchVehicleOption,
    val estimatedCostUsd: Double,
    val costPerKg: Double,
    val isRideshare: Boolean,
    val requiredForceNewtons: Double,
    val requiredSpeedMps: Double,
    val feasibilityNote: String,
    val allOptions: List<Pair<LaunchVehicleOption, Double>>
)

object RocketMarketDatabase {
    val ALL_VEHICLES = listOf(
        LaunchVehicleOption(
            id = "electron",
            name = "Electron",
            provider = "Rocket Lab",
            leoCapacityKg = 300.0,
            gtoCapacityKg = 40.0,
            baseCostUsd = 7500000.0,
            rideshareCostPerKg = 25000.0,
            reliabilityPercent = 94.0,
            diameterMeters = 1.2,
            description = "Dedicated small-satellite orbital launch with 3D-printed Rutherford engines."
        ),
        LaunchVehicleOption(
            id = "vega_c",
            name = "Vega-C",
            provider = "Arianespace / ESA",
            leoCapacityKg = 2300.0,
            gtoCapacityKg = 800.0,
            baseCostUsd = 35000000.0,
            rideshareCostPerKg = 15000.0,
            reliabilityPercent = 90.0,
            diameterMeters = 3.0,
            description = "European light launch vehicle for medium scientific and polar payloads."
        ),
        LaunchVehicleOption(
            id = "falcon9",
            name = "Falcon 9 (Reusable)",
            provider = "SpaceX",
            leoCapacityKg = 17500.0,
            gtoCapacityKg = 5500.0,
            baseCostUsd = 69750000.0,
            rideshareCostPerKg = 6000.0,
            reliabilityPercent = 99.3,
            diameterMeters = 3.7,
            description = "World's most flown orbital workhorse with flight-proven booster recovery."
        ),
        LaunchVehicleOption(
            id = "falcon_heavy",
            name = "Falcon Heavy",
            provider = "SpaceX",
            leoCapacityKg = 63800.0,
            gtoCapacityKg = 26700.0,
            baseCostUsd = 97000000.0,
            rideshareCostPerKg = 4500.0,
            reliabilityPercent = 100.0,
            diameterMeters = 12.2,
            description = "Super-heavy lifter powered by 27 Merlin engines for deep space & massive payloads."
        ),
        LaunchVehicleOption(
            id = "starship",
            name = "Starship",
            provider = "SpaceX",
            leoCapacityKg = 150000.0,
            gtoCapacityKg = 40000.0,
            baseCostUsd = 20000000.0, // Projected commercial cost
            rideshareCostPerKg = 2000.0,
            reliabilityPercent = 88.0,
            diameterMeters = 9.0,
            description = "Fully reusable next-generation interplanetary heavy launch system."
        ),
        LaunchVehicleOption(
            id = "vulcan",
            name = "Vulcan Centaur",
            provider = "United Launch Alliance (ULA)",
            leoCapacityKg = 27200.0,
            gtoCapacityKg = 14500.0,
            baseCostUsd = 110000000.0,
            rideshareCostPerKg = 8000.0,
            reliabilityPercent = 100.0,
            diameterMeters = 5.4,
            description = "High-energy Centaur V upper stage for national security & interplanetary insertion."
        )
    )

    fun evaluatePayload(payloadKg: Double, orbit: TargetOrbit): PayloadEvaluation {
        val validVehicles = ALL_VEHICLES.map { vehicle ->
            val cost = vehicle.calculateCost(payloadKg, orbit)
            Pair(vehicle, cost)
        }

        // Filter those capable of lifting the payload
        val capable = validVehicles.filter { it.second > 0 }
            .sortedBy { it.second } // cheapest capable option

        val recommended = capable.firstOrNull()?.first ?: ALL_VEHICLES.last()
        val cost = capable.firstOrNull()?.second ?: -1.0
        val isRideshare = payloadKg <= 500.0 && recommended.leoCapacityKg > 5000.0
        val costPerKg = if (cost > 0 && payloadKg > 0) cost / payloadKg else 0.0

        // Calculate orbital velocity & gravitational force at target orbit
        val r = EARTH_RADIUS + (orbit.altitudeKm * 1000.0)
        val vOrbital = sqrt(EARTH_MU / r)
        val gravityAcc = EARTH_MU / (r * r)
        val forceAtOrbit = payloadKg * gravityAcc

        val feasibility = when {
            capable.isEmpty() -> "Payload exceeds all operational commercial rockets. Requires multi-launch orbital assembly."
            isRideshare -> "Eligible for SmallSat Rideshare program (e.g. Transporter mission), drastically reducing launch cost!"
            payloadKg > 40000.0 -> "Requires super-heavy lifter category (Falcon Heavy or Starship)."
            else -> "Standard dedicated commercial launch manifest."
        }

        return PayloadEvaluation(
            payloadKg = payloadKg,
            targetOrbit = orbit,
            recommendedVehicle = recommended,
            estimatedCostUsd = cost,
            costPerKg = costPerKg,
            isRideshare = isRideshare,
            requiredForceNewtons = forceAtOrbit,
            requiredSpeedMps = vOrbital,
            feasibilityNote = feasibility,
            allOptions = validVehicles
        )
    }
}

/**
 * Mission History & Flight Log Report Data Structure
 */
data class MissionReport(
    val id: String,
    val missionName: String,
    val dateFormatted: String,
    val rocketName: String,
    val payloadKg: Double,
    val maxAltitudeKm: Double,
    val maxVelocityMps: Double,
    val maxDynamicPressurePa: Double,
    val isOrbitAchieved: Boolean,
    val estimatedCostUsd: Double,
    val flightDurationSec: Double,
    val flightEvents: List<String>
)

object MissionHistoryRepository {
    private val historicalList = mutableListOf(
        MissionReport(
            id = "MSN-2026-0812",
            missionName = "Falcon 9 Starlink Group 7-18",
            dateFormatted = "2026-08-12 14:22 UTC",
            rocketName = "Falcon 9 (Block 5)",
            payloadKg = 16200.0,
            maxAltitudeKm = 295.4,
            maxVelocityMps = 7810.0,
            maxDynamicPressurePa = 31200.0,
            isOrbitAchieved = true,
            estimatedCostUsd = 67000000.0,
            flightDurationSec = 520.0,
            flightEvents = listOf(
                "T+00:00: Liftoff from SLC-40",
                "T+01:12: Max-Q passed (31.2 kPa)",
                "T+02:30: Main Engine Cutoff (MECO)",
                "T+02:35: Stage 1 Separation",
                "T+08:45: Second Engine Cutoff (SECO-1)",
                "T+14:20: LEO Orbital Insertion Confirmed (295 km circular)"
            )
        ),
        MissionReport(
            id = "MSN-2026-0704",
            missionName = "Electron 'Baby Come Back'",
            dateFormatted = "2026-07-04 03:50 UTC",
            rocketName = "Rocket Lab Electron",
            payloadKg = 180.0,
            maxAltitudeKm = 510.0,
            maxVelocityMps = 7650.0,
            maxDynamicPressurePa = 24500.0,
            isOrbitAchieved = true,
            estimatedCostUsd = 7500000.0,
            flightDurationSec = 590.0,
            flightEvents = listOf(
                "T+00:00: Rutherford engines ignition",
                "T+01:15: Max-Q passed cleanly",
                "T+02:28: Stage 1 MECO & pneumatic separation",
                "T+09:12: Kick Stage burn to 500 km Sun-Synchronous Orbit"
            )
        ),
        MissionReport(
            id = "MSN-2026-0518",
            missionName = "Starship Integrated Flight Test 5",
            dateFormatted = "2026-05-18 12:00 UTC",
            rocketName = "Starship + Super Heavy",
            payloadKg = 45000.0,
            maxAltitudeKm = 215.0,
            maxVelocityMps = 7780.0,
            maxDynamicPressurePa = 38000.0,
            isOrbitAchieved = true,
            estimatedCostUsd = 20000000.0,
            flightDurationSec = 390.0,
            flightEvents = listOf(
                "T+00:00: 33 Raptor 3 engines liftoff",
                "T+01:02: Max-Q peak aerodynamic pressure",
                "T+02:42: Hot-staging ring separation",
                "T+08:30: Orbital velocity threshold passed"
            )
        )
    )

    fun getAllReports(): List<MissionReport> = historicalList.toList()

    fun addReport(report: MissionReport) {
        historicalList.add(0, report)
    }
}

/**
 * Conjunction Analysis & Safe Launch Window Scheduler (COLA)
 */
data class CollisionRiskWindow(
    val launchDelaySeconds: Int,
    val windowTimeDisplay: String,
    val closestApproachSatellite: String,
    val missDistanceKm: Double,
    val riskLevel: RiskLevel
)

enum class RiskLevel(val label: String) {
    SAFE("SAFE / CLEAR WINDOW"),
    CAUTION("CAUTION (< 50 km)"),
    CRITICAL("COLLISION HAZARD (< 20 km)")
}

object CollisionScheduler {
    fun calculateLaunchWindows(activeSatellites: List<SatelliteTLE>): List<CollisionRiskWindow> {
        val windows = mutableListOf<CollisionRiskWindow>()
        val currentTime = System.currentTimeMillis()
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)

        for (offsetMin in listOf(0, 7, 15, 23, 31, 45)) {
            val delaySec = offsetMin * 60
            val windowTime = sdf.format(Date(currentTime + delaySec * 1000L))

            // Simulate satellite positions at window time
            var minDistanceKm = 9999.0
            var closestSat = "None"

            for (sat in activeSatellites) {
                val satPos = sat.computePosition(delaySec.toDouble())
                // Trajectory pass height estimation (~300 km)
                val satAltKm = (satPos.magnitude() - EARTH_RADIUS) / 1000.0
                val distKm = abs(satAltKm - 300.0) + (abs(satPos.x) % 400000.0) / 1000.0

                if (distKm < minDistanceKm) {
                    minDistanceKm = distKm
                    closestSat = sat.name.split(" ")[0]
                }
            }

            val risk = when {
                minDistanceKm < 25.0 -> RiskLevel.CRITICAL
                minDistanceKm < 65.0 -> RiskLevel.CAUTION
                else -> RiskLevel.SAFE
            }

            windows.add(
                CollisionRiskWindow(
                    launchDelaySeconds = delaySec,
                    windowTimeDisplay = windowTime,
                    closestApproachSatellite = closestSat,
                    missDistanceKm = minDistanceKm,
                    riskLevel = risk
                )
            )
        }
        return windows
    }
}

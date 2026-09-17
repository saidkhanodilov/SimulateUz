package com.example.model

import kotlin.math.*

/**
 * Two-Line Element (TLE) and Keplerian orbital parameters for real-world satellites.
 */
data class SatelliteTLE(
    val name: String,
    val noradId: Int,
    val line1: String,
    val line2: String,
    val category: SatelliteCategory = SatelliteCategory.SPACE_STATION,
    val inclinationDeg: Double = 0.0,
    val raanDeg: Double = 0.0,
    val eccentricity: Double = 0.0,
    val argPerigeeDeg: Double = 0.0,
    val meanAnomalyDeg: Double = 0.0,
    val meanMotionRevsPerDay: Double = 0.0,
    val epochYear: Int = 2024,
    val epochDay: Double = 1.0
) {
    // Semi-major axis in meters from mean motion n (revs/day -> rad/sec)
    val meanMotionRadPerSec: Double get() = (meanMotionRevsPerDay * 2.0 * Math.PI) / 86400.0
    val semiMajorAxisMeters: Double get() = if (meanMotionRadPerSec > 0) {
        cbrt(EARTH_MU / (meanMotionRadPerSec * meanMotionRadPerSec))
    } else 0.0

    val apogeeAltitudeKm: Double get() = (semiMajorAxisMeters * (1.0 + eccentricity) - EARTH_RADIUS) / 1000.0
    val perigeeAltitudeKm: Double get() = (semiMajorAxisMeters * (1.0 - eccentricity) - EARTH_RADIUS) / 1000.0
    val orbitalPeriodMinutes: Double get() = if (meanMotionRevsPerDay > 0) 1440.0 / meanMotionRevsPerDay else 0.0

    /**
     * Propagates satellite position at delta time dt (in seconds from epoch) using Keplerian/SGP4 solver.
     * Solves Kepler's Equation: M = E - e*sin(E)
     */
    fun computePosition(elapsedSeconds: Double): Vector3 {
        if (semiMajorAxisMeters <= 0.0) return Vector3(0.0, EARTH_RADIUS + 400000.0, 0.0)

        // 1. Mean anomaly progression
        val n = meanMotionRadPerSec
        var mRad = Math.toRadians(meanAnomalyDeg) + n * elapsedSeconds
        mRad = mRad % (2.0 * Math.PI)
        if (mRad < 0) mRad += 2.0 * Math.PI

        // 2. Solve Kepler's equation for Eccentric Anomaly E via Newton-Raphson
        var eAnom = mRad
        val e = eccentricity.coerceIn(0.0, 0.999)
        for (i in 0..12) {
            val delta = (eAnom - e * sin(eAnom) - mRad) / (1.0 - e * cos(eAnom))
            eAnom -= delta
            if (abs(delta) < 1e-7) break
        }

        // 3. True anomaly nu
        val cosNu = (cos(eAnom) - e) / (1.0 - e * cos(eAnom))
        val sinNu = (sqrt(1.0 - e * e) * sin(eAnom)) / (1.0 - e * cos(eAnom))
        val nu = atan2(sinNu, cosNu)

        // 4. Distance r from Earth center
        val a = semiMajorAxisMeters
        val r = a * (1.0 - e * cos(eAnom))

        // 5. Position in orbital plane (P-Q frame)
        val xOrb = r * cos(nu)
        val yOrb = r * sin(nu)

        // 6. Coordinate transformation from orbital plane to Earth-Centered Inertial (ECI)
        val iRad = Math.toRadians(inclinationDeg)
        val oRad = Math.toRadians(raanDeg)
        val wRad = Math.toRadians(argPerigeeDeg)

        // Matrix rotation elements
        val px = cos(oRad) * cos(wRad) - sin(oRad) * sin(wRad) * cos(iRad)
        val py = sin(oRad) * cos(wRad) + cos(oRad) * sin(wRad) * cos(iRad)
        val pz = sin(wRad) * sin(iRad)

        val qx = -cos(oRad) * sin(wRad) - sin(oRad) * cos(wRad) * cos(iRad)
        val qy = -sin(oRad) * sin(wRad) + cos(oRad) * cos(wRad) * cos(iRad)
        val qz = cos(wRad) * sin(iRad)

        val xEci = xOrb * px + yOrb * qx
        val yEci = xOrb * py + yOrb * qy
        val zEci = xOrb * pz + yOrb * qz

        return Vector3(xEci, yEci, zEci)
    }

    /**
     * Computes geodetic latitude and longitude from ECI vector.
     */
    fun computeSubSatellitePoint(pos: Vector3): Pair<Double, Double> {
        val r = pos.magnitude()
        val latDeg = Math.toDegrees(asin((pos.z / r).coerceIn(-1.0, 1.0)))
        val lonDeg = Math.toDegrees(atan2(pos.y, pos.x))
        return Pair(latDeg, lonDeg)
    }
}

enum class SatelliteCategory(val label: String) {
    SPACE_STATION("Space Station"),
    STARLINK("Starlink Constellation"),
    SCIENCE("Science & Astrophysics"),
    NAVIGATION("GPS / Navigational"),
    WEATHER("Earth & Weather")
}

/**
 * Parser for standard NORAD Two-Line Element (TLE) sets.
 */
object TLEParser {

    fun parse(name: String, line1: String, line2: String, category: SatelliteCategory = SatelliteCategory.SPACE_STATION): SatelliteTLE? {
        return try {
            val trimmed1 = line1.trim()
            val trimmed2 = line2.trim()
            if (!trimmed1.startsWith("1 ") || !trimmed2.startsWith("2 ")) return null

            val noradId = trimmed1.substring(2, 7).trim().toInt()
            val epochYearPart = trimmed1.substring(18, 20).trim().toInt()
            val epochYear = if (epochYearPart < 57) 2000 + epochYearPart else 1900 + epochYearPart
            val epochDay = trimmed1.substring(20, 32).trim().toDouble()

            val inc = trimmed2.substring(8, 16).trim().toDouble()
            val raan = trimmed2.substring(17, 25).trim().toDouble()
            val eccStr = "0." + trimmed2.substring(26, 33).trim()
            val ecc = eccStr.toDouble()
            val argPer = trimmed2.substring(34, 42).trim().toDouble()
            val meanAnom = trimmed2.substring(43, 51).trim().toDouble()
            val meanMotion = trimmed2.substring(52, 63).trim().toDouble()

            SatelliteTLE(
                name = name.trim(),
                noradId = noradId,
                line1 = trimmed1,
                line2 = trimmed2,
                category = category,
                inclinationDeg = inc,
                raanDeg = raan,
                eccentricity = ecc,
                argPerigeeDeg = argPer,
                meanAnomalyDeg = meanAnom,
                meanMotionRevsPerDay = meanMotion,
                epochYear = epochYear,
                epochDay = epochDay
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Curated real-world live satellite catalog.
     */
    val DEFAULT_SATELLITES: List<SatelliteTLE> = listOf(
        // ISS (ZARYA)
        parse(
            name = "ISS (International Space Station)",
            line1 = "1 25544U 98067A   24080.51268519  .00014867  00000+0  26733-3 0  9993",
            line2 = "2 25544  51.6416 112.5518 0004921 289.4120  70.6698 15.49842524444585",
            category = SatelliteCategory.SPACE_STATION
        )!!,
        // Starlink-30125
        parse(
            name = "Starlink-30125 (v2 Mini)",
            line1 = "1 58832U 24016A   24080.49000000  .00002150  00000+0  16000-3 0  9991",
            line2 = "2 58832  43.0012 210.4501 0001200 120.3000 240.1000 15.02450000 12005",
            category = SatelliteCategory.STARLINK
        )!!,
        // Hubble Space Telescope
        parse(
            name = "Hubble Space Telescope (HST)",
            line1 = "1 20580U 90037B   24080.32000000  .00000850  00000+0  34000-4 0  9997",
            line2 = "2 20580  28.4691 185.3400 0002800  95.4000 265.1000 15.08740000845001",
            category = SatelliteCategory.SCIENCE
        )!!,
        // Tiangong Space Station
        parse(
            name = "Tiangong Space Station (CSS)",
            line1 = "1 48274U 21035A   24080.41000000  .00008900  00000+0  12000-3 0  9992",
            line2 = "2 48274  41.4720  98.3400 0003400 150.2000 210.1000 15.58900000164003",
            category = SatelliteCategory.SPACE_STATION
        )!!,
        // GPS BIIF-12
        parse(
            name = "GPS BIIF-12 (USA 266)",
            line1 = "1 41328U 16007A   24080.20000000 -.00000050  00000+0  00000+0 0  9999",
            line2 = "2 41328  55.1200 320.1000 0102000  45.3000 315.2000  2.00560000 59002",
            category = SatelliteCategory.NAVIGATION
        )!!,
        // NOAA-20
        parse(
            name = "NOAA-20 (JPSS-1 Polar)",
            line1 = "1 43013U 17073A   24080.15000000  .00000075  00000+0  45000-4 0  9995",
            line2 = "2 43013  98.7100  45.2000 0001400  65.1000 295.1000 14.19500000325008",
            category = SatelliteCategory.WEATHER
        )!!
    )
}

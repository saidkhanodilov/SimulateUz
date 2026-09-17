package com.example

import com.example.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testPayloadEvaluationFalcon9() {
        val eval = RocketMarketDatabase.evaluatePayload(5000.0, TargetOrbit.LEO)
        assertNotNull(eval.recommendedVehicle)
        assertTrue(eval.estimatedCostUsd > 0)
        assertTrue(eval.requiredSpeedMps > 7000.0)
        assertTrue(eval.requiredForceNewtons > 0.0)
    }

    @Test
    fun testCollisionSchedulerLaunchWindows() {
        val windows = CollisionScheduler.calculateLaunchWindows(TLEParser.DEFAULT_SATELLITES)
        assertEquals(6, windows.size)
        assertTrue(windows.any { it.riskLevel == RiskLevel.SAFE })
    }

    @Test
    fun testTsiolkovskyDeltaV() {
        val falconStage = RocketPresets.FALCON_9.stages[0]
        val dv = falconStage.computeDeltaV(0.0)
        assertTrue("Stage 1 Delta-v should be > 5000 m/s", dv > 5000.0)
    }
}

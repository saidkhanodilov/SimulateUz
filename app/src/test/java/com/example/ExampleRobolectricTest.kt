package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SimulateUz", appName)
  }

  @Test
  fun `verify tsiolkovsky rocket equation`() {
    val stage = com.example.model.RocketPresets.FALCON_9.stages[0]
    val dv = stage.computeDeltaV(0.0)
    org.junit.Assert.assertTrue("Delta-v should exceed 5000 m/s", dv > 5000.0)
  }
}

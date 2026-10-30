package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.EngineState
import com.example.data.model.LockState
import com.example.data.model.VehicleState
import com.example.util.LocationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app_name string resource from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("My Mazda", appName)
  }

  @Test
  fun `verify vehicle initial state parameters`() {
    val state = VehicleState()
    assertEquals(LockState.LOCKED, state.lockState)
    assertEquals(EngineState.OFF, state.engineState)
    assertEquals(36.0, state.tpms.frontLeft.recommendedPsi, 0.01)
    assertTrue("Battery voltage must be healthy", state.batteryHealth.voltage >= 12.0)
  }

  @Test
  fun `verify distance calculation between coordinates`() {
    // Distance between Seattle Space Needle and Pike Place Market (~1.4 km)
    val distance = LocationHelper.calculateDistanceMeters(
      47.6205, -122.3493,
      47.6097, -122.3422
    )
    assertTrue("Distance should be approximately 1300-1500m", distance in 1100.0..1600.0)
  }
}

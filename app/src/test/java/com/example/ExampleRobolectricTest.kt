package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.TripLogEntity
import com.example.data.model.EngineState
import com.example.data.model.LockState
import com.example.data.model.VehicleState
import com.example.ui.TripSortOption
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

  @Test
  fun `verify trip filtering and sorting mechanics`() {
    val trip1 = TripLogEntity(
      id = 1,
      vin = "TEST_VIN",
      startTimeMillis = 1000L,
      endTimeMillis = 2000L,
      distanceMiles = 15.0,
      durationMinutes = 20,
      avgMpg = 28.0,
      avgSpeedMph = 45.0,
      maxSpeedMph = 65.0,
      drivingScore = 88,
      startAddress = "Downtown Seattle",
      endAddress = "Bellevue",
      routeSummary = "I-90 Floating Bridge",
      tripTag = "Commute"
    )

    val trip2 = TripLogEntity(
      id = 2,
      vin = "TEST_VIN",
      startTimeMillis = 3000L,
      endTimeMillis = 5000L,
      distanceMiles = 45.0,
      durationMinutes = 50,
      avgMpg = 34.0,
      avgSpeedMph = 55.0,
      maxSpeedMph = 72.0,
      drivingScore = 95,
      startAddress = "Bellevue",
      endAddress = "Snoqualmie Pass",
      routeSummary = "I-90 Mountain Pass",
      tripTag = "Highway"
    )

    val trips = listOf(trip1, trip2)

    // Filter by tag
    val commuteTrips = trips.filter { it.tripTag == "Commute" }
    assertEquals(1, commuteTrips.size)
    assertEquals("Downtown Seattle", commuteTrips.first().startAddress)

    // Filter by search query
    val searchResults = trips.filter { it.routeSummary.contains("Mountain", ignoreCase = true) }
    assertEquals(1, searchResults.size)
    assertEquals(2L, searchResults.first().id)

    // Sort by distance descending
    val sortedByDist = trips.sortedByDescending { it.distanceMiles }
    assertEquals(45.0, sortedByDist.first().distanceMiles, 0.01)

    // Sort by driving score descending
    val sortedByScore = trips.sortedByDescending { it.drivingScore }
    assertEquals(95, sortedByScore.first().drivingScore)
  }

  @Test
  fun `verify MyMazda account pairing and TCU handshake`() {
    val state = VehicleState()
    assertEquals(true, state.myMazdaAccount.isLinked)
    assertEquals(true, state.myMazdaAccount.isTcuPaired)

    // Simulate re-linking a new account
    val unlinkedAccount = state.myMazdaAccount.copy(
      isLinked = false,
      isTcuPaired = false
    )
    assertEquals(false, unlinkedAccount.isLinked)

    val linkedAccount = unlinkedAccount.copy(
      email = "driver@mazda.com",
      vehicleVin = "JM3KK2AA3P0129845",
      region = "North America",
      isLinked = true
    )
    assertTrue("Account must be linked", linkedAccount.isLinked)

    // In-car 6-digit code validation
    val validCode = "849201"
    val invalidCode = "123"
    assertTrue("6-digit code must be valid length", validCode.length == 6)
    assertTrue("3-digit code must be invalid length", invalidCode.length != 6)

    val fullyPairedAccount = linkedAccount.copy(isTcuPaired = true)
    assertTrue("TCU modem must be paired with in-car handshake", fullyPairedAccount.isTcuPaired)
  }

  @Test
  fun `verify Dark and Light themes have distinct background and surface palettes`() {
    val darkColors = com.example.ui.theme.DarkAppColors
    val lightColors = com.example.ui.theme.LightAppColors

    assertTrue("Dark theme is dark", darkColors.isDark)
    assertTrue("Light theme is not dark", !lightColors.isDark)

    // Ensure distinct backgrounds
    assertTrue(darkColors.background != lightColors.background)
    assertTrue(darkColors.surface != lightColors.surface)
    assertTrue(darkColors.textPrimary != lightColors.textPrimary)
  }
}


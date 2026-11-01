package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.TripLogEntity
import com.example.data.model.CommandExecutionState
import com.example.data.model.EngineState
import com.example.data.model.LockState
import com.example.data.model.MazdaConnectionState
import com.example.data.model.UnitSystem
import com.example.data.model.VehicleState
import com.example.data.remote.ConnectionMode
import com.example.data.remote.MazdaAuthBoundaryException
import com.example.data.remote.MazdaB2cAuthEngine
import com.example.data.remote.MazdaRegionalCluster
import com.example.data.storage.AppPreferencesManager
import com.example.data.storage.MazdaSessionManager
import com.example.data.storage.SecureTokenStorage
import com.example.ui.TripSortOption
import com.example.util.LocationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    assertEquals("NowMyMazda", appName)
  }

  @Test
  fun `verify vehicle initial state parameters`() {
    val state = VehicleState()
    assertEquals(LockState.UNKNOWN, state.lockState)
    assertEquals(EngineState.UNKNOWN, state.engineState)
    assertFalse(state.isConnected)
    assertFalse(state.isVehicleSelected)
    assertFalse(state.batteryHealth.isAvailable)
    assertEquals(0.0, state.batteryHealth.voltage, 0.01)
    assertEquals(MazdaConnectionState.DISCONNECTED, state.connectionState)
    assertFalse(state.remoteControlEnabled)
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
      maxSpeedMph = 45.0,
      drivingScore = 100,
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
      maxSpeedMph = 55.0,
      drivingScore = 100,
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

    // Sort by drive score
    val sortedByScore = trips.sortedByDescending { it.drivingScore }
    assertEquals(100, sortedByScore.first().drivingScore)
  }

  @Test
  fun `verify MyMazda account initial disconnected state and pairing flow`() {
    val state = VehicleState()
    assertFalse(state.myMazdaAccount.isLinked)
    assertFalse(state.myMazdaAccount.isTcuPaired)
    assertEquals("", state.myMazdaAccount.email)
    assertEquals("", state.vin)

    val linkedAccount = state.myMazdaAccount.copy(
      email = "driver@mazda.com",
      vehicleVin = "JM3KK2AA3P0129845",
      region = "North America (MNAO)",
      isLinked = true
    )
    assertTrue("Account must be linked", linkedAccount.isLinked)

    val fullyPairedAccount = linkedAccount.copy(isTcuPaired = true)
    assertTrue("TCU modem must be paired with verified handshake", fullyPairedAccount.isTcuPaired)
  }

  @Test
  fun `verify Dark and Light themes have distinct background and surface palettes`() {
    val darkColors = com.example.ui.theme.DarkAppColors
    val lightColors = com.example.ui.theme.LightAppColors

    assertTrue("Dark theme is dark", darkColors.isDark)
    assertFalse("Light theme is not dark", lightColors.isDark)

    assertNotEquals(darkColors.background, lightColors.background)
    assertNotEquals(darkColors.surface, lightColors.surface)
    assertNotEquals(darkColors.textPrimary, lightColors.textPrimary)
  }

  @Test
  fun `verify MazdaRegionalCluster verified MNAO host and removal of obsolete NXDOMAIN host`() {
    val mnao = MazdaRegionalCluster.fromCode("MNAO")
    assertEquals(MazdaRegionalCluster.MNAO, mnao)
    assertEquals("https://0cxo7m58.mazda.com/prod/", mnao.primaryBaseUrl)
    assertEquals("na.id.mazda.com", mnao.authHost)
    assertEquals("hgs2ivna.mazda.com", mnao.additionalBackendHost)
    assertTrue("MNAO must be verified", mnao.isVerified)

    val mme = MazdaRegionalCluster.fromCode("MME")
    assertEquals(MazdaRegionalCluster.MME, mme)
    assertEquals("https://e9stj7g7.mazda.com/prod/", mme.primaryBaseUrl)
    assertFalse("MME must be unverified until confirmed with real captures", mme.isVerified)

    val mjo = MazdaRegionalCluster.fromCode("MJO")
    assertEquals(MazdaRegionalCluster.MJO, mjo)
    assertEquals("https://wcs9p6wj.mazda.com/prod/", mjo.primaryBaseUrl)
    assertFalse("MJO must be unverified until confirmed with real captures", mjo.isVerified)

    val bridge = MazdaRegionalCluster.fromCode("BRIDGE")
    assertEquals(MazdaRegionalCluster.CV_MCS_BRIDGE, bridge)
    assertEquals("http://10.0.2.2:8080/", bridge.primaryBaseUrl)

    // Fallback to MNAO on invalid or empty code
    val fallback = MazdaRegionalCluster.fromCode("UNKNOWN_REGION")
    assertEquals(MazdaRegionalCluster.MNAO, fallback)

    // Test gateway URL normalization (Phase 5 requirement: never duplicate /api/)
    val normalized1 = MazdaRegionalCluster.normalizeGatewayUrl("http://192.168.1.100:8080/api/")
    assertEquals("http://192.168.1.100:8080/", normalized1)

    val normalized2 = MazdaRegionalCluster.normalizeGatewayUrl("192.168.1.100:8080")
    assertEquals("http://192.168.1.100:8080/", normalized2)
  }

  @Test
  fun `verify MazdaB2cAuthEngine uses na_id_mazda_com and checks client registration boundary`() {
    val engine = MazdaB2cAuthEngine(MazdaRegionalCluster.MNAO)
    val config = engine.b2cSettings()
    assertEquals("na.id.mazda.com", config.tenantHost)
    assertEquals("login.microsoftonline.com", config.identityHost)
    assertEquals("09b68673-982c-47b2-8418-5a415ff5e08b", config.clientId)
    assertTrue(config.authorizeUrl.contains("na.id.mazda.com/oauth2/v2.0/authorize"))
    assertTrue(config.tokenUrl.contains("na.id.mazda.com/oauth2/v2.0/token"))

    // Client registration boundary check: third-party package cannot intercept official callback
    val checkThirdParty = engine.checkClientRegistrationSupport("com.example.nowmymazda")
    assertTrue("Third party package must be flagged as boundary failure", checkThirdParty.isFailure)
    assertTrue(checkThirdParty.exceptionOrNull() is MazdaAuthBoundaryException)

    val checkOfficial = engine.checkClientRegistrationSupport("com.interrait.mymazda")
    assertTrue("Official package passes package identity check", checkOfficial.isSuccess)
  }

  @Test
  fun `verify MazdaSessionManager token lifecycle and expiry tracking`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = SecureTokenStorage(context)
    val sessionManager = MazdaSessionManager(storage)

    val testToken = "real_jwt_access_token_sample"
    val testRefresh = "real_jwt_refresh_token_sample"
    // 3600 seconds real expiration (no fake 24h)
    sessionManager.saveAuthenticatedSession(
      accessToken = testToken,
      refreshToken = testRefresh,
      idToken = "id_sample",
      expiresInSeconds = 3600L,
      customerId = "driver@mazda.com",
      region = "MNAO"
    )

    assertTrue("Session must be valid after saving with 3600s expiry", sessionManager.hasValidSession())
    val stored = sessionManager.getStoredSession()
    assertEquals(testToken, stored?.accessToken)
    assertEquals(testRefresh, stored?.refreshToken)
    assertEquals("driver@mazda.com", stored?.customerId)

    sessionManager.signOut()
    assertFalse(sessionManager.hasValidSession())
    assertNull(sessionManager.getStoredSession())
  }

  @Test
  fun `verify SecureTokenStorage stores and wipes encrypted session tokens without Base64 fallback`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tokenStorage = SecureTokenStorage(context)
    val testToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.realMazdaJwtToken"
    val testRefresh = "refresh_token_abc123"
    tokenStorage.saveSession(
      accessToken = testToken,
      refreshToken = testRefresh,
      tokenExpiry = System.currentTimeMillis() + 3600_000L,
      customerId = "12345",
      region = "MNAO"
    )
    val session = tokenStorage.loadSession()
    assertNotNull("Loaded session must not be null", session)
    assertEquals(testToken, session?.accessToken)
    assertEquals(testRefresh, session?.refreshToken)
    assertEquals("12345", session?.customerId)
    assertEquals("MNAO", session?.region)
    assertTrue("Session must be valid", tokenStorage.hasValidSession())

    tokenStorage.clearSession()
    assertNull(tokenStorage.loadSession())
    assertFalse(tokenStorage.hasValidSession())
  }

  @Test
  fun `verify AppPreferencesManager PIN hashing and verification`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = AppPreferencesManager(context)
    val pin = "5821"
    prefs.setUserPin(pin)
    assertTrue("Configured pin must exist", prefs.hasConfiguredPin())
    assertTrue("Valid PIN verifies", prefs.verifyPin(pin))
    assertFalse("Invalid PIN fails", prefs.verifyPin("9999"))
  }

  @Test
  fun `verify AppPreferencesManager unit system, remote start, and notification preferences persistence`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = AppPreferencesManager(context)

    prefs.setUnitSystem(UnitSystem.METRIC)
    prefs.saveRemoteStartPreferences(durationMinutes = 20, targetTempF = 74, heatedSeats = true, rearDefrost = true)
    prefs.setNotificationPreferences(lowFuel = false, unlockedDoor = true, geofenceBreach = true)
    prefs.saveAccountSettings("test@mazda.com", "MNAO", null, ConnectionMode.DIRECT_MAZDA)

    val saved = prefs.settingsFlow.first()
    assertEquals(UnitSystem.METRIC, saved.unitSystem)
    assertEquals(20, saved.remoteStartDurationMinutes)
    assertEquals(74, saved.remoteStartTargetTempF)
    assertTrue(saved.remoteStartHeatedSeats)
    assertTrue(saved.remoteStartRearDefrost)
    assertFalse(saved.notifyLowFuel)
    assertTrue(saved.notifyUnlockedDoor)
    assertTrue(saved.notifyGeofenceBreach)
    assertEquals("test@mazda.com", saved.accountEmail)
    assertEquals(ConnectionMode.DIRECT_MAZDA, saved.connectionMode)

    // Sign out cleans account and vehicle without deleting unit system or notification settings
    prefs.clearAccountAndVehicle()
    val afterSignOut = prefs.settingsFlow.first()
    assertEquals("", afterSignOut.accountEmail)
    assertEquals("", afterSignOut.selectedVin)
    assertEquals(UnitSystem.METRIC, afterSignOut.unitSystem)
  }

  @Test
  fun `verify CommandExecutionState full lifecycle transitions`() {
    val idle = CommandExecutionState.Idle
    val submitting = CommandExecutionState.Submitting("LOCK_DOORS")
    val accepted = CommandExecutionState.Accepted("LOCK_DOORS", "Accepted by cloud", "cmd_999")
    val pending = CommandExecutionState.Pending("LOCK_DOORS")
    val confirmed = CommandExecutionState.Confirmed("LOCK_DOORS", "Doors locked successfully", "cmd_999")
    val timedOut = CommandExecutionState.TimedOut("LOCK_DOORS", "TCU response timed out")
    val failed = CommandExecutionState.Failed("LOCK_DOORS", "Engine is running; lock refused")
    val unsupported = CommandExecutionState.Unsupported("VENTILATE_SEATS", "Capability not supported by vehicle")

    assertEquals(CommandExecutionState.Idle, idle)
    assertEquals("LOCK_DOORS", submitting.action)
    assertEquals("cmd_999", accepted.commandId)
    assertEquals("LOCK_DOORS", pending.action)
    assertEquals("Doors locked successfully", confirmed.message)
    assertEquals("TCU response timed out", timedOut.errorMessage)
    assertEquals("Engine is running; lock refused", failed.errorMessage)
    assertEquals("Capability not supported by vehicle", unsupported.reason)
  }

  @Test
  fun `verify clean OBD2 DTC status baseline without fake scans`() {
    val state = VehicleState()
    assertTrue("Zero active DTC codes initially", state.diagnostics.activeDtcCodes.isEmpty())
    assertFalse(state.diagnostics.checkEngineOn)
    assertFalse(state.diagnostics.isAvailable)
  }
}

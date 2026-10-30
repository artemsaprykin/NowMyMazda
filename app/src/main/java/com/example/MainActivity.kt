package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.VehicleNavTab
import com.example.ui.VehicleViewModel
import com.example.ui.components.BiometricPromptDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.LocateScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TripLogScreen
import com.example.ui.theme.CabinBackground
import com.example.ui.theme.CabinBorder
import com.example.ui.theme.CabinSurface
import com.example.ui.theme.DiagnosticGreen
import com.example.ui.theme.MetallicGray
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PlatinumSilver
import com.example.ui.theme.SoulRedDark
import com.example.ui.theme.SoulRedLight
import com.example.ui.theme.SoulRedPrimary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: VehicleViewModel = viewModel()
) {
    val vehicleState by viewModel.vehicleState.collectAsStateWithLifecycle()
    val tripLogs by viewModel.tripLogs.collectAsStateWithLifecycle()
    val maintenanceReminders by viewModel.maintenanceReminders.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val securityConfig by viewModel.securityConfig.collectAsStateWithLifecycle()
    val pendingAction by viewModel.pendingSecurityAction.collectAsStateWithLifecycle()
    val biometricErrorMessage by viewModel.biometricErrorMessage.collectAsStateWithLifecycle()

    // Request notification permissions on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Handle system back navigation to return to Dashboard if on sub-screen
    BackHandler(enabled = currentTab != VehicleNavTab.DASHBOARD) {
        viewModel.selectTab(VehicleNavTab.DASHBOARD)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CabinBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Mazda Winged Badge
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SoulRedDark)
                                .border(1.dp, SoulRedLight.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = PlatinumSilver,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PlatinumSilver
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(DiagnosticGreen)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = vehicleState.nickname,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MetallicGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.syncCloudApi() },
                        modifier = Modifier.testTag("app_bar_sync_btn")
                    ) {
                        if (vehicleState.cloudConfig.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = SoulRedLight
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync",
                                tint = PlatinumSilver
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CabinBackground,
                    titleContentColor = PlatinumSilver
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CabinSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == VehicleNavTab.DASHBOARD,
                    onClick = { viewModel.selectTab(VehicleNavTab.DASHBOARD) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = stringResource(id = R.string.nav_dashboard)
                        )
                    },
                    label = { Text(stringResource(id = R.string.nav_dashboard), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = SoulRedLight,
                        indicatorColor = SoulRedPrimary,
                        unselectedIconColor = MetallicGray,
                        unselectedTextColor = MetallicGray
                    ),
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                NavigationBarItem(
                    selected = currentTab == VehicleNavTab.DIAGNOSTICS,
                    onClick = { viewModel.selectTab(VehicleNavTab.DIAGNOSTICS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = stringResource(id = R.string.nav_diagnostics)
                        )
                    },
                    label = { Text(stringResource(id = R.string.nav_diagnostics), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = SoulRedLight,
                        indicatorColor = SoulRedPrimary,
                        unselectedIconColor = MetallicGray,
                        unselectedTextColor = MetallicGray
                    ),
                    modifier = Modifier.testTag("nav_tab_diagnostics")
                )

                NavigationBarItem(
                    selected = currentTab == VehicleNavTab.LOCATE,
                    onClick = { viewModel.selectTab(VehicleNavTab.LOCATE) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = stringResource(id = R.string.nav_location)
                        )
                    },
                    label = { Text(stringResource(id = R.string.nav_location), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = SoulRedLight,
                        indicatorColor = SoulRedPrimary,
                        unselectedIconColor = MetallicGray,
                        unselectedTextColor = MetallicGray
                    ),
                    modifier = Modifier.testTag("nav_tab_locate")
                )

                NavigationBarItem(
                    selected = currentTab == VehicleNavTab.TRIPS,
                    onClick = { viewModel.selectTab(VehicleNavTab.TRIPS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = stringResource(id = R.string.nav_trips)
                        )
                    },
                    label = { Text(stringResource(id = R.string.nav_trips), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = SoulRedLight,
                        indicatorColor = SoulRedPrimary,
                        unselectedIconColor = MetallicGray,
                        unselectedTextColor = MetallicGray
                    ),
                    modifier = Modifier.testTag("nav_tab_trips")
                )

                NavigationBarItem(
                    selected = currentTab == VehicleNavTab.SETTINGS,
                    onClick = { viewModel.selectTab(VehicleNavTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(id = R.string.nav_settings)
                        )
                    },
                    label = { Text(stringResource(id = R.string.nav_settings), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = SoulRedLight,
                        indicatorColor = SoulRedPrimary,
                        unselectedIconColor = MetallicGray,
                        unselectedTextColor = MetallicGray
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentTab,
            modifier = Modifier.padding(innerPadding),
            label = "tab_crossfade"
        ) { tab ->
            when (tab) {
                VehicleNavTab.DASHBOARD -> DashboardScreen(
                    vehicleState = vehicleState,
                    onRemoteStart = { duration, temp -> viewModel.requestRemoteStart(duration, temp) },
                    onEngineStop = { viewModel.requestStopEngine() },
                    onDoorToggle = { viewModel.requestDoorToggle() },
                    onHazardToggle = { viewModel.toggleHazardLights() },
                    onPanicHorn = { viewModel.panicHorn() },
                    onTrunkToggle = { viewModel.toggleTrunk() },
                    onCalibrateTpms = { viewModel.calibrateTpms() },
                    onSimulateTireLeak = { viewModel.simulateTirePressureLeak() },
                    onSyncCloud = { viewModel.syncCloudApi() },
                    onSaveCloudConfig = { url, token -> viewModel.updateCloudConfig(url, token) }
                )

                VehicleNavTab.DIAGNOSTICS -> DiagnosticsScreen(
                    vehicleState = vehicleState,
                    maintenanceReminders = maintenanceReminders,
                    onReminderToggle = { id, done -> viewModel.setReminderCompleted(id, done) },
                    onCalibrateTpms = { viewModel.calibrateTpms() },
                    onSimulateTireLeak = { viewModel.simulateTirePressureLeak() },
                    onSimulateBatteryDrain = { viewModel.simulateBatteryDrain() },
                    onRestoreBattery = { viewModel.restoreHealthyBattery() }
                )

                VehicleNavTab.LOCATE -> LocateScreen(
                    vehicleState = vehicleState,
                    onFindCarFlashHorn = { viewModel.panicHorn() },
                    onSetSafeZone = { viewModel.setSafeZoneCurrent() },
                    onUpdateRadius = { radius -> viewModel.updateGeofenceRadius(radius) },
                    onToggleGeofence = { enabled -> viewModel.toggleGeofence(enabled) },
                    onSimulateBreach = { viewModel.simulateGeofenceBreach() },
                    onDismissAlarm = { viewModel.requestDismissTheftAlarm() }
                )

                VehicleNavTab.TRIPS -> TripLogScreen(
                    trips = tripLogs,
                    onAddTrip = { dist, dur, mpg, start, end, tag ->
                        viewModel.addManualTrip(dist, dur, mpg, start, end, tag)
                    }
                )

                VehicleNavTab.SETTINGS -> SettingsScreen(
                    vehicleState = vehicleState,
                    securityConfig = securityConfig,
                    onUpdateSecurityConfig = { cfg -> viewModel.updateSecurityConfig(cfg) },
                    onSaveCloudConfig = { url, token -> viewModel.updateCloudConfig(url, token) },
                    onSyncCloud = { viewModel.syncCloudApi() }
                )
            }
        }

        // Biometric Security Gate Modal
        pendingAction?.let { action ->
            BiometricPromptDialog(
                pendingAction = action,
                errorMessage = biometricErrorMessage,
                onBiometricSuccess = { viewModel.authenticateWithBiometricsSuccess() },
                onPinSubmit = { pin -> viewModel.authenticateWithPin(pin) },
                onDismiss = { viewModel.cancelPendingAction() }
            )
        }
    }
}

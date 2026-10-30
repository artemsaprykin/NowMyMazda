package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EngineState
import com.example.data.model.LockState
import com.example.data.model.PressureStatus
import com.example.data.model.TireInfo
import com.example.data.model.VehicleState
import com.example.ui.components.CloudApiConfigDialog
import com.example.ui.components.TireDetailDialog
import com.example.ui.components.VehicleTopDownView
import com.example.ui.theme.CabinBorder
import com.example.ui.theme.CabinSurface
import com.example.ui.theme.CabinSurfaceElevated
import com.example.ui.theme.ClimateBlue
import com.example.ui.theme.DiagnosticAmber
import com.example.ui.theme.DiagnosticGreen
import com.example.ui.theme.DiagnosticRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MetallicGray
import com.example.ui.theme.PlatinumSilver
import com.example.ui.theme.SoulRedDark
import com.example.ui.theme.SoulRedLight
import com.example.ui.theme.SoulRedPrimary

@Composable
fun DashboardScreen(
    vehicleState: VehicleState,
    onRemoteStart: (Int, Int) -> Unit,
    onEngineStop: () -> Unit,
    onDoorToggle: () -> Unit,
    onHazardToggle: () -> Unit,
    onPanicHorn: () -> Unit,
    onTrunkToggle: () -> Unit,
    onCalibrateTpms: () -> Unit,
    onSimulateTireLeak: () -> Unit,
    onSyncCloud: () -> Unit,
    onSaveCloudConfig: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTire by remember { mutableStateOf<TireInfo?>(null) }
    var showCloudDialog by remember { mutableStateOf(false) }

    var selectedDuration by remember { mutableIntStateOf(15) }
    var selectedTemp by remember { mutableIntStateOf(72) }
    var heatedSeats by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Cloud Connection Header
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CabinSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CabinBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCloudDialog = true }
                    .testTag("cloud_status_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(DiagnosticGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Mazda Cloud Telematics: Online",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = PlatinumSilver
                            )
                            Text(
                                text = "TCU 4G LTE • Signal ${vehicleState.cloudConfig.cellularSignalBars}/5 bars",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MetallicGray
                            )
                        }
                    }

                    IconButton(
                        onClick = onSyncCloud,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("sync_cloud_button")
                    ) {
                        if (vehicleState.cloudConfig.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = SoulRedLight
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync",
                                tint = MetallicGray
                            )
                        }
                    }
                }
            }
        }

        // Active Alert Banner if any tire or battery has issue
        item {
            val hasLowTire = vehicleState.tpms.frontLeft.status != PressureStatus.NORMAL ||
                    vehicleState.tpms.frontRight.status != PressureStatus.NORMAL ||
                    vehicleState.tpms.rearLeft.status != PressureStatus.NORMAL ||
                    vehicleState.tpms.rearRight.status != PressureStatus.NORMAL

            val hasBatteryWarning = vehicleState.batteryHealth.voltage < 12.0

            if (hasLowTire || hasBatteryWarning || vehicleState.geofence.theftAlarmActive) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1519)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DiagnosticRed)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = DiagnosticRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = when {
                                    vehicleState.geofence.theftAlarmActive -> "THEFT ALARM: Geofence Breached!"
                                    hasBatteryWarning -> "12V Battery Low: ${vehicleState.batteryHealth.voltage}V"
                                    else -> "Tire Pressure Warning Detected"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Open Diagnostics to inspect real-time telematics and resolve.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = PlatinumSilver.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Interactive Vehicle Diagram
        item {
            VehicleTopDownView(
                vehicleState = vehicleState,
                onTireClick = { selectedTire = it }
            )
        }

        // Remote Controls Grid
        item {
            Text(
                text = "Remote Controls",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PlatinumSilver
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Remote Engine Start / Stop Master Button
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CabinSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CabinBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (vehicleState.engineState == EngineState.RUNNING) SoulRedDark else CabinSurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (vehicleState.engineState == EngineState.RUNNING) Icons.Default.Stop else Icons.Default.ElectricBolt,
                                    contentDescription = null,
                                    tint = if (vehicleState.engineState == EngineState.RUNNING) Color.White else SoulRedLight,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (vehicleState.engineState == EngineState.RUNNING) "Engine is Running" else "Remote Engine Start",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PlatinumSilver
                                )
                                Text(
                                    text = if (vehicleState.engineState == EngineState.RUNNING) {
                                        val remMins = vehicleState.remoteStart.remainingSeconds / 60
                                        val remSecs = vehicleState.remoteStart.remainingSeconds % 60
                                        "Auto-stop in ${remMins}m ${remSecs}s • Climate ${vehicleState.remoteStart.targetTempF}°F"
                                    } else {
                                        "Secure cloud ignition with climate preset"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MetallicGray
                                )
                            }
                        }
                    }

                    if (vehicleState.engineState != EngineState.RUNNING) {
                        Spacer(modifier = Modifier.height(14.dp))
                        // Climate & Timer selectors
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Runtime:", style = MaterialTheme.typography.bodySmall, color = MetallicGray)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(10, 15, 20).forEach { mins ->
                                    FilterChip(
                                        selected = selectedDuration == mins,
                                        onClick = { selectedDuration = mins },
                                        label = { Text("${mins}m") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SoulRedPrimary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AcUnit,
                                    contentDescription = null,
                                    tint = ClimateBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Climate Preset:", style = MaterialTheme.typography.bodySmall, color = MetallicGray)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(68, 72, 76).forEach { temp ->
                                    FilterChip(
                                        selected = selectedTemp == temp,
                                        onClick = { selectedTemp = temp },
                                        label = { Text("${temp}°F") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SoulRedPrimary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onRemoteStart(selectedDuration, selectedTemp) },
                            colors = ButtonDefaults.buttonColors(containerColor = SoulRedPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("remote_start_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Engine (${selectedDuration} min • ${selectedTemp}°F)", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onEngineStop,
                            colors = ButtonDefaults.buttonColors(containerColor = DiagnosticRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("stop_engine_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Turn Off Engine", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Control Buttons (Lock, Flash Lights, Panic Horn, Trunk)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    icon = if (vehicleState.lockState == LockState.LOCKED) Icons.Default.LockOpen else Icons.Default.Lock,
                    label = if (vehicleState.lockState == LockState.LOCKED) "Unlock" else "Lock Doors",
                    subtext = if (vehicleState.lockState == LockState.LOCKED) "Secured" else "Open",
                    isActive = vehicleState.lockState == LockState.LOCKED,
                    modifier = Modifier.weight(1f),
                    testTag = "lock_toggle_btn",
                    onClick = onDoorToggle
                )

                QuickActionButton(
                    icon = Icons.Default.LightMode,
                    label = "Hazard Flash",
                    subtext = if (vehicleState.hazardLightsActive) "Flashing" else "Off",
                    isActive = vehicleState.hazardLightsActive,
                    activeColor = DiagnosticAmber,
                    modifier = Modifier.weight(1f),
                    testTag = "hazard_toggle_btn",
                    onClick = onHazardToggle
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.NotificationsActive,
                    label = "Panic Alarm",
                    subtext = if (vehicleState.hornActive) "Honking" else "Horn & Flash",
                    isActive = vehicleState.hornActive,
                    activeColor = DiagnosticRed,
                    modifier = Modifier.weight(1f),
                    testTag = "panic_horn_btn",
                    onClick = onPanicHorn
                )

                QuickActionButton(
                    icon = Icons.Default.FlightTakeoff,
                    label = "Liftgate Trunk",
                    subtext = if (vehicleState.doors.isTrunkOpen) "Trunk Open" else "Closed",
                    isActive = vehicleState.doors.isTrunkOpen,
                    activeColor = ElectricCyan,
                    modifier = Modifier.weight(1f),
                    testTag = "trunk_toggle_btn",
                    onClick = onTrunkToggle
                )
            }
        }

        // Fuel & Battery Progress Cards
        item {
            Text(
                text = "Vehicle Telemetry",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PlatinumSilver
            )
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CabinSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CabinBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Fuel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = PlatinumSilver, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fuel Level", style = MaterialTheme.typography.bodyMedium, color = PlatinumSilver)
                        }
                        Text(
                            text = "${vehicleState.fuelPercent}% • ${vehicleState.estimatedRangeMiles} mi range",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PlatinumSilver
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { vehicleState.fuelPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = SoulRedPrimary,
                        trackColor = CabinSurfaceElevated
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 12V Battery Health bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = DiagnosticGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("12V Battery Health (SoH)", style = MaterialTheme.typography.bodyMedium, color = PlatinumSilver)
                        }
                        Text(
                            text = "${vehicleState.batteryHealth.voltage}V • ${vehicleState.batteryHealth.stateOfHealthPercent}% SoH",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (vehicleState.batteryHealth.voltage >= 12.2) DiagnosticGreen else DiagnosticAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { vehicleState.batteryHealth.stateOfHealthPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = DiagnosticGreen,
                        trackColor = CabinSurfaceElevated
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal Tire Dialog
    selectedTire?.let { tire ->
        TireDetailDialog(
            tire = tire,
            onCalibrate = onCalibrateTpms,
            onSimulateLeak = onSimulateTireLeak,
            onDismiss = { selectedTire = null }
        )
    }

    // Modal Cloud Config Dialog
    if (showCloudDialog) {
        CloudApiConfigDialog(
            config = vehicleState.cloudConfig,
            onSave = onSaveCloudConfig,
            onTestPing = onSyncCloud,
            onDismiss = { showCloudDialog = false }
        )
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    subtext: String,
    isActive: Boolean,
    activeColor: Color = DiagnosticGreen,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = CabinSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) activeColor.copy(alpha = 0.5f) else CabinBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isActive) activeColor.copy(alpha = 0.2f) else CabinSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) activeColor else MetallicGray,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PlatinumSilver
                )
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = if (isActive) activeColor else MetallicGray
                )
            }
        }
    }
}

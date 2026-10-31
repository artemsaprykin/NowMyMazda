package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.UnitSystem
import com.example.data.model.VehicleState
import com.example.ui.components.MyMazdaConnectDialog
import com.example.ui.theme.AppTheme
import com.example.ui.theme.DiagnosticAmber
import com.example.ui.theme.DiagnosticGreen
import com.example.ui.theme.DiagnosticRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SoulRedDark
import com.example.ui.theme.SoulRedLight
import com.example.ui.theme.SoulRedPrimary
import com.example.ui.theme.ThemeMode
import com.example.util.BiometricSecurityConfig

@Composable
fun SettingsScreen(
    vehicleState: VehicleState,
    securityConfig: BiometricSecurityConfig,
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onUnitSystemChange: (UnitSystem) -> Unit = {},
    onSaveRemoteStartPreferences: (durationMinutes: Int, targetTempF: Int, heatedSeats: Boolean, rearDefrost: Boolean) -> Unit = { _, _, _, _ -> },
    onSaveNotificationPreferences: (lowFuel: Boolean, unlockedDoor: Boolean, geofenceBreach: Boolean) -> Unit = { _, _, _ -> },
    onLinkMyMazda: (email: String, vin: String, region: String) -> Unit,
    onPairInCarCode: (code: String) -> Unit,
    onUpdateSecurityConfig: (BiometricSecurityConfig) -> Unit,
    onSaveCloudConfig: (String, String) -> Unit,
    onSyncCloud: () -> Unit,
    onSetUserPin: (String) -> Unit = {},
    onOpenVehicleConnection: () -> Unit = {},
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    var showMyMazdaDialog by remember { mutableStateOf(false) }
    var showPinChangeDialog by remember { mutableStateOf(false) }

    // Remote start preferences state
    var remoteDuration by remember(vehicleState.remoteStart.runDurationMinutes) {
        mutableIntStateOf(vehicleState.remoteStart.runDurationMinutes)
    }
    var remoteTemp by remember(vehicleState.remoteStart.targetTempF) {
        mutableIntStateOf(vehicleState.remoteStart.targetTempF)
    }
    var remoteHeatedSeats by remember(vehicleState.remoteStart.heatedSeatsEnabled) {
        mutableStateOf(vehicleState.remoteStart.heatedSeatsEnabled)
    }
    var remoteRearDefrost by remember(vehicleState.remoteStart.rearDefrostEnabled) {
        mutableStateOf(vehicleState.remoteStart.rearDefrostEnabled)
    }

    // Notification preferences state
    var notifyLowFuel by remember { mutableStateOf(true) }
    var notifyUnlockedDoor by remember { mutableStateOf(true) }
    var notifyGeofence by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Preferences & Security Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = "Appearance, measurement units, climate presets, notifications, and security.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }

        // Appearance & Units Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("theme_settings_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SoulRedDark.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = SoulRedLight, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Appearance & Units",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Select display theme and measurement units",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Theme Mode", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeOptionTile(
                            title = "Dark Cockpit",
                            subtitle = "Soul Red & Obsidian",
                            icon = Icons.Default.Brightness4,
                            isSelected = currentTheme == ThemeMode.DARK,
                            onClick = { onThemeChange(ThemeMode.DARK) },
                            modifier = Modifier.weight(1f),
                            testTag = "theme_option_dark"
                        )
                        ThemeOptionTile(
                            title = "Light Horizon",
                            subtitle = "Clean Metallic Silver",
                            icon = Icons.Default.Brightness7,
                            isSelected = currentTheme == ThemeMode.LIGHT,
                            onClick = { onThemeChange(ThemeMode.LIGHT) },
                            modifier = Modifier.weight(1f),
                            testTag = "theme_option_light"
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Measurement Units", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = vehicleState.unitSystem == UnitSystem.IMPERIAL,
                            onClick = { onUnitSystemChange(UnitSystem.IMPERIAL) },
                            label = { Text("Imperial (mi, psi, °F)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoulRedPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = colors.surfaceElevated,
                                labelColor = colors.textSecondary
                            ),
                            modifier = Modifier.testTag("unit_chip_imperial")
                        )
                        FilterChip(
                            selected = vehicleState.unitSystem == UnitSystem.METRIC,
                            onClick = { onUnitSystemChange(UnitSystem.METRIC) },
                            label = { Text("Metric (km, bar, °C)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoulRedPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = colors.surfaceElevated,
                                labelColor = colors.textSecondary
                            ),
                            modifier = Modifier.testTag("unit_chip_metric")
                        )
                    }
                }
            }
        }

        // Remote Climate & Engine Start Presets Card (Phase 9)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("remote_start_settings_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SoulRedDark.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AcUnit, contentDescription = null, tint = SoulRedLight, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Remote Start & Climate Presets",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Configures default engine run duration and cabin temperature",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Engine Run Duration", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 15, 20).forEach { mins ->
                            FilterChip(
                                selected = remoteDuration == mins,
                                onClick = {
                                    remoteDuration = mins
                                    onSaveRemoteStartPreferences(mins, remoteTemp, remoteHeatedSeats, remoteRearDefrost)
                                },
                                label = { Text("$mins Minutes") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SoulRedPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = colors.surfaceElevated,
                                    labelColor = colors.textSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Target Cabin Temperature", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(68, 70, 72, 74).forEach { temp ->
                            FilterChip(
                                selected = remoteTemp == temp,
                                onClick = {
                                    remoteTemp = temp
                                    onSaveRemoteStartPreferences(remoteDuration, temp, remoteHeatedSeats, remoteRearDefrost)
                                },
                                label = { Text("$temp°F") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SoulRedPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = colors.surfaceElevated,
                                    labelColor = colors.textSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchRow(
                        title = "Enable Heated Seats",
                        subtitle = "Automatically warms driver seat during remote start",
                        checked = remoteHeatedSeats,
                        onCheckedChange = {
                            remoteHeatedSeats = it
                            onSaveRemoteStartPreferences(remoteDuration, remoteTemp, it, remoteRearDefrost)
                        },
                        tag = "switch_heated_seats"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSwitchRow(
                        title = "Enable Rear Window Defrost",
                        subtitle = "Activates rear defrost grid in cold temperatures",
                        checked = remoteRearDefrost,
                        onCheckedChange = {
                            remoteRearDefrost = it
                            onSaveRemoteStartPreferences(remoteDuration, remoteTemp, remoteHeatedSeats, it)
                        },
                        tag = "switch_rear_defrost"
                    )
                }
            }
        }

        // Notification Alerts Card (Phase 9)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notification_settings_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SoulRedDark.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = SoulRedLight, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Push Notifications & Alerts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Manage vehicle security and health notifications",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingSwitchRow(
                        title = "Low Fuel Warning",
                        subtitle = "Alerts when fuel drops below 15% capacity",
                        checked = notifyLowFuel,
                        onCheckedChange = {
                            notifyLowFuel = it
                            onSaveNotificationPreferences(it, notifyUnlockedDoor, notifyGeofence)
                        },
                        tag = "switch_notify_fuel"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSwitchRow(
                        title = "Unlocked Doors Alert",
                        subtitle = "Sends alert if vehicle remains unlocked after parking",
                        checked = notifyUnlockedDoor,
                        onCheckedChange = {
                            notifyUnlockedDoor = it
                            onSaveNotificationPreferences(notifyLowFuel, it, notifyGeofence)
                        },
                        tag = "switch_notify_unlocked"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSwitchRow(
                        title = "Geofence Perimeter Breach",
                        subtitle = "Urgent alert if vehicle leaves designated safe zone",
                        checked = notifyGeofence,
                        onCheckedChange = {
                            notifyGeofence = it
                            onSaveNotificationPreferences(notifyLowFuel, notifyUnlockedDoor, it)
                        },
                        tag = "switch_notify_geofence"
                    )
                }
            }
        }

        // Biometric Security Controls
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("biometric_settings_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SoulRedDark.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = SoulRedLight, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Biometric Authentication Gate",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Require fingerprint or biometric scan before sensitive vehicle commands",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingSwitchRow(
                        title = "Require Biometric for Remote Start",
                        subtitle = "Prevents unauthorized remote engine ignition",
                        checked = securityConfig.requireForRemoteStart,
                        onCheckedChange = {
                            onUpdateSecurityConfig(securityConfig.copy(requireForRemoteStart = it))
                        },
                        tag = "bio_switch_remote_start"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingSwitchRow(
                        title = "Require Biometric for Door Unlock",
                        subtitle = "Verifies fingerprint before unlocking doors remotely",
                        checked = securityConfig.requireForDoorUnlock,
                        onCheckedChange = {
                            onUpdateSecurityConfig(securityConfig.copy(requireForDoorUnlock = it))
                        },
                        tag = "bio_switch_door_unlock"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingSwitchRow(
                        title = "Require Biometric for Theft Siren Dismiss",
                        subtitle = "Requires biometric scan to silence geofence alarms",
                        checked = securityConfig.requireForTheftAlarm,
                        onCheckedChange = {
                            onUpdateSecurityConfig(securityConfig.copy(requireForTheftAlarm = it))
                        },
                        tag = "bio_switch_theft_alarm"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showPinChangeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("change_pin_btn")
                    ) {
                        Icon(Icons.Default.Password, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (securityConfig.hasConfiguredPin) "Change 4-Digit Security PIN" else "Configure 4-Digit Security PIN",
                            color = colors.textPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // MyMazda Connected Vehicle Account Card
        item {
            val isConnected = vehicleState.isConnected && vehicleState.isVehicleSelected
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mymazda_account_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SoulRedDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "MyMazda Account & Telematics",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Connected via ${vehicleState.myMazdaAccount.region}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isConnected) Color(0xFF142B1F) else Color(0xFF2E2014)
                        ) {
                            Text(
                                text = if (isConnected) "Connected" else "Disconnected",
                                color = if (isConnected) DiagnosticGreen else DiagnosticAmber,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Linked Email", color = colors.textSecondary, fontSize = 11.sp)
                                Text(vehicleState.myMazdaAccount.email.ifBlank { "No Account Linked" }, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Active VIN", color = colors.textSecondary, fontSize = 11.sp)
                                Text(vehicleState.vin.ifBlank { "No Vehicle Selected" }, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TCU Modem", color = colors.textSecondary, fontSize = 11.sp)
                                Text(
                                    if (vehicleState.myMazdaAccount.isTcuPaired) "Active & Paired" else "Not Connected",
                                    color = if (vehicleState.myMazdaAccount.isTcuPaired) DiagnosticGreen else DiagnosticAmber,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenVehicleConnection,
                            colors = ButtonDefaults.buttonColors(containerColor = SoulRedPrimary),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("open_vehicle_connection_btn")
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Vehicle Connection", fontSize = 12.sp)
                        }

                        if (vehicleState.myMazdaAccount.isLinked) {
                            OutlinedButton(
                                onClick = onSignOut,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sign_out_account_btn")
                            ) {
                                Text("Sign Out", fontSize = 12.sp, color = DiagnosticAmber)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showMyMazdaDialog) {
        MyMazdaConnectDialog(
            account = vehicleState.myMazdaAccount,
            onLinkAccount = onLinkMyMazda,
            onPairWithInCarCode = onPairInCarCode,
            onDismiss = { showMyMazdaDialog = false }
        )
    }

    if (showPinChangeDialog) {
        ChangePinDialog(
            onSavePin = { pin ->
                onSetUserPin(pin)
                showPinChangeDialog = false
            },
            onDismiss = { showPinChangeDialog = false }
        )
    }
}

@Composable
fun ThemeOptionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val colors = AppTheme.colors

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) SoulRedDark.copy(alpha = 0.25f) else colors.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) SoulRedPrimary else colors.border
        ),
        modifier = modifier
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) SoulRedLight else colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(SoulRedPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String = ""
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SoulRedPrimary,
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.surfaceElevated
            )
        )
    }
}

@Composable
fun ChangePinDialog(
    onSavePin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    var pinValue by remember { mutableStateOf("") }
    var pinConfirmValue by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("change_pin_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Configure 4-Digit Security PIN",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Text(
                    text = "This PIN serves as a backup authorization mechanism for remote engine start and door unlock operations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                OutlinedTextField(
                    value = pinValue,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinValue = it },
                    label = { Text("Enter 4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoulRedPrimary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("new_pin_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = pinConfirmValue,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinConfirmValue = it },
                    label = { Text("Confirm 4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoulRedPrimary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("confirm_pin_input")
                )

                errorText?.let { err ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = err, color = DiagnosticRed, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                    Button(
                        onClick = {
                            if (pinValue.length != 4) {
                                errorText = "PIN must be exactly 4 digits."
                            } else if (pinValue != pinConfirmValue) {
                                errorText = "PIN entries do not match."
                            } else {
                                onSavePin(pinValue)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoulRedPrimary),
                        modifier = Modifier.weight(1f).testTag("save_pin_submit_btn")
                    ) {
                        Text("Save PIN")
                    }
                }
            }
        }
    }
}

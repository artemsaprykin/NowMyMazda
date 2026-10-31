package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.VehicleState
import com.example.ui.components.CloudApiConfigDialog
import com.example.ui.components.MyMazdaConnectDialog
import com.example.ui.theme.AppTheme
import com.example.ui.theme.DiagnosticGreen
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
    onLinkMyMazda: (email: String, vin: String, region: String) -> Unit,
    onPairInCarCode: (code: String) -> Unit,
    onUpdateSecurityConfig: (BiometricSecurityConfig) -> Unit,
    onSaveCloudConfig: (String, String) -> Unit,
    onSyncCloud: () -> Unit,
    onOpenVehicleConnection: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    var showCloudDialog by remember { mutableStateOf(false) }
    var showMyMazdaDialog by remember { mutableStateOf(false) }
    var showPinChangeDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Security & App Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = "Themes, MyMazda account linking, in-car pairing, and biometric authorization.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }

        // Appearance & Themes (Dark, Light, System)
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
                                text = "Appearance & Themes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Switch between Dark Cabin and Light Ceramic styling",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = currentTheme == mode,
                                onClick = { onThemeChange(mode) },
                                label = { Text(mode.title) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (mode) {
                                            ThemeMode.DARK -> Icons.Default.Brightness4
                                            ThemeMode.LIGHT -> Icons.Default.Brightness7
                                            ThemeMode.SYSTEM -> Icons.Default.Palette
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SoulRedPrimary,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    containerColor = colors.surfaceElevated,
                                    labelColor = colors.textSecondary,
                                    iconColor = colors.textSecondary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_chip_${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // MyMazda Connected Vehicle Account Card
        item {
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
                                    text = "Connect vehicle via account credentials or in-car PIN",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF142B1F)
                        ) {
                            Text(
                                text = "Connected",
                                color = DiagnosticGreen,
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
                                Text(vehicleState.myMazdaAccount.email, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Linked VIN", color = colors.textSecondary, fontSize = 11.sp)
                                Text(vehicleState.myMazdaAccount.vehicleVin, color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TCU Encryption", color = colors.textSecondary, fontSize = 11.sp)
                                Text("Active & Paired (4G LTE)", color = DiagnosticGreen, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
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

                        Button(
                            onClick = { showMyMazdaDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("how_to_connect_btn")
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fast Dialog", color = colors.textPrimary, fontSize = 12.sp)
                        }
                    }
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
                        testTag = "toggle_bio_start"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchRow(
                        title = "Require Biometric for Door Unlock",
                        subtitle = "Verifies identity before sending unlock signal",
                        checked = securityConfig.requireForDoorUnlock,
                        onCheckedChange = {
                            onUpdateSecurityConfig(securityConfig.copy(requireForDoorUnlock = it))
                        },
                        testTag = "toggle_bio_unlock"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchRow(
                        title = "Require Biometric for Theft Alarm Deactivation",
                        subtitle = "Ensures only authorized driver can silence alarm",
                        checked = securityConfig.requireForTheftAlarm,
                        onCheckedChange = {
                            onUpdateSecurityConfig(securityConfig.copy(requireForTheftAlarm = it))
                        },
                        testTag = "toggle_bio_theft"
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
                        Text("Change 4-Digit Security Fallback PIN (Current: ${securityConfig.userPin})", color = colors.textPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Vehicle Profile Summary
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = SoulRedLight, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Vehicle Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    VehicleInfoRow(label = "Model", value = "${vehicleState.year} ${vehicleState.modelName}")
                    VehicleInfoRow(label = "Powertrain", value = "Skyactiv-G 2.5 Turbo AWD (256 hp)")
                    VehicleInfoRow(label = "Color", value = "Soul Red Crystal Metallic")
                    VehicleInfoRow(label = "Odometer", value = "${vehicleState.odometerMiles} miles")
                    VehicleInfoRow(label = "TCU Modem", value = "4G LTE Telematics Control Unit")
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

    if (showCloudDialog) {
        CloudApiConfigDialog(
            config = vehicleState.cloudConfig,
            onSave = onSaveCloudConfig,
            onTestPing = onSyncCloud,
            onDismiss = { showCloudDialog = false }
        )
    }

    if (showPinChangeDialog) {
        ChangePinDialog(
            currentPin = securityConfig.userPin,
            onSavePin = { newPin ->
                onUpdateSecurityConfig(securityConfig.copy(userPin = newPin))
                showPinChangeDialog = false
            },
            onDismiss = { showPinChangeDialog = false }
        )
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val colors = AppTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SoulRedPrimary
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
fun VehicleInfoRow(label: String, value: String) {
    val colors = AppTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
    }
}

@Composable
fun ChangePinDialog(
    currentPin: String,
    onSavePin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    var newPin by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Update Security PIN",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4) newPin = it },
                    label = { Text("New 4-Digit PIN") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = SoulRedPrimary,
                        unfocusedBorderColor = colors.border
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (newPin.length == 4) onSavePin(newPin)
                    },
                    enabled = newPin.length == 4,
                    colors = ButtonDefaults.buttonColors(containerColor = SoulRedPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save PIN")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = colors.textPrimary)
                }
            }
        }
    }
}

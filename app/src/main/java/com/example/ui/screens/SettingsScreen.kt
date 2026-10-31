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
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.VehicleState
import com.example.ui.components.CloudApiConfigDialog
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
    onLinkMyMazda: (email: String, vin: String, region: String) -> Unit,
    onPairInCarCode: (code: String) -> Unit,
    onUpdateSecurityConfig: (BiometricSecurityConfig) -> Unit,
    onSaveCloudConfig: (String, String) -> Unit,
    onSyncCloud: () -> Unit,
    onSetUserPin: (String) -> Unit = {},
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

        // Appearance / Theme Section Card
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
                                text = "Appearance Theme",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Select your preferred contrast and background style",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

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
                                    text = "Connect vehicle via account credentials or cv/mcs bridge",
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
                                    if (vehicleState.myMazdaAccount.isTcuPaired) "Active & Paired (4G LTE)" else "Not Connected",
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

                    VehicleInfoRow(
                        label = "Model",
                        value = if (vehicleState.isVehicleSelected) "${if (vehicleState.year > 0) vehicleState.year else ""} ${vehicleState.modelName}" else "No Vehicle Selected"
                    )
                    VehicleInfoRow(label = "VIN", value = vehicleState.vin.ifBlank { "Not Connected" })
                    VehicleInfoRow(label = "Nickname", value = vehicleState.nickname.ifBlank { "Not Set" })
                    VehicleInfoRow(label = "Odometer", value = if (vehicleState.isOdometerAvailable) "${vehicleState.odometerMiles} miles" else "Unavailable")
                    VehicleInfoRow(label = "Connection", value = if (vehicleState.isConnected) vehicleState.myMazdaAccount.region else "Disconnected")
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
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = colors.textSecondary)
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
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
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = colors.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, fontSize = 11.sp)
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
    onSavePin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                    text = "Configure 4-Digit Security PIN",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "This PIN is salted and hashed using SHA-256 and stored in secure DataStore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) newPin = it },
                    label = { Text("New 4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = SoulRedPrimary,
                        unfocusedBorderColor = colors.border
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) confirmPin = it },
                    label = { Text("Confirm 4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = SoulRedPrimary,
                        unfocusedBorderColor = colors.border
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = DiagnosticRed,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (newPin.length != 4) {
                            errorMessage = "PIN must be exactly 4 digits."
                        } else if (newPin != confirmPin) {
                            errorMessage = "PINs do not match. Please re-enter."
                        } else {
                            errorMessage = null
                            onSavePin(newPin)
                        }
                    },
                    enabled = newPin.length == 4 && confirmPin.length == 4,
                    colors = ButtonDefaults.buttonColors(containerColor = SoulRedPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save PIN Securely")
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

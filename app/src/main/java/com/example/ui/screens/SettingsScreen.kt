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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.ui.theme.CabinBorder
import com.example.ui.theme.CabinSurface
import com.example.ui.theme.CabinSurfaceElevated
import com.example.ui.theme.DiagnosticGreen
import com.example.ui.theme.MetallicGray
import com.example.ui.theme.PlatinumSilver
import com.example.ui.theme.SoulRedDark
import com.example.ui.theme.SoulRedLight
import com.example.ui.theme.SoulRedPrimary
import com.example.util.BiometricSecurityConfig

@Composable
fun SettingsScreen(
    vehicleState: VehicleState,
    securityConfig: BiometricSecurityConfig,
    onUpdateSecurityConfig: (BiometricSecurityConfig) -> Unit,
    onSaveCloudConfig: (String, String) -> Unit,
    onSyncCloud: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCloudDialog by remember { mutableStateOf(false) }
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
                color = PlatinumSilver
            )
            Text(
                text = "Biometric enforcement, vehicle connection parameters, and notification alerts.",
                style = MaterialTheme.typography.bodySmall,
                color = MetallicGray
            )
        }

        // Biometric Security Controls
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CabinSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CabinBorder)),
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
                                color = PlatinumSilver
                            )
                            Text(
                                text = "Require fingerprint or biometric scan before sensitive vehicle commands",
                                style = MaterialTheme.typography.bodySmall,
                                color = MetallicGray
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
                        colors = ButtonDefaults.buttonColors(containerColor = CabinSurfaceElevated),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("change_pin_btn")
                    ) {
                        Icon(Icons.Default.Password, contentDescription = null, tint = PlatinumSilver, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change 4-Digit Security Fallback PIN (Current: ${securityConfig.userPin})", color = PlatinumSilver, fontSize = 12.sp)
                    }
                }
            }
        }

        // Cloud API Connection Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CabinSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CabinBorder)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cloud_settings_card")
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
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = SoulRedLight, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Mazda Cloud Connected Vehicle API",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PlatinumSilver
                            )
                            Text(
                                text = "Live bidirectional telematics connection",
                                style = MaterialTheme.typography.bodySmall,
                                color = MetallicGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CabinSurfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("VIN: ${vehicleState.vin}", fontWeight = FontWeight.Bold, color = PlatinumSilver)
                            Text("Endpoint: ${vehicleState.cloudConfig.apiUrl}", color = MetallicGray, fontSize = 11.sp)
                            Text("Status: Connected & Synchronized", color = DiagnosticGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showCloudDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SoulRedPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("configure_api_btn")
                    ) {
                        Text("Configure API Gateway & Auth Token")
                    }
                }
            }
        }

        // Vehicle Profile Summary
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CabinSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CabinBorder)),
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
                            color = PlatinumSilver
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = PlatinumSilver)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MetallicGray, fontSize = 11.sp)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MetallicGray)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = PlatinumSilver)
    }
}

@Composable
fun ChangePinDialog(
    currentPin: String,
    onSavePin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newPin by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CabinSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CabinBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Update Security PIN",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PlatinumSilver
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4) newPin = it },
                    label = { Text("New 4-Digit PIN") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PlatinumSilver,
                        unfocusedTextColor = PlatinumSilver,
                        focusedBorderColor = SoulRedPrimary
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
                    Text("Cancel", color = PlatinumSilver)
                }
            }
        }
    }
}

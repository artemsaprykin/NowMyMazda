package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.MyMazdaAccount
import com.example.ui.theme.AppTheme
import com.example.ui.theme.DiagnosticGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SoulRedDark
import com.example.ui.theme.SoulRedLight
import com.example.ui.theme.SoulRedPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MyMazdaConnectDialog(
    account: MyMazdaAccount,
    onLinkAccount: (email: String, vin: String, region: String) -> Unit,
    onPairWithInCarCode: (code: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("MyMazda Sign-In", "In-Car Pairing", "How to Connect")

    var emailInput by remember { mutableStateOf(account.email) }
    var passwordInput by remember { mutableStateOf("••••••••••••") }
    var regionInput by remember { mutableStateOf(account.region) }
    var vinInput by remember { mutableStateOf(account.vehicleVin) }
    var inCarCodeInput by remember { mutableStateOf("") }

    var isAuthenticating by remember { mutableStateOf(false) }
    var pairingSuccessMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(640.dp)
                .padding(4.dp)
                .testTag("mymazda_connect_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header with Close Button
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
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MyMazda Connected Services",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (account.isLinked) DiagnosticGreen else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (account.isLinked) "TCU Telematics Linked" else "Not Linked",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (account.isLinked) DiagnosticGreen else colors.textSecondary
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = colors.surface,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = SoulRedPrimary
                        )
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    color = if (selectedTab == index) SoulRedLight else colors.textSecondary,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content with vertical scroll
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTab) {
                        0 -> {
                            // TAB 1: MyMazda Account Sign-In & Vehicle Enrollment
                            Text(
                                text = "Existing MyMazda Account Credentials",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Sign in using the same email and password registered in your official MyMazda mobile app or mazdausa.com portal.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("MyMazda Account Email") },
                                leadingIcon = {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = colors.textSecondary)
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary,
                                    focusedBorderColor = SoulRedPrimary,
                                    unfocusedBorderColor = colors.border
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("mymazda_email_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Account Password") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = colors.textSecondary)
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary,
                                    focusedBorderColor = SoulRedPrimary,
                                    unfocusedBorderColor = colors.border
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("mymazda_password_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = vinInput,
                                onValueChange = { vinInput = it },
                                label = { Text("Vehicle Identification Number (17-digit VIN)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = colors.textSecondary)
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary,
                                    focusedBorderColor = SoulRedPrimary,
                                    unfocusedBorderColor = colors.border
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("mymazda_vin_input")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            if (pairingSuccessMessage != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF142B1F),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DiagnosticGreen),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DiagnosticGreen, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(pairingSuccessMessage ?: "", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            Button(
                                onClick = {
                                    isAuthenticating = true
                                    pairingSuccessMessage = null
                                    scope.launch {
                                        delay(1400)
                                        isAuthenticating = false
                                        onLinkAccount(emailInput, vinInput, regionInput)
                                        pairingSuccessMessage = "Successfully connected to MyMazda account '$emailInput'. Vehicle profile & TCU telemetry linked!"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SoulRedPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("authenticate_mymazda_btn")
                            ) {
                                if (isAuthenticating) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Authenticating with Mazda IDP...")
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Authenticate & Fetch My Mazda Vehicles")
                                }
                            }
                        }

                        1 -> {
                            // TAB 2: In-Car Center Screen 6-Digit Pairing Assistant
                            Text(
                                text = "Pair Directly from Mazda Connect Center Screen",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "If you are inside your car, generate a 6-digit authorization code on your Mazda Connect screen to pair this app directly to the vehicle TCU modem.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Infotainment Screen Simulation
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF0F141C),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, SoulRedLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("MAZDA CONNECT™ 10.25\"", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text("4G LTE TCU: ACTIVE", color = DiagnosticGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Settings ➔ Connectivity ➔ Connected Services ➔ Pair Mobile Device",
                                        color = ElectricCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Verification Pairing PIN:",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "591 - 824",
                                        color = SoulRedLight,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = inCarCodeInput,
                                onValueChange = { if (it.length <= 6) inCarCodeInput = it },
                                label = { Text("Enter 6-Digit In-Car PIN (e.g. 591824)") },
                                placeholder = { Text("591824") },
                                leadingIcon = {
                                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = colors.textSecondary)
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary,
                                    focusedBorderColor = SoulRedPrimary,
                                    unfocusedBorderColor = colors.border
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("in_car_pin_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    isAuthenticating = true
                                    scope.launch {
                                        delay(1200)
                                        isAuthenticating = false
                                        onPairWithInCarCode(inCarCodeInput)
                                        pairingSuccessMessage = "In-Car Pairing Verified! TCU modem linked with vehicle authorization certificate."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SoulRedPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("confirm_in_car_pairing_btn")
                            ) {
                                if (isAuthenticating) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Verifying In-Car Code...")
                                } else {
                                    Text("Verify & Complete In-Car Pairing")
                                }
                            }
                        }

                        2 -> {
                            // TAB 3: How to Connect Instructions & Troubleshooting
                            Text(
                                text = "How to Connect Your Car to this App",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            InstructionStep(
                                stepNumber = 1,
                                title = "Vehicle Compatibility Check",
                                description = "Mazda Connected Services is factory-installed on: 2023–2025 Mazda CX-50, 2021–2025 CX-5 & CX-9, 2020–2025 CX-30, 2019–2025 Mazda3, and 2024–2025 CX-90 / CX-70."
                            )

                            InstructionStep(
                                stepNumber = 2,
                                title = "Locate your 17-Digit VIN",
                                description = "Found on the driver-side door jamb sticker, the lower corner of the front windshield on the driver's side, or in your vehicle registration card."
                            )

                            InstructionStep(
                                stepNumber = 3,
                                title = "Turn Ignition ON & Enable Telematics",
                                description = "In the car, use the center rotary dial on the Mazda Connect display: go to Settings > In-Vehicle Displays > Connectivity Settings > Connected Services, and ensure 'Data Sharing' is toggled ON."
                            )

                            InstructionStep(
                                stepNumber = 4,
                                title = "Pairing with Existing Account or In-Car Code",
                                description = "Use Tab 1 to sign in directly with your MyMazda email/password, or use Tab 2 to enter the 6-digit verification code shown on your center screen."
                            )

                            InstructionStep(
                                stepNumber = 5,
                                title = "Remote Start Safety Checklist",
                                description = "For remote engine start to ignite: all doors and trunk must be closed and locked, vehicle must be in PARK (P), hood closed, and smart key fobs must NOT be left inside the vehicle."
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = colors.textPrimary)
                }
            }
        }
    }
}

@Composable
fun InstructionStep(
    stepNumber: Int,
    title: String,
    description: String
) {
    val colors = AppTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(SoulRedPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$stepNumber",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VehicleState
import com.example.ui.theme.AppTheme
import com.example.ui.theme.DiagnosticAmber
import com.example.ui.theme.DiagnosticGreen
import com.example.ui.theme.DiagnosticRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SoulRedDark
import com.example.ui.theme.SoulRedLight
import com.example.ui.theme.SoulRedPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleConnectionScreen(
    vehicleState: VehicleState,
    onAuthenticateBridge: (email: String, pass: String, region: String, vin: String?, onComplete: (Boolean, String) -> Unit) -> Unit,
    onPairInCarCode: (code: String) -> Boolean,
    onSyncCloud: () -> Unit,
    onSaveCloudConfig: (url: String, token: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val account = vehicleState.myMazdaAccount
    val cloudConfig = vehicleState.cloudConfig

    // Form states
    var emailInput by remember { mutableStateOf(account.email) }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var selectedRegion by remember {
        mutableStateOf(
            if (account.region.contains("MME", ignoreCase = true)) "MME"
            else if (account.region.contains("MJO", ignoreCase = true)) "MJO"
            else "MNAO"
        )
    }
    var vinInput by remember { mutableStateOf(vehicleState.vin) }
    var inCarCodeInput by remember { mutableStateOf("") }

    var isAuthenticating by remember { mutableStateOf(false) }
    var authFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var isAuthSuccess by remember { mutableStateOf(false) }

    var inCarCodeMessage by remember { mutableStateOf<String?>(null) }
    var isInCarCodeSuccess by remember { mutableStateOf(false) }

    var activeTab by remember { mutableIntStateOf(0) } // 0: Account Link, 1: Step-by-Step Guide, 2: API Permissions, 3: Gateway Status

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Navigation Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("vehicle_connection_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Vehicle Connection",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Mazda Connected Services Bridge (cv/mcs)",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // Live Connection Status Hero Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("connection_hero_card")
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
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (account.isLinked) DiagnosticGreen else DiagnosticAmber)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (account.isLinked) "BRIDGE CONNECTED" else "BRIDGE DISCONNECTED",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (account.isLinked) DiagnosticGreen else DiagnosticAmber
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SoulRedDark.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SoulRedLight.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "TCU 4G LTE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SoulRedLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = vehicleState.nickname,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "VIN: ${vehicleState.vin} • Region: ${account.region}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Gateway Latency",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                            Text(
                                text = "${cloudConfig.lastLatencyMs} ms (RTT)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                        }

                        Column {
                            Text(
                                text = "In-Car Pairing",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                            Text(
                                text = if (account.isTcuPaired) "Bonded (6-digit)" else "Pending",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (account.isTcuPaired) DiagnosticGreen else DiagnosticAmber
                            )
                        }

                        IconButton(
                            onClick = onSyncCloud,
                            modifier = Modifier.testTag("connection_refresh_btn")
                        ) {
                            if (cloudConfig.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = SoulRedLight
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync telematics",
                                    tint = SoulRedLight
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section Navigation Tabs (Account Link / Guide / Permissions / Gateway)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    label = { Text("Account Link", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SoulRedPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("tab_account_link")
                )

                FilterChip(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    label = { Text("Setup Guide", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SoulRedPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("tab_setup_guide")
                )

                FilterChip(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    label = { Text("Permissions", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SoulRedPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("tab_permissions")
                )
            }
        }

        // TAB CONTENT
        when (activeTab) {
            0 -> {
                // TAB 0: Interactive Credentials Exchange & In-Car Code Bonding
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = SoulRedLight,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "MyMazda Account Authentication",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Direct login through cv/mcs protocol bridge",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            // Region Selector
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Mazda Regional API Cluster",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val regions = listOf(
                                        "MNAO" to "North America",
                                        "MME" to "Europe",
                                        "MJO" to "Japan"
                                    )
                                    regions.forEach { (code, label) ->
                                        FilterChip(
                                            selected = selectedRegion == code,
                                            onClick = { selectedRegion = code },
                                            label = { Text("$code ($label)", fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SoulRedPrimary,
                                                selectedLabelColor = Color.White
                                            ),
                                            modifier = Modifier.testTag("region_chip_$code")
                                        )
                                    }
                                }
                            }

                            // Email
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text("MyMazda Account Email") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = SoulRedLight
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_email_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SoulRedLight,
                                    unfocusedBorderColor = colors.border,
                                    focusedLabelColor = SoulRedLight
                                )
                            )

                            // Password
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("MyMazda Password") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Password,
                                        contentDescription = null,
                                        tint = SoulRedLight
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { isPasswordVisible = !isPasswordVisible },
                                        modifier = Modifier.testTag("toggle_password_visibility")
                                    ) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password visibility",
                                            tint = colors.textSecondary
                                        )
                                    }
                                },
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_password_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SoulRedLight,
                                    unfocusedBorderColor = colors.border,
                                    focusedLabelColor = SoulRedLight
                                )
                            )

                            // Vehicle VIN (optional override)
                            OutlinedTextField(
                                value = vinInput,
                                onValueChange = { vinInput = it },
                                label = { Text("Vehicle Identification Number (VIN)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = SoulRedLight
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_vin_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SoulRedLight,
                                    unfocusedBorderColor = colors.border,
                                    focusedLabelColor = SoulRedLight
                                )
                            )

                            // Feedback Message
                            authFeedbackMessage?.let { msg ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isAuthSuccess) DiagnosticGreen.copy(alpha = 0.15f) else DiagnosticRed.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isAuthSuccess) DiagnosticGreen else DiagnosticRed
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isAuthSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (isAuthSuccess) DiagnosticGreen else DiagnosticRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textPrimary
                                        )
                                    }
                                }
                            }

                            // Authenticate Button
                            Button(
                                onClick = {
                                    if (emailInput.isNotBlank()) {
                                        isAuthenticating = true
                                        authFeedbackMessage = null
                                        val effectivePass = if (passwordInput.isNotBlank()) passwordInput else "mzd_session_token_keep"
                                        onAuthenticateBridge(
                                            emailInput.trim(),
                                            effectivePass,
                                            selectedRegion,
                                            vinInput.trim().ifBlank { null }
                                        ) { success, resultText ->
                                            isAuthenticating = false
                                            isAuthSuccess = success
                                            authFeedbackMessage = resultText
                                        }
                                    }
                                },
                                enabled = !isAuthenticating && emailInput.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("authenticate_bridge_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SoulRedPrimary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isAuthenticating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Authenticating Bridge...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.VpnKey,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Authenticate with Mazda API", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // In-Car 6-Digit Handshake Pairing Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "In-Car 6-Digit Handshake",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Cryptographic bonding with Mazda Connect TCU",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            Text(
                                text = "Enter the 6-digit numeric verification code displayed on your car's center infotainment display (under Settings > Connected Services > Register App) to authorize this device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = inCarCodeInput,
                                    onValueChange = {
                                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                            inCarCodeInput = it
                                        }
                                    },
                                    label = { Text("6-Digit Code") },
                                    placeholder = { Text("e.g. 849201") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("incar_code_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ElectricCyan,
                                        unfocusedBorderColor = colors.border,
                                        focusedLabelColor = ElectricCyan
                                    )
                                )

                                Button(
                                    onClick = {
                                        if (inCarCodeInput.length == 6) {
                                            val paired = onPairInCarCode(inCarCodeInput)
                                            isInCarCodeSuccess = paired
                                            inCarCodeMessage = if (paired) {
                                                "TCU Cryptographic Bond Verified! In-Car Data Sharing active."
                                            } else {
                                                "Invalid or expired 6-digit code. Please refresh code in car."
                                            }
                                        }
                                    },
                                    enabled = inCarCodeInput.length == 6,
                                    modifier = Modifier
                                        .height(56.dp)
                                        .testTag("pair_code_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SoulRedDark,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Bond Car", fontWeight = FontWeight.Bold)
                                }
                            }

                            inCarCodeMessage?.let { msg ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isInCarCodeSuccess) DiagnosticGreen.copy(alpha = 0.15f) else DiagnosticRed.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isInCarCodeSuccess) DiagnosticGreen else DiagnosticRed
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isInCarCodeSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (isInCarCodeSuccess) DiagnosticGreen else DiagnosticRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // TAB 1: Step-by-Step Instructions on How to Link Existing Mazda Account
                item {
                    Text(
                        text = "Official Account Linking Guide",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Follow these 5 steps to connect your Mazda vehicle directly using the Mazda Connected Services backend.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                val guideSteps = listOf(
                    StepInfo(
                        stepNumber = 1,
                        title = "Verify Vehicle Compatibility",
                        summary = "Factory 4G LTE Telematics Control Unit (TCU)",
                        detail = "Ensure your vehicle has Mazda Connected Services hardware installed. Compatible models include:\n• 2019+ Mazda 3 Sedan & Hatchback\n• 2020+ CX-30 Crossover\n• 2021+ CX-5 & CX-9 (Select trims)\n• 2023+ CX-50 Turbo & Natural Aspirated\n• 2024+ CX-90 / CX-70 Mild Hybrid & PHEV\n• 2022+ MX-30 EV",
                        icon = Icons.Default.DirectionsCar,
                        accentColor = SoulRedLight
                    ),
                    StepInfo(
                        stepNumber = 2,
                        title = "Enable In-Car Telematics & Data Sharing",
                        summary = "Mazda Connect Center Console Display",
                        detail = "1. Sit in your vehicle and switch ignition to ON (or start the engine).\n2. Using the Commander Control dial on the center console, select 'Settings'.\n3. Navigate to 'Connectivity Settings' > 'Connected Services'.\n4. Set 'In-Vehicle Telematics & Data Sharing' to ON.\n5. Confirm the privacy and safety agreement on the display.",
                        icon = Icons.Default.Security,
                        accentColor = ElectricCyan
                    ),
                    StepInfo(
                        stepNumber = 3,
                        title = "Generate In-Car 6-Digit Pairing Code",
                        summary = "Vehicle Handshake Authentication",
                        detail = "1. On the vehicle screen under 'Connected Services', select 'Enroll MyMazda / Register App'.\n2. The screen will generate a unique 6-digit numeric authorization token.\n3. Keep the vehicle ignition on. This authorization token remains valid for 15 minutes.\n4. Note the 6-digit code to enter on the Account Link tab.",
                        icon = Icons.Default.Key,
                        accentColor = DiagnosticAmber
                    ),
                    StepInfo(
                        stepNumber = 4,
                        title = "Authenticate via MCS Bridge in NowMyMazda",
                        summary = "Direct API Token Exchange (cv/mcs protocol)",
                        detail = "1. Switch to the 'Account Link' tab on this screen.\n2. Enter your existing MyMazda username (email) and password.\n3. Choose your vehicle's delivery region (MNAO for US/Canada, MME for Europe, MJO for Japan).\n4. Tap 'Authenticate with Mazda API' to generate session credentials.\n5. Enter the 6-digit in-car code into the Handshake box and tap 'Bond Car'.",
                        icon = Icons.Default.Link,
                        accentColor = DiagnosticGreen
                    ),
                    StepInfo(
                        stepNumber = 5,
                        title = "Remote Control Safety Prerequisites",
                        summary = "Mandatory safety conditions before commands execute",
                        detail = "Mazda's onboard BCM and PCM require the following safety interlocks before executing remote actions:\n• Transmission MUST be locked in PARK (P).\n• All doors, engine hood, and rear liftgate must be fully closed and latched.\n• All Smart Key fobs must be OUTSIDE the cabin.\n• Fuel level must be at or above 10% capacity.\n• 12V auxiliary battery voltage must be ≥ 12.0V.",
                        icon = Icons.Default.Speed,
                        accentColor = SoulRedLight
                    )
                )

                guideSteps.forEach { step ->
                    item(key = step.stepNumber) {
                        StepGuideCard(step = step)
                    }
                }
            }

            2 -> {
                // TAB 2: Necessary API Permissions for Remote Control
                item {
                    Text(
                        text = "Mazda API Permissions & Scope",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "To allow NowMyMazda to control features seamlessly, your MyMazda account grant includes the following secure scopes:",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                val permissionsList = listOf(
                    PermissionItem(
                        scopeKey = "REMOTE_ENGINE_START",
                        title = "Remote Engine Ignition & HVAC",
                        description = "Permits starting the combustion engine or EV pre-conditioning remotely. Activates climate temperature control, front defroster, and heated seats according to cabin presets.",
                        isGranted = account.remoteStartPermitted,
                        icon = Icons.Default.ElectricBolt,
                        badge = "HIGH PRIVILEGE"
                    ),
                    PermissionItem(
                        scopeKey = "DOOR_LOCK_UNLOCK",
                        title = "Power Door Locks & Tailgate",
                        description = "Enables encrypted commands to lock and unlock all vehicle doors and release the power rear liftgate. Protected by secondary biometric confirmation.",
                        isGranted = account.doorLockPermitted,
                        icon = Icons.Default.Lock,
                        badge = "HIGH PRIVILEGE"
                    ),
                    PermissionItem(
                        scopeKey = "TELEMATICS_TELEMETRY",
                        title = "Diagnostic Sensors & TPMS Polling",
                        description = "Grants real-time read access to individual tire pressure monitors (FL, FR, RL, RR in PSI), 12V battery state of health (SOH), fuel level percentage, and vehicle odometer.",
                        isGranted = account.telematicsPermitted,
                        icon = Icons.Default.Speed,
                        badge = "CORE READ"
                    ),
                    PermissionItem(
                        scopeKey = "GPS_LOCATOR",
                        title = "Global Positioning & Geofencing",
                        description = "Transmits vehicle latitude and longitude coordinates over TCU 4G LTE. Enables real-time map location, compass heading, and departure/perimeter theft alerts.",
                        isGranted = account.gpsTrackingPermitted,
                        icon = Icons.Default.LocationOn,
                        badge = "CORE READ"
                    ),
                    PermissionItem(
                        scopeKey = "OBD2_DTC_DIAGNOSTICS",
                        title = "ECU Trouble Codes & Service Health",
                        description = "Allows scanning internal Controller Area Network (CAN) diagnostic trouble codes (DTCs), reading oil degradation percentage, brake wear, and clearing temporary faults.",
                        isGranted = account.dtcDiagnosticsPermitted,
                        icon = Icons.Default.CheckCircle,
                        badge = "DIAGNOSTIC"
                    ),
                    PermissionItem(
                        scopeKey = "PUSH_NOTIFICATIONS",
                        title = "Vehicle Alert Dispatch",
                        description = "Delivers push notifications for geofence breaches, unlocked door reminders, low tire pressure alerts, and scheduled maintenance intervals.",
                        isGranted = true,
                        icon = Icons.Default.NotificationsActive,
                        badge = "NOTIFICATIONS"
                    )
                )

                permissionsList.forEach { perm ->
                    item(key = perm.scopeKey) {
                        PermissionCard(perm = perm)
                    }
                }
            }
        }

        // Bottom info / Architecture footnote
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = SoulRedLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Direct Cloud Connection Architecture",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "All communications use direct TLS 1.3 HTTPS requests to Mazda Connected Services regional gateways. No third-party servers store your vehicle keys.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private data class StepInfo(
    val stepNumber: Int,
    val title: String,
    val summary: String,
    val detail: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
private fun StepGuideCard(step: StepInfo) {
    val colors = AppTheme.colors
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("step_card_${step.stepNumber}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(step.accentColor.copy(alpha = 0.15f))
                            .border(1.dp, step.accentColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = step.stepNumber.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = step.accentColor
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = step.summary,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = step.accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.background,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = step.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(10.dp),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

private data class PermissionItem(
    val scopeKey: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val icon: ImageVector,
    val badge: String
)

@Composable
private fun PermissionCard(perm: PermissionItem) {
    val colors = AppTheme.colors

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("permission_card_${perm.scopeKey}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (perm.isGranted) DiagnosticGreen.copy(alpha = 0.15f) else DiagnosticAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = perm.icon,
                            contentDescription = null,
                            tint = if (perm.isGranted) DiagnosticGreen else DiagnosticAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = perm.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = perm.scopeKey,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (perm.isGranted) DiagnosticGreen.copy(alpha = 0.2f) else DiagnosticAmber.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (perm.isGranted) DiagnosticGreen.copy(alpha = 0.5f) else DiagnosticAmber.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = if (perm.isGranted) "GRANTED" else "REQUIRED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (perm.isGranted) DiagnosticGreen else DiagnosticAmber,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = perm.description,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = colors.textSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

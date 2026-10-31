package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DiscoveredVehicle
import com.example.data.model.VehicleState
import com.example.data.remote.MazdaRegionalCluster
import com.example.ui.theme.AppTheme
import com.example.ui.theme.DiagnosticAmber
import com.example.ui.theme.DiagnosticGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SoulRedDark
import com.example.ui.theme.SoulRedLight
import com.example.ui.theme.SoulRedPrimary

@Composable
fun VehicleConnectionScreen(
    vehicleState: VehicleState,
    onAuthenticate: (email: String, pass: String, region: String, customUrl: String?, onComplete: (Boolean, String) -> Unit) -> Unit,
    onSelectVehicle: (vin: String) -> Unit,
    onPairWithInCarCode: (code: String) -> Boolean = { false },
    onTestHandshake: (onResult: (String) -> Unit) -> Unit,
    onDisconnect: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val account = vehicleState.myMazdaAccount
    val cloudConfig = vehicleState.cloudConfig

    var emailInput by remember { mutableStateOf(account.email) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedRegion by remember { mutableStateOf(MazdaRegionalCluster.fromCode(account.regionCode)) }
    var customGatewayUrl by remember { mutableStateOf(cloudConfig.apiUrl) }
    var useCustomGateway by remember { mutableStateOf(false) }

    var isConnecting by remember { mutableStateOf(false) }
    var connectionFeedback by remember { mutableStateOf<String?>(null) }
    var isFeedbackError by remember { mutableStateOf(false) }

    var isTestingHandshake by remember { mutableStateOf(false) }
    var handshakeResult by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar Navigation & Title
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("vehicle_conn_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Vehicle Connection & Cloud API",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Mazda Connected Services • cv/mcs & Home Assistant Bridge",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // Live Connection Status Hero Banner
        item {
            val isConnected = account.isLinked && vehicleState.isVehicleSelected
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected) colors.surface else Color(0xFF1E1417)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isConnected) DiagnosticGreen.copy(alpha = 0.5f) else SoulRedPrimary.copy(alpha = 0.6f)
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("connection_hero_banner")
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
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isConnected) DiagnosticGreen.copy(alpha = 0.2f)
                                        else SoulRedDark.copy(alpha = 0.4f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (isConnected) DiagnosticGreen else SoulRedLight,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isConnected) "Connected to Mazda Cloud" else "Disconnected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = if (isConnected) "Active Gateway: ${cloudConfig.regionEndpoint}" else "No active Mazda Connected Services session",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isConnected) DiagnosticGreen else colors.textSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isConnected) Color(0xFF142B1F) else Color(0xFF2C191B)
                        ) {
                            Text(
                                text = if (isConnected) "LIVE UPLINK" else "OFFLINE",
                                color = if (isConnected) DiagnosticGreen else SoulRedLight,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isConnected) {
                            "Authenticated as ${account.email} • Active VIN: ${vehicleState.vin} • Telematics Status: Verified"
                        } else {
                            "Connect your existing MyMazda account to enable remote engine start, door lock actuation, TPMS telemetry, and GPS tracking."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isTestingHandshake = true
                                onTestHandshake { result ->
                                    isTestingHandshake = false
                                    handshakeResult = result
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_handshake_btn")
                        ) {
                            if (isTestingHandshake) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.textPrimary
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.textPrimary)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Gateway Ping", fontSize = 11.sp, color = colors.textPrimary)
                        }

                        if (account.isLinked) {
                            OutlinedButton(
                                onClick = onDisconnect,
                                modifier = Modifier.testTag("disconnect_account_btn")
                            ) {
                                Text("Sign Out", fontSize = 11.sp, color = DiagnosticAmber)
                            }
                        }
                    }

                    handshakeResult?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            fontSize = 11.sp,
                            color = if (msg.contains("OK")) DiagnosticGreen else DiagnosticAmber,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Account Linking Form
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_linking_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Link MyMazda Credentials",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Authenticates using Mazda Azure AD B2C with PKCE token rotation matching cv/mcs, or connects via a self-hosted cv/mcs Gateway Adapter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    // Region Selector
                    Text("Regional Cloud Cluster:", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MazdaRegionalCluster.entries.forEach { cluster ->
                            val chipLabel = when (cluster) {
                                MazdaRegionalCluster.MNAO -> "MNAO (Verified)"
                                MazdaRegionalCluster.MME -> "MME (Unverified)"
                                MazdaRegionalCluster.MJO -> "MJO (Unverified)"
                                MazdaRegionalCluster.CV_MCS_BRIDGE -> "Gateway"
                            }
                            FilterChip(
                                selected = selectedRegion == cluster,
                                onClick = {
                                    selectedRegion = cluster
                                    useCustomGateway = (cluster == MazdaRegionalCluster.CV_MCS_BRIDGE)
                                },
                                label = { Text(chipLabel, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SoulRedPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = colors.surfaceElevated,
                                    labelColor = colors.textSecondary
                                ),
                                modifier = Modifier.testTag("region_chip_${cluster.code}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Email Input
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("MyMazda Account Email") },
                        placeholder = { Text("driver@email.com") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = SoulRedPrimary,
                            unfocusedBorderColor = colors.border
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_email_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password Input
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("MyMazda Account Password") },
                        placeholder = { Text("Enter account password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password",
                                    tint = colors.textSecondary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = SoulRedPrimary,
                            unfocusedBorderColor = colors.border
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Optional Custom Gateway URL Toggle
                    TextButton(
                        onClick = { useCustomGateway = !useCustomGateway },
                        modifier = Modifier.testTag("toggle_custom_gateway")
                    ) {
                        Text(
                            text = if (useCustomGateway) "- Hide Custom Gateway Host" else "+ Use Custom cv/mcs Local Bridge URL",
                            fontSize = 11.sp,
                            color = SoulRedLight
                        )
                    }

                    if (useCustomGateway) {
                        OutlinedTextField(
                            value = customGatewayUrl,
                            onValueChange = { customGatewayUrl = it },
                            label = { Text("Custom Gateway / cv/mcs Daemon URL") },
                            placeholder = { Text("http://10.0.2.2:8080/") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                focusedBorderColor = SoulRedPrimary,
                                unfocusedBorderColor = colors.border
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_gateway_url_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    connectionFeedback?.let { feedback ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isFeedbackError) DiagnosticAmber.copy(alpha = 0.15f) else DiagnosticGreen.copy(alpha = 0.15f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = feedback,
                                color = if (isFeedbackError) DiagnosticAmber else DiagnosticGreen,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (emailInput.isBlank() || passwordInput.isBlank()) {
                                isFeedbackError = true
                                connectionFeedback = "Please enter both your MyMazda account email and password."
                                return@Button
                            }
                            isConnecting = true
                            connectionFeedback = null
                            onAuthenticate(
                                emailInput.trim(),
                                passwordInput,
                                selectedRegion.code,
                                if (useCustomGateway) customGatewayUrl.trim() else null
                            ) { success, msg ->
                                isConnecting = false
                                isFeedbackError = !success
                                connectionFeedback = msg
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoulRedPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("connect_authenticate_btn")
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Authenticating & Discovering Vehicles...")
                        } else {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect & Authenticate with Mazda Cloud")
                        }
                    }
                }
            }
        }

        // Discovered Vehicles List
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("discovered_vehicles_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Registered Vehicles on Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (account.isLinked) "${account.discoveredVehicles.size} vehicle(s) linked to ${account.email}" else "No account currently linked",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }

                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = SoulRedLight)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (account.discoveredVehicles.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No Vehicles Discovered",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Authenticate your MyMazda account above to retrieve your registered Mazda vehicles.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } else {
                        account.discoveredVehicles.forEach { vehicle ->
                            VehicleItemRow(
                                vehicle = vehicle,
                                onSelect = { onSelectVehicle(vehicle.vin) },
                                colors = colors
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        // Required API Permissions & In-Vehicle Telematics Entitlements
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("permissions_checklist_card")
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
                            Icon(Icons.Default.Security, contentDescription = null, tint = SoulRedLight, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Required Telematics Permissions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Prerequisites verified against Mazda cloud for remote commands.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PermissionCheckItem(
                        title = "1. In-Vehicle Data Sharing Agreement (Mazda Connect)",
                        description = "Enable on your center infotainment display: Settings > Connectivity Settings > Connected Services > Data Sharing = ON.",
                        isGranted = account.dataSharingAgreementGranted,
                        colors = colors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionCheckItem(
                        title = "2. Connected Services Remote Control Entitlement",
                        description = "Requires active Connected Services subscription on your MyMazda account.",
                        isGranted = account.remoteControlEntitlementActive,
                        colors = colors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionCheckItem(
                        title = "3. TCU 4G LTE Modem Cellular Reception",
                        description = "Telematics Control Unit must have active cellular signal bars and be registered in Mazda cloud.",
                        isGranted = account.inCarVerificationCompleted,
                        colors = colors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionCheckItem(
                        title = "4. Vehicle CAN-Bus Remote Start Safety Interlocks",
                        description = "Transmission in PARK (P), all doors/trunk closed, hood closed, and fobs outside cabin.",
                        isGranted = account.safetyInterlocksClear,
                        colors = colors
                    )
                }
            }
        }

        // In-Vehicle Connectivity & Telematics Verification Prerequisite
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("in_car_handshake_card")
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
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = SoulRedLight, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "In-Vehicle Mazda Connect™ Pairing",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Authentic cloud enrollment and in-car configuration",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "1. Turn on vehicle ignition (engine off is fine).",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "2. On the center display, open Settings > Connectivity Settings > Connected Services.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "3. Confirm 'Data Sharing' is toggled ON to permit TCU telematics transmission.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "4. If prompted with an in-car confirmation prompt, accept it to enroll this device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }
        }

        // Step-by-Step Instructions Walkthrough
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("instructions_walkthrough_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Step-by-Step Connection Instructions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Follow these 4 steps to link your Mazda vehicle to NowMyMazda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    ConnectionStepItem(
                        stepNum = "1",
                        title = "Verify MyMazda Account & VIN Enrollment",
                        description = "Make sure your car is enrolled in your MyMazda account. Your 17-digit VIN is located on the driver door sticker or windshield lower corner.",
                        colors = colors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ConnectionStepItem(
                        stepNum = "2",
                        title = "Enable In-Car Data Sharing",
                        description = "In your vehicle, navigate to Mazda Connect™ Settings > Connectivity Settings > Connected Services and verify Data Sharing is enabled.",
                        colors = colors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ConnectionStepItem(
                        stepNum = "3",
                        title = "Select Regional Cluster (MNAO / MME / MJO)",
                        description = "Select MNAO for US/Canada, MME for Europe, or MJO for Japan. If you run a local cv/mcs daemon or Home Assistant proxy, select BRIDGE.",
                        colors = colors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ConnectionStepItem(
                        stepNum = "4",
                        title = "Connect & Select Active Vehicle",
                        description = "Enter your credentials above. All vehicles linked to your Mazda account will be discovered automatically with full remote control capabilities.",
                        colors = colors
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun VehicleItemRow(
    vehicle: DiscoveredVehicle,
    onSelect: () -> Unit,
    colors: com.example.ui.theme.AppColors
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (vehicle.isSelected) SoulRedDark.copy(alpha = 0.25f) else colors.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(
            width = if (vehicle.isSelected) 1.5.dp else 1.dp,
            color = if (vehicle.isSelected) SoulRedPrimary else colors.border
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vehicle_row_${vehicle.vin}")
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (vehicle.isSelected) SoulRedPrimary else colors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = if (vehicle.isSelected) Color.White else colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "${if (vehicle.modelYear > 0) vehicle.modelYear else ""} ${vehicle.modelName}".trim(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "VIN: ${vehicle.vin}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (vehicle.isTcuPaired) "TCU: Paired" else "TCU: Ready",
                            fontSize = 10.sp,
                            color = DiagnosticGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (vehicle.canRemoteControl) "Remote: Enabled" else "Remote: Standard",
                            fontSize = 10.sp,
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (vehicle.isSelected) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SoulRedPrimary
                ) {
                    Text(
                        text = "ACTIVE",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Button(
                    onClick = onSelect,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                    modifier = Modifier.testTag("select_vehicle_${vehicle.vin}")
                ) {
                    Text("Select", fontSize = 11.sp, color = colors.textPrimary)
                }
            }
        }
    }
}

@Composable
fun PermissionCheckItem(
    title: String,
    description: String,
    isGranted: Boolean,
    colors: com.example.ui.theme.AppColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isGranted) DiagnosticGreen else Color(0xFF2C241E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Lock,
                contentDescription = null,
                tint = if (isGranted) Color.White else DiagnosticAmber,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun ConnectionStepItem(
    stepNum: String,
    title: String,
    description: String,
    colors: com.example.ui.theme.AppColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(SoulRedDark),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNum,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                fontSize = 11.sp
            )
        }
    }
}

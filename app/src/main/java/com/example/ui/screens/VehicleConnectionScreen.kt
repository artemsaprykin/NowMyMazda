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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
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
    onPairWithInCarCode: (code: String) -> Boolean,
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
    var inCarCodeInput by remember { mutableStateOf("") }
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
                        imageVector = Icons.Default.ArrowBack,
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
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (account.isLinked) colors.surface else Color(0xFF1E1417)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (account.isLinked) DiagnosticGreen.copy(alpha = 0.5f) else SoulRedPrimary.copy(alpha = 0.6f)
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("connection_status_hero")
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
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (account.isLinked) DiagnosticGreen else DiagnosticAmber)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (account.isLinked) "Connected to Mazda Cloud" else "Ready to Link Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.surfaceElevated
                        ) {
                            Text(
                                text = selectedRegion.code,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SoulRedLight,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (account.isLinked) {
                            "Authenticated as ${account.email} • VIN: ${vehicleState.vin} • Telematics Uplink: Active (${cloudConfig.lastLatencyMs}ms)"
                        } else {
                            "Connect your existing MyMazda account to enable remote engine ignition, door locks, TPMS telemetry, and GPS tracking."
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
                            Text("Test Gateway Handshake", fontSize = 11.sp, color = colors.textPrimary)
                        }

                        if (account.isLinked) {
                            OutlinedButton(
                                onClick = onDisconnect,
                                modifier = Modifier.testTag("disconnect_account_btn")
                            ) {
                                Text("Unlink", fontSize = 11.sp, color = DiagnosticAmber)
                            }
                        }
                    }

                    handshakeResult?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            fontSize = 11.sp,
                            color = DiagnosticGreen,
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
                        text = "Link Existing MyMazda Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Authenticates directly with Mazda Connected Services using RSA public key client-side encryption (compatible with cv/mcs & pymazda).",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                    )

                    // Region Selector Chips
                    Text(
                        text = "Select Mazda Regional Server Cluster:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MazdaRegionalCluster.entries.forEach { cluster ->
                            FilterChip(
                                selected = selectedRegion == cluster,
                                onClick = {
                                    selectedRegion = cluster
                                    if (cluster == MazdaRegionalCluster.CV_MCS_BRIDGE) {
                                        useCustomGateway = true
                                    }
                                },
                                label = { Text(cluster.code, fontSize = 11.sp) },
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
                            placeholder = { Text("http://10.0.2.2:8080/api/") },
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
                            if (emailInput.isNotBlank()) {
                                isConnecting = true
                                connectionFeedback = null
                                onAuthenticate(
                                    emailInput,
                                    passwordInput.ifBlank { "saved_key_token" },
                                    selectedRegion.code,
                                    if (useCustomGateway) customGatewayUrl else null
                                ) { success, msg ->
                                    isConnecting = false
                                    isFeedbackError = !success
                                    connectionFeedback = msg
                                }
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
                                text = "${account.discoveredVehicles.size} vehicle(s) linked to ${account.email}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }

                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = SoulRedLight)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

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
                                text = "Required Permissions to Enable Remote Control",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "All 4 permissions must be satisfied for remote ignition and lock actuation.",
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
                        description = "Requires active 3-year complimentary trial or Connected Services enrollment on your MyMazda account.",
                        isGranted = account.remoteControlEntitlementActive,
                        colors = colors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionCheckItem(
                        title = "3. TCU 4G LTE Modem & 6-Digit In-Car PIN",
                        description = "Telematics Control Unit must complete a one-time 6-digit confirmation handshake displayed on your in-car screen.",
                        isGranted = account.inCarVerificationCompleted,
                        colors = colors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionCheckItem(
                        title = "4. Vehicle CAN-Bus Remote Start Safety Interlocks",
                        description = "Transmission must be in PARK (P), all doors/trunk closed, hood latch closed, and smart key fobs outside cabin.",
                        isGranted = account.safetyInterlocksClear,
                        colors = colors
                    )
                }
            }
        }

        // In-Car 6-Digit Verification PIN Handshake Section
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
                    Text(
                        text = "In-Car Telematics Handshake (6-Digit PIN)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "If pairing a new mobile device to your Mazda, sit in the driver seat, turn ignition ON, and navigate to Connected Services pairing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    // Simulated in-car screen display mockup
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F141C),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SoulRedLight.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("MAZDA CONNECT™ TCU 4G LTE", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("ANTENNA SIGNAL: 4/4 BARS", color = DiagnosticGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Settings > Connectivity > Connected Services > Pair Mobile Device",
                                color = ElectricCyan,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Verification Pairing Code:",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "591 - 824",
                                color = SoulRedLight,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inCarCodeInput,
                            onValueChange = { if (it.length <= 6) inCarCodeInput = it },
                            placeholder = { Text("Enter 6 digits (591824)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                focusedBorderColor = SoulRedPrimary,
                                unfocusedBorderColor = colors.border
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("in_car_code_input")
                        )

                        Button(
                            onClick = {
                                if (inCarCodeInput.length == 6) {
                                    onPairWithInCarCode(inCarCodeInput)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoulRedPrimary),
                            modifier = Modifier.testTag("submit_in_car_code_btn")
                        ) {
                            Text("Verify Code")
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
                        description = "Make sure your car is enrolled in the official MyMazda mobile app. Your 17-digit VIN is located on the driver door sticker or windshield lower corner.",
                        colors = colors
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ConnectionStepItem(
                        stepNum = "2",
                        title = "Enable In-Vehicle Data Sharing",
                        description = "In the car, turn ignition ON. On the center display rotary dial: Settings > In-Vehicle Displays > Connectivity Settings > Connected Services, and toggle 'Data Sharing' to ON.",
                        colors = colors
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ConnectionStepItem(
                        stepNum = "3",
                        title = "Select Regional Server Cluster",
                        description = "Select your vehicle's market (MNAO for USA/Canada, MME for Europe, MJO for Japan). The app queries the regional auth server and encrypts credentials with an RSA public key.",
                        colors = colors
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ConnectionStepItem(
                        stepNum = "4",
                        title = "cv/mcs & Home Assistant Local Bridge (Optional)",
                        description = "If you host the cv/mcs Go binary or Home Assistant on your home network, enter the local proxy URL (e.g. http://10.0.2.2:8080/api/) to bypass regional rate limits.",
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
private fun VehicleItemRow(
    vehicle: DiscoveredVehicle,
    onSelect: () -> Unit,
    colors: com.example.ui.theme.AppColors
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (vehicle.isSelected) SoulRedDark.copy(alpha = 0.25f) else colors.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (vehicle.isSelected) SoulRedPrimary else colors.border
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("vehicle_row_${vehicle.vin}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = vehicle.nickname,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    if (vehicle.isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SoulRedPrimary
                        ) {
                            Text(
                                text = "ACTIVE",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${vehicle.modelYear} ${vehicle.modelName} • VIN: ${vehicle.vin}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (vehicle.isTcuPaired) DiagnosticGreen else DiagnosticAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (vehicle.isTcuPaired) "TCU 4G LTE Paired • Remote Control Active" else "TCU Unpaired",
                        fontSize = 10.sp,
                        color = if (vehicle.isTcuPaired) DiagnosticGreen else DiagnosticAmber
                    )
                }
            }

            if (!vehicle.isSelected) {
                OutlinedButton(
                    onClick = onSelect,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Select", fontSize = 11.sp, color = colors.textPrimary)
                }
            }
        }
    }
}

@Composable
private fun PermissionCheckItem(
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
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isGranted) DiagnosticGreen.copy(alpha = 0.2f) else DiagnosticAmber.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Info,
                contentDescription = null,
                tint = if (isGranted) DiagnosticGreen else DiagnosticAmber,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun ConnectionStepItem(
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
                .size(26.dp)
                .clip(CircleShape)
                .background(SoulRedPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNum,
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
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MaintenanceEntity
import com.example.data.model.PressureStatus
import com.example.data.model.TireInfo
import com.example.data.model.VehicleState
import com.example.ui.components.TireDetailDialog
import com.example.ui.theme.AppTheme
import com.example.ui.theme.DiagnosticAmber
import com.example.ui.theme.DiagnosticGreen
import com.example.ui.theme.DiagnosticRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SoulRedLight
import com.example.ui.theme.SoulRedPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DiagnosticsScreen(
    vehicleState: VehicleState,
    maintenanceReminders: List<MaintenanceEntity>,
    onReminderToggle: (Long, Boolean) -> Unit,
    onCalibrateTpms: () -> Unit,
    onRequestTpmsScan: () -> Unit,
    onRunBatteryLoadTest: () -> Unit,
    onRunDtcScan: () -> Unit,
    onClearDtcCodes: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTire by remember { mutableStateOf<TireInfo?>(null) }
    var isRunningEcuScan by remember { mutableStateOf(false) }
    var scanSuccessMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Vehicle Health & Diagnostics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = "Real-time battery metrics, TPMS telemetry, and scheduled maintenance reminders.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }

        // 12V Battery Health Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("battery_health_card")
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (vehicleState.batteryHealth.voltage >= 12.2) DiagnosticGreen.copy(alpha = 0.2f)
                                        else DiagnosticAmber.copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (vehicleState.batteryHealth.voltage >= 12.2) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert,
                                    contentDescription = null,
                                    tint = if (vehicleState.batteryHealth.voltage >= 12.2) DiagnosticGreen else DiagnosticAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "12V Battery Health & Charging",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = vehicleState.batteryHealth.healthSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (vehicleState.batteryHealth.voltage >= 12.2) DiagnosticGreen else DiagnosticAmber
                                 )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.surfaceElevated
                        ) {
                            Text(
                                text = "${vehicleState.batteryHealth.voltage}V",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (vehicleState.batteryHealth.voltage >= 12.2) DiagnosticGreen else DiagnosticAmber,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Key battery metrics grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BatteryMetricTile(
                            label = "State of Health",
                            value = "${vehicleState.batteryHealth.stateOfHealthPercent}%",
                            subtext = "Capacity Remaining",
                            modifier = Modifier.weight(1f)
                        )
                        BatteryMetricTile(
                            label = "State of Charge",
                            value = "${vehicleState.batteryHealth.stateOfChargePercent}%",
                            subtext = "Usable Energy",
                            modifier = Modifier.weight(1f)
                        )
                        BatteryMetricTile(
                            label = "Temp",
                            value = "${vehicleState.batteryHealth.temperatureF}°F",
                            subtext = "Optimal Range",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Alternator Telemetry: ${vehicleState.batteryHealth.alternatorStatus}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onRunBatteryLoadTest,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("battery_load_test_btn")
                    ) {
                        Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run 12V Battery & Alternator Load Test", color = colors.textPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        // TPMS Tire Pressure Monitoring System Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tpms_section_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Tire Pressure System (TPMS)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Cold recommended target: 36.0 PSI all wheels",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }

                        Button(
                            onClick = onCalibrateTpms,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                            modifier = Modifier.testTag("calibrate_all_tpms_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset", color = colors.textPrimary, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4 Tire Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TireCard(
                            tire = vehicleState.tpms.frontLeft,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTire = vehicleState.tpms.frontLeft }
                        )
                        TireCard(
                            tire = vehicleState.tpms.frontRight,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTire = vehicleState.tpms.frontRight }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TireCard(
                            tire = vehicleState.tpms.rearLeft,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTire = vehicleState.tpms.rearLeft }
                        )
                        TireCard(
                            tire = vehicleState.tpms.rearRight,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTire = vehicleState.tpms.rearRight }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onRequestTpmsScan,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("request_tpms_scan_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Request Live TPMS Diagnostic Scan from Vehicle", color = colors.textPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Fluid Levels & Service Schedule
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Fluids & Consumable Wear",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Synthetic Oil
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.InvertColors, contentDescription = null, tint = SoulRedLight, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Synthetic Engine Oil (0W-20)", color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            text = "${vehicleState.diagnostics.oilLifePercent}% (${vehicleState.diagnostics.nextServiceMiles} mi left)",
                            color = if (vehicleState.diagnostics.oilLifePercent > 20) ElectricCyan else DiagnosticAmber,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { vehicleState.diagnostics.oilLifePercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (vehicleState.diagnostics.oilLifePercent > 20) ElectricCyan else DiagnosticAmber,
                        trackColor = colors.surfaceElevated
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Brake Pads
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = DiagnosticGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Brake Pad Thickness", color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            text = "${vehicleState.diagnostics.brakePadsPercent}% (8.5mm - Good)",
                            color = DiagnosticGreen,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { vehicleState.diagnostics.brakePadsPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = DiagnosticGreen,
                        trackColor = colors.surfaceElevated
                    )
                }
            }
        }

        // Scheduled Maintenance Reminders (From Room Database)
        item {
            Text(
                text = "Push Maintenance Reminders",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = "Automated alerts sent when odometer approaches scheduled mileage thresholds.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }

        items(maintenanceReminders, key = { it.id }) { reminder ->
            MaintenanceReminderItem(
                reminder = reminder,
                currentOdometer = vehicleState.odometerMiles,
                onToggle = { onReminderToggle(reminder.id, !reminder.isCompleted) }
            )
        }

        // OBD-II Diagnostics Scanner
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Engineering, contentDescription = null, tint = SoulRedLight, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "OBD-II Cloud DTC Diagnostic Scanner",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Scans PCM, TCM, BCM, and i-ACTIVSENSE Radar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (scanSuccessMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DiagnosticGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DiagnosticGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DiagnosticGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = scanSuccessMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text(
                        text = vehicleState.diagnostics.scanStatusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = DiagnosticGreen,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Button(
                        onClick = onRunDtcScan,
                        colors = ButtonDefaults.buttonColors(containerColor = SoulRedPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_ecu_scan_btn")
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Live OBD-II CAN Telematics Diagnostics Scan")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    selectedTire?.let { tire ->
        TireDetailDialog(
            tire = tire,
            onCalibrate = onCalibrateTpms,
            onScanSensor = onRequestTpmsScan,
            onDismiss = { selectedTire = null }
        )
    }
}

@Composable
fun BatteryMetricTile(
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceElevated
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtext, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, fontSize = 9.sp)
        }
    }
}

@Composable
fun TireCard(
    tire: TireInfo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val statusColor = when (tire.status) {
        PressureStatus.NORMAL -> DiagnosticGreen
        PressureStatus.LOW -> DiagnosticAmber
        PressureStatus.CRITICAL -> DiagnosticRed
        PressureStatus.UNKNOWN -> colors.textSecondary
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("tire_card_${tire.id}"),
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${tire.id} • ${tire.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${tire.currentPsi} PSI",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Text(
                text = "Target: ${tire.recommendedPsi} PSI • ${tire.tempF}°F",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun MaintenanceReminderItem(
    reminder: MaintenanceEntity,
    currentOdometer: Int,
    onToggle: () -> Unit
) {
    val colors = AppTheme.colors
    val milesRemaining = reminder.dueOdometerMiles - currentOdometer
    val isDueSoon = milesRemaining in 1..1000

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (reminder.isCompleted) colors.border else if (isDueSoon) DiagnosticAmber.copy(alpha = 0.6f) else colors.border
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("reminder_item_${reminder.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (reminder.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (reminder.isCompleted) DiagnosticGreen else colors.textSecondary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (reminder.isCompleted) colors.textSecondary else colors.textPrimary
                )
                Text(
                    text = reminder.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (reminder.isCompleted) "Completed"
                    else if (milesRemaining > 0) "Due at ${reminder.dueOdometerMiles} mi (${milesRemaining} mi remaining)"
                    else "Service is due now (${currentOdometer} mi)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (reminder.isCompleted) DiagnosticGreen else if (isDueSoon) DiagnosticAmber else colors.textPrimary
                )
            }
        }
    }
}

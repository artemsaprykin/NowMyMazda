package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EngineState
import com.example.data.model.LockState
import com.example.data.model.PressureStatus
import com.example.data.model.TireInfo
import com.example.data.model.VehicleState
import com.example.ui.theme.CabinBorder
import com.example.ui.theme.CabinSurface
import com.example.ui.theme.CabinSurfaceElevated
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
fun VehicleTopDownView(
    vehicleState: VehicleState,
    modifier: Modifier = Modifier,
    onTireClick: (TireInfo) -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "engine_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val hazardFlash by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hazardFlash"
    )

    val colors = com.example.ui.theme.AppTheme.colors

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("vehicle_diagram_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(colors.border, colors.border.copy(alpha = 0.5f))))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with car model & status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = vehicleState.nickname,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "${vehicleState.year} ${vehicleState.modelName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                // Lock & Engine Status Badges
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (vehicleState.lockState == LockState.LOCKED)
                            Color(0xFF172B1E) else Color(0xFF331E20),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (vehicleState.lockState == LockState.LOCKED) DiagnosticGreen.copy(alpha = 0.6f) else DiagnosticRed.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (vehicleState.lockState == LockState.LOCKED) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (vehicleState.lockState == LockState.LOCKED) DiagnosticGreen else DiagnosticRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (vehicleState.lockState == LockState.LOCKED) "Locked" else "Unlocked",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (vehicleState.lockState == LockState.LOCKED) DiagnosticGreen else DiagnosticRed
                            )
                        }
                    }

                    if (vehicleState.engineState == EngineState.RUNNING) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SoulRedDark.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SoulRedLight)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(SoulRedLight)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Engine ON",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SoulRedLight
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Top-Down Canvas with Tires and Chassis
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Car Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(260.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    // Hazard Lights Aura if active
                    if (vehicleState.hazardLightsActive && hazardFlash > 0.4f) {
                        drawCircle(
                            color = DiagnosticAmber.copy(alpha = 0.25f),
                            radius = w * 0.7f,
                            center = Offset(w / 2, h / 2)
                        )
                    }

                    // Main Vehicle Silhouette (Aerodynamic Mazda Kodo Design curves)
                    val carPath = Path().apply {
                        moveTo(w * 0.28f, h * 0.05f)
                        // Front hood curve
                        cubicTo(w * 0.35f, h * 0.01f, w * 0.65f, h * 0.01f, w * 0.72f, h * 0.05f)
                        // Right front fender
                        lineTo(w * 0.88f, h * 0.16f)
                        // Front right wheel arch
                        lineTo(w * 0.88f, h * 0.32f)
                        // Cabin right side
                        cubicTo(w * 0.87f, h * 0.45f, w * 0.87f, h * 0.62f, w * 0.90f, h * 0.72f)
                        // Rear right wheel arch
                        lineTo(w * 0.90f, h * 0.84f)
                        // Rear bumper
                        cubicTo(w * 0.75f, h * 0.98f, w * 0.25f, h * 0.98f, w * 0.10f, h * 0.84f)
                        // Rear left wheel arch
                        lineTo(w * 0.10f, h * 0.72f)
                        // Cabin left side
                        cubicTo(w * 0.13f, h * 0.62f, w * 0.13f, h * 0.45f, w * 0.12f, h * 0.32f)
                        // Front left fender
                        lineTo(w * 0.12f, h * 0.16f)
                        close()
                    }

                    // Car body fill
                    drawPath(
                        path = carPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(SoulRedLight, SoulRedPrimary, SoulRedDark, Color(0xFF5A0810)),
                            startY = 0f,
                            endY = h
                        )
                    )

                    // Car body contour border
                    drawPath(
                        path = carPath,
                        color = Color(0xFFFF8A95),
                        style = Stroke(width = 2.5f)
                    )

                    // Windshield and Cabin Roof (Tinted Glass)
                    val windshieldPath = Path().apply {
                        moveTo(w * 0.24f, h * 0.30f)
                        lineTo(w * 0.76f, h * 0.30f)
                        lineTo(w * 0.70f, h * 0.48f)
                        lineTo(w * 0.30f, h * 0.48f)
                        close()
                    }
                    drawPath(
                        path = windshieldPath,
                        color = Color(0xCC090C0F)
                    )
                    drawPath(
                        path = windshieldPath,
                        color = Color(0x44FFFFFF),
                        style = Stroke(width = 1.5f)
                    )

                    // Panoramic Sunroof
                    drawRoundRect(
                        color = Color(0xDD0D1117),
                        topLeft = Offset(w * 0.28f, h * 0.50f),
                        size = Size(w * 0.44f, h * 0.20f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    // Rear Windshield
                    val rearWindowPath = Path().apply {
                        moveTo(w * 0.28f, h * 0.72f)
                        lineTo(w * 0.72f, h * 0.72f)
                        lineTo(w * 0.66f, h * 0.82f)
                        lineTo(w * 0.34f, h * 0.82f)
                        close()
                    }
                    drawPath(
                        path = rearWindowPath,
                        color = Color(0xDD090C0F)
                    )

                    // Engine Bay Glow when RUNNING
                    if (vehicleState.engineState == EngineState.RUNNING) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFB03A).copy(alpha = pulseAlpha),
                                    Color(0xFFFF5722).copy(alpha = pulseAlpha * 0.5f),
                                    Color.Transparent
                                ),
                                center = Offset(w * 0.5f, h * 0.17f),
                                radius = w * 0.32f
                            ),
                            radius = w * 0.32f,
                            center = Offset(w * 0.5f, h * 0.17f)
                        )
                    }

                    // Headlights / Taillights
                    drawRoundRect(
                        color = if (vehicleState.engineState == EngineState.RUNNING || vehicleState.hazardLightsActive)
                            Color(0xFFFFF9E6) else Color(0x66FFFFFF),
                        topLeft = Offset(w * 0.18f, h * 0.06f),
                        size = Size(w * 0.16f, h * 0.035f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = if (vehicleState.engineState == EngineState.RUNNING || vehicleState.hazardLightsActive)
                            Color(0xFFFFF9E6) else Color(0x66FFFFFF),
                        topLeft = Offset(w * 0.66f, h * 0.06f),
                        size = Size(w * 0.16f, h * 0.035f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )

                    // Mazda Soul Red Signature Tail Lights
                    drawRoundRect(
                        color = Color(0xFFFF2233),
                        topLeft = Offset(w * 0.16f, h * 0.88f),
                        size = Size(w * 0.18f, h * 0.025f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = Color(0xFFFF2233),
                        topLeft = Offset(w * 0.66f, h * 0.88f),
                        size = Size(w * 0.18f, h * 0.025f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }

                // 4 Tire Widgets positioned on Left & Right
                // Front Left (top-left)
                TirePressureBadge(
                    tire = vehicleState.tpms.frontLeft,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = 28.dp),
                    onClick = { onTireClick(vehicleState.tpms.frontLeft) }
                )

                // Front Right (top-right)
                TirePressureBadge(
                    tire = vehicleState.tpms.frontRight,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 12.dp, top = 28.dp),
                    onClick = { onTireClick(vehicleState.tpms.frontRight) }
                )

                // Rear Left (bottom-left)
                TirePressureBadge(
                    tire = vehicleState.tpms.rearLeft,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 28.dp),
                    onClick = { onTireClick(vehicleState.tpms.rearLeft) }
                )

                // Rear Right (bottom-right)
                TirePressureBadge(
                    tire = vehicleState.tpms.rearRight,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 28.dp),
                    onClick = { onTireClick(vehicleState.tpms.rearRight) }
                )

                // Engine Center Badge
                if (vehicleState.engineState == EngineState.RUNNING) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xEE141920),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SoulRedLight),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 60.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = SoulRedLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val remMins = vehicleState.remoteStart.remainingSeconds / 60
                            val remSecs = vehicleState.remoteStart.remainingSeconds % 60
                            Text(
                                text = String.format(java.util.Locale.US, "%02d:%02d", remMins, remSecs),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Status Pill row: Battery 12V, Fuel %, Odometer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusMetricPill(
                    icon = Icons.Default.BatteryChargingFull,
                    label = "12V Battery",
                    value = if (vehicleState.batteryHealth.isAvailable) "${vehicleState.batteryHealth.voltage}V" else "Unavailable",
                    color = if (vehicleState.batteryHealth.isAvailable && vehicleState.batteryHealth.voltage >= 12.2) DiagnosticGreen else DiagnosticAmber
                )
                StatusMetricPill(
                    icon = Icons.Default.Warning,
                    label = "Oil Life",
                    value = if (vehicleState.diagnostics.isAvailable) "${vehicleState.diagnostics.oilLifePercent}%" else "Unavailable",
                    color = if (vehicleState.diagnostics.isAvailable && vehicleState.diagnostics.oilLifePercent > 20) ElectricCyan else DiagnosticAmber
                )
                StatusMetricPill(
                    icon = null,
                    label = "Est. Range",
                    value = if (vehicleState.isFuelAvailable) "${vehicleState.estimatedRangeMiles} mi" else "Unavailable",
                    color = colors.textPrimary
                )
            }
        }
    }
}

@Composable
fun TirePressureBadge(
    tire: TireInfo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = com.example.ui.theme.AppTheme.colors
    val statusColor = when (tire.status) {
        PressureStatus.NORMAL -> DiagnosticGreen
        PressureStatus.LOW -> DiagnosticAmber
        PressureStatus.CRITICAL -> DiagnosticRed
        PressureStatus.UNKNOWN -> colors.textSecondary
    }

    Surface(
        modifier = modifier
            .testTag("tire_badge_${tire.id}")
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.8f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = tire.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${tire.currentPsi} PSI",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = "${tire.tempF}°F",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 9.sp,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
fun StatusMetricPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    label: String,
    value: String,
    color: Color
) {
    val colors = com.example.ui.theme.AppTheme.colors

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceElevated,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = colors.textSecondary
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

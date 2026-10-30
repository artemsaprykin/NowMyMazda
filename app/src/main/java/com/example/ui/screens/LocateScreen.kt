package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.example.util.LocationHelper

@Composable
fun LocateScreen(
    vehicleState: VehicleState,
    onFindCarFlashHorn: () -> Unit,
    onSetSafeZone: () -> Unit,
    onUpdateRadius: (Int) -> Unit,
    onToggleGeofence: (Boolean) -> Unit,
    onRefreshVehicleGps: () -> Unit,
    onDismissAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    var walkingGuideActive by remember { mutableStateOf(false) }
    val colors = AppTheme.colors

    // Runtime permission launcher for GPS
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    val alarmPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alarmPulse"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "GPS Vehicle Locator & Geofencing",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = "Track your Mazda's location in real-time and configure anti-theft security perimeter.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }

        // Active Stolen Vehicle / Geofence Breach Banner
        item {
            if (vehicleState.geofence.theftAlarmActive) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4A1015)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DiagnosticRed.copy(alpha = alarmPulse))
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("theft_alarm_banner")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = DiagnosticRed,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "🚨 GEOFENCE SECURITY BREACH!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Vehicle exited '${vehicleState.geofence.safeZoneName}' safe zone without authorization.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onDismissAlarm,
                            colors = ButtonDefaults.buttonColors(containerColor = DiagnosticRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dismiss_theft_alarm_btn")
                        ) {
                            Text("Deactivate Alarm (Requires Biometrics / PIN)")
                        }
                    }
                }
            }
        }

        // Interactive Radar / Map Visualization Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gps_map_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Map Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = SoulRedLight, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Parked Location",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.surfaceElevated
                        ) {
                            Text(
                                text = "GPS Lock Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = DiagnosticGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = vehicleState.location.address,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Lat: ${vehicleState.location.latitude}, Lng: ${vehicleState.location.longitude}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Radar Screen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0A0D11)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val center = Offset(w / 2, h / 2)

                            // Grid lines
                            for (i in 1..4) {
                                val radius = (w.coerceAtMost(h) / 2) * (i / 4f)
                                drawCircle(
                                    color = Color(0xFF1E2836),
                                    radius = radius,
                                    center = center,
                                    style = Stroke(width = 1.2f)
                                )
                            }
                            drawLine(
                                color = Color(0xFF1E2836),
                                start = Offset(0f, h / 2),
                                end = Offset(w, h / 2),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = Color(0xFF1E2836),
                                start = Offset(w / 2, 0f),
                                end = Offset(w / 2, h),
                                strokeWidth = 1f
                            )

                            // Geofence Safe Zone Circle if enabled
                            if (vehicleState.geofence.isEnabled) {
                                val geofenceRadiusPixels = (w * 0.38f) * (vehicleState.geofence.radiusMeters / 500f).coerceIn(0.4f, 1.2f)
                                val geofenceColor = if (vehicleState.geofence.isBreached) DiagnosticRed else ElectricCyan
                                drawCircle(
                                    color = geofenceColor.copy(alpha = 0.15f),
                                    radius = geofenceRadiusPixels,
                                    center = center
                                )
                                drawCircle(
                                    color = geofenceColor,
                                    radius = geofenceRadiusPixels,
                                    center = center,
                                    style = Stroke(width = 2.5f)
                                )
                            }

                            // Rotating radar sweep
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(Color.Transparent, SoulRedLight.copy(alpha = 0.3f), Color.Transparent),
                                    center = center
                                ),
                                startAngle = sweepAngle,
                                sweepAngle = 45f,
                                useCenter = true,
                                topLeft = Offset(center.x - w / 2, center.y - h / 2),
                                size = size
                            )

                            // User Phone Position (offset slightly south-west)
                            val userOffset = Offset(center.x - w * 0.22f, center.y + h * 0.22f)
                            drawCircle(
                                color = ElectricCyan.copy(alpha = 0.3f),
                                radius = 18f,
                                center = userOffset
                            )
                            drawCircle(
                                color = ElectricCyan,
                                radius = 8f,
                                center = userOffset
                            )

                            // Vehicle Position at Center
                            val vehicleColor = if (vehicleState.geofence.isBreached) DiagnosticRed else SoulRedPrimary
                            drawCircle(
                                color = vehicleColor.copy(alpha = 0.4f),
                                radius = 24f,
                                center = center
                            )
                            drawCircle(
                                color = vehicleColor,
                                radius = 10f,
                                center = center
                            )
                        }

                        // Labels inside radar
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xDD0F141C),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(top = 40.dp)
                        ) {
                            Text(
                                text = "Mazda CX-50",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xDD0F141C),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 24.dp, bottom = 24.dp)
                        ) {
                            Text(
                                text = "Your Phone (320 ft)",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons (Find in Lot / Walk to Car)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onFindCarFlashHorn,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("find_car_sound_btn")
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = SoulRedLight, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Honk & Flash", color = colors.textPrimary)
                        }

                        Button(
                            onClick = { walkingGuideActive = !walkingGuideActive },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (walkingGuideActive) SoulRedPrimary else colors.border
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("walk_to_car_btn")
                        ) {
                            Icon(Icons.Default.DirectionsWalk, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (walkingGuideActive) "Guiding..." else "Walk to Car")
                        }
                    }

                    // Walking Compass guidance overlay
                    AnimatedVisibility(visible = walkingGuideActive) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colors.surfaceElevated,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Navigation,
                                            contentDescription = null,
                                            tint = SoulRedLight,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .rotate(45f)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Head North-East",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                            Text(
                                                text = "Vehicle is ~320 feet away across parking area",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }
                                    Text(
                                        text = "~1 min walk",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Geofence Security Perimeter Configuration Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(colors.border)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("geofence_card")
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
                                    .background(SoulRedDark.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = SoulRedLight, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Anti-Theft Geofence Guard",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = if (vehicleState.geofence.isEnabled) "Perimeter armed around ${vehicleState.geofence.safeZoneName}" else "Perimeter inactive",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (vehicleState.geofence.isEnabled) DiagnosticGreen else colors.textSecondary
                                )
                            }
                        }

                        Switch(
                            checked = vehicleState.geofence.isEnabled,
                            onCheckedChange = onToggleGeofence,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SoulRedPrimary
                            ),
                            modifier = Modifier.testTag("geofence_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Safety Perimeter Radius",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Radius selector chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(100, 300, 500, 1000).forEach { radius ->
                            FilterChip(
                                selected = vehicleState.geofence.radiusMeters == radius,
                                onClick = { onUpdateRadius(radius) },
                                label = { Text("${radius}m") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SoulRedPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onSetSafeZone,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("set_safe_zone_btn")
                    ) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set Current Parked Location as Safe Zone", color = colors.textPrimary)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onRefreshVehicleGps,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("refresh_vehicle_gps_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Query Vehicle TCU for Live GPS Fix", color = colors.textPrimary, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val context = androidx.compose.ui.platform.LocalContext.current
                    OutlinedButton(
                        onClick = {
                            LocationHelper.openNavigationDirections(
                                context,
                                vehicleState.location.latitude,
                                vehicleState.location.longitude,
                                vehicleState.nickname
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_maps_nav_btn")
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Navigation in Google Maps", color = colors.textPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

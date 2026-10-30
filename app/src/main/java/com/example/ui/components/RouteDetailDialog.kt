package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.TripLogEntity
import com.example.ui.theme.CabinBackground
import com.example.ui.theme.CabinBorder
import com.example.ui.theme.CabinSurface
import com.example.ui.theme.CabinSurfaceElevated
import com.example.ui.theme.DiagnosticGreen
import com.example.ui.theme.DiagnosticRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MetallicGray
import com.example.ui.theme.PlatinumSilver
import com.example.ui.theme.SoulRedDark
import com.example.ui.theme.SoulRedLight
import com.example.ui.theme.SoulRedPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RouteDetailDialog(
    trip: TripLogEntity,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEEE, MMM d, yyyy • h:mm a", Locale.US) }
    val formattedDate = remember(trip.startTimeMillis) { dateFormat.format(Date(trip.startTimeMillis)) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CabinSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CabinBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("route_detail_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header with Tag & i-DM
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SoulRedDark.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SoulRedLight.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "${trip.tripTag} Journey",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = SoulRedLight,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF142B1F),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DiagnosticGreen.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Eco, contentDescription = null, tint = DiagnosticGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "i-DM: ${trip.drivingScore}/100",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = DiagnosticGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MetallicGray
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Route Map Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0A0D12))
                        .border(1.dp, CabinBorder, RoundedCornerShape(16.dp))
                ) {
                    Canvas(modifier = Modifier.padding(16.dp).fillMaxWidth().height(140.dp)) {
                        val w = size.width
                        val h = size.height

                        // Background stylized grid
                        for (i in 1..4) {
                            val y = h * (i / 5f)
                            drawLine(
                                color = Color(0xFF17202B),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f
                            )
                        }
                        for (i in 1..5) {
                            val x = w * (i / 6f)
                            drawLine(
                                color = Color(0xFF17202B),
                                start = Offset(x, 0f),
                                end = Offset(x, h),
                                strokeWidth = 1f
                            )
                        }

                        // S-curve route polyline
                        val routePath = Path().apply {
                            moveTo(w * 0.12f, h * 0.82f)
                            cubicTo(
                                w * 0.25f, h * 0.35f,
                                w * 0.45f, h * 0.85f,
                                w * 0.65f, h * 0.40f
                            )
                            cubicTo(
                                w * 0.75f, h * 0.15f,
                                w * 0.85f, h * 0.25f,
                                w * 0.90f, h * 0.18f
                            )
                        }

                        // Glow behind route
                        drawPath(
                            path = routePath,
                            color = SoulRedLight.copy(alpha = 0.3f),
                            style = Stroke(width = 10f)
                        )

                        // Main route line
                        drawPath(
                            path = routePath,
                            brush = Brush.horizontalGradient(
                                listOf(SoulRedLight, SoulRedPrimary, ElectricCyan)
                            ),
                            style = Stroke(width = 4f)
                        )

                        // Origin pin (green)
                        drawCircle(
                            color = DiagnosticGreen.copy(alpha = 0.3f),
                            radius = 12f,
                            center = Offset(w * 0.12f, h * 0.82f)
                        )
                        drawCircle(
                            color = DiagnosticGreen,
                            radius = 6f,
                            center = Offset(w * 0.12f, h * 0.82f)
                        )

                        // Destination pin (cyan)
                        drawCircle(
                            color = ElectricCyan.copy(alpha = 0.3f),
                            radius = 12f,
                            center = Offset(w * 0.90f, h * 0.18f)
                        )
                        drawCircle(
                            color = ElectricCyan,
                            radius = 6f,
                            center = Offset(w * 0.90f, h * 0.18f)
                        )

                        // Intermediate waypoint
                        drawCircle(
                            color = PlatinumSilver,
                            radius = 3.5f,
                            center = Offset(w * 0.55f, h * 0.60f)
                        )
                    }

                    // Route overlay labels
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xCC0E141C),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Origin",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = DiagnosticGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xCC0E141C),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Destination",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Route summary path
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CabinSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = DiagnosticGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(trip.startAddress, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = PlatinumSilver)
                        }
                        Box(
                            modifier = Modifier
                                .padding(start = 7.dp)
                                .width(2.dp)
                                .height(12.dp)
                                .background(CabinBorder)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(trip.endAddress, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = PlatinumSilver)
                        }

                        if (trip.routeSummary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Route: ${trip.routeSummary}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MetallicGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Telemetry Details Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryMiniCard(
                        icon = Icons.Default.DirectionsCar,
                        label = "Distance",
                        value = "${trip.distanceMiles} mi",
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMiniCard(
                        icon = Icons.Default.Timer,
                        label = "Duration",
                        value = "${trip.durationMinutes} min",
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMiniCard(
                        icon = Icons.Default.LocalGasStation,
                        label = "Economy",
                        value = "${trip.avgMpg} MPG",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryMiniCard(
                        icon = Icons.Default.Speed,
                        label = "Avg Speed",
                        value = "${trip.avgSpeedMph} mph",
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMiniCard(
                        icon = Icons.Default.Speed,
                        label = "Max Speed",
                        value = "${trip.maxSpeedMph} mph",
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMiniCard(
                        icon = Icons.Default.LocalGasStation,
                        label = "Fuel Used",
                        value = "${trip.fuelConsumedGal} gal",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDelete(trip.id)
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DiagnosticRed),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("delete_trip_btn")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Trip", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = CabinBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close", color = PlatinumSilver)
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryMiniCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CabinSurfaceElevated,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MetallicGray, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = MetallicGray, fontSize = 9.sp)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = PlatinumSilver, fontSize = 12.sp)
        }
    }
}

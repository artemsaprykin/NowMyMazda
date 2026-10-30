package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.example.data.local.TripLogEntity
import com.example.ui.theme.CabinBorder
import com.example.ui.theme.CabinSurface
import com.example.ui.theme.CabinSurfaceElevated
import com.example.ui.theme.DiagnosticGreen
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
fun TripLogScreen(
    trips: List<TripLogEntity>,
    onAddTrip: (Double, Int, Double, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    val totalMiles = trips.sumOf { it.distanceMiles }
    val avgMpg = if (trips.isNotEmpty()) trips.map { it.avgMpg }.average() else 0.0
    val avgScore = if (trips.isNotEmpty()) trips.map { it.drivingScore }.average().toInt() else 0

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Trip History & Drive Logs",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PlatinumSilver
                        )
                        Text(
                            text = "Automated telematics journey logs & Mazda i-DM drive scores.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MetallicGray
                        )
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SoulRedPrimary),
                        modifier = Modifier.testTag("add_trip_top_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Drive", fontSize = 12.sp)
                    }
                }
            }

            // Summary Stats Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CabinSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CabinBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Lifetime Driving Analytics",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MetallicGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TripStatItem(
                                label = "Total Tracked",
                                value = String.format(Locale.US, "%.1f mi", totalMiles),
                                subtext = "${trips.size} drives"
                            )
                            TripStatItem(
                                label = "Avg Economy",
                                value = String.format(Locale.US, "%.1f MPG", avgMpg),
                                subtext = "Skyactiv-G 2.5T",
                                valueColor = ElectricCyan
                            )
                            TripStatItem(
                                label = "i-DM Score",
                                value = "$avgScore/100",
                                subtext = "Drive Master",
                                valueColor = DiagnosticGreen
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Recent Drives (${trips.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PlatinumSilver
                )
            }

            if (trips.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CabinSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Timeline, contentDescription = null, tint = MetallicGray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No trips logged yet", color = PlatinumSilver, fontWeight = FontWeight.Bold)
                            Text("Start a trip or tap '+ Log Drive' above to record telemetry.", color = MetallicGray, fontSize = 12.sp)
                        }
                    }
                }
            }

            items(trips, key = { it.id }) { trip ->
                TripHistoryCard(trip = trip)
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showAddDialog) {
        AddTripModal(
            onSave = { distance, duration, mpg, start, end, tag ->
                onAddTrip(distance, duration, mpg, start, end, tag)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
fun TripStatItem(
    label: String,
    value: String,
    subtext: String,
    valueColor: Color = PlatinumSilver
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MetallicGray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor)
        Text(subtext, style = MaterialTheme.typography.bodySmall, color = MetallicGray, fontSize = 10.sp)
    }
}

@Composable
fun TripHistoryCard(trip: TripLogEntity) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US) }
    val dateString = remember(trip.startTimeMillis) { dateFormat.format(Date(trip.startTimeMillis)) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CabinSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CabinBorder)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trip_card_${trip.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Tag and Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SoulRedDark.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = trip.tripTag,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SoulRedLight,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MetallicGray,
                        fontSize = 11.sp
                    )
                }

                // i-DM Score pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF142B1F),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DiagnosticGreen.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Eco, contentDescription = null, tint = DiagnosticGreen, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "i-DM: ${trip.drivingScore}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DiagnosticGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Route points
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, tint = SoulRedLight, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = trip.startAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PlatinumSilver
                )
            }

            Row(
                modifier = Modifier.padding(start = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(14.dp)
                        .background(CabinBorder)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = trip.endAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PlatinumSilver
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics row: Distance, Duration, Avg MPG, Speed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CabinSurfaceElevated, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Distance", color = MetallicGray, fontSize = 10.sp)
                    Text("${trip.distanceMiles} mi", fontWeight = FontWeight.Bold, color = PlatinumSilver, fontSize = 13.sp)
                }
                Column {
                    Text("Duration", color = MetallicGray, fontSize = 10.sp)
                    Text("${trip.durationMinutes} min", fontWeight = FontWeight.Bold, color = PlatinumSilver, fontSize = 13.sp)
                }
                Column {
                    Text("Economy", color = MetallicGray, fontSize = 10.sp)
                    Text("${trip.avgMpg} MPG", fontWeight = FontWeight.Bold, color = ElectricCyan, fontSize = 13.sp)
                }
                Column {
                    Text("Avg Speed", color = MetallicGray, fontSize = 10.sp)
                    Text(String.format(Locale.US, "%.0f mph", trip.avgSpeedMph), fontWeight = FontWeight.Bold, color = PlatinumSilver, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun AddTripModal(
    onSave: (Double, Int, Double, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var distanceStr by remember { mutableStateOf("18.5") }
    var durationStr by remember { mutableStateOf("28") }
    var mpgStr by remember { mutableStateOf("28.4") }
    var startAddress by remember { mutableStateOf("Home Garage, Seattle") }
    var endAddress by remember { mutableStateOf("Bellevue Square, WA") }
    var tag by remember { mutableStateOf("Commute") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CabinSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CabinBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("add_trip_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Record Vehicle Trip",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PlatinumSilver
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = startAddress,
                    onValueChange = { startAddress = it },
                    label = { Text("Starting Location") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PlatinumSilver,
                        unfocusedTextColor = PlatinumSilver,
                        focusedBorderColor = SoulRedPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = endAddress,
                    onValueChange = { endAddress = it },
                    label = { Text("Destination") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PlatinumSilver,
                        unfocusedTextColor = PlatinumSilver,
                        focusedBorderColor = SoulRedPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = distanceStr,
                        onValueChange = { distanceStr = it },
                        label = { Text("Miles") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PlatinumSilver,
                            unfocusedTextColor = PlatinumSilver,
                            focusedBorderColor = SoulRedPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = durationStr,
                        onValueChange = { durationStr = it },
                        label = { Text("Mins") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PlatinumSilver,
                            unfocusedTextColor = PlatinumSilver,
                            focusedBorderColor = SoulRedPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = mpgStr,
                        onValueChange = { mpgStr = it },
                        label = { Text("MPG") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PlatinumSilver,
                            unfocusedTextColor = PlatinumSilver,
                            focusedBorderColor = SoulRedPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val distance = distanceStr.toDoubleOrNull() ?: 10.0
                        val duration = durationStr.toIntOrNull() ?: 20
                        val mpg = mpgStr.toDoubleOrNull() ?: 28.0
                        onSave(distance, duration, mpg, startAddress, endAddress, tag)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoulRedPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_trip_btn")
                ) {
                    Text("Save to Vehicle Logbook")
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

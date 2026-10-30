package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.example.data.model.VehicleState
import com.example.ui.TripSortOption
import com.example.ui.components.ActiveDriveCard
import com.example.ui.components.RouteDetailDialog
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
    vehicleState: VehicleState,
    trips: List<TripLogEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedTagFilter: String,
    onTagFilterChange: (String) -> Unit,
    selectedSort: TripSortOption,
    onSortChange: (TripSortOption) -> Unit,
    onStartLiveDrive: (String, String, String) -> Unit,
    onStopLiveDrive: () -> Unit,
    onAddTrip: (Double, Int, Double, String, String, String) -> Unit,
    onDeleteTrip: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartDriveDialog by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var selectedTripForDetails by remember { mutableStateOf<TripLogEntity?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val totalMiles = trips.sumOf { it.distanceMiles }
    val avgMpg = if (trips.isNotEmpty()) trips.map { it.avgMpg }.average() else 0.0
    val avgScore = if (trips.isNotEmpty()) trips.map { it.drivingScore }.average().toInt() else 0

    val availableTags = listOf("All", "Commute", "Highway", "City", "Scenic", "Business")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Trip History & Telematics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PlatinumSilver
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(DiagnosticGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Auto-Logging: Active on Ignition & Movement",
                            style = MaterialTheme.typography.bodySmall,
                            color = DiagnosticGreen,
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = { showStartDriveDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SoulRedPrimary),
                    modifier = Modifier.testTag("start_drive_sim_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Drive Now", fontSize = 12.sp)
                }
            }
        }

        // Live Drive Telemetry Card (Shown when driving session is active)
        item {
            AnimatedVisibility(visible = vehicleState.activeDriveSession != null) {
                vehicleState.activeDriveSession?.let { session ->
                    ActiveDriveCard(
                        session = session,
                        onStopDrive = onStopLiveDrive,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }

        // Lifetime Analytics Banner
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CabinSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CabinBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Trip History Analytics",
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
                            label = "Total Miles",
                            value = String.format(Locale.US, "%.1f mi", totalMiles),
                            subtext = "${trips.size} logged trips"
                        )
                        TripStatItem(
                            label = "Average MPG",
                            value = String.format(Locale.US, "%.1f", avgMpg),
                            subtext = "Skyactiv-G 2.5T",
                            valueColor = ElectricCyan
                        )
                        TripStatItem(
                            label = "Mazda i-DM",
                            value = "$avgScore/100",
                            subtext = "Drive Master Score",
                            valueColor = DiagnosticGreen
                        )
                    }
                }
            }
        }

        // Search Bar & Sort Dropdown
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search routes, streets, tags...", color = MetallicGray, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MetallicGray, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MetallicGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PlatinumSilver,
                        unfocusedTextColor = PlatinumSilver,
                        focusedBorderColor = SoulRedPrimary,
                        unfocusedBorderColor = CabinBorder,
                        focusedContainerColor = CabinSurface,
                        unfocusedContainerColor = CabinSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("trip_search_input")
                )

                // Sort Dropdown Button
                Box {
                    Button(
                        onClick = { sortMenuExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CabinSurfaceElevated),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("trip_sort_button")
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort", tint = PlatinumSilver, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sort", color = PlatinumSilver, fontSize = 12.sp)
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        modifier = Modifier.background(CabinSurface)
                    ) {
                        TripSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.displayName,
                                        color = if (selectedSort == option) SoulRedLight else PlatinumSilver,
                                        fontWeight = if (selectedSort == option) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSortChange(option)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Filter Tags Horizontal Scroll
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableTags.forEach { tag ->
                    FilterChip(
                        selected = selectedTagFilter == tag,
                        onClick = { onTagFilterChange(tag) },
                        label = { Text(tag) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SoulRedPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = CabinSurface,
                            labelColor = MetallicGray
                        ),
                        modifier = Modifier.testTag("filter_tag_$tag")
                    )
                }
            }
        }

        // Active List Count & Manual Record Link
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Showing ${trips.size} trips (${selectedSort.displayName})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MetallicGray
                )

                Text(
                    text = "+ Manual Log",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SoulRedLight,
                    modifier = Modifier
                        .clickable { showManualAddDialog = true }
                        .padding(4.dp)
                        .testTag("manual_log_trigger")
                )
            }
        }

        // Trip Items
        if (trips.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CabinSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CabinBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Timeline, contentDescription = null, tint = MetallicGray, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No matching trips found", color = PlatinumSilver, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try adjusting your search terms or filter." else "Drive your vehicle to auto-record trips.",
                            color = MetallicGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        items(trips, key = { it.id }) { trip ->
            TripHistoryDetailedCard(
                trip = trip,
                onViewRouteMap = { selectedTripForDetails = trip }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal: Drive Simulation Starter
    if (showStartDriveDialog) {
        StartLiveDriveModal(
            onStart = { start, dest, tag ->
                onStartLiveDrive(start, dest, tag)
                showStartDriveDialog = false
            },
            onDismiss = { showStartDriveDialog = false }
        )
    }

    // Modal: Manual Trip Record
    if (showManualAddDialog) {
        AddTripModal(
            onSave = { distance, duration, mpg, start, end, tag ->
                onAddTrip(distance, duration, mpg, start, end, tag)
                showManualAddDialog = false
            },
            onDismiss = { showManualAddDialog = false }
        )
    }

    // Modal: Route Taken Interactive Visualizer
    selectedTripForDetails?.let { trip ->
        RouteDetailDialog(
            trip = trip,
            onDelete = { id ->
                onDeleteTrip(id)
                selectedTripForDetails = null
            },
            onDismiss = { selectedTripForDetails = null }
        )
    }
}

@Composable
fun TripHistoryDetailedCard(
    trip: TripLogEntity,
    onViewRouteMap: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.US) }

    val dateStr = remember(trip.startTimeMillis) { dateFormat.format(Date(trip.startTimeMillis)) }
    val startTimeStr = remember(trip.startTimeMillis) { timeFormat.format(Date(trip.startTimeMillis)) }
    val endTimeStr = remember(trip.endTimeMillis) { timeFormat.format(Date(trip.endTimeMillis)) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CabinSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CabinBorder)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewRouteMap() }
            .testTag("trip_card_${trip.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
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
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$dateStr • $startTimeStr → $endTimeStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MetallicGray,
                        fontSize = 11.sp
                    )
                }

                // i-DM score badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF142B1F),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DiagnosticGreen.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Eco, contentDescription = null, tint = DiagnosticGreen, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "i-DM: ${trip.drivingScore}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DiagnosticGreen,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Route Points
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, tint = DiagnosticGreen, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = trip.startAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PlatinumSilver
                )
            }

            Box(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .width(2.dp)
                    .height(10.dp)
                    .background(CabinBorder)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = trip.endAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PlatinumSilver
                )
            }

            // Route Summary Taken
            if (trip.routeSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Route taken: ${trip.routeSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MetallicGray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metrics Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CabinSurfaceElevated, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Distance", color = MetallicGray, fontSize = 9.sp)
                    Text("${trip.distanceMiles} mi", fontWeight = FontWeight.Bold, color = PlatinumSilver, fontSize = 12.sp)
                }
                Column {
                    Text("Duration", color = MetallicGray, fontSize = 9.sp)
                    Text("${trip.durationMinutes} min", fontWeight = FontWeight.Bold, color = PlatinumSilver, fontSize = 12.sp)
                }
                Column {
                    Text("Economy", color = MetallicGray, fontSize = 9.sp)
                    Text("${trip.avgMpg} MPG", fontWeight = FontWeight.Bold, color = ElectricCyan, fontSize = 12.sp)
                }
                Column {
                    Text("Avg Speed", color = MetallicGray, fontSize = 9.sp)
                    Text("${trip.avgSpeedMph.toInt()} mph", fontWeight = FontWeight.Bold, color = PlatinumSilver, fontSize = 12.sp)
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CabinBorder,
                    modifier = Modifier.clickable { onViewRouteMap() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, tint = PlatinumSilver, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Map", fontSize = 10.sp, color = PlatinumSilver, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StartLiveDriveModal(
    onStart: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPresetIndex by remember { mutableStateOf(0) }

    val presets = listOf(
        Triple("Home Garage, Seattle", "Bellevue Tech Center, WA", "Commute"),
        Triple("Downtown Seattle", "Snoqualmie Pass Summit, WA", "Highway"),
        Triple("Queen Anne, Seattle", "Capitol Hill Pine St, WA", "City"),
        Triple("Alki Beach, West Seattle", "Magnolia Viewpoint, WA", "Scenic")
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CabinSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CabinBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("start_drive_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = SoulRedLight, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Start Vehicle Drive",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PlatinumSilver
                        )
                        Text(
                            text = "Ignition will turn ON and auto-record journey telemetry.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MetallicGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select Route Destination:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MetallicGray
                )
                Spacer(modifier = Modifier.height(8.dp))

                presets.forEachIndexed { index, (origin, destination, tag) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedPresetIndex == index) SoulRedDark.copy(alpha = 0.3f) else CabinSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedPresetIndex == index) SoulRedLight else CabinBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedPresetIndex = index }
                            .testTag("preset_route_$index")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(tag, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SoulRedLight)
                                if (selectedPresetIndex == index) {
                                    Text("Selected", style = MaterialTheme.typography.labelSmall, color = DiagnosticGreen)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(destination, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = PlatinumSilver)
                            Text("From: $origin", style = MaterialTheme.typography.bodySmall, color = MetallicGray, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val (origin, destination, tag) = presets[selectedPresetIndex]
                        onStart(origin, destination, tag)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoulRedPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_start_drive_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Driving & Auto-Record")
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

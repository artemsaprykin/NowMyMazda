package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.PressureStatus
import com.example.data.model.TireInfo
import com.example.ui.theme.CabinBorder
import com.example.ui.theme.CabinSurface
import com.example.ui.theme.CabinSurfaceElevated
import com.example.ui.theme.DiagnosticAmber
import com.example.ui.theme.DiagnosticGreen
import com.example.ui.theme.DiagnosticRed
import com.example.ui.theme.MetallicGray
import com.example.ui.theme.PlatinumSilver
import com.example.ui.theme.SoulRedPrimary

@Composable
fun TireDetailDialog(
    tire: TireInfo,
    onCalibrate: () -> Unit,
    onSimulateLeak: () -> Unit,
    onDismiss: () -> Unit
) {
    val statusColor = when (tire.status) {
        PressureStatus.NORMAL -> DiagnosticGreen
        PressureStatus.LOW -> DiagnosticAmber
        PressureStatus.CRITICAL -> DiagnosticRed
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CabinSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CabinBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("tire_detail_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.2f))
                        .border(1.dp, statusColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "${tire.name} (${tire.id})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PlatinumSilver
                )

                Text(
                    text = when (tire.status) {
                        PressureStatus.NORMAL -> "Optimal Pressure Verified"
                        PressureStatus.LOW -> "Low Pressure Warning"
                        PressureStatus.CRITICAL -> "Critical Pressure Alert"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CabinSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Current Pressure", color = MetallicGray)
                            Text("${tire.currentPsi} PSI", fontWeight = FontWeight.Bold, color = PlatinumSilver)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Recommended Target", color = MetallicGray)
                            Text("${tire.recommendedPsi} PSI", fontWeight = FontWeight.SemiBold, color = DiagnosticGreen)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tire Internal Temp", color = MetallicGray)
                            Text("${tire.tempF}°F", color = PlatinumSilver)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onCalibrate()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoulRedPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calibrate_tire_btn")
                ) {
                    Text("Calibrate / Inflate to 36.0 PSI")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        onSimulateLeak()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Simulate Slow Pressure Leak", color = DiagnosticAmber)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = PlatinumSilver)
                }
            }
        }
    }
}

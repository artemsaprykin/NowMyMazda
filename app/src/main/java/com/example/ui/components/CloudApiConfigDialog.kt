package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CloudApiConfig
import com.example.ui.theme.AppTheme
import com.example.ui.theme.DiagnosticGreen
import com.example.ui.theme.SoulRedPrimary

@Composable
fun CloudApiConfigDialog(
    config: CloudApiConfig,
    onSave: (String, Boolean) -> Unit,
    onTestPing: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    var apiUrl by remember { mutableStateOf(config.apiUrl) }
    var isGateway by remember { mutableStateOf(config.isGatewayAdapter) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("cloud_api_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Cloud Vehicle API Connection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Text(
                    text = "Connect directly to Mazda Connected Services regional clusters or via a cv/mcs Gateway Adapter.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                OutlinedTextField(
                    value = apiUrl,
                    onValueChange = { apiUrl = it },
                    label = { Text("API Host / Gateway URL") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoulRedPrimary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_url_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            apiUrl = "https://0cxo7m58.mazda.com/prod/"
                            isGateway = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mazda MNAO", fontSize = 11.sp, color = colors.textPrimary)
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            apiUrl = "http://10.0.2.2:8080/"
                            isGateway = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("cv/mcs Gateway", fontSize = 11.sp, color = colors.textPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Session: ${config.sessionStateSummary}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (config.hasValidSession) DiagnosticGreen else colors.textSecondary
                        )
                        Text(
                            text = "VIN: ${config.vin.ifBlank { "None Selected" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onTestPing,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ping_api_button")
                ) {
                    if (config.isSyncing) {
                        CircularProgressIndicator(color = colors.textPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Test Live Handshake Ping", color = colors.textPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        onSave(apiUrl, isGateway)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoulRedPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_cloud_api_button")
                ) {
                    Text("Save Connection Settings")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = colors.textPrimary)
                }
            }
        }
    }
}

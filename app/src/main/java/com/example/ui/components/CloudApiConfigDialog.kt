package com.example.ui.components

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.CloudApiConfig
import com.example.ui.theme.CabinBorder
import com.example.ui.theme.CabinSurface
import com.example.ui.theme.DiagnosticGreen
import com.example.ui.theme.MetallicGray
import com.example.ui.theme.PlatinumSilver
import com.example.ui.theme.SoulRedPrimary

@Composable
fun CloudApiConfigDialog(
    config: CloudApiConfig,
    onSave: (String, String) -> Unit,
    onTestPing: () -> Unit,
    onDismiss: () -> Unit
) {
    var apiUrl by remember { mutableStateOf(config.apiUrl) }
    var apiToken by remember { mutableStateOf(config.apiToken) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CabinSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CabinBorder),
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
                    color = PlatinumSilver
                )

                Text(
                    text = "Secure telematics REST API gateway for remote commands, telemetry polling, and DTC diagnostic alerts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MetallicGray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                OutlinedTextField(
                    value = apiUrl,
                    onValueChange = { apiUrl = it },
                    label = { Text("API Gateway URL") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoulRedPrimary,
                        unfocusedBorderColor = CabinBorder,
                        focusedTextColor = PlatinumSilver,
                        unfocusedTextColor = PlatinumSilver
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_url_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = apiToken,
                    onValueChange = { apiToken = it },
                    label = { Text("Secure Telematics Bearer Token") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoulRedPrimary,
                        unfocusedBorderColor = CabinBorder,
                        focusedTextColor = PlatinumSilver,
                        unfocusedTextColor = PlatinumSilver
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_token_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "VIN: ${config.vin}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MetallicGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onTestPing,
                    colors = ButtonDefaults.buttonColors(containerColor = CabinBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ping_api_button")
                ) {
                    if (config.isSyncing) {
                        CircularProgressIndicator(color = PlatinumSilver, strokeWidth = 2.dp)
                    } else {
                        Text("Test Live Gateway Ping & Sync", color = PlatinumSilver)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        onSave(apiUrl, apiToken)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoulRedPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_cloud_api_button")
                ) {
                    Text("Save API Credentials")
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

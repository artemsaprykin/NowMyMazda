package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.PendingSecurityAction
import com.example.ui.theme.AppTheme
import com.example.ui.theme.DiagnosticRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SoulRedDark
import com.example.ui.theme.SoulRedLight
import com.example.ui.theme.SoulRedPrimary

@Composable
fun BiometricPromptDialog(
    pendingAction: PendingSecurityAction,
    errorMessage: String?,
    onBiometricSuccess: () -> Unit,
    onPinSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    var showPinInput by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("biometric_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SoulRedDark.copy(alpha = 0.35f))
                        .border(1.dp, SoulRedLight.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = SoulRedLight,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = pendingAction.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = pendingAction.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (!showPinInput) {
                    // Biometric Sensor Touch Simulation
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                            .border(2.dp, ElectricCyan.copy(alpha = 0.7f), CircleShape)
                            .clickable { onBiometricSuccess() }
                            .testTag("biometric_fingerprint_touch"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Fingerprint Sensor",
                            tint = ElectricCyan,
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Touch sensor to authenticate",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onBiometricSuccess,
                        colors = ButtonDefaults.buttonColors(containerColor = SoulRedPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("biometric_confirm_button")
                    ) {
                        Text("Confirm with Biometrics")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { showPinInput = true },
                        modifier = Modifier.testTag("use_pin_button")
                    ) {
                        Text(
                            text = "Use 4-Digit Security PIN instead",
                            color = colors.textSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    // PIN Entry Pad
                    Text(
                        text = "Enter Security PIN",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 4-dot indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        for (i in 0 until 4) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i < enteredPin.length) SoulRedLight else colors.surfaceElevated
                                    )
                                    .border(1.dp, colors.border, CircleShape)
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            color = DiagnosticRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Keypad 1-9, 0, Backspace
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("C", "0", "OK")
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        for (row in rows) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                for (key in row) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = colors.surfaceElevated,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clickable {
                                                when (key) {
                                                    "C" -> enteredPin = ""
                                                    "OK" -> {
                                                        if (enteredPin.length == 4) onPinSubmit(enteredPin)
                                                    }
                                                    else -> {
                                                        if (enteredPin.length < 4) {
                                                            enteredPin += key
                                                            if (enteredPin.length == 4) {
                                                                onPinSubmit(enteredPin)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            .testTag("pin_key_$key")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = key,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { showPinInput = false }) {
                        Text("Back to Biometrics", color = colors.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = colors.textPrimary)
                }
            }
        }
    }
}

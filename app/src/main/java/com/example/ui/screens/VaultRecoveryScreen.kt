package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.design.theme.MaiaTheme

/**
 * Calm recovery screen displayed when the encrypted SQLCipher vault cannot be unwrapped
 * by this device's Android Keystore (for example, after a hardware key reset or an
 * unsupported device-to-device data copy).
 */
@Composable
fun VaultRecoveryScreen(
    errorMessage: String,
    onResetVault: () -> Unit
) {
    var showConfirmReset by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaiaTheme.colors.canvas)
            .padding(horizontal = 28.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Locked vault",
            tint = MaiaTheme.colors.maiaPresence,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Your vault can't be opened on this device",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaiaTheme.colors.ink,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Maia's encryption key is bound to the hardware security chip on the phone where the vault was created. Because that key isn't available here, the previous encrypted database cannot be decrypted.",
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MaiaTheme.colors.inkMuted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaiaTheme.colors.inkMuted.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = { showConfirmReset = true },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaiaTheme.colors.maiaPresence,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "Create a Fresh Vault",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }

    if (showConfirmReset) {
        AlertDialog(
            onDismissRequest = { showConfirmReset = false },
            title = { Text("Create a fresh encrypted vault?") },
            text = {
                Text(
                    "This clears the unreadable database file on this phone and generates a new hardware-backed key so you can start fresh with Maia."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmReset = false
                        onResetVault()
                    }
                ) {
                    Text("Reset & Start Fresh", color = MaiaTheme.colors.maiaPresence)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmReset = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


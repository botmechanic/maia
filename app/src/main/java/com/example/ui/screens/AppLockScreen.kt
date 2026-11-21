package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.design.components.MaiaOnDevicePill
import com.example.core.design.components.MaiaOrb
import com.example.core.design.theme.MaiaTheme
import com.example.security.BiometricAuthHelper

/**
 * Full-app minimalist biometric lock screen. Auto-launches fingerprint authentication
 * on appearance and offers a clean manual retry pill if dismissed.
 */
@Composable
fun AppLockScreen(
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun triggerPrompt() {
        val activity = BiometricAuthHelper.findFragmentActivity(context)
        if (activity != null && BiometricAuthHelper.canAuthenticate(context)) {
            BiometricAuthHelper.authenticate(
                activity = activity,
                title = "Unlock Maia",
                subtitle = "Confirm your fingerprint or screen lock",
                onSuccess = onUnlocked,
                onError = { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            onUnlocked()
        }
    }

    LaunchedEffect(Unit) {
        triggerPrompt()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaiaTheme.colors.canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        MaiaOnDevicePill(label = "VAULT LOCKED")

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            MaiaOrb(size = 84.dp)
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Maia is locked",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.6).sp
                ),
                color = MaiaTheme.colors.ink,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Use your fingerprint or screen lock to continue.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = MaiaTheme.colors.inkMuted,
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = { triggerPrompt() },
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaiaTheme.colors.maiaPresence,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("app_unlock_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Unlock with Fingerprint",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

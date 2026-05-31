package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SettingsViewModel
import com.example.ui.screens.SettingsMainScreen
import com.example.ui.theme.ReaderSettingsTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fully support full-bleed notch/navigation safely
        enableEdgeToEdge()

        setContent {
            val themeSetting by viewModel.theme.collectAsState()
            val isAppLockEnabled by viewModel.appLockEnabled.collectAsState()
            val correctPIN by viewModel.appLockPIN.collectAsState()

            var isUnlocked by remember { mutableStateOf(!isAppLockEnabled) }

            // Sync with PIN state updates in parent
            LaunchedEffect(isAppLockEnabled) {
                if (!isAppLockEnabled) {
                    isUnlocked = true
                }
            }

            ReaderSettingsTheme(themeSetting = themeSetting) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Safe Area Handling for Status Bar and Navigation Bar
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        AnimatedContent(
                            targetState = isUnlocked,
                            label = "MainSecurityGateway"
                        ) { unlocked ->
                            if (unlocked) {
                                // Renders the main Settings screen once unlocked or if app lock is inactive
                                SettingsMainScreen(
                                    viewModel = viewModel,
                                    activity = this@MainActivity
                                )
                            } else {
                                // Full screen premium PIN numpad vault
                                AppVaultLockScreen(
                                    correctPIN = correctPIN,
                                    onUnlockSuccess = {
                                        isUnlocked = true
                                        Toast.makeText(this@MainActivity, "تم فك قفل القارئ المتقدم!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-apply Screen Brightness & Security parameters when app gains focus
        viewModel.applyActivitySpecificSettings(this)
    }
}

// Vault Security Lock Screen Composable
@Composable
fun AppVaultLockScreen(
    correctPIN: String,
    onUnlockSuccess: () -> Unit
) {
    var pinEntered by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Vault Lock Header
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Locked Vault",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "تطبيق القارئ الحصين محمي بـ PIN",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = "الرجاء كتابة الرقم السري المكون من 4 أرقام المبرمج مسبقاً للدخول",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Simulated hidden code circles
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..4) {
                val isActive = pinEntered.length >= i
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Custom Security Numpad Layout
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val numpadRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("حذف", "0", "بصمة")
            )

            numpadRows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { buttonText ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    when (buttonText) {
                                        "حذف", "بصمة" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable {
                                    errorMessage = ""
                                    when (buttonText) {
                                        "حذف" -> {
                                            if (pinEntered.isNotEmpty()) {
                                                pinEntered = pinEntered.dropLast(1)
                                            }
                                        }
                                        "بصمة" -> {
                                            // Mock Biometrics fingerprint unlock simulation
                                            onUnlockSuccess()
                                        }
                                        else -> {
                                            if (pinEntered.length < 4) {
                                                pinEntered += buttonText
                                                if (pinEntered.length == 4) {
                                                    if (pinEntered == correctPIN) {
                                                        onUnlockSuccess()
                                                    } else {
                                                        errorMessage = "الرمز السري غير صحيح! (تلميح: $correctPIN)"
                                                        pinEntered = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (buttonText == "بصمة") {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Simulate Biometric",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = buttonText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.AppProvider
import com.example.ui.navigation.MainAppNavigation
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GoldPrimary
import com.example.ui.viewmodel.MainViewModel
import com.example.util.AppLocalizationProvider
import com.example.util.NotificationHelper
import com.example.util.SecurityValidator
import com.example.util.Translations
import com.example.util.tr

class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val chatId = intent.getStringExtra("OPEN_CHAT_ID")
            ?: intent.getStringExtra(NotificationHelper.EXTRA_CHAT_ID)
        if (!chatId.isNullOrBlank()) {
            com.example.util.MessengerBubbleManager.navigateToChatInApp(chatId)
        }
        com.example.util.ClientCardDeepLinkRouter.handleIncomingUri(intent.data)
    }

    override fun onResume() {
        super.onResume()
        // App is open in foreground -> hide floating bubble so it never appears inside the app or duplicates
        com.example.util.MessengerBubbleManager.setAppInForeground(this, true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.argb(0x16, 0x0F, 0x17, 0x2A),
                android.graphics.Color.argb(0x2E, 0x0F, 0x17, 0x2A)
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.argb(0x14, 0x0F, 0x17, 0x2A),
                android.graphics.Color.argb(0x2E, 0x0F, 0x17, 0x2A)
            )
        )

        // Initialize translation engine and notification channels in active language
        Translations.init(applicationContext)
        NotificationHelper.createNotificationChannels(applicationContext)
        com.example.util.FcmNotificationManager.initialize(applicationContext)
        com.example.util.MessengerBubbleManager.initialize(applicationContext)

        val startChatId = intent?.getStringExtra("OPEN_CHAT_ID")
            ?: intent?.getStringExtra(NotificationHelper.EXTRA_CHAT_ID)
        if (!startChatId.isNullOrBlank()) {
            com.example.util.MessengerBubbleManager.navigateToChatInApp(startChatId)
        }
        com.example.util.ClientCardDeepLinkRouter.handleIncomingUri(intent?.data)

        // Log uncaught exceptions and delegate to system handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("OOZOX_CRASH", "Uncaught exception in thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        setContent {
            var devModeStatus by remember {
                mutableStateOf(SecurityValidator.checkDeveloperMode(this@MainActivity))
            }

            // Observe both lifecycle ON_RESUME and system Settings ContentObserver so any change
            // to Developer Options or USB Debugging immediately locks/unlocks the app in real time.
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        devModeStatus = SecurityValidator.checkDeveloperMode(this@MainActivity)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)

                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                val settingsObserver = object : android.database.ContentObserver(handler) {
                    override fun onChange(selfChange: Boolean) {
                        devModeStatus = SecurityValidator.checkDeveloperMode(this@MainActivity)
                    }
                }
                try {
                    contentResolver.registerContentObserver(
                        android.provider.Settings.Global.getUriFor(
                            android.provider.Settings.Global.DEVELOPMENT_SETTINGS_ENABLED
                        ),
                        false,
                        settingsObserver
                    )
                    contentResolver.registerContentObserver(
                        android.provider.Settings.Global.getUriFor(
                            android.provider.Settings.Global.ADB_ENABLED
                        ),
                        false,
                        settingsObserver
                    )
                } catch (_: Exception) {
                }

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    try {
                        contentResolver.unregisterContentObserver(settingsObserver)
                    } catch (_: Exception) {
                    }
                }
            }

            if (devModeStatus.isDevModeActive) {
                val activeLang = remember { Translations.activeLanguage(this@MainActivity) }
                AppLocalizationProvider(currentLanguage = activeLang) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(52.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = tr("security.dev_mode_title"),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = tr("security.dev_mode_desc"),
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Security Threat Details Card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tr("security.dev_options"),
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (devModeStatus.isDevSettingsEnabled) tr("security.enabled_warn") else tr("security.disabled_ok"),
                                    fontSize = 12.5.sp,
                                    color = if (devModeStatus.isDevSettingsEnabled) Color(0xFFFB7185) else Color(0xFF34D399),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (devModeStatus.isAdbEnabled) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tr("security.usb_debug"),
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = tr("security.enabled_warn"),
                                        fontSize = 12.5.sp,
                                        color = Color(0xFFFB7185),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Open Settings Button
                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                    startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                                        startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tr("security.open_settings"), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Re-check Button
                        OutlinedButton(
                            onClick = {
                                devModeStatus = SecurityValidator.checkDeveloperMode(this@MainActivity)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tr("security.recheck"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Exit App Button
                        TextButton(
                            onClick = { finishAffinity() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("security.exit_app"), color = Color(0xFF9CA3AF), fontSize = 13.sp)
                        }
                    }
                }
                }
                return@setContent
            }

            // Request Notification Permission on Android 13+ (Location permissions are requested on-demand only when needed)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    val notifLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        Log.d("OOZOX_PERMISSIONS", "POST_NOTIFICATIONS granted: $isGranted")
                    }

                    LaunchedEffect(Unit) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            var hasFatalError by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf("") }

            if (hasFatalError) {
                val activeLang = remember { Translations.activeLanguage(this@MainActivity) }
                AppLocalizationProvider(currentLanguage = activeLang) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = tr("security.crash_title"),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tr("security.crash_desc"),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                hasFatalError = false
                                recreate()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tr("security.restart_app"), color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                }
            } else {
                AppProvider(viewModel = viewModel) {
                    MainAppNavigation(viewModel = viewModel)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Automatically revert to main profile upon leaving or closing the app for stealth privacy
        viewModel.lockToMainProfile()
        // App is backgrounded/closed -> launch floating bubble if permitted & enabled
        if (!isChangingConfigurations) {
            com.example.util.MessengerBubbleManager.setAppInForeground(this, false)
        }
    }
}


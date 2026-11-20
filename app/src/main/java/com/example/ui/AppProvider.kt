package com.example.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppToastController
import com.example.ui.components.LocalAppToast
import com.example.ui.components.ToastNotificationHost
import com.example.ui.components.PersistentConnectivityBanner
import com.example.ui.components.GlobalConnectivityBanner
import com.example.ui.spatial.SpatialLayoutEngineProvider
import com.example.ui.theme.AppThemePalette
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldChatTheme
import com.example.ui.theme.RealGoldAmber
import com.example.ui.viewmodel.MainViewModel
import com.example.util.DatabaseIntegrityChecker
import com.example.util.DatabaseVersionManager
import com.example.util.OfflineSyncManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Centralized ThemeManager interface enforcing a single source of truth for the app's
 * theme state (Light/Dark mode, Color Palette, Font Scale).
 */
interface ThemeManager {
    val isDarkTheme: Boolean
    val currentPalette: AppThemePalette
    val fontScale: com.example.ui.theme.AppFontScale
    fun toggleTheme()
    fun setDarkTheme(isDark: Boolean)
    fun setPalette(palette: AppThemePalette)
    fun setFontScale(scale: com.example.ui.theme.AppFontScale)
}

/**
 * Concrete implementation of [ThemeManager] providing state holders and mutation logic.
 */
data class AppThemeManager(
    override val isDarkTheme: Boolean,
    override val currentPalette: AppThemePalette,
    override val fontScale: com.example.ui.theme.AppFontScale,
    private val onToggleTheme: () -> Unit,
    private val onSetDarkTheme: (Boolean) -> Unit,
    private val onSetPalette: (AppThemePalette) -> Unit,
    private val onSetFontScale: (com.example.ui.theme.AppFontScale) -> Unit
) : ThemeManager {
    override fun toggleTheme() = onToggleTheme()
    override fun setDarkTheme(isDark: Boolean) = onSetDarkTheme(isDark)
    override fun setPalette(palette: AppThemePalette) = onSetPalette(palette)
    override fun setFontScale(scale: com.example.ui.theme.AppFontScale) = onSetFontScale(scale)
}

typealias AppThemeController = ThemeManager

/**
 * Global CompositionLocal providing access to the centralized [ThemeManager]
 * as the single source of truth across any component in the UI hierarchy.
 */
val LocalThemeManager = staticCompositionLocalOf<ThemeManager> {
    AppThemeManager(
        isDarkTheme = false,
        currentPalette = AppThemePalette.OOZOX_INFINITY,
        fontScale = com.example.ui.theme.AppFontScale.STANDARD,
        onToggleTheme = {},
        onSetDarkTheme = {},
        onSetPalette = {},
        onSetFontScale = {}
    )
}

val LocalAppTheme = LocalThemeManager

/**
 * Key constants used for storage persistence
 */
object AppThemeStorage {
    const val PREFS_NAME = "app_settings"
    const val KEY_IS_DARK_THEME = "is_dark_theme"
    const val KEY_THEME_PALETTE = "theme_palette"
    const val KEY_FONT_SCALE = "app_font_scale"
}

/**
 * AppProvider is the root application provider component.
 * It manages and persists global theme states (Light / Dark mode and theme palettes)
 * in SharedPreferences, propagates updates through [LocalAppTheme], applies [GoldChatTheme]
 * to the entire UI, and initializes spatial layout configurations.
 */
@Composable
fun AppProvider(
    viewModel: MainViewModel? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs: SharedPreferences = remember {
        context.getSharedPreferences(AppThemeStorage.PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Collect states from ViewModel if present, or fallback to internal Compose state backed by SharedPreferences
    val vmDarkTheme = viewModel?.isDarkTheme?.collectAsStateWithLifecycle()
    val vmPalette = viewModel?.selectedThemePalette?.collectAsStateWithLifecycle()
    val vmFontScale = viewModel?.appFontScale?.collectAsStateWithLifecycle()

    var internalDarkTheme by remember {
        mutableStateOf(prefs.getBoolean(AppThemeStorage.KEY_IS_DARK_THEME, false))
    }
    var internalPalette by remember {
        mutableStateOf(
            AppThemePalette.fromId(prefs.getString(AppThemeStorage.KEY_THEME_PALETTE, "OOZOX_INFINITY"))
        )
    }
    var internalFontScale by remember {
        mutableStateOf(
            com.example.ui.theme.AppFontScale.fromName(prefs.getString(AppThemeStorage.KEY_FONT_SCALE, "STANDARD"))
        )
    }

    val isDark = vmDarkTheme?.value ?: internalDarkTheme
    val palette = vmPalette?.value ?: internalPalette
    val fontScale = vmFontScale?.value ?: internalFontScale

    val themeManager: ThemeManager = remember(isDark, palette, fontScale, viewModel) {
        AppThemeManager(
            isDarkTheme = isDark,
            currentPalette = palette,
            fontScale = fontScale,
            onToggleTheme = {
                val newMode = !isDark
                prefs.edit().putBoolean(AppThemeStorage.KEY_IS_DARK_THEME, newMode).apply()
                internalDarkTheme = newMode
                viewModel?.toggleTheme(newMode)
            },
            onSetDarkTheme = { dark ->
                prefs.edit().putBoolean(AppThemeStorage.KEY_IS_DARK_THEME, dark).apply()
                internalDarkTheme = dark
                viewModel?.toggleTheme(dark)
            },
            onSetPalette = { newPalette ->
                prefs.edit().putString(AppThemeStorage.KEY_THEME_PALETTE, newPalette.id).apply()
                internalPalette = newPalette
                viewModel?.setThemePalette(newPalette)
            },
            onSetFontScale = { newScale ->
                prefs.edit().putString(AppThemeStorage.KEY_FONT_SCALE, newScale.name).apply()
                internalFontScale = newScale
                viewModel?.setAppFontScale(newScale)
            }
        )
    }

    val toastController = remember { AppToastController() }

    // Initialize network state and offline sync state
    LaunchedEffect(Unit) {
        OfflineSyncManager.initNetworkMonitoring(context)
    }

    // Periodic Database Schema Version & Integrity Validation (runs once on startup, then periodically)
    LaunchedEffect(Unit) {
        // Initial check on application launch via DatabaseVersionManager
        DatabaseVersionManager.validateSchemaAndLog(context)

        // Periodic check every 15 minutes (or 900,000 ms) while app is active
        while (isActive) {
            delay(15 * 60 * 1000L)
            DatabaseIntegrityChecker.performIntegrityCheck(context)
        }
    }

    val isOnline by OfflineSyncManager.isNetworkOnline.collectAsStateWithLifecycle()
    val pendingSyncCount by OfflineSyncManager.pendingSyncCount.collectAsStateWithLifecycle()

    CompositionLocalProvider(
        LocalThemeManager provides themeManager,
        LocalAppToast provides toastController
    ) {
        GoldChatTheme(darkTheme = isDark, palette = palette, fontScale = fontScale) {
            SpatialLayoutEngineProvider {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Global Persistent Connectivity Status Banner at the very top of the window
                        GlobalConnectivityBanner(
                            onRetryClick = {
                                OfflineSyncManager.triggerImmediateSync(context)
                            }
                        )

                        // App Content
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            content()
                        }
                    }

                    ToastNotificationHost(toastController = toastController)
                }
            }
        }
    }
}

/**
 * Global Theme Toggle Icon Button
 * Provides an animated, tactile toggle button between Light and Dark mode
 * anywhere in the application.
 */
@Composable
fun GlobalThemeToggleButton(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    backgroundColor: Color? = null
) {
    val themeController = LocalAppTheme.current
    val isDark = themeController.isDarkTheme

    val rotationAngle by animateFloatAsState(
        targetValue = if (isDark) 0f else 180f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "theme_icon_rotation"
    )

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val defaultBg = remember(isDark, surfaceVariant, primaryContainer) {
        if (isDark) {
            surfaceVariant.copy(alpha = 0.6f)
        } else {
            primaryContainer.copy(alpha = 0.5f)
        }
    }

    val defaultTint = remember(tint, isDark) {
        tint ?: if (isDark) GoldPrimary else RealGoldAmber
    }

    IconButton(
        onClick = { themeController.toggleTheme() },
        modifier = modifier
            .testTag("global_theme_toggle_button")
            .size(36.dp)
            .clip(CircleShape)
            .background(backgroundColor ?: defaultBg)
    ) {
        AnimatedContent(
            targetState = isDark,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
            },
            label = "theme_toggle_anim"
        ) { targetIsDark ->
            Icon(
                imageVector = if (targetIsDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = if (targetIsDark) "التبديل إلى الوضع الفاتح" else "التبديل إلى الوضع الداكن",
                tint = defaultTint,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = rotationAngle }
            )
        }
    }
}

/**
 * Global Theme Toggle Switch
 * Material 3 switch with integrated storage synchronization.
 */
@Composable
fun GlobalThemeToggleSwitch(
    modifier: Modifier = Modifier
) {
    val themeController = LocalAppTheme.current

    Switch(
        checked = themeController.isDarkTheme,
        onCheckedChange = { themeController.setDarkTheme(it) },
        modifier = modifier.testTag("global_theme_toggle_switch"),
        colors = SwitchDefaults.colors(
            checkedThumbColor = GoldLight,
            checkedTrackColor = GoldPrimary,
            uncheckedThumbColor = RealGoldAmber,
            uncheckedTrackColor = Color(0xFFFDE68A)
        )
    )
}

/**
 * Global Theme Toggle Card
 * Full settings card component showing active theme with single-tap dual selector.
 */
@Composable
fun GlobalThemeToggleCard(
    modifier: Modifier = Modifier
) {
    val themeController = LocalAppTheme.current
    val isDark = themeController.isDarkTheme

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("global_theme_toggle_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مظهر التطبيق ووضع الشاشة",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFFEF3C7),
                    border = BorderStroke(
                        1.dp,
                        if (isDark) GoldPrimary.copy(alpha = 0.5f) else RealGoldAmber.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = if (isDark) "الوضع الداكن" else "الوضع الفاتح ️",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) GoldLight else Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFFFFBEB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = if (isDark) GoldPrimary else RealGoldAmber,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "التبديل بين الداكن والفاتح",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isDark) "واجهة مريحة للعينين في الإضاءة المنخفضة" else "واجهة نهارية مشرقة عالية الوضوح",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                GlobalThemeToggleSwitch()
            }

            // Dual Mode Quick Select Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Dark Button
                Surface(
                    onClick = { if (!isDark) themeController.setDarkTheme(true) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(
                        width = if (isDark) 2.dp else 1.dp,
                        color = if (isDark) GoldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("app_provider_select_dark_button")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = if (isDark) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الوضع الداكن",
                            fontSize = 12.sp,
                            fontWeight = if (isDark) FontWeight.Bold else FontWeight.Medium,
                            color = if (isDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Light Button
                Surface(
                    onClick = { if (isDark) themeController.setDarkTheme(false) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (!isDark) RealGoldAmber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(
                        width = if (!isDark) 2.dp else 1.dp,
                        color = if (!isDark) RealGoldAmber else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("app_provider_select_light_button")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.LightMode,
                            contentDescription = null,
                            tint = if (!isDark) RealGoldAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الوضع الفاتح ️",
                            fontSize = 12.sp,
                            fontWeight = if (!isDark) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Global Font Scale Settings Card
 * Enables easy font enlargement for users with low vision or readability needs.
 */
@Composable
fun GlobalFontScaleCard(
    modifier: Modifier = Modifier
) {
    val themeController = LocalAppTheme.current
    val currentScale = themeController.fontScale
    val isDark = themeController.isDarkTheme
    val primaryAccent = if (isDark) GoldPrimary else MaterialTheme.colorScheme.primary

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("global_font_scale_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isDark) GoldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A+",
                        fontWeight = FontWeight.Black,
                        color = primaryAccent,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "حجم الخط ونصوص التطبيق",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تكبير النصوص لسهولة القراءة ودعم ضعاف النظر",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Live Text Preview Box
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "معاينة فورية:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryAccent
                        )
                        Text(
                            text = currentScale.subtitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "الذهب عيار 21: 3,450 ج.م | الرصيد: 125,000 ج.م",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Font Scale Selector Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.example.ui.theme.AppFontScale.entries.forEach { scale ->
                    val isSelected = scale == currentScale
                    Surface(
                        onClick = { themeController.setFontScale(scale) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) (if (isDark) GoldPrimary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) primaryAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("font_scale_option_${scale.name}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { themeController.setFontScale(scale) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = primaryAccent,
                                        unselectedColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = scale.titleAr,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) (if (isDark) GoldLight else Color(0xFF0F172A)) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = scale.descriptionAr,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = "Aa",
                                fontSize = (14 * scale.scaleFactor).sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) primaryAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}


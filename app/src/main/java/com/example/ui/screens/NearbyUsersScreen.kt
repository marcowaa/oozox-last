package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NearbyUserModel
import com.example.ui.components.OozoxAvatarImage
import com.example.ui.theme.*

enum class MerchantSortOption(val label: String, val icon: String) {
    RATING("الأعلى تقييماً ⭐", "⭐"),
    REVIEWS("الأكثر مراجعات 🏆", "🏆"),
    DISTANCE("الأقرب مسافة 📍", "📍")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyUsersScreen(
    nearbyUsers: List<NearbyUserModel>,
    onStartChat: (NearbyUserModel) -> Unit,
    onOpenMerchantStore: (String) -> Unit = {}
) {
    var isScanning by remember { mutableStateOf(true) }
    var selectedDistanceFilter by remember { mutableStateOf<Int?>(null) } // null = All, or 500, 2000, 5000
    var selectedSortOption by remember { mutableStateOf(MerchantSortOption.RATING) }
    var searchQuery by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    // Infinite radar sweep animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing)
        ),
        label = "angle"
    )

    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    val filteredList = remember(nearbyUsers, selectedDistanceFilter, selectedSortOption, searchQuery) {
        nearbyUsers
            .filter { selectedDistanceFilter == null || it.distanceMeters <= selectedDistanceFilter!! }
            .filter {
                searchQuery.isBlank() ||
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.shopName?.contains(searchQuery, ignoreCase = true) == true ||
                        it.jobTitle?.contains(searchQuery, ignoreCase = true) == true ||
                        it.skills?.contains(searchQuery, ignoreCase = true) == true
            }
            .let { list ->
                when (selectedSortOption) {
                    MerchantSortOption.RATING -> list.sortedByDescending { it.ratingScore }
                    MerchantSortOption.REVIEWS -> list.sortedByDescending { it.totalReviewsCount }
                    MerchantSortOption.DISTANCE -> list.sortedBy { it.distanceMeters }
                }
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Radar Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, GoldDark.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "رادار التجار ومقدمي الخدمات 📡",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = adaptiveGoldPrimary
                            )
                            Text(
                                text = "اكتشف الصاغة وورش التشكيل وتصفح متاجرهم وتقييماتهم",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { isScanning = !isScanning },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isScanning) Icons.Default.Radar else Icons.Default.GpsOff,
                                contentDescription = "رادار",
                                tint = adaptiveGoldPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Canvas Radar Graphic
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        val goldPrimaryColor = GoldPrimary
                        val goldAccentColor = GoldAccent

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2, size.height / 2)
                            val maxRadius = size.width / 2

                            // Draw concentric distance circles
                            drawCircle(color = goldPrimaryColor.copy(alpha = 0.15f), radius = maxRadius * 0.33f, style = Stroke(1.dp.toPx()))
                            drawCircle(color = goldPrimaryColor.copy(alpha = 0.25f), radius = maxRadius * 0.66f, style = Stroke(1.dp.toPx()))
                            drawCircle(color = goldPrimaryColor.copy(alpha = 0.4f), radius = maxRadius * 0.98f, style = Stroke(1.5.dp.toPx()))

                            // Draw crosshair axes
                            drawLine(color = goldPrimaryColor.copy(alpha = 0.2f), start = Offset(0f, center.y), end = Offset(size.width, center.y), strokeWidth = 1.dp.toPx())
                            drawLine(color = goldPrimaryColor.copy(alpha = 0.2f), start = Offset(center.x, 0f), end = Offset(center.x, size.height), strokeWidth = 1.dp.toPx())

                            // Pulse effect
                            if (isScanning) {
                                drawCircle(
                                    color = goldAccentColor.copy(alpha = 0.3f * (1f - pulseRadius)),
                                    radius = maxRadius * pulseRadius,
                                    style = Stroke(2.dp.toPx())
                                )

                                // Sweep line
                                val angleRad = Math.toRadians(sweepAngle.toDouble())
                                val lineEnd = Offset(
                                    x = center.x + (maxRadius * Math.cos(angleRad)).toFloat(),
                                    y = center.y + (maxRadius * Math.sin(angleRad)).toFloat()
                                )
                                drawLine(
                                    color = goldAccentColor.copy(alpha = 0.8f),
                                    start = center,
                                    end = lineEnd,
                                    strokeWidth = 2.dp.toPx()
                                )
                            }

                            // Center self dot
                            drawCircle(color = goldAccentColor, radius = 5.dp.toPx(), center = center)
                        }

                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Distance filter chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isAllSelected = selectedDistanceFilter == null
                        FilterChip(
                            selected = isAllSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedDistanceFilter = null
                            },
                            leadingIcon = if (isAllSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    "الكل",
                                    fontSize = 11.sp,
                                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = MaterialTheme.colorScheme.background,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.background
                            )
                        )
                        val is500Selected = selectedDistanceFilter == 500
                        FilterChip(
                            selected = is500Selected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedDistanceFilter = 500
                            },
                            leadingIcon = if (is500Selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    "أقل من 500m",
                                    fontSize = 11.sp,
                                    fontWeight = if (is500Selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = MaterialTheme.colorScheme.background,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.background
                            )
                        )
                        val is2kSelected = selectedDistanceFilter == 2000
                        FilterChip(
                            selected = is2kSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedDistanceFilter = 2000
                            },
                            leadingIcon = if (is2kSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    "أقل من 2km",
                                    fontSize = 11.sp,
                                    fontWeight = if (is2kSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = MaterialTheme.colorScheme.background,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                }
            }
        }

        // Search and Sort Control Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث بالاسم أو المهنة", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = adaptiveGoldPrimary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = adaptiveGoldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Sorting Options Row (نظام ترتيب البحث حسب التقييم والمراجعات)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ترتيب النتائج حسب:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MerchantSortOption.entries.forEach { option ->
                            val isSelected = selectedSortOption == option
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedSortOption = option
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else null,
                                label = {
                                    Text(
                                        option.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = Color.Black,
                                    selectedLeadingIconColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }
        }

        // List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "التجار ومقدمو الخدمات القريبون (${filteredList.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "🟢 GPS متصل",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Items
        items(filteredList, key = { it.id }) { user ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GoldDark.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // User Avatar
                            OozoxAvatarImage(
                                model = user.avatarUri,
                                name = user.name,
                                size = 52.dp,
                                isTrader = user.userType == "GOLD_TRADER",
                                contentDescription = user.name
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    if (user.isVerified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "موثق",
                                            tint = GoldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (!user.jobTitle.isNull_or_blank()) {
                                    Text(
                                        text = "💼 ${user.jobTitle}",
                                        fontSize = 12.sp,
                                        color = GoldPrimary,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else if (!user.shopName.isNull_or_blank()) {
                                    Text(
                                        text = "🏪 ${user.shopName}",
                                        fontSize = 12.sp,
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Rating & Distance Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Rating chip
                                    Surface(
                                        color = GoldDark.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("⭐", fontSize = 10.sp)
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "${String.format("%.1f", user.ratingScore)} (${user.totalReviewsCount})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldPrimary
                                            )
                                        }
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (user.distanceMeters < 1000) "${user.distanceMeters} متر" else "${"%.1f".format(user.distanceMeters / 1000.0)} كم",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Skills Pills preview
                    val skills = user.skills
                    if (skills != null && skills.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val skillsSample = skills.split(",").take(3).map { it.trim() }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(skillsSample, key = { it }) { skill ->
                                Surface(
                                    color = MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = "✓ $skill",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Actions Bar: Visit Store & Chat
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onOpenMerchantStore(user.id) },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, GoldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("زيارة المتجر والملف 🏪", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                        }

                        Button(
                            onClick = { onStartChat(user) },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تواصل", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()

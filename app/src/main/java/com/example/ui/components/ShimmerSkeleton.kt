package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GoldPrimary

/**
 * Modifier extension to apply a high-performance animated shimmer gradient skeleton effect.
 */
fun Modifier.shimmerEffect(
    shape: Shape = RoundedCornerShape(8.dp),
    customBaseColor: Color? = null,
    customHighlightColor: Color? = null
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translation"
    )

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val baseColor = customBaseColor ?: if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    } else {
        Color(0xFFE2E8F0)
    }

    val highlightColor = customHighlightColor ?: if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    } else {
        Color(0xFFF8FAFC)
    }

    val brush = Brush.linearGradient(
        colors = listOf(
            baseColor,
            highlightColor,
            baseColor
        ),
        start = Offset(translateAnim - 300f, translateAnim - 300f),
        end = Offset(translateAnim + 300f, translateAnim + 300f)
    )

    this
        .clip(shape)
        .background(brush)
}

/**
 * Reusable single Shimmer Placeholder element
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    width: Dp? = null,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val mod = if (width != null) {
        modifier
            .width(width)
            .height(height)
    } else {
        modifier
            .fillMaxWidth()
            .height(height)
    }
    Box(
        modifier = mod.shimmerEffect(shape = shape)
    )
}

// ----------------------------------------------------
// PROJECT SCREEN SKELETONS
// ----------------------------------------------------

@Composable
fun ProjectHeaderKpiSkeleton() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(3) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    ShimmerBox(height = 10.dp, width = 60.dp, shape = RoundedCornerShape(4.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    ShimmerBox(height = 16.dp, width = 80.dp, shape = RoundedCornerShape(4.dp))
                }
            }
        }
    }
}

@Composable
fun ProjectCardSkeleton() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Icon + Title & Type badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .shimmerEffect(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        ShimmerBox(height = 16.dp, width = 140.dp, shape = RoundedCornerShape(4.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        ShimmerBox(height = 11.dp, width = 85.dp, shape = RoundedCornerShape(4.dp))
                    }
                }
                ShimmerBox(height = 22.dp, width = 60.dp, shape = RoundedCornerShape(8.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location & CoManager row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(height = 12.dp, width = 120.dp, shape = RoundedCornerShape(4.dp))
                ShimmerBox(height = 12.dp, width = 90.dp, shape = RoundedCornerShape(4.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics Box
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(3) {
                        Column {
                            ShimmerBox(height = 10.dp, width = 45.dp, shape = RoundedCornerShape(3.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            ShimmerBox(height = 14.dp, width = 65.dp, shape = RoundedCornerShape(4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBox(height = 10.dp, width = 100.dp, shape = RoundedCornerShape(3.dp))
                ShimmerBox(height = 10.dp, width = 30.dp, shape = RoundedCornerShape(3.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            ShimmerBox(height = 6.dp, shape = RoundedCornerShape(3.dp))
        }
    }
}

@Composable
fun ProjectListSkeleton(count: Int = 3) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(count) {
            ProjectCardSkeleton()
        }
    }
}

@Composable
fun ProjectDetailSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                ShimmerBox(height = 20.dp, width = 200.dp, shape = RoundedCornerShape(6.dp))
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerBox(height = 14.dp, width = 130.dp, shape = RoundedCornerShape(4.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShimmerBox(height = 50.dp, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                    ShimmerBox(height = 50.dp, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                }
            }
        }

        // Analytics Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                ShimmerBox(height = 16.dp, width = 160.dp, shape = RoundedCornerShape(4.dp))
                Spacer(modifier = Modifier.height(12.dp))
                ShimmerBox(height = 120.dp, shape = RoundedCornerShape(14.dp))
            }
        }
    }
}

// ----------------------------------------------------
// GOLD & SILVER PRICES SCREEN SKELETONS
// ----------------------------------------------------

@Composable
fun GoldPriceTileSkeleton(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            ShimmerBox(height = 11.dp, width = 60.dp, shape = RoundedCornerShape(3.dp))
            Spacer(modifier = Modifier.height(6.dp))
            ShimmerBox(height = 18.dp, width = 95.dp, shape = RoundedCornerShape(4.dp))
        }
    }
}

@Composable
fun GoldPricesHeaderSkeleton() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .shimmerEffect(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ShimmerBox(height = 18.dp, width = 170.dp, shape = RoundedCornerShape(4.dp))
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .shimmerEffect(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoldPriceTileSkeleton(modifier = Modifier.weight(1f))
                GoldPriceTileSkeleton(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoldPriceTileSkeleton(modifier = Modifier.weight(1f))
                GoldPriceTileSkeleton(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoldPriceTileSkeleton(modifier = Modifier.weight(1f))
                GoldPriceTileSkeleton(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun GoldPurchaseItemSkeleton() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShimmerBox(height = 20.dp, width = 45.dp, shape = RoundedCornerShape(6.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    ShimmerBox(height = 16.dp, width = 110.dp, shape = RoundedCornerShape(4.dp))
                }
                ShimmerBox(height = 16.dp, width = 80.dp, shape = RoundedCornerShape(4.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(height = 12.dp, width = 160.dp, shape = RoundedCornerShape(4.dp))
                ShimmerBox(height = 16.dp, width = 90.dp, shape = RoundedCornerShape(6.dp))
            }
        }
    }
}

@Composable
fun GoldPurchasesListSkeleton(count: Int = 3) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(count) {
            GoldPurchaseItemSkeleton()
        }
    }
}

@Composable
fun GoldAlertItemSkeleton() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShimmerBox(height = 20.dp, width = 80.dp, shape = RoundedCornerShape(6.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    ShimmerBox(height = 14.dp, width = 70.dp, shape = RoundedCornerShape(4.dp))
                }
                ShimmerBox(height = 24.dp, width = 40.dp, shape = RoundedCornerShape(12.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBox(height = 12.dp, width = 100.dp, shape = RoundedCornerShape(4.dp))
                ShimmerBox(height = 12.dp, width = 90.dp, shape = RoundedCornerShape(4.dp))
            }
        }
    }
}

@Composable
fun GoldAlertsListSkeleton(count: Int = 2) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(count) {
            GoldAlertItemSkeleton()
        }
    }
}

// ----------------------------------------------------
// ADMIN DASHBOARD SKELETONS
// ----------------------------------------------------

@Composable
fun AdminMetricCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(height = 12.dp, width = 90.dp, shape = RoundedCornerShape(4.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .shimmerEffect(CircleShape)
                )
            }
            ShimmerBox(height = 22.dp, width = 110.dp, shape = RoundedCornerShape(6.dp))
        }
    }
}

@Composable
fun AdminMetricsTabSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top section header
        ShimmerBox(height = 20.dp, width = 180.dp, shape = RoundedCornerShape(6.dp))

        // Metrics grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminMetricCardSkeleton(modifier = Modifier.weight(1f))
            AdminMetricCardSkeleton(modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminMetricCardSkeleton(modifier = Modifier.weight(1f))
            AdminMetricCardSkeleton(modifier = Modifier.weight(1f))
        }

        // Quick Actions & Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerBox(height = 16.dp, width = 140.dp, shape = RoundedCornerShape(4.dp))
                ShimmerBox(height = 12.dp, width = 220.dp, shape = RoundedCornerShape(4.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShimmerBox(height = 40.dp, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp))
                    ShimmerBox(height = 40.dp, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp))
                }
            }
        }
    }
}

@Composable
fun AdminUserItemSkeleton() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .shimmerEffect(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerBox(height = 16.dp, width = 120.dp, shape = RoundedCornerShape(4.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        ShimmerBox(height = 12.dp, width = 160.dp, shape = RoundedCornerShape(4.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    ShimmerBox(height = 20.dp, width = 60.dp, shape = RoundedCornerShape(6.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    ShimmerBox(height = 30.dp, width = 150.dp, shape = RoundedCornerShape(8.dp))
                }
            }
        }
    }
}

@Composable
fun AdminUsersTabSkeleton(count: Int = 4) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search bar
        ShimmerBox(height = 50.dp, shape = RoundedCornerShape(12.dp))

        // Bulk action header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(height = 20.dp, width = 90.dp, shape = RoundedCornerShape(4.dp))
            ShimmerBox(height = 32.dp, width = 110.dp, shape = RoundedCornerShape(8.dp))
        }

        // List
        repeat(count) {
            AdminUserItemSkeleton()
        }
    }
}

@Composable
fun AdminPromoItemSkeleton() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .shimmerEffect(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShimmerBox(height = 16.dp, width = 110.dp, shape = RoundedCornerShape(4.dp))
                    ShimmerBox(height = 18.dp, width = 45.dp, shape = RoundedCornerShape(4.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerBox(height = 12.dp, width = 180.dp, shape = RoundedCornerShape(4.dp))
                Spacer(modifier = Modifier.height(4.dp))
                ShimmerBox(height = 11.dp, width = 130.dp, shape = RoundedCornerShape(4.dp))
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .shimmerEffect(CircleShape)
            )
        }
    }
}

@Composable
fun AdminPlansTabSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerBox(height = 20.dp, width = 160.dp, shape = RoundedCornerShape(6.dp))
            ShimmerBox(height = 36.dp, width = 120.dp, shape = RoundedCornerShape(10.dp))
        }

        repeat(2) {
            AdminPromoItemSkeleton()
        }
    }
}

@Composable
fun AdminRequestItemSkeleton() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(height = 16.dp, width = 130.dp, shape = RoundedCornerShape(4.dp))
                ShimmerBox(height = 20.dp, width = 70.dp, shape = RoundedCornerShape(6.dp))
            }
            ShimmerBox(height = 12.dp, width = 200.dp, shape = RoundedCornerShape(4.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShimmerBox(height = 36.dp, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                ShimmerBox(height = 36.dp, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
            }
        }
    }
}

@Composable
fun AdminRequestsTabSkeleton(count: Int = 3) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerBox(height = 32.dp, width = 80.dp, shape = RoundedCornerShape(8.dp))
            ShimmerBox(height = 32.dp, width = 90.dp, shape = RoundedCornerShape(8.dp))
            ShimmerBox(height = 32.dp, width = 80.dp, shape = RoundedCornerShape(8.dp))
        }
        repeat(count) {
            AdminRequestItemSkeleton()
        }
    }
}

@Composable
fun AdminDashboardSkeleton(selectedTab: Int = 0) {
    when (selectedTab) {
        0 -> AdminMetricsTabSkeleton()
        1 -> AdminUsersTabSkeleton()
        2 -> AdminPlansTabSkeleton()
        3 -> AdminRequestsTabSkeleton()
        else -> AdminMetricsTabSkeleton()
    }
}

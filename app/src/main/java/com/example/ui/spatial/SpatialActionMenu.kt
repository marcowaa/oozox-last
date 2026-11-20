package com.example.ui.spatial

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpatialActionMenuSheet(
    onDismiss: () -> Unit,
    onNewChatClick: () -> Unit,
    onGoldDealClick: () -> Unit,
    onNewProjectClick: () -> Unit,
    onSmartAccountantClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "الإجراءات المباشرة ⚡",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionSquare(
                    title = "محادثة جديدة",
                    icon = Icons.Default.Chat,
                    containerColor = Color(0xFFD4AF37).copy(alpha = 0.15f),
                    tint = Color(0xFFD4AF37),
                    modifier = Modifier.weight(1f),
                    onClick = { onNewChatClick(); onDismiss() }
                )

                QuickActionSquare(
                    title = "صفقة ذهب",
                    icon = Icons.Default.Diamond,
                    containerColor = Color(0xFFAA7C11).copy(alpha = 0.18f),
                    tint = Color(0xFFAA7C11),
                    modifier = Modifier.weight(1f),
                    onClick = { onGoldDealClick(); onDismiss() }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionSquare(
                    title = "مشروع جديد",
                    icon = Icons.Default.BusinessCenter,
                    containerColor = Color(0xFF3B82F6).copy(alpha = 0.15f),
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f),
                    onClick = { onNewProjectClick(); onDismiss() }
                )

                QuickActionSquare(
                    title = "المحاسب المالي",
                    icon = Icons.Default.AutoAwesome,
                    containerColor = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f),
                    onClick = { onSmartAccountantClick(); onDismiss() }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun QuickActionSquare(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

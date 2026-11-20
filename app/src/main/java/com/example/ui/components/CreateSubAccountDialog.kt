package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.HiddenProfileEntity
import com.example.ui.theme.*
import com.example.util.SecurityValidator

/**
 * Dedicated Sub-Account Creation Dialog
 * Allows creating independent sub-accounts, hidden financial vaults, and cashier profiles
 * with distinct security PINs and permissions in Luxury Minimalism design.
 */
@Composable
fun CreateSubAccountDialog(
    onDismiss: () -> Unit,
    onCreateSubAccount: (name: String, secretPin: String, companyName: String, roleTitle: String, initialCapital: Double, silentNotifications: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var roleTitle by remember { mutableStateOf("متجر ذهب فرعي") }
    var initialCapitalStr by remember { mutableStateOf("") }
    var secretPin by remember { mutableStateOf("") }
    var silentNotifications by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val roleOptions = listOf(
        "متجر ذهب وصاغة فرعي",
        "خزانة مالية سرية",
        "كاشير ونقطة بيع",
        "محاسب مالي وشريك",
        "ورشة تصنيع وسبك"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GoldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "فتح حساب فرعي جديد",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "خزانة مشفرة أو نقطة بيع مستقلة",
                                fontSize = 11.sp,
                                color = GoldPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    thickness = 0.8.dp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMessage = null },
                        label = { Text("اسم الحساب الفرعي (مثال: فرع الصاغة 2)", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("اسم الكيان التجاري / الشركة", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Role Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("نوع النشاط / الصلاحية:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            roleOptions.take(2).forEach { role ->
                                val isSelected = roleTitle == role
                                Surface(
                                    color = if (isSelected) GoldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(0.8.dp, if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { roleTitle = role }
                                ) {
                                    Text(
                                        text = role,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = initialCapitalStr,
                        onValueChange = { initialCapitalStr = it },
                        label = { Text("رأس المال الافتتاحي (ج.م)", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = secretPin,
                        onValueChange = { secretPin = it; errorMessage = null },
                        label = { Text("رمز PIN السري للدخول", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Password, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Silent notification toggle
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("كتم الإشعارات التلقائي", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("حماية وسرية تامة للحساب الفرعي", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = silentNotifications,
                                onCheckedChange = { silentNotifications = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = GoldPrimary.copy(alpha = 0.4f))
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    thickness = 0.8.dp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val sanitizedName = SecurityValidator.sanitizeInput(name)
                            val sanitizedCompany = SecurityValidator.sanitizeInput(companyName)
                            if (sanitizedName.isBlank() || secretPin.length < 3) {
                                errorMessage = "يرجى كتابة اسم الحساب ورمز سري مكون من 3 أحرف/أرقام على الأقل."
                            } else {
                                val cap = initialCapitalStr.toDoubleOrNull() ?: 0.0
                                onCreateSubAccount(
                                    sanitizedName,
                                    secretPin.trim(),
                                    sanitizedCompany.ifBlank { "مجموعة OOZOX التجارية" },
                                    roleTitle,
                                    cap,
                                    silentNotifications
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = MaterialTheme.colorScheme.background),
                        modifier = Modifier
                            .weight(1.6f)
                            .testTag("btn_create_sub_account_confirm")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إنشاء الحساب", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

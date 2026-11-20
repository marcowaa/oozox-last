package com.example.ui.components

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.combinedClickable
import com.example.data.model.ContactEntity
import com.example.data.model.UserType
import com.example.ui.theme.*
import com.example.util.GoldUtils
import com.example.util.ContactSyncEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatDialog(
    contacts: List<ContactEntity>,
    onSelectContact: (ContactEntity) -> Unit,
    onImportContacts: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isImporting by remember { mutableStateOf(false) }
    var isDailySyncOn by remember { mutableStateOf(ContactSyncEngine.isDailySyncEnabled(context)) }

    val pickContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val (pickedName, pickedPhone) = withContext(Dispatchers.IO) {
                    ContactSyncEngine.extractContactFromUri(context, uri)
                }
                if (pickedPhone.isNotBlank()) {
                    val newContact = ContactEntity(
                        id = "contact_picked_${UUID.randomUUID()}",
                        name = pickedName.ifBlank { pickedPhone },
                        phone = pickedPhone,
                        username = pickedName.lowercase().replace("\\s+".toRegex(), "_").take(24).ifBlank { "user_${pickedPhone.takeLast(4)}" },
                        type = "",
                        isVerified = false,
                        fromPhoneBook = true
                    )
                    withContext(Dispatchers.IO) {
                        com.example.data.local.AppDatabase.getInstance(context).contactDao().insertContacts(listOf(newContact))
                    }
                    onSelectContact(newContact)
                } else {
                    Toast.makeText(context, "تعذر قراءة رقم الهاتف لجهة الاتصال المختارة", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (onImportContacts != null) {
                onImportContacts()
            } else {
                isImporting = true
                coroutineScope.launch {
                    val imported = withContext(Dispatchers.IO) {
                        ContactSyncEngine.importPhoneContacts(context)
                    }
                    if (imported.isNotEmpty()) {
                        withContext(Dispatchers.IO) {
                            com.example.data.local.AppDatabase.getInstance(context).contactDao().insertContacts(imported)
                        }
                    }
                    isImporting = false
                    Toast.makeText(context, "تم استيراد وحفظ ${imported.size} جهة اتصال في حسابك بنجاح", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(context, "يلزم السماح بصلاحية جهات الاتصال لاستيرادها للحساب", Toast.LENGTH_SHORT).show()
        }
    }

    var showAddContactForm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }

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
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GoldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (showAddContactForm) Icons.Default.PersonAdd else Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = if (showAddContactForm) "إضافة جهة اتصال" else "محادثة جديدة",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (showAddContactForm) "بيانات جهة الاتصال" else "اختر جهة اتصال",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

                if (showAddContactForm) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("الاسم", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = { jobTitle = it },
                        label = { Text("الوظيفة (اختياري)", fontSize = 12.sp) },
                        placeholder = { Text("المسمى أو النشاط...", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.WorkOutline,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddContactForm = false },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("رجوع", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                if (name.isNotBlank() && phone.isNotBlank()) {
                                    val newContact = ContactEntity(
                                        id = UUID.randomUUID().toString(),
                                        name = name.trim(),
                                        phone = phone.trim(),
                                        username = name.trim().lowercase().replace("\\s+".toRegex(), "_"),
                                        type = jobTitle.trim(),
                                        isVerified = false
                                    )
                                    coroutineScope.launch(Dispatchers.IO) {
                                        com.example.data.local.AppDatabase.getInstance(context).contactDao().insertContacts(listOf(newContact))
                                        withContext(Dispatchers.Main) {
                                            onSelectContact(newContact)
                                        }
                                    }
                                }
                            },
                            enabled = name.isNotBlank() && phone.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = MaterialTheme.colorScheme.background
                            ),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("بدء المحادثة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                } else {
                    // Quick Action: Pick Contact from Device directly
                    Button(
                        onClick = {
                            try {
                                pickContactLauncher.launch(null)
                            } catch (e: Exception) {
                                Toast.makeText(context, "تعذر فتح جهات الاتصال", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PermContactCalendar, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختيار من جهات الاتصال", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (contacts.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Contacts,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "لا توجد جهات اتصال",
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(contacts) { contact ->
                                val interactionSource = remember { MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()
                                val scale by animateFloatAsState(
                                    targetValue = if (isPressed) 0.96f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                    label = "contact_item_scale"
                                )

                                Surface(
                                    onClick = { onSelectContact(contact) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
                                    interactionSource = interactionSource
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OozoxAvatarImage(
                                            model = null,
                                            name = contact.name,
                                            size = 42.dp,
                                            isTrader = contact.type == UserType.GOLD_TRADER.name,
                                            contentDescription = contact.name
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = contact.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (contact.type.isNotBlank() && contact.type != UserType.BUSINESSMAN.name) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = GoldPrimary.copy(alpha = 0.12f),
                                                        border = BorderStroke(0.5.dp, GoldPrimary.copy(alpha = 0.3f))
                                                    ) {
                                                        Text(
                                                            text = contact.type,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = GoldPrimary,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = GoldUtils.formatPhoneForDisplay(
                                                    phone = contact.phone,
                                                    username = contact.username,
                                                    name = contact.name
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (contact.type == UserType.ADMIN.name) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Daily sync toggle
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("المزامنة التلقائية اليومية", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Switch(
                                checked = isDailySyncOn,
                                onCheckedChange = {
                                    isDailySyncOn = it
                                    ContactSyncEngine.setDailySyncEnabled(context, it)
                                },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    }

                    // Import All Contacts Button
                    OutlinedButton(
                        onClick = {
                            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        },
                        enabled = !isImporting,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, OozoxTeal.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OozoxTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OozoxTeal, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جارٍ الاستيراد...", fontSize = 12.5.sp)
                        } else {
                            Icon(Icons.Default.ContactPhone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("استيراد جهات اتصال الهاتف", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                        }
                    }

                    // Manual Add Contact Button
                    Button(
                        onClick = { showAddContactForm = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary.copy(alpha = 0.15f),
                            contentColor = GoldPrimary
                        ),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إضافة جهة اتصال يدوياً", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}


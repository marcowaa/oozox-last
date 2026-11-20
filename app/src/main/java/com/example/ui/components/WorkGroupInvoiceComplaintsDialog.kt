package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.FinancialComplaintEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.WorkGroupEntity
import com.example.ui.theme.*
import com.example.util.FileUtils
import com.example.util.GoldUtils
import com.example.util.acctStr
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkGroupInvoiceComplaintsDialog(
    workGroup: WorkGroupEntity,
    allInvoices: List<InvoiceEntity>,
    complaints: List<FinancialComplaintEntity>,
    currentStaffId: String,
    currentStaffName: String,
    onSaveComplaint: (FinancialComplaintEntity) -> Unit,
    onUpdateComplaintResponse: (complaintId: String, status: String, reason: String?, proofUri: String?, proofMediaType: String?, isDeducted: Boolean) -> Unit,
    onApplyJointDeduction: (complaintId: String, primaryAmount: Double, receiverAmount: Double, receiverId: String?, notes: String?) -> Unit = { _, _, _, _, _ -> },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showCreateComplaintModal by remember { mutableStateOf(false) }
    var selectedComplaintForResponse by remember { mutableStateOf<FinancialComplaintEntity?>(null) }
    var selectedComplaintForForensic by remember { mutableStateOf<FinancialComplaintEntity?>(null) }
    var previewMediaItem by remember { mutableStateOf<MediaPreviewItem?>(null) }
    var statusFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, REJECTED, ACCEPTED

    val primaryGold = adaptiveGoldPrimary
    val greenColor = adaptiveStatusGreen
    val redColor = adaptiveStatusRed
    val orangeColor = adaptiveStatusOrange

    val groupComplaints = remember(complaints, workGroup.id, workGroup.name) {
        complaints.filter { it.groupId == workGroup.id || it.subGroupId == workGroup.name }.ifEmpty { complaints }
    }

    val filteredComplaints = remember(groupComplaints, statusFilter) {
        when (statusFilter) {
            "PENDING" -> groupComplaints.filter { it.employeeResponseStatus == "PENDING" }
            "REJECTED" -> groupComplaints.filter { it.employeeResponseStatus == "REJECTED" }
            "ACCEPTED" -> groupComplaints.filter { it.employeeResponseStatus == "ACCEPTED" }
            else -> groupComplaints
        }
    }

    val pendingCount = remember(groupComplaints) { groupComplaints.count { it.employeeResponseStatus == "PENDING" } }
    val acceptedCount = remember(groupComplaints) { groupComplaints.count { it.employeeResponseStatus == "ACCEPTED" } }
    val totalDisputedAmount = remember(groupComplaints) { groupComplaints.sumOf { it.claimAmount } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, primaryGold.copy(alpha = 0.35f))
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = redColor.copy(alpha = 0.14f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.ReportProblem,
                                                contentDescription = null,
                                                tint = redColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            acctStr("disputes_title"),
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${workGroup.name} • ${acctStr("disputes_subtitle")}",
                                            fontSize = 11.sp,
                                            color = primaryGold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = acctStr("close"))
                                }
                            },
                            actions = {
                                Button(
                                    onClick = { showCreateComplaintModal = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = redColor, contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .heightIn(min = 44.dp)
                                        .padding(end = 4.dp)
                                        .testTag("open_create_invoice_complaint_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(acctStr("new_dispute"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        // Compact Summary + Filter Strip
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = redColor.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                            Text(acctStr("total_disputed"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(GoldUtils.formatCurrency(totalDisputedAmount), fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = redColor, maxLines = 1)
                                        }
                                    }
                                    Surface(
                                        color = orangeColor.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(0.75f)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(acctStr("pending_status"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                            Text("$pendingCount", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = orangeColor)
                                        }
                                    }
                                    Surface(
                                        color = greenColor.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(0.75f)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(acctStr("deducted_status"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                            Text("$acceptedCount", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = greenColor)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "ALL" to "${acctStr("all")} (${groupComplaints.size})",
                                        "PENDING" to "${acctStr("pending_status")} ($pendingCount)",
                                        "REJECTED" to acctStr("rejected_status"),
                                        "ACCEPTED" to "${acctStr("deducted_status")} ($acceptedCount)"
                                    ).forEach { (key, label) ->
                                        FilterChip(
                                            selected = statusFilter == key,
                                            onClick = { statusFilter = key },
                                            shape = RoundedCornerShape(12.dp),
                                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = primaryGold,
                                                selectedLabelColor = adaptiveOnPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        if (filteredComplaints.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.CheckCircleOutline,
                                        contentDescription = null,
                                        tint = greenColor,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        acctStr("no_disputes"),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 28.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredComplaints, key = { it.id }) { comp ->
                                    ComplaintCardItem(
                                        complaint = comp,
                                        onOpenResponseModal = { selectedComplaintForResponse = comp },
                                        onOpenForensicModal = { selectedComplaintForForensic = comp },
                                        onPreviewProof = { uri, isVideo ->
                                            previewMediaItem = MediaPreviewItem(
                                                uri = uri,
                                                caption = "إثبات رفض الشكوى: ${comp.title}",
                                                senderName = comp.accusedEmployeeName ?: comp.submittedByStaffName,
                                                timestamp = comp.timestamp,
                                                isVideo = isVideo
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateComplaintModal) {
        val newDisputeLabel = acctStr("new_dispute")
        CreateInvoiceComplaintDialog(
            workGroup = workGroup,
            allInvoices = allInvoices,
            currentStaffId = currentStaffId,
            currentStaffName = currentStaffName,
            onDismiss = { showCreateComplaintModal = false },
            onSubmit = { newComp ->
                onSaveComplaint(newComp)
                showCreateComplaintModal = false
                Toast.makeText(context, newComp.accusedEmployeeName ?: newDisputeLabel, Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (selectedComplaintForResponse != null) {
        val viewAttachmentLabel = acctStr("view_attachment")
        val deductedStatusLabel = acctStr("deducted_status")
        val rejectedStatusLabel = acctStr("rejected_status")
        EmployeeComplaintResponseDialog(
            complaint = selectedComplaintForResponse!!,
            onDismiss = { selectedComplaintForResponse = null },
            onPreviewProof = { uri, isVideo ->
                previewMediaItem = MediaPreviewItem(
                    uri = uri,
                    caption = viewAttachmentLabel,
                    senderName = currentStaffName,
                    timestamp = System.currentTimeMillis(),
                    isVideo = isVideo
                )
            },
            onSubmitResponse = { status, reason, proofUri, proofMediaType, isDeducted ->
                onUpdateComplaintResponse(
                    selectedComplaintForResponse!!.id,
                    status,
                    reason,
                    proofUri,
                    proofMediaType,
                    isDeducted
                )
                selectedComplaintForResponse = null
                Toast.makeText(
                    context,
                    if (status == "ACCEPTED") deductedStatusLabel else rejectedStatusLabel,
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    if (selectedComplaintForForensic != null) {
        ForensicLiabilityDialog(
            complaint = selectedComplaintForForensic!!,
            onApplyJointDeduction = { cId, pAmt, rAmt, rId, notes ->
                onApplyJointDeduction(cId, pAmt, rAmt, rId, notes)
                selectedComplaintForForensic = null
            },
            onDismiss = { selectedComplaintForForensic = null }
        )
    }

    if (previewMediaItem != null) {
        MediaPreviewModal(
            item = previewMediaItem!!,
            onDismiss = { previewMediaItem = null }
        )
    }
}

@Composable
private fun ComplaintCardItem(
    complaint: FinancialComplaintEntity,
    onOpenResponseModal: () -> Unit,
    onOpenForensicModal: () -> Unit,
    onPreviewProof: (uri: String, isVideo: Boolean) -> Unit
) {
    val primaryGold = adaptiveGoldPrimary
    val greenColor = adaptiveStatusGreen
    val redColor = adaptiveStatusRed
    val orangeColor = adaptiveStatusOrange
    val blueColor = adaptiveStatusBlue

    val statusColor = when (complaint.employeeResponseStatus) {
        "ACCEPTED" -> redColor
        "REJECTED" -> greenColor
        else -> orangeColor
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, statusColor.copy(alpha = 0.65f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (complaint.employeeResponseStatus == "ACCEPTED") Icons.Default.Cancel else Icons.Default.AssignmentLate,
                        contentDescription = null,
                        tint = if (complaint.employeeResponseStatus == "ACCEPTED") redColor else primaryGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        complaint.title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when (complaint.employeeResponseStatus) {
                            "ACCEPTED" -> acctStr("deducted_status")
                            "REJECTED" -> acctStr("rejected_status")
                            else -> acctStr("pending_status")
                        },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!complaint.invoiceNumber.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = primaryGold, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "#${complaint.invoiceNumber}",
                            fontSize = 11.5.sp,
                            color = primaryGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (!complaint.mentionTag.isNullOrBlank() || !complaint.accusedEmployeeName.isNullOrBlank()) {
                    Surface(
                        color = blueColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, blueColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = complaint.mentionTag ?: "@${complaint.accusedEmployeeName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = blueColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Text(complaint.details, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    GoldUtils.formatCurrency(complaint.claimAmount),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = redColor
                )
                Text(
                    complaint.submittedByStaffName,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (complaint.employeeResponseStatus == "REJECTED") {
                Surface(
                    color = greenColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, greenColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            complaint.employeeRejectionReason ?: acctStr("rejected_status"),
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val proof = complaint.employeeRejectionProofUri
                        if (!proof.isNullOrBlank()) {
                            val isVid = complaint.employeeProofMediaType == "VIDEO" || FileUtils.isVideoUriString(proof)
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, greenColor.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPreviewProof(proof, isVid) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        if (!isVid && (proof.startsWith("content://") || proof.startsWith("file://") || proof.startsWith("/"))) {
                                            AsyncImage(
                                                model = proof,
                                                contentDescription = acctStr("view_attachment"),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        } else {
                                            Icon(
                                                imageVector = if (isVid) Icons.Default.Videocam else Icons.Default.Image,
                                                contentDescription = null,
                                                tint = greenColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = acctStr("view_attachment"),
                                            fontSize = 11.sp,
                                            color = greenColor,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(Icons.Default.OpenInFull, contentDescription = acctStr("view_attachment"), tint = greenColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            } else if (complaint.employeeResponseStatus == "ACCEPTED") {
                Surface(
                    color = redColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "${acctStr("deducted_status")} • ${GoldUtils.formatCurrency(complaint.claimAmount)}",
                        fontSize = 11.sp,
                        color = redColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            if (complaint.forensicAnalysisCompleted) {
                Surface(
                    color = redColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, redColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Balance, contentDescription = null, tint = redColor, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${acctStr("forensic_audit")}: ${complaint.forensicLiabilityDistribution ?: ""}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = redColor
                            )
                        }
                        if (!complaint.forensicAuditDetails.isNullOrBlank()) {
                            Text(complaint.forensicAuditDetails, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onOpenForensicModal,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryGold),
                border = BorderStroke(1.dp, primaryGold.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("open_forensic_liability_audit_btn")
            ) {
                Icon(Icons.Default.Balance, contentDescription = null, modifier = Modifier.size(16.dp), tint = primaryGold)
                Spacer(modifier = Modifier.width(6.dp))
                Text(acctStr("forensic_audit"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }

            if (complaint.employeeResponseStatus == "PENDING") {
                Button(
                    onClick = onOpenResponseModal,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryGold, contentColor = adaptiveOnPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(acctStr("staff_reply"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateInvoiceComplaintDialog(
    workGroup: WorkGroupEntity,
    allInvoices: List<InvoiceEntity>,
    currentStaffId: String,
    currentStaffName: String,
    onDismiss: () -> Unit,
    onSubmit: (FinancialComplaintEntity) -> Unit
) {
    var selectedInvoice by remember { mutableStateOf(allInvoices.firstOrNull()) }
    var title by remember { mutableStateOf("فرق حساب بالفاتورة") }
    var claimAmountText by remember { mutableStateOf(selectedInvoice?.totalAmount?.toInt()?.toString() ?: "") }
    var detailsText by remember { mutableStateOf("") }
    var customEmployeeName by remember { mutableStateOf("") }

    val primaryGold = adaptiveGoldPrimary
    val redColor = adaptiveStatusRed
    val blueColor = adaptiveStatusBlue

    val autoCashierName = remember(selectedInvoice, customEmployeeName) {
        if (customEmployeeName.isNotBlank()) customEmployeeName
        else selectedInvoice?.cashierName?.ifBlank { null } ?: selectedInvoice?.senderName ?: "الكاشير المعني"
    }
    val autoMentionTag = "@${autoCashierName.replace(" ", "_")}"
    val parsedAmount = claimAmountText.toDoubleOrNull() ?: 0.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 650.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(listOf(redColor.copy(alpha = 0.6f), MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)))
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                        Surface(color = redColor.copy(alpha = 0.14f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = redColor, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(acctStr("new_dispute"), fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(workGroup.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 12.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(acctStr("select_invoice"), fontSize = 12.sp, fontWeight = FontWeight.Bold)

                        if (allInvoices.isNotEmpty()) {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = "${selectedInvoice?.invoiceNumber ?: acctStr("select_invoice")} (${GoldUtils.formatCurrency(selectedInvoice?.totalAmount ?: 0.0)})",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    allInvoices.take(15).forEach { inv ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "#${inv.invoiceNumber} - ${GoldUtils.formatCurrency(inv.totalAmount)} (${inv.cashierName ?: inv.senderName})",
                                                    fontSize = 12.sp
                                                )
                                            },
                                            onClick = {
                                                selectedInvoice = inv
                                                claimAmountText = inv.totalAmount.toInt().toString()
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(acctStr("dispute_title")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = customEmployeeName,
                            onValueChange = { customEmployeeName = it },
                            label = { Text(acctStr("target_staff")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Surface(
                            color = blueColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, blueColor.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = blueColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(autoMentionTag, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = blueColor)
                            }
                        }

                        OutlinedTextField(
                            value = claimAmountText,
                            onValueChange = { claimAmountText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text(acctStr("claim_amount")) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = detailsText,
                            onValueChange = { detailsText = it },
                            label = { Text(acctStr("dispute_details")) },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(acctStr("cancel"), color = MaterialTheme.colorScheme.onSurface)
                        }
                        val defaultDisputeTitle = acctStr("new_dispute")
                        val disputesCategoryLabel = acctStr("disputes_title")
                        Button(
                            onClick = {
                                val amount = if (parsedAmount > 0.0) parsedAmount else 100.0
                                val comp = FinancialComplaintEntity(
                                    id = "comp_${System.currentTimeMillis()}_${(100..999).random()}",
                                    groupId = workGroup.id,
                                    subGroupId = workGroup.name,
                                    profileId = workGroup.id,
                                    submittedByStaffId = currentStaffId,
                                    submittedByStaffName = currentStaffName,
                                    submittedByStaffRole = "مسؤول",
                                    title = title.trim().ifBlank { defaultDisputeTitle },
                                    details = detailsText.trim().ifBlank { "#${selectedInvoice?.invoiceNumber ?: ""}" },
                                    targetDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                                    impactType = "DEDUCTION",
                                    amount = amount,
                                    category = disputesCategoryLabel,
                                    status = "PENDING_REVIEW",
                                    accusedEmployeeId = selectedInvoice?.senderId ?: "staff",
                                    accusedEmployeeName = autoCashierName,
                                    invoiceId = selectedInvoice?.id,
                                    invoiceNumber = selectedInvoice?.invoiceNumber,
                                    mentionTag = autoMentionTag,
                                    claimAmount = amount,
                                    employeeResponseStatus = "PENDING",
                                    timestamp = System.currentTimeMillis()
                                )
                                onSubmit(comp)
                            },
                            enabled = title.isNotBlank() && detailsText.isNotBlank() && parsedAmount > 0.0,
                            colors = ButtonDefaults.buttonColors(containerColor = redColor, contentColor = Color.White),
                            modifier = Modifier
                                .weight(1.4f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(acctStr("submit_dispute"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeComplaintResponseDialog(
    complaint: FinancialComplaintEntity,
    onDismiss: () -> Unit,
    onPreviewProof: (uri: String, isVideo: Boolean) -> Unit,
    onSubmitResponse: (status: String, reason: String?, proofUri: String?, proofMediaType: String?, isDeducted: Boolean) -> Unit
) {
    val context = LocalContext.current
    var responseChoice by remember { mutableStateOf<String?>(null) } // "ACCEPT" or "REJECT"
    var rejectionReason by remember { mutableStateOf("") }
    var proofUri by remember { mutableStateOf<String?>(null) }
    var proofMediaType by remember { mutableStateOf("IMAGE") } // "IMAGE" or "VIDEO"

    val primaryGold = adaptiveGoldPrimary
    val greenColor = adaptiveStatusGreen
    val redColor = adaptiveStatusRed

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = FileUtils.copyMediaUriToInternalStorage(context, uri, isVideo = false, targetFolder = "complaint_proofs") ?: uri.toString()
            proofUri = savedPath
            proofMediaType = "IMAGE"
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = FileUtils.copyMediaUriToInternalStorage(context, uri, isVideo = true, targetFolder = "complaint_proofs") ?: uri.toString()
            proofUri = savedPath
            proofMediaType = "VIDEO"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 650.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(listOf(primaryGold.copy(alpha = 0.6f), MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)))
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                        Surface(color = primaryGold.copy(alpha = 0.15f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = primaryGold, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(acctStr("staff_reply"), fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
                            Text(complaint.title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 12.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(complaint.title, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                Text(
                                    "#${complaint.invoiceNumber ?: "-"} • ${GoldUtils.formatCurrency(complaint.claimAmount)}",
                                    fontSize = 11.5.sp,
                                    color = redColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(complaint.details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { responseChoice = "ACCEPT" },
                                border = BorderStroke(1.5.dp, if (responseChoice == "ACCEPT") redColor else MaterialTheme.colorScheme.outlineVariant),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (responseChoice == "ACCEPT") redColor.copy(alpha = 0.12f) else Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Text(
                                    acctStr("accept_deduction"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (responseChoice == "ACCEPT") redColor else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            OutlinedButton(
                                onClick = { responseChoice = "REJECT" },
                                border = BorderStroke(1.5.dp, if (responseChoice == "REJECT") greenColor else MaterialTheme.colorScheme.outlineVariant),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (responseChoice == "REJECT") greenColor.copy(alpha = 0.12f) else Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Text(
                                    acctStr("reject_with_proof"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (responseChoice == "REJECT") greenColor else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (responseChoice == "ACCEPT") {
                            Surface(
                                color = redColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "${acctStr("accept_deduction")} • ${GoldUtils.formatCurrency(complaint.claimAmount)}",
                                    fontSize = 12.sp,
                                    color = redColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        if (responseChoice == "REJECT") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = rejectionReason,
                                    onValueChange = { rejectionReason = it },
                                    label = { Text(acctStr("staff_reply_reason")) },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            imagePickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp), tint = primaryGold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(acctStr("attach_proof_image"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            videoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp), tint = primaryGold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(acctStr("attach_proof_video"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (proofUri != null) {
                                    Surface(
                                        color = greenColor.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, greenColor.copy(alpha = 0.45f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onPreviewProof(proofUri!!, proofMediaType == "VIDEO") }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                if (proofMediaType != "VIDEO") {
                                                    AsyncImage(
                                                        model = proofUri,
                                                        contentDescription = acctStr("view_attachment"),
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                    )
                                                } else {
                                                    Icon(Icons.Default.Videocam, contentDescription = null, tint = greenColor, modifier = Modifier.size(22.dp))
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = acctStr("view_attachment"),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = greenColor
                                                )
                                            }
                                            IconButton(
                                                onClick = { proofUri = null },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = acctStr("delete"), tint = redColor)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val canSubmit = when (responseChoice) {
                        "ACCEPT" -> true
                        "REJECT" -> rejectionReason.isNotBlank() && proofUri != null
                        else -> false
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(acctStr("cancel"), color = MaterialTheme.colorScheme.onSurface)
                        }
                        val acceptedStatusLabel = acctStr("deducted_status")
                        Button(
                            onClick = {
                                if (responseChoice == "ACCEPT") {
                                    onSubmitResponse("ACCEPTED", acceptedStatusLabel, null, null, true)
                                } else if (responseChoice == "REJECT") {
                                    onSubmitResponse("REJECTED", rejectionReason.trim(), proofUri, proofMediaType, false)
                                }
                            },
                            enabled = canSubmit,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (responseChoice == "ACCEPT") redColor else greenColor,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1.4f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = acctStr("confirm"),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserEntity
import com.example.ui.theme.StatusGreen
import com.example.util.AppLanguage
import com.example.util.ReliabilityRecord
import com.example.util.ReportExporter

private fun parseCvHex(hex: String, fallback: Color = Color(0xFF0F172A)): Color =
    try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }

/**
 * Miniature visual CV layout thumbnail representing each template's authentic page structure
 * (single-column ATS, top-band, sidebar split, prestige double frame, 2-col compact, ledger, etc.).
 */
@Composable
fun CvTemplateMiniThumbnail(
    template: CvTemplateSpec,
    userName: String,
    jobTitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val headerBg = remember(template) { parseCvHex(template.headerBgHex, Color.White) }
    val accent = remember(template) { parseCvHex(template.accentHex, Color(0xFF0F172A)) }
    val secondary = remember(template) { parseCvHex(template.secondaryHex, Color(0xFF475569)) }
    val shortName = remember(userName) { userName.ifBlank { "أحمد محمد" } }
    val shortRole = remember(jobTitle) { jobTitle.ifBlank { "عضو معتمد" } }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0)
        ),
        modifier = modifier.aspectRatio(0.76f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (template.layoutStyle) {
                CvVisualLayout.SIDEBAR_SPLIT -> {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Colored Sidebar
                        Column(
                            modifier = Modifier
                                .weight(0.36f)
                                .fillMaxHeight()
                                .background(headerBg)
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                                    .border(1.dp, secondary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = shortName.take(1),
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(secondary)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            repeat(4) { idx ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(if (idx % 2 == 0) 0.9f else 0.7f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color.White.copy(alpha = 0.55f))
                                )
                            }
                        }
                        // Main Canvas
                        Column(
                            modifier = Modifier
                                .weight(0.64f)
                                .fillMaxHeight()
                                .padding(7.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = shortName,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = shortRole,
                                fontSize = 6.sp,
                                color = accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.5.dp)
                                    .background(accent.copy(alpha = 0.7f))
                            )
                            MiniSectionLines(accent = accent, count = 2)
                            MiniSectionLines(accent = accent, count = 3)
                        }
                    }
                }

                CvVisualLayout.EXECUTIVE_BANNER,
                CvVisualLayout.CORPORATE_SPLIT_HEADER,
                CvVisualLayout.PRESTIGE_DOUBLE_FRAME,
                CvVisualLayout.COMMERCE_SHOWCASE -> {
                    val outerModifier = if (template.layoutStyle == CvVisualLayout.PRESTIGE_DOUBLE_FRAME) {
                        Modifier
                            .fillMaxSize()
                            .padding(3.dp)
                            .border(1.dp, accent.copy(alpha = 0.75f), RoundedCornerShape(7.dp))
                    } else {
                        Modifier.fillMaxSize()
                    }
                    Column(modifier = outerModifier) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(headerBg)
                                .padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (template.layoutStyle == CvVisualLayout.CORPORATE_SPLIT_HEADER) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = shortName,
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(16.dp)
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(secondary)
                                    )
                                }
                            } else {
                                Text(
                                    text = shortName,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = shortRole,
                                    fontSize = 5.5.sp,
                                    color = secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (template.layoutStyle == CvVisualLayout.COMMERCE_SHOWCASE) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(accent)
                                            .padding(horizontal = 3.dp, vertical = 1.dp)
                                    ) {
                                        Text("98%", fontSize = 5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        // Accent divider bar (double for Prestige)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .background(accent)
                        )
                        if (template.layoutStyle == CvVisualLayout.PRESTIGE_DOUBLE_FRAME) {
                            Spacer(modifier = Modifier.height(1.5.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(secondary)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            MiniSectionLines(accent = accent, count = 2)
                            MiniSectionLines(accent = accent, count = 3)
                            MiniSkillDotsRow(accent = accent)
                        }
                    }
                }

                CvVisualLayout.ATS_TOP_BAND,
                CvVisualLayout.ACADEMIC_FORMAL -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(headerBg)
                                .padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (template.layoutStyle == CvVisualLayout.ACADEMIC_FORMAL) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(accent)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Text(
                                text = shortName,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = shortRole,
                                fontSize = 5.5.sp,
                                color = Color(0xFF475569),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.5.dp)
                                .background(accent)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            MiniSectionLines(accent = accent, count = 2)
                            MiniSectionLines(accent = accent, count = 3)
                            MiniSectionLines(accent = secondary, count = 2)
                        }
                    }
                }

                CvVisualLayout.COMPACT_TWO_COL -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(7.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(headerBg)
                                .padding(5.dp)
                        ) {
                            Text(
                                text = shortName,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = shortRole,
                                fontSize = 5.5.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        MiniSectionLines(accent = accent, count = 2)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                MiniSectionLines(accent = accent, count = 3)
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                MiniSectionLines(accent = secondary, count = 3)
                            }
                        }
                    }
                }

                CvVisualLayout.ATS_AUDIT_LEDGER -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = shortName,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = shortRole,
                            fontSize = 5.5.sp,
                            color = Color(0xFF475569),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(accent))
                        // Bordered Ledger Mini Table
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.7.dp, Color(0xFFCBD5E1), RoundedCornerShape(3.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F5F9))
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(Modifier.width(16.dp).height(2.5.dp).background(accent))
                                Box(Modifier.width(12.dp).height(2.5.dp).background(secondary))
                                Box(Modifier.width(12.dp).height(2.5.dp).background(accent))
                            }
                            repeat(2) {
                                Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFE2E8F0)))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(Modifier.width(20.dp).height(2.dp).background(Color(0xFF94A3B8)))
                                    Box(Modifier.width(10.dp).height(2.dp).background(secondary.copy(alpha = 0.7f)))
                                    Box(Modifier.width(10.dp).height(2.dp).background(Color(0xFF94A3B8)))
                                }
                            }
                        }
                        MiniSectionLines(accent = accent, count = 2)
                    }
                }

                CvVisualLayout.ATS_TECH_ACCENT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(accent)
                            )
                            Column {
                                Text(
                                    text = shortName,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = shortRole,
                                    fontSize = 5.5.sp,
                                    color = accent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        MiniSkillDotsRow(accent = accent)
                        MiniSectionLines(accent = accent, count = 2, leftAccentBar = true)
                        MiniSectionLines(accent = accent, count = 2, leftAccentBar = true)
                    }
                }

                CvVisualLayout.MINIMAL_EDITORIAL -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = shortName,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = shortRole,
                                fontSize = 5.5.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(modifier = Modifier.width(22.dp).height(1.5.dp).background(accent))
                        MiniSectionLines(accent = Color(0xFF94A3B8), count = 2)
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFE2E8F0)))
                        MiniSectionLines(accent = Color(0xFF94A3B8), count = 3)
                    }
                }

                CvVisualLayout.ATS_SINGLE_CLASSIC -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = shortName,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = shortRole,
                            fontSize = 5.5.sp,
                            color = secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(modifier = Modifier.fillMaxWidth().height(1.2.dp).background(accent))
                        MiniSectionLines(accent = accent, count = 2)
                        MiniSectionLines(accent = accent, count = 3)
                        MiniSectionLines(accent = accent, count = 2)
                    }
                }
            }

            // Corner Badges: ATS Pill & Selected Check
            if (template.isAtsFriendly) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = StatusGreen.copy(alpha = 0.14f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                ) {
                    Text(
                        text = "ATS",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusGreen,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniSectionLines(
    accent: Color,
    count: Int,
    leftAccentBar: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.5.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (leftAccentBar) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(5.dp)
                        .background(accent)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(accent.copy(alpha = 0.85f))
            )
        }
        repeat(count) { idx ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (idx == count - 1) 0.68f else 0.94f)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color(0xFFCBD5E1))
            )
        }
    }
}

@Composable
private fun MiniSkillDotsRow(accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent.copy(alpha = 0.16f))
                    .border(0.5.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(3.dp))
            )
        }
    }
}

/**
 * Live A4 CV sheet preview populated dynamically with the client's actual profile & reliability data.
 * Renders cleanly with white paper aesthetics so the user sees precisely what their CV & PDF look like.
 */
@Composable
fun CvLivePreviewSheet(
    user: UserEntity,
    template: CvTemplateSpec,
    reliabilityRecords: List<ReliabilityRecord>,
    trustScore: Int,
    currentLanguage: AppLanguage,
    sectionOrder: List<String>? = null,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isArabic = currentLanguage == AppLanguage.ARABIC
    val headerBg = remember(template) { parseCvHex(template.headerBgHex, Color.White) }
    val accent = remember(template) { parseCvHex(template.accentHex, Color(0xFF0F172A)) }
    val secondary = remember(template) { parseCvHex(template.secondaryHex, Color(0xFF475569)) }

    val orderedSections = remember(sectionOrder, user.cvSectionOrder) {
        val raw = sectionOrder?.takeIf { it.isNotEmpty() }
            ?: user.cvSectionOrder?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
        if (!raw.isNullOrEmpty()) raw else listOf("BIO", "EXPERIENCE", "RELIABILITY", "SKILLS", "EDUCATION", "STORE")
    }

    val displayedSections = remember(orderedSections, compact) {
        if (compact) orderedSections.take(4) else orderedSections
    }

    val roleText = user.jobTitle?.takeIf { it.isNotBlank() }
        ?: if (isArabic) "عضو معتمد" else "Certified Professional"
    val bioText = user.merchantBio?.takeIf { it.isNotBlank() }
        ?: if (isArabic) "محترف متميز يمتلك خبرة عملية وسجلاً موثقاً من الالتزام والجودة." else "Distinguished professional dedicated to performance excellence and verified quality."
    val skillsList = remember(user.skills) {
        user.skills?.split(",", "،", "\n")?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()
    }
    val storeText = user.traderShopName?.takeIf { it.isNotBlank() }
    val educationText = user.education?.takeIf { it.isNotBlank() }
    val latestRel = reliabilityRecords.firstOrNull()

    val outerPaperShape = RoundedCornerShape(12.dp)
    val inkPrimary = Color(0xFF0F172A)
    val inkSecondary = Color(0xFF475569)
    val inkMuted = Color(0xFF64748B)

    val paperBorder = if (template.layoutStyle == CvVisualLayout.PRESTIGE_DOUBLE_FRAME) {
        BorderStroke(1.5.dp, accent.copy(alpha = 0.85f))
    } else {
        BorderStroke(1.dp, Color(0xFFCBD5E1))
    }

    CompositionLocalProvider(LocalLayoutDirection provides currentLanguage.direction) {
        Surface(
            shape = outerPaperShape,
            color = Color.White,
            shadowElevation = 3.dp,
            border = paperBorder,
            modifier = modifier.fillMaxWidth()
        ) {
            val innerFrameModifier = if (template.layoutStyle == CvVisualLayout.PRESTIGE_DOUBLE_FRAME) {
                Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .border(0.8.dp, secondary.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            } else {
                Modifier.fillMaxWidth()
            }

            Column(modifier = innerFrameModifier) {
                if (template.layoutStyle == CvVisualLayout.SIDEBAR_SPLIT) {
                    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        // Left/Start Sidebar
                        Column(
                            modifier = Modifier
                                .weight(0.36f)
                                .fillMaxHeight()
                                .background(headerBg)
                                .padding(if (compact) 10.dp else 14.dp),
                            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (compact) 36.dp else 48.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                                    .border(1.5.dp, secondary, CircleShape)
                                    .align(Alignment.CenterHorizontally),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.name.take(1).ifBlank { "O" },
                                    color = Color.White,
                                    fontSize = if (compact) 14.sp else 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.14f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isArabic) "ثقة $trustScore%" else "Trust $trustScore%",
                                    color = secondary,
                                    fontSize = if (compact) 9.sp else 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = if (isArabic) "التواصل" else "CONTACT",
                                    color = secondary,
                                    fontSize = if (compact) 8.sp else 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = user.phone.ifBlank { "@${user.username}" },
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = if (compact) 8.sp else 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "@${user.username}",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = if (compact) 8.sp else 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (skillsList.isNotEmpty() && "SKILLS" in displayedSections) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = if (isArabic) "المهارات" else "SKILLS",
                                        color = secondary,
                                        fontSize = if (compact) 8.sp else 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    skillsList.take(if (compact) 3 else 6).forEach { skill ->
                                        Text(
                                            text = "• $skill",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = if (compact) 8.sp else 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Main Content Column
                        Column(
                            modifier = Modifier
                                .weight(0.64f)
                                .padding(if (compact) 10.dp else 16.dp),
                            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = user.name,
                                        color = inkPrimary,
                                        fontSize = if (compact) 13.sp else 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (user.isVerified || user.verificationStatus == "APPROVED") {
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
                                    }
                                }
                                Text(
                                    text = roleText,
                                    color = accent,
                                    fontSize = if (compact) 10.sp else 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(accent)
                                )
                            }

                            displayedSections.filter { it != "SKILLS" }.forEach { secKey ->
                                PreviewCvSectionBlock(
                                    secKey = secKey,
                                    template = template,
                                    accent = accent,
                                    inkPrimary = inkPrimary,
                                    inkSecondary = inkSecondary,
                                    isArabic = isArabic,
                                    compact = compact,
                                    bioText = bioText,
                                    roleText = roleText,
                                    experienceYears = user.experienceYears,
                                    skillsList = skillsList,
                                    educationText = educationText,
                                    storeText = storeText,
                                    latestRel = latestRel,
                                    reliabilityRecords = reliabilityRecords,
                                    trustScore = trustScore
                                )
                            }
                        }
                    }
                } else {
                    // Header Area tailored by layoutStyle
                    val isDarkBanner = template.layoutStyle in listOf(
                        CvVisualLayout.EXECUTIVE_BANNER,
                        CvVisualLayout.CORPORATE_SPLIT_HEADER,
                        CvVisualLayout.PRESTIGE_DOUBLE_FRAME,
                        CvVisualLayout.COMMERCE_SHOWCASE
                    )
                    val isSoftBand = template.layoutStyle in listOf(
                        CvVisualLayout.ATS_TOP_BAND,
                        CvVisualLayout.ACADEMIC_FORMAL,
                        CvVisualLayout.COMPACT_TWO_COL
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDarkBanner || isSoftBand) headerBg else Color.White)
                            .padding(
                                horizontal = if (compact) 12.dp else 18.dp,
                                vertical = if (compact) 10.dp else 15.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (template.layoutStyle == CvVisualLayout.ACADEMIC_FORMAL) {
                            Box(modifier = Modifier.fillMaxWidth().height(2.5.dp).background(accent))
                            Spacer(modifier = Modifier.height(2.dp))
                        }

                        if (template.layoutStyle == CvVisualLayout.CORPORATE_SPLIT_HEADER) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.name,
                                        color = Color.White,
                                        fontSize = if (compact) 13.sp else 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = roleText,
                                        color = secondary,
                                        fontSize = if (compact) 10.sp else 12.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (user.phone.isNotBlank()) {
                                        Text(user.phone, color = Color.White.copy(alpha = 0.85f), fontSize = if (compact) 8.sp else 10.sp)
                                    }
                                    Text("@${user.username}", color = Color.White.copy(alpha = 0.8f), fontSize = if (compact) 8.sp else 10.sp)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (template.layoutStyle == CvVisualLayout.ATS_TECH_ACCENT) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(if (compact) 26.dp else 34.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(accent)
                                        )
                                    }
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = user.name,
                                                color = if (isDarkBanner) Color.White else accent,
                                                fontSize = if (compact) 13.sp else 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (user.isVerified || user.verificationStatus == "APPROVED") {
                                                Icon(
                                                    Icons.Default.Verified,
                                                    contentDescription = null,
                                                    tint = if (isDarkBanner) secondary else accent,
                                                    modifier = Modifier.size(if (compact) 12.dp else 15.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = roleText,
                                            color = if (isDarkBanner) secondary else inkSecondary,
                                            fontSize = if (compact) 10.sp else 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                if (template.layoutStyle == CvVisualLayout.COMMERCE_SHOWCASE) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = accent
                                    ) {
                                        Text(
                                            text = if (isArabic) "$trustScore% موثوق" else "$trustScore% Verified",
                                            color = Color.White,
                                            fontSize = if (compact) 8.5.sp else 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            val contactLine = buildString {
                                if (user.phone.isNotBlank()) append("${user.phone}  |  ")
                                append("@${user.username}  |  oozox.com/u/${user.username}")
                            }
                            Text(
                                text = contactLine,
                                color = if (isDarkBanner) Color.White.copy(alpha = 0.78f) else inkMuted,
                                fontSize = if (compact) 8.5.sp else 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Header accent rule
                    if (template.layoutStyle == CvVisualLayout.MINIMAL_EDITORIAL) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = if (compact) 12.dp else 18.dp)
                                .width(42.dp)
                                .height(2.dp)
                                .background(accent)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isDarkBanner) 3.dp else 1.8.dp)
                                .background(accent)
                        )
                        if (template.layoutStyle == CvVisualLayout.PRESTIGE_DOUBLE_FRAME) {
                            Spacer(modifier = Modifier.height(1.5.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(secondary)
                            )
                        }
                    }

                    // Body Sections
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = if (compact) 12.dp else 18.dp,
                                vertical = if (compact) 10.dp else 14.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
                    ) {
                        if (template.layoutStyle == CvVisualLayout.COMPACT_TWO_COL) {
                            // Top sections full width, then 2-column grid for Skills + Education/Store
                            val topKeys = displayedSections.filter { it !in listOf("SKILLS", "EDUCATION", "STORE") }
                            val leftColKeys = displayedSections.filter { it == "SKILLS" }
                            val rightColKeys = displayedSections.filter { it == "EDUCATION" || it == "STORE" }

                            topKeys.forEach { secKey ->
                                PreviewCvSectionBlock(
                                    secKey = secKey,
                                    template = template,
                                    accent = accent,
                                    inkPrimary = inkPrimary,
                                    inkSecondary = inkSecondary,
                                    isArabic = isArabic,
                                    compact = compact,
                                    bioText = bioText,
                                    roleText = roleText,
                                    experienceYears = user.experienceYears,
                                    skillsList = skillsList,
                                    educationText = educationText,
                                    storeText = storeText,
                                    latestRel = latestRel,
                                    reliabilityRecords = reliabilityRecords,
                                    trustScore = trustScore
                                )
                            }

                            if (leftColKeys.isNotEmpty() || rightColKeys.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        leftColKeys.forEach { secKey ->
                                            PreviewCvSectionBlock(
                                                secKey = secKey,
                                                template = template,
                                                accent = accent,
                                                inkPrimary = inkPrimary,
                                                inkSecondary = inkSecondary,
                                                isArabic = isArabic,
                                                compact = compact,
                                                bioText = bioText,
                                                roleText = roleText,
                                                experienceYears = user.experienceYears,
                                                skillsList = skillsList,
                                                educationText = educationText,
                                                storeText = storeText,
                                                latestRel = latestRel,
                                                reliabilityRecords = reliabilityRecords,
                                                trustScore = trustScore
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rightColKeys.forEach { secKey ->
                                            PreviewCvSectionBlock(
                                                secKey = secKey,
                                                template = template,
                                                accent = accent,
                                                inkPrimary = inkPrimary,
                                                inkSecondary = inkSecondary,
                                                isArabic = isArabic,
                                                compact = compact,
                                                bioText = bioText,
                                                roleText = roleText,
                                                experienceYears = user.experienceYears,
                                                skillsList = skillsList,
                                                educationText = educationText,
                                                storeText = storeText,
                                                latestRel = latestRel,
                                                reliabilityRecords = reliabilityRecords,
                                                trustScore = trustScore
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            displayedSections.forEach { secKey ->
                                PreviewCvSectionBlock(
                                    secKey = secKey,
                                    template = template,
                                    accent = accent,
                                    inkPrimary = inkPrimary,
                                    inkSecondary = inkSecondary,
                                    isArabic = isArabic,
                                    compact = compact,
                                    bioText = bioText,
                                    roleText = roleText,
                                    experienceYears = user.experienceYears,
                                    skillsList = skillsList,
                                    educationText = educationText,
                                    storeText = storeText,
                                    latestRel = latestRel,
                                    reliabilityRecords = reliabilityRecords,
                                    trustScore = trustScore
                                )
                            }
                        }
                    }
                }

                // Digital Verification Paper Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isArabic) "موثق رقمياً عبر منصة OOZOX • ${template.nameAr}" else "Digitally Verified via OOZOX • ${template.nameEn}",
                        color = inkMuted,
                        fontSize = if (compact) 8.sp else 9.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewCvSectionBlock(
    secKey: String,
    template: CvTemplateSpec,
    accent: Color,
    inkPrimary: Color,
    inkSecondary: Color,
    isArabic: Boolean,
    compact: Boolean,
    bioText: String,
    roleText: String,
    experienceYears: Int,
    skillsList: List<String>,
    educationText: String?,
    storeText: String?,
    latestRel: ReliabilityRecord?,
    reliabilityRecords: List<ReliabilityRecord>,
    trustScore: Int
) {
    val titleFontSize = if (compact) 10.sp else 12.sp
    val bodyFontSize = if (compact) 9.sp else 11.sp

    when (secKey) {
        "BIO" -> {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                PreviewSectionHeader(
                    title = if (isArabic) "النبذة المهنية" else "PROFESSIONAL SUMMARY",
                    template = template,
                    accent = accent,
                    fontSize = titleFontSize
                )
                Text(
                    text = bioText,
                    color = inkSecondary,
                    fontSize = bodyFontSize,
                    lineHeight = if (compact) 13.sp else 16.sp,
                    maxLines = if (compact) 2 else 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        "EXPERIENCE" -> {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                PreviewSectionHeader(
                    title = if (isArabic) "الخبرة والمسمى الوظيفي" else "EXPERIENCE & ROLE",
                    template = template,
                    accent = accent,
                    fontSize = titleFontSize
                )
                Text(
                    text = if (isArabic) "• $roleText — $experienceYears سنوات خبرة" else "• $roleText — $experienceYears yrs experience",
                    color = inkPrimary,
                    fontSize = bodyFontSize,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        "RELIABILITY" -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PreviewSectionHeader(
                    title = if (isArabic) "سجل الموثوقية والالتزام" else "VERIFIED RELIABILITY",
                    template = template,
                    accent = accent,
                    fontSize = titleFontSize
                )
                if (template.layoutStyle == CvVisualLayout.ATS_AUDIT_LEDGER) {
                    // Structured bordered KPI ledger table
                    val rowsToShow = if (reliabilityRecords.isNotEmpty()) {
                        reliabilityRecords.take(if (compact) 2 else 4)
                    } else {
                        listOf(
                            ReliabilityRecord(
                                period = if (isArabic) "السجل العام" else "Overall",
                                presenceRate = trustScore,
                                locationCompliance = 100,
                                punctuality = trustScore,
                                overallRating = if (isArabic) "موثق" else "Verified"
                            )
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.8.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (isArabic) "الفترة" else "Period", fontSize = bodyFontSize, fontWeight = FontWeight.Bold, color = inkPrimary, modifier = Modifier.weight(1.3f))
                            Text(if (isArabic) "الحضور" else "Presence", fontSize = bodyFontSize, fontWeight = FontWeight.Bold, color = inkPrimary, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                            Text(if (isArabic) "الالتزام" else "Punctuality", fontSize = bodyFontSize, fontWeight = FontWeight.Bold, color = inkPrimary, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                        }
                        rowsToShow.forEach { rec ->
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(rec.period, fontSize = bodyFontSize, color = inkSecondary, modifier = Modifier.weight(1.3f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${rec.presenceRate}%", fontSize = bodyFontSize, color = StatusGreen, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                                Text("${rec.punctuality}%", fontSize = bodyFontSize, color = inkPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    val relLine = latestRel?.let {
                        if (isArabic) "درجة الثقة $trustScore% • الحضور ${it.presenceRate}% • الالتزام ${it.punctuality}%"
                        else "Trust $trustScore% • Presence ${it.presenceRate}% • Punctuality ${it.punctuality}%"
                    } ?: if (isArabic) "درجة الثقة الفعلية الموثقة: $trustScore%" else "Verified Trust Score: $trustScore%"

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accent.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = relLine,
                            color = inkPrimary,
                            fontSize = bodyFontSize,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        "SKILLS" -> {
            if (skillsList.isNotEmpty() || !compact) {
                val items = if (skillsList.isNotEmpty()) skillsList.take(if (compact) 4 else 8)
                else listOf(if (isArabic) "إدارة المشاريع" else "Project Management", if (isArabic) "التفاوض" else "Negotiation")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PreviewSectionHeader(
                        title = if (isArabic) "المهارات والتخصصات" else "SKILLS",
                        template = template,
                        accent = accent,
                        fontSize = titleFontSize
                    )
                    if (template.layoutStyle == CvVisualLayout.ATS_TECH_ACCENT || template.layoutStyle == CvVisualLayout.COMMERCE_SHOWCASE) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            items.forEach { skill ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = accent.copy(alpha = 0.1f),
                                    border = BorderStroke(0.6.dp, accent.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = skill,
                                        color = accent,
                                        fontSize = if (compact) 8.5.sp else 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = items.joinToString("  •  "),
                            color = inkSecondary,
                            fontSize = bodyFontSize,
                            maxLines = if (compact) 2 else 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        "EDUCATION" -> {
            if (educationText != null) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    PreviewSectionHeader(
                        title = if (isArabic) "المؤهلات العلمية" else "EDUCATION",
                        template = template,
                        accent = accent,
                        fontSize = titleFontSize
                    )
                    Text(
                        text = "• $educationText",
                        color = inkSecondary,
                        fontSize = bodyFontSize,
                        maxLines = if (compact) 2 else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        "STORE" -> {
            if (storeText != null) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    PreviewSectionHeader(
                        title = if (isArabic) "المتجر والنشاط التجاري" else "BUSINESS & STORE",
                        template = template,
                        accent = accent,
                        fontSize = titleFontSize
                    )
                    Text(
                        text = "• $storeText",
                        color = inkPrimary,
                        fontSize = bodyFontSize,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewSectionHeader(
    title: String,
    template: CvTemplateSpec,
    accent: Color,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (template.layoutStyle == CvVisualLayout.ATS_TECH_ACCENT) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )
        }
        Text(
            text = title,
            color = accent,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
        if (template.layoutStyle != CvVisualLayout.ATS_TECH_ACCENT && template.layoutStyle != CvVisualLayout.MINIMAL_EDITORIAL) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(accent.copy(alpha = 0.25f))
            )
        }
    }
}

/**
 * Full-screen/Modal Interactive CV Preview Dialog allowing the user to compare all 12 visual
 * templates live with their actual data and export to PDF.
 */
@Composable
fun FullCvPreviewDialog(
    user: UserEntity,
    reliabilityRecords: List<ReliabilityRecord>,
    trustScore: Int,
    currentLanguage: AppLanguage,
    selectedTemplate: CvTemplateSpec,
    onSelectTemplate: (CvTemplateSpec) -> Unit,
    sectionOrder: List<String>? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isArabic = currentLanguage == AppLanguage.ARABIC

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides currentLanguage.direction) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.92f)
                    .widthIn(max = 620.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "معاينة السيرة الذاتية" else "CV Live Preview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (selectedTemplate.isAtsFriendly) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = StatusGreen.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = "ATS",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${selectedTemplate.nameAr} • ${selectedTemplate.descriptionAr}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = if (isArabic) "إغلاق" else "Close")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Horizontal visual template switcher strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CvTemplateSpec.entries.forEach { tpl ->
                            val isSelected = tpl == selectedTemplate
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .width(74.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectTemplate(tpl) }
                                    .padding(2.dp)
                            ) {
                                CvTemplateMiniThumbnail(
                                    template = tpl,
                                    userName = user.name,
                                    jobTitle = user.jobTitle ?: "",
                                    isSelected = isSelected,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = tpl.nameAr,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Scrollable live A4 paper canvas
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CvLivePreviewSheet(
                            user = user,
                            template = selectedTemplate,
                            reliabilityRecords = reliabilityRecords,
                            trustScore = trustScore,
                            currentLanguage = currentLanguage,
                            sectionOrder = sectionOrder,
                            compact = false,
                            modifier = Modifier.widthIn(max = 520.dp)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Equal-width footer buttons without icons/emojis
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_close_full_cv_preview")
                        ) {
                            Text(if (isArabic) "إغلاق" else "Close", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                val order = sectionOrder?.takeIf { it.isNotEmpty() }
                                    ?: user.cvSectionOrder?.split(",")
                                ReportExporter.exportEmployeeCvToPdf(
                                    context = context,
                                    user = user,
                                    reliabilityRecords = reliabilityRecords,
                                    language = currentLanguage,
                                    templateId = selectedTemplate.id,
                                    customSectionOrder = order
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_export_from_full_preview")
                        ) {
                            Text(if (isArabic) "تصدير PDF" else "Export PDF", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

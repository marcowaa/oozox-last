package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserEntity
import com.example.data.model.UserType
import com.example.ui.theme.StatusGreen
import com.example.util.AppLanguage
import com.example.util.LocalAppLanguage
import com.example.util.ReliabilityRecord
import com.example.util.ReportExporter
import com.example.util.Translations

enum class CvVisualLayout {
    ATS_SINGLE_CLASSIC,
    ATS_TOP_BAND,
    ATS_TECH_ACCENT,
    ATS_AUDIT_LEDGER,
    EXECUTIVE_BANNER,
    MINIMAL_EDITORIAL,
    CORPORATE_SPLIT_HEADER,
    PRESTIGE_DOUBLE_FRAME,
    SIDEBAR_SPLIT,
    COMPACT_TWO_COL,
    ACADEMIC_FORMAL,
    COMMERCE_SHOWCASE
}

enum class CvTemplateSpec(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val categoryAr: String,
    val isAtsFriendly: Boolean,
    val headerBgHex: String,
    val accentHex: String,
    val secondaryHex: String,
    val layoutStyle: CvVisualLayout,
    val descriptionAr: String
) {
    ATS_CLASSIC(
        id = "ATS_CLASSIC",
        nameAr = "ATS قياسي",
        nameEn = "ATS Classic",
        categoryAr = "ATS",
        isAtsFriendly = true,
        headerBgHex = "#FFFFFF",
        accentHex = "#0F172A",
        secondaryHex = "#475569",
        layoutStyle = CvVisualLayout.ATS_SINGLE_CLASSIC,
        descriptionAr = "عمود واحد مقروء آلياً 100% لأنظمة التوظيف"
    ),
    ATS_EXECUTIVE(
        id = "ATS_EXECUTIVE",
        nameAr = "ATS تنفيذي",
        nameEn = "ATS Executive",
        categoryAr = "ATS",
        isAtsFriendly = true,
        headerBgHex = "#F1F5F9",
        accentHex = "#1E3A8A",
        secondaryHex = "#334155",
        layoutStyle = CvVisualLayout.ATS_TOP_BAND,
        descriptionAr = "هيكل قيادي واضح متوافق مع أنظمة الفرز الآلي"
    ),
    ATS_TECH_ENGINEERING(
        id = "ATS_TECH_ENGINEERING",
        nameAr = "ATS هندسي وتقني",
        nameEn = "ATS Technical",
        categoryAr = "ATS",
        isAtsFriendly = true,
        headerBgHex = "#FFFFFF",
        accentHex = "#0F766E",
        secondaryHex = "#115E59",
        layoutStyle = CvVisualLayout.ATS_TECH_ACCENT,
        descriptionAr = "تركيز على المهارات والموثوقية والاعتمادات"
    ),
    ATS_FINANCE_AUDIT(
        id = "ATS_FINANCE_AUDIT",
        nameAr = "ATS مالي ومحاسبي",
        nameEn = "ATS Finance",
        categoryAr = "ATS",
        isAtsFriendly = true,
        headerBgHex = "#F8FAFC",
        accentHex = "#1E293B",
        secondaryHex = "#0F766E",
        layoutStyle = CvVisualLayout.ATS_AUDIT_LEDGER,
        descriptionAr = "تنظيم رقمي دقيق للمناصب المالية والإدارية"
    ),
    EXECUTIVE_MODERN(
        id = "EXECUTIVE_MODERN",
        nameAr = "تنفيذي عصري",
        nameEn = "Executive Modern",
        categoryAr = "تنفيذي",
        isAtsFriendly = false,
        headerBgHex = "#1E293B",
        accentHex = "#D4AF37",
        secondaryHex = "#F8FAFC",
        layoutStyle = CvVisualLayout.EXECUTIVE_BANNER,
        descriptionAr = "واجهة داكنة أنيقة بلمسة ذهبية احترافية"
    ),
    NORDIC_MINIMAL(
        id = "NORDIC_MINIMAL",
        nameAr = "مينيمال هادئ",
        nameEn = "Nordic Minimal",
        categoryAr = "عصري",
        isAtsFriendly = true,
        headerBgHex = "#FFFFFF",
        accentHex = "#334155",
        secondaryHex = "#64748B",
        layoutStyle = CvVisualLayout.MINIMAL_EDITORIAL,
        descriptionAr = "مساحات بيضاء واسعة وخطوط نظيفة"
    ),
    CORPORATE_NAVY(
        id = "CORPORATE_NAVY",
        nameAr = "شركات رسمي",
        nameEn = "Corporate Navy",
        categoryAr = "تنفيذي",
        isAtsFriendly = false,
        headerBgHex = "#0F172A",
        accentHex = "#0284C7",
        secondaryHex = "#38BDF8",
        layoutStyle = CvVisualLayout.CORPORATE_SPLIT_HEADER,
        descriptionAr = "طابع مؤسسي رصين للشركات الكبرى"
    ),
    GOLD_PRESTIGE(
        id = "GOLD_PRESTIGE",
        nameAr = "بريستيج الأعمال",
        nameEn = "Business Prestige",
        categoryAr = "فاخر",
        isAtsFriendly = false,
        headerBgHex = "#18181B",
        accentHex = "#CA8A04",
        secondaryHex = "#FACC15",
        layoutStyle = CvVisualLayout.PRESTIGE_DOUBLE_FRAME,
        descriptionAr = "مصمم لرواد الأعمال وأصحاب المتاجر"
    ),
    CREATIVE_GRID(
        id = "CREATIVE_GRID",
        nameAr = "إبداعي منظم",
        nameEn = "Creative Grid",
        categoryAr = "عصري",
        isAtsFriendly = false,
        headerBgHex = "#134E4A",
        accentHex = "#0D9488",
        secondaryHex = "#CCFBF1",
        layoutStyle = CvVisualLayout.SIDEBAR_SPLIT,
        descriptionAr = "توزيع مرئي بعمود جانبي للمهارات والأعمال"
    ),
    COMPACT_ONE_PAGE(
        id = "COMPACT_ONE_PAGE",
        nameAr = "مكثف صفحة واحدة",
        nameEn = "Compact One-Page",
        categoryAr = "مختصر",
        isAtsFriendly = true,
        headerBgHex = "#F8FAFC",
        accentHex = "#475569",
        secondaryHex = "#334155",
        layoutStyle = CvVisualLayout.COMPACT_TWO_COL,
        descriptionAr = "يجمع كل الإنجازات في صفحة واحدة مركزة"
    ),
    ACADEMIC_RESEARCH(
        id = "ACADEMIC_RESEARCH",
        nameAr = "أكاديمي ومهني",
        nameEn = "Academic",
        categoryAr = "أكاديمي",
        isAtsFriendly = true,
        headerBgHex = "#EFF6FF",
        accentHex = "#1D4ED8",
        secondaryHex = "#1E40AF",
        layoutStyle = CvVisualLayout.ACADEMIC_FORMAL,
        descriptionAr = "يبرز المؤهلات العلمية والشهادات المعتمدة"
    ),
    COMMERCE_PORTFOLIO(
        id = "COMMERCE_PORTFOLIO",
        nameAr = "تجاري وريادي",
        nameEn = "Commerce Portfolio",
        categoryAr = "تجاري",
        isAtsFriendly = false,
        headerBgHex = "#1E1B4B",
        accentHex = "#4F46E5",
        secondaryHex = "#A5B4FC",
        layoutStyle = CvVisualLayout.COMMERCE_SHOWCASE,
        descriptionAr = "يعرض النشاط التجاري والمتجر وتقييمات العملاء"
    );

    fun localizedName(language: AppLanguage): String = Translations.get(
        when (this) {
            ATS_CLASSIC -> "cv.tpl_ats_classic_name"
            ATS_EXECUTIVE -> "cv.tpl_ats_exec_name"
            ATS_TECH_ENGINEERING -> "cv.tpl_ats_tech_name"
            ATS_FINANCE_AUDIT -> "cv.tpl_ats_fin_name"
            EXECUTIVE_MODERN -> "cv.tpl_exec_mod_name"
            NORDIC_MINIMAL -> "cv.tpl_nordic_name"
            CORPORATE_NAVY -> "cv.tpl_corp_navy_name"
            GOLD_PRESTIGE -> "cv.tpl_prestige_name"
            CREATIVE_GRID -> "cv.tpl_creative_name"
            COMPACT_ONE_PAGE -> "cv.tpl_compact_name"
            ACADEMIC_RESEARCH -> "cv.tpl_academic_name"
            COMMERCE_PORTFOLIO -> "cv.tpl_commerce_name"
        },
        language
    )

    fun localizedCategory(language: AppLanguage): String = Translations.get(
        when (this) {
            ATS_CLASSIC, ATS_EXECUTIVE, ATS_TECH_ENGINEERING, ATS_FINANCE_AUDIT -> "cv.cat_ats"
            EXECUTIVE_MODERN, CORPORATE_NAVY -> "cv.cat_exec"
            NORDIC_MINIMAL, CREATIVE_GRID -> "cv.cat_modern"
            GOLD_PRESTIGE -> "cv.cat_luxury"
            COMPACT_ONE_PAGE -> "cv.cat_compact"
            ACADEMIC_RESEARCH -> "cv.cat_academic"
            COMMERCE_PORTFOLIO -> "cv.cat_commerce"
        },
        language
    )

    fun localizedDescription(language: AppLanguage): String = Translations.get(
        when (this) {
            ATS_CLASSIC -> "cv.tpl_ats_classic_desc"
            ATS_EXECUTIVE -> "cv.tpl_ats_exec_desc"
            ATS_TECH_ENGINEERING -> "cv.tpl_ats_tech_desc"
            ATS_FINANCE_AUDIT -> "cv.tpl_ats_fin_desc"
            EXECUTIVE_MODERN -> "cv.tpl_exec_mod_desc"
            NORDIC_MINIMAL -> "cv.tpl_nordic_desc"
            CORPORATE_NAVY -> "cv.tpl_corp_navy_desc"
            GOLD_PRESTIGE -> "cv.tpl_prestige_desc"
            CREATIVE_GRID -> "cv.tpl_creative_desc"
            COMPACT_ONE_PAGE -> "cv.tpl_compact_desc"
            ACADEMIC_RESEARCH -> "cv.tpl_academic_desc"
            COMMERCE_PORTFOLIO -> "cv.tpl_commerce_desc"
        },
        language
    )

    companion object {
        fun fromId(id: String?): CvTemplateSpec =
            entries.firstOrNull { it.id == id } ?: ATS_CLASSIC
    }
}

/**
 * Professional Edit Profile Dialog (clean Material 3, no emojis, supports custom free store name).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileProfessionalDialog(
    user: UserEntity,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        username: String,
        type: UserType,
        jobTitle: String?,
        bio: String?,
        education: String?,
        storeName: String?,
        skills: String?
    ) -> Unit
) {
    var editName by remember { mutableStateOf(user.name) }
    var editUsername by remember { mutableStateOf(user.username) }
    var editStoreName by remember { mutableStateOf(user.traderShopName ?: "") }
    var editJobTitle by remember {
        mutableStateOf(
            user.jobTitle?.takeIf { it.isNotBlank() }
                ?: Translations.get("profile.prof_businessman", currentLanguage)
        )
    }
    var editEducation by remember { mutableStateOf(user.education ?: "") }
    var editBio by remember { mutableStateOf(user.merchantBio ?: "") }
    var editSkills by remember { mutableStateOf(user.skills ?: "") }
    var editError by remember { mutableStateOf<String?>(null) }
    var jobDropdownExpanded by remember { mutableStateOf(false) }

    val professionOptions = remember(currentLanguage, Translations.catalogVersion) {
        listOf(
            Translations.get("profile.prof_businessman", currentLanguage),
            Translations.get("profile.prof_merchant", currentLanguage),
            Translations.get("profile.prof_director", currentLanguage),
            Translations.get("profile.prof_contractor", currentLanguage),
            Translations.get("profile.prof_accountant", currentLanguage),
            Translations.get("profile.prof_engineer", currentLanguage),
            Translations.get("profile.prof_tech", currentLanguage),
            Translations.get("profile.prof_gold", currentLanguage),
            Translations.get("profile.prof_bd", currentLanguage),
            Translations.get("profile.prof_freelance", currentLanguage)
        )
    }

    val inferredUserType = remember(editJobTitle) {
        val lower = editJobTitle.lowercase()
        if (lower.contains("ذهب") || lower.contains("صائغ") || lower.contains("مجوهرات") ||
            lower.contains("سبائك") || lower.contains("gold") || lower.contains("jewel")
        ) {
            UserType.GOLD_TRADER
        } else {
            UserType.BUSINESSMAN
        }
    }

    val fieldShape = RoundedCornerShape(12.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        focusedContainerColor = MaterialTheme.colorScheme.surface
    )
    val fieldTextStyle = MaterialTheme.typography.bodyMedium

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides currentLanguage.direction,
            LocalAppLanguage provides currentLanguage
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 500.dp)
                    .heightIn(max = 650.dp)
                    .padding(vertical = 12.dp)
                    .imePadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Compact Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ManageAccounts,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = Translations.get("profile.edit_title", currentLanguage),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = Translations.get("profile.edit_subtitle", currentLanguage),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = Translations.get("common.close", currentLanguage),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Refined Compact Form Body
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = Translations.get("profile.sec_identity", currentLanguage),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it; editError = null },
                            label = { Text(Translations.get("profile.full_name", currentLanguage), style = MaterialTheme.typography.bodySmall) },
                            placeholder = { Text(Translations.get("profile.full_name_hint", currentLanguage), style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            textStyle = fieldTextStyle,
                            colors = fieldColors,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = fieldShape,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editUsername,
                            onValueChange = { editUsername = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' }; editError = null },
                            label = { Text(Translations.get("profile.username_label", currentLanguage), style = MaterialTheme.typography.bodySmall) },
                            placeholder = { Text("username", style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            textStyle = fieldTextStyle.copy(textDirection = TextDirection.Ltr),
                            colors = fieldColors,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.AlternateEmail,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = fieldShape,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = Translations.get("profile.sec_business", currentLanguage),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        ExposedDropdownMenuBox(
                            expanded = jobDropdownExpanded,
                            onExpandedChange = { jobDropdownExpanded = !jobDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = editJobTitle,
                                onValueChange = { editJobTitle = it },
                                label = { Text(Translations.get("profile.job_title", currentLanguage), style = MaterialTheme.typography.bodySmall) },
                                singleLine = true,
                                textStyle = fieldTextStyle,
                                colors = fieldColors,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.WorkOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = jobDropdownExpanded) },
                                shape = fieldShape,
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryEditable)
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = jobDropdownExpanded,
                                onDismissRequest = { jobDropdownExpanded = false }
                            ) {
                                professionOptions.forEach { role ->
                                    DropdownMenuItem(
                                        text = { Text(role, style = MaterialTheme.typography.bodyMedium) },
                                        onClick = {
                                            editJobTitle = role
                                            jobDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = editStoreName,
                            onValueChange = { editStoreName = it },
                            label = { Text(Translations.get("profile.store_name", currentLanguage), style = MaterialTheme.typography.bodySmall) },
                            placeholder = { Text(Translations.get("profile.store_hint", currentLanguage), style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            textStyle = fieldTextStyle,
                            colors = fieldColors,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = fieldShape,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = Translations.get("profile.sec_credentials", currentLanguage),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        // Multiline bio box without awkward floating vertical centered icon
                        OutlinedTextField(
                            value = editBio,
                            onValueChange = { editBio = it },
                            label = { Text(Translations.get("profile.bio_label", currentLanguage), style = MaterialTheme.typography.bodySmall) },
                            placeholder = { Text(Translations.get("profile.bio_hint", currentLanguage), style = MaterialTheme.typography.bodySmall) },
                            minLines = 2,
                            maxLines = 4,
                            textStyle = fieldTextStyle,
                            colors = fieldColors,
                            shape = fieldShape,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editEducation,
                            onValueChange = { editEducation = it },
                            label = { Text(Translations.get("profile.edu_label", currentLanguage), style = MaterialTheme.typography.bodySmall) },
                            placeholder = { Text(Translations.get("profile.edu_hint", currentLanguage), style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            textStyle = fieldTextStyle,
                            colors = fieldColors,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = fieldShape,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editSkills,
                            onValueChange = { editSkills = it },
                            label = { Text(Translations.get("profile.skills_label", currentLanguage), style = MaterialTheme.typography.bodySmall) },
                            placeholder = { Text(Translations.get("profile.skills_hint", currentLanguage), style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            textStyle = fieldTextStyle,
                            colors = fieldColors,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = fieldShape,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (editError != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = editError!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Equal Footer Actions (no icons/emojis)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text(Translations.get("common.cancel", currentLanguage), fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                if (editName.isBlank()) {
                                    editError = Translations.get("profile.err_enter_name", currentLanguage)
                                } else if (editUsername.isBlank()) {
                                    editError = Translations.get("profile.err_enter_username", currentLanguage)
                                } else {
                                    onSave(
                                        editName.trim(),
                                        editUsername.trim(),
                                        inferredUserType,
                                        editJobTitle.trim().ifBlank { null },
                                        editBio.trim().ifBlank { null },
                                        editEducation.trim().ifBlank { null },
                                        editStoreName.trim().ifBlank { null },
                                        editSkills.trim().ifBlank { null }
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_save_profile_edits")
                        ) {
                            Text(Translations.get("common.save", currentLanguage), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom CV Section Order, Visibility & Template Selector Dialog.
 */
@Composable
fun CvSectionCustomizerDialog(
    user: UserEntity,
    currentLanguage: AppLanguage,
    selectedTemplate: CvTemplateSpec,
    onSelectTemplate: (CvTemplateSpec) -> Unit,
    onDismiss: () -> Unit,
    onSaveSectionOrder: (String) -> Unit,
    reliabilityRecords: List<ReliabilityRecord> = emptyList(),
    trustScore: Int = 95
) {
    val allSections = remember(currentLanguage, Translations.catalogVersion) {
        listOf(
            "BIO" to Translations.get("cv.sec_bio", currentLanguage),
            "EXPERIENCE" to Translations.get("cv.sec_experience", currentLanguage),
            "RELIABILITY" to Translations.get("cv.sec_reliability", currentLanguage),
            "SKILLS" to Translations.get("cv.sec_skills", currentLanguage),
            "EDUCATION" to Translations.get("cv.sec_education", currentLanguage),
            "STORE" to Translations.get("cv.sec_store", currentLanguage)
        )
    }

    val initialList = remember(user.cvSectionOrder) {
        val saved = user.cvSectionOrder?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        val valid = saved.filter { key -> allSections.any { it.first == key } }
        if (valid.isNotEmpty()) valid else allSections.map { it.first }
    }

    var activeSections by remember { mutableStateOf(initialList) }
    var currentTemplate by remember { mutableStateOf(selectedTemplate) }
    var showPreviewModal by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides currentLanguage.direction,
            LocalAppLanguage provides currentLanguage
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 520.dp)
                    .heightIn(max = 660.dp)
                    .padding(vertical = 12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = Translations.get("cv.customizer_title", currentLanguage),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = Translations.get("cv.customizer_sub", currentLanguage),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Close, contentDescription = Translations.get("common.close", currentLanguage), modifier = Modifier.size(20.dp))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Translations.format("cv.template_fmt", currentLanguage, "name" to currentTemplate.localizedName(currentLanguage)),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(onClick = { showPreviewModal = true }) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Translations.get("cv.preview_shape", currentLanguage), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CvTemplateSpec.entries.forEach { tpl ->
                                val isSelected = currentTemplate == tpl
                                Surface(
                                    onClick = { currentTemplate = tpl },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    else MaterialTheme.colorScheme.surfaceContainerLowest,
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                    ),
                                    modifier = Modifier.width(92.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        CvTemplateMiniThumbnail(
                                            template = tpl,
                                            userName = user.name,
                                            jobTitle = user.jobTitle ?: Translations.get("cv.certified_member", currentLanguage),
                                            isSelected = isSelected,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text = tpl.localizedName(currentLanguage),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        Text(
                            text = Translations.get("cv.section_order", currentLanguage),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        activeSections.forEachIndexed { index, key ->
                            val label = allSections.firstOrNull { it.first == key }?.second ?: key
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        IconButton(
                                            onClick = {
                                                if (index > 0) {
                                                    val list = activeSections.toMutableList()
                                                    val item = list.removeAt(index)
                                                    list.add(index - 1, item)
                                                    activeSections = list
                                                }
                                            },
                                            enabled = index > 0,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = Translations.get("cv.move_up", currentLanguage), modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                if (index < activeSections.size - 1) {
                                                    val list = activeSections.toMutableList()
                                                    val item = list.removeAt(index)
                                                    list.add(index + 1, item)
                                                    activeSections = list
                                                }
                                            },
                                            enabled = index < activeSections.size - 1,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = Translations.get("cv.move_down", currentLanguage), modifier = Modifier.size(18.dp))
                                        }
                                        if (activeSections.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    activeSections = activeSections.filterNot { it == key }
                                                },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.VisibilityOff,
                                                    contentDescription = Translations.get("cv.hide_sec", currentLanguage),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val hiddenSections = allSections.filter { pair -> activeSections.none { it == pair.first } }
                        if (hiddenSections.isNotEmpty()) {
                            Text(
                                text = Translations.get("cv.hidden_sections", currentLanguage),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                hiddenSections.forEach { (key, label) ->
                                    AssistChip(
                                        onClick = { activeSections = activeSections + key },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp)) }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text(Translations.get("common.cancel", currentLanguage), fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                onSelectTemplate(currentTemplate)
                                onSaveSectionOrder(activeSections.joinToString(","))
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text(Translations.get("common.save", currentLanguage), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showPreviewModal) {
        FullCvPreviewDialog(
            user = user,
            reliabilityRecords = reliabilityRecords,
            trustScore = trustScore,
            currentLanguage = currentLanguage,
            selectedTemplate = currentTemplate,
            onSelectTemplate = { currentTemplate = it },
            sectionOrder = activeSections,
            onDismiss = { showPreviewModal = false }
        )
    }
}

/**
 * Complete CV Sharing, Client Card, and 12+ ATS/Executive Template Hub.
 */
@Composable
fun ProfileShareAndCvHubDialog(
    user: UserEntity,
    reliabilityRecords: List<ReliabilityRecord>,
    trustScore: Int,
    currentLanguage: AppLanguage,
    selectedTemplate: CvTemplateSpec,
    onSelectTemplate: (CvTemplateSpec) -> Unit,
    onOpenSectionCustomizer: () -> Unit,
    projectsCount: Int = 0,
    completedDealsCount: Int = 0,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) } // 0: CV Link, 1: Client Card, 2: 12 Templates (ATS)
    var selectedExpiryHours by remember { mutableIntStateOf(72) }
    var tokenSeed by remember { mutableStateOf(java.util.UUID.randomUUID().toString().take(10)) }
    var atsOnlyFilter by remember { mutableStateOf(false) }
    var showFullPreview by remember { mutableStateOf(false) }

    // Persistent Customizable Client Card State
    var cardConfig by remember(user.id) {
        mutableStateOf(com.example.util.ClientCardPrefsStore.load(context, user))
    }

    val publicCvUrl = remember(user.username, tokenSeed, selectedExpiryHours, selectedTemplate) {
        val handle = user.username.ifBlank { user.id.take(8) }
        val expiryEpoch = System.currentTimeMillis() + (selectedExpiryHours * 3600_000L)
        "https://cv.oozox.com/$handle?tpl=${selectedTemplate.id}&v=$tokenSeed&exp=$expiryEpoch"
    }

    val reliabilitySummary = reliabilityRecords.firstOrNull()?.let {
        Translations.format(
            "cv.rel_bar_fmt",
            currentLanguage,
            "presence" to it.presenceRate,
            "punctuality" to it.punctuality,
            "location" to it.locationCompliance
        )
    } ?: Translations.format("cv.rel_summary_fallback_fmt", currentLanguage, "trust" to trustScore)

    val cardText = remember(cardConfig, user.username, trustScore, reliabilitySummary, projectsCount, completedDealsCount, user.skills) {
        com.example.util.ClientCardRenderer.buildSocialCaption(
            config = cardConfig,
            username = user.username.ifBlank { user.id.take(8) },
            trustScore = trustScore,
            reliabilitySummary = reliabilitySummary,
            projectsCount = projectsCount,
            completedDealsCount = completedDealsCount,
            skills = user.skills
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides currentLanguage.direction,
            LocalAppLanguage provides currentLanguage
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 520.dp)
                    .heightIn(max = 670.dp)
                    .padding(vertical = 12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Sleek Compact Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = Translations.get("cv.hub_title", currentLanguage),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = Translations.get("cv.hub_sub", currentLanguage),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Close, contentDescription = Translations.get("common.close", currentLanguage), modifier = Modifier.size(20.dp))
                        }
                    }

                    // Compact 3-Tab Switcher
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            modifier = Modifier.height(48.dp),
                            text = { Text(Translations.get("cv.tab_link", currentLanguage), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            modifier = Modifier.height(48.dp),
                            text = { Text(Translations.get("cv.tab_card", currentLanguage), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            modifier = Modifier.height(48.dp),
                            text = { Text(Translations.get("cv.tab_templates", currentLanguage), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                        )
                    }

                    // Scrollable Tab Body
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (activeTab) {
                            0 -> {
                                // TAB 1: VERIFIED CV LINK
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                                            Text(
                                                text = Translations.get("cv.verified_cv_badge", currentLanguage),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = StatusGreen
                                            )
                                        }
                                        Text(
                                            text = Translations.format(
                                                "cv.summary_card_fmt",
                                                currentLanguage,
                                                "name" to user.name,
                                                "username" to user.username,
                                                "role" to (user.jobTitle ?: Translations.get("cv.certified_member", currentLanguage)),
                                                "trust" to trustScore,
                                                "relSummary" to reliabilitySummary,
                                                "template" to selectedTemplate.localizedName(currentLanguage)
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }

                                Text(
                                    text = Translations.get("cv.link_expiry", currentLanguage),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        24 to Translations.get("cv.exp_24h", currentLanguage),
                                        72 to Translations.get("cv.exp_3d", currentLanguage),
                                        168 to Translations.get("cv.exp_7d", currentLanguage),
                                        720 to Translations.get("cv.exp_30d", currentLanguage)
                                    ).forEach { (hrs, lbl) ->
                                        FilterChip(
                                            selected = selectedExpiryHours == hrs,
                                            onClick = { selectedExpiryHours = hrs },
                                            label = { Text(lbl, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Text(
                                                text = publicCvUrl,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                clipboard?.setPrimaryClip(ClipData.newPlainText("CV Link", publicCvUrl))
                                                Toast.makeText(context, Translations.get("cv.link_copied", currentLanguage), Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("btn_copy_public_cv_link")
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = Translations.get("cv.copy_link", currentLanguage), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = {
                                        tokenSeed = java.util.UUID.randomUUID().toString().take(10)
                                        Toast.makeText(context, Translations.get("cv.link_refreshed", currentLanguage), Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(Translations.get("cv.new_link", currentLanguage), style = MaterialTheme.typography.labelSmall)
                                    }

                                    TextButton(onClick = { showFullPreview = true }) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(Translations.get("cv.preview_tpl", currentLanguage), style = MaterialTheme.typography.labelSmall)
                                    }

                                    TextButton(onClick = onOpenSectionCustomizer) {
                                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(Translations.get("cv.sections_btn", currentLanguage), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            1 -> {
                                // TAB 2: FULL EDITABLE CLIENT BUSINESS CARD STUDIO
                                ClientCardStudioTabContent(
                                    user = user,
                                    config = cardConfig,
                                    onConfigChange = { updated ->
                                        cardConfig = updated
                                        com.example.util.ClientCardPrefsStore.save(context, user.id, updated)
                                    },
                                    trustScore = trustScore,
                                    reliabilitySummary = reliabilitySummary,
                                    projectsCount = projectsCount,
                                    completedDealsCount = completedDealsCount
                                )
                            }

                            else -> {
                                // TAB 3: 12 CV TEMPLATES WITH LIVE PREVIEW & EQUAL-HEIGHT GRID CARDS
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = Translations.format("cv.preview_prefix_fmt", currentLanguage, "name" to selectedTemplate.localizedName(currentLanguage)),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = selectedTemplate.localizedDescription(currentLanguage),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            TextButton(
                                                onClick = { showFullPreview = true },
                                                modifier = Modifier.testTag("btn_open_full_cv_preview")
                                            ) {
                                                Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(Translations.get("cv.zoom", currentLanguage), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { showFullPreview = true }
                                        ) {
                                            CvLivePreviewSheet(
                                                user = user,
                                                template = selectedTemplate,
                                                reliabilityRecords = reliabilityRecords,
                                                trustScore = trustScore,
                                                currentLanguage = currentLanguage,
                                                compact = true
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = Translations.get("cv.templates_heading", currentLanguage),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    FilterChip(
                                        selected = atsOnlyFilter,
                                        onClick = { atsOnlyFilter = !atsOnlyFilter },
                                        label = { Text(Translations.get("cv.ats_only", currentLanguage), style = MaterialTheme.typography.labelSmall) },
                                        leadingIcon = if (atsOnlyFilter) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        } else null
                                    )
                                }

                                val filteredTemplates = remember(atsOnlyFilter) {
                                    if (atsOnlyFilter) CvTemplateSpec.entries.filter { it.isAtsFriendly }
                                    else CvTemplateSpec.entries
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    filteredTemplates.chunked(2).forEach { rowTemplates ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(IntrinsicSize.Min),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            rowTemplates.forEach { tpl ->
                                                val isSelected = selectedTemplate == tpl
                                                Surface(
                                                    onClick = { onSelectTemplate(tpl) },
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                                    else MaterialTheme.colorScheme.surfaceContainerLowest,
                                                    border = BorderStroke(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                                    ),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .testTag("template_card_${tpl.id}")
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(10.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        CvTemplateMiniThumbnail(
                                                            template = tpl,
                                                            userName = user.name,
                                                            jobTitle = user.jobTitle ?: Translations.get("cv.certified_member", currentLanguage),
                                                            isSelected = isSelected,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )

                                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = tpl.localizedName(currentLanguage),
                                                                    style = MaterialTheme.typography.labelLarge,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    modifier = Modifier.weight(1f)
                                                                )
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Surface(
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                                                ) {
                                                                    Text(
                                                                        text = tpl.localizedCategory(currentLanguage),
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        fontSize = 10.sp,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                            Text(
                                                                text = tpl.localizedDescription(currentLanguage),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                minLines = 2,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis,
                                                                lineHeight = 14.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            if (rowTemplates.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Anchored Equal-Width Footer Actions per Tab (no icons/emojis)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (activeTab) {
                            0 -> {
                                OutlinedButton(
                                    onClick = {
                                        val order = user.cvSectionOrder?.split(",")
                                        ReportExporter.exportEmployeeCvToPdf(
                                            context = context,
                                            user = user,
                                            reliabilityRecords = reliabilityRecords,
                                            language = currentLanguage,
                                            templateId = selectedTemplate.id,
                                            customSectionOrder = order
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text(Translations.get("cv.export_pdf", currentLanguage), fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = {
                                        val shareMessage = Translations.format(
                                            "cv.share_msg_fmt",
                                            currentLanguage,
                                            "name" to user.name,
                                            "role" to (user.jobTitle ?: Translations.get("cv.certified_member", currentLanguage)),
                                            "trust" to trustScore,
                                            "relSummary" to reliabilitySummary,
                                            "template" to selectedTemplate.localizedName(currentLanguage),
                                            "url" to publicCvUrl
                                        )
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareMessage)
                                        }
                                        context.startActivity(Intent.createChooser(intent, Translations.get("cv.share_chooser", currentLanguage)))
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("btn_confirm_share_cv")
                                ) {
                                    Text(Translations.get("cv.share_link_btn", currentLanguage), fontWeight = FontWeight.Bold)
                                }
                            }

                            1 -> {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("Client Card", cardText))
                                        Toast.makeText(context, Translations.get("cv.card_copied", currentLanguage), Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text(Translations.get("cv.copy_card_btn", currentLanguage), fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = {
                                        com.example.util.ClientCardRenderer.shareCardToSocialPlatform(
                                            context = context,
                                            user = user,
                                            config = cardConfig,
                                            trustScore = trustScore,
                                            reliabilitySummary = reliabilitySummary,
                                            platform = com.example.util.SocialSharePlatform.ALL,
                                            projectsCount = projectsCount,
                                            completedDealsCount = completedDealsCount
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text(Translations.get("cv.share_card_btn", currentLanguage), fontWeight = FontWeight.Bold)
                                }
                            }

                            else -> {
                                OutlinedButton(
                                    onClick = { showFullPreview = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text(Translations.get("cv.full_preview_btn", currentLanguage), fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = {
                                        val order = user.cvSectionOrder?.split(",")
                                        ReportExporter.exportEmployeeCvToPdf(
                                            context = context,
                                            user = user,
                                            reliabilityRecords = reliabilityRecords,
                                            language = currentLanguage,
                                            templateId = selectedTemplate.id,
                                            customSectionOrder = order
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text(Translations.get("cv.export_pdf", currentLanguage), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFullPreview) {
        FullCvPreviewDialog(
            user = user,
            reliabilityRecords = reliabilityRecords,
            trustScore = trustScore,
            currentLanguage = currentLanguage,
            selectedTemplate = selectedTemplate,
            onSelectTemplate = onSelectTemplate,
            onDismiss = { showFullPreview = false }
        )
    }
}

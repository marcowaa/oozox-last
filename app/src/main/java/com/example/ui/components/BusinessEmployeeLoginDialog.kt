package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OozoxTeal
import com.example.util.WorkplaceLocationPolicyManager
import com.example.util.tr
import kotlinx.coroutines.launch

/**
 * Mandatory Employee Login + GPS Shift Check-In Dialog:
 * Authenticates employee username/password, forces GPS permission & active device location,
 * verifies HQ geofence compliance, and notifies manager automatically on violations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeLoginMandatoryGpsDialog(
    onDismiss: () -> Unit,
    onLoginWithMandatoryGps: suspend (
        username: String,
        password: String
    ) -> Pair<Boolean, WorkplaceLocationPolicyManager.LocationPolicyCheckResult?>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var gpsResult by remember { mutableStateOf<WorkplaceLocationPolicyManager.LocationPolicyCheckResult?>(null) }

    val invalidCredsMsg = tr("hr.login_invalid_creds")
    val gpsPermRequiredMsg = tr("biz.gps_perm_needed")

    fun executeLoginAndGpsVerification() {
        scope.launch {
            isProcessing = true
            authError = null
            gpsResult = null
            val (credsOk, geoRes) = onLoginWithMandatoryGps(username.trim(), password.trim())
            isProcessing = false
            if (!credsOk) {
                authError = invalidCredsMsg
            } else {
                gpsResult = geoRes
                if (geoRes == null || geoRes.isCompliant) {
                    onDismiss()
                }
            }
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            executeLoginAndGpsVerification()
        } else {
            authError = gpsPermRequiredMsg
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("employee_login_gps_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, OozoxTeal.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = OozoxTeal)
                        Text(
                            text = tr("hr.employee_login_title"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = tr("common.close"))
                    }
                }

                Text(
                    text = tr("hr.login_portal_desc"),
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.trim() },
                    label = { Text(tr("hr.username")) },
                    leadingIcon = { Text("@", fontWeight = FontWeight.Bold, color = GoldPrimary) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("emp_login_username_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(tr("hr.password")) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("emp_login_password_input")
                )

                if (authError != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = authError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                val geo = gpsResult
                if (geo != null && !geo.isCompliant) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = tr("hr.gps_violation_title"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFEF4444)
                            )
                            Text(text = geo.messageAr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            if (!geo.isGpsActive) {
                                TextButton(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                        } catch (_: Exception) {}
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(tr("biz.open_gps_settings"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 46.dp)
                    ) {
                        Text(tr("common.cancel"))
                    }
                    Button(
                        onClick = {
                            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (!hasFine && !hasCoarse) {
                                permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            } else {
                                executeLoginAndGpsVerification()
                            }
                        },
                        enabled = !isProcessing && username.isNotBlank() && password.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal, contentColor = Color.White),
                        modifier = Modifier.weight(1.7f).heightIn(min = 46.dp).testTag("emp_login_submit_btn")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tr("hr.gps_checking"), fontSize = 11.5.sp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("hr.login_submit_btn"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

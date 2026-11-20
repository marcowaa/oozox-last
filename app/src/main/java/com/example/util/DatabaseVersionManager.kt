package com.example.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Utility responsible for validating Room database schema versioning upon application launch,
 * detecting any schema discrepancies or integrity issues, and logging them to a centralized
 * diagnostic console for easier debugging and administrative visibility.
 */
object DatabaseVersionManager {

    private const val TAG = "DatabaseVersionManager"
    const val EXPECTED_SCHEMA_VERSION = 26
    private const val DATABASE_NAME = "gold_chat_database"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _schemaVersion = MutableStateFlow(EXPECTED_SCHEMA_VERSION)
    val schemaVersion: StateFlow<Int> = _schemaVersion.asStateFlow()

    private val _hasIntegrityDiscrepancy = MutableStateFlow(false)
    val hasIntegrityDiscrepancy: StateFlow<Boolean> = _hasIntegrityDiscrepancy.asStateFlow()

    private val _lastDiagnosticLog = MutableStateFlow("لم يتم الفحص بعد")
    val lastDiagnosticLog: StateFlow<String> = _lastDiagnosticLog.asStateFlow()

    /**
     * Initializes and checks the Room database schema version upon application launch.
     * Logs integrity discrepancies to DatabaseIntegrityChecker and Android Logcat.
     */
    fun checkSchemaVersionOnLaunch(context: Context) {
        scope.launch {
            validateSchemaAndLog(context)
        }
    }

    /**
     * Performs schema version validation and inspects database integrity.
     */
    suspend fun validateSchemaAndLog(context: Context): IntegrityReport = withContext(Dispatchers.IO) {
        Log.i(TAG, "Checking Room database schema version (expected: V$EXPECTED_SCHEMA_VERSION)...")

        // First run the full DatabaseIntegrityChecker routine
        val report = DatabaseIntegrityChecker.performIntegrityCheck(context)

        _schemaVersion.value = report.currentVersion
        val discrepancy = !report.isVersionValid || !report.isQuickCheckOk
        _hasIntegrityDiscrepancy.value = discrepancy
        _lastDiagnosticLog.value = report.integrityMessage
        _isInitialized.value = true

        if (discrepancy) {
            Log.w(TAG, "DIAGNOSTIC ALERT: Schema discrepancy detected! Current=V${report.currentVersion}, Expected=V$EXPECTED_SCHEMA_VERSION. QuickCheck=${report.isQuickCheckOk}")
        } else {
            Log.i(TAG, "DIAGNOSTIC OK: Schema version V${report.currentVersion} verified successfully with all tables intact.")
        }

        report
    }
}

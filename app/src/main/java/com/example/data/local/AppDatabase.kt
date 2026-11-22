package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*

@Database(
    entities = [
        UserEntity::class,
        ContactEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        ChatAccountEntity::class,
        HashtagAccountEntity::class,
        HashtagEntryEntity::class,
        CommentEntity::class,
        WorkGroupEntity::class,
        GroupMemberEntity::class,
        ProjectEntity::class,
        TransactionEntity::class,
        GoldPurchaseEntity::class,
        TraderRequestEntity::class,
        AdminConfigEntity::class,
        ProjectMemberEntity::class,
        ProjectAuditLogEntity::class,
        LedgerEntryEntity::class,
        ClearingSettlementEntity::class,
        SyncQueueEntity::class,
        SubscriptionPlanEntity::class,
        SubscriptionRequestEntity::class,
        PromoAnnouncementEntity::class,
        MerchantProductEntity::class,
        MerchantDailySaleEntity::class,
        MerchantDailyClosingReportEntity::class,
        PriceAlertEntity::class,
        HiddenProfileEntity::class,
        HiddenSubAccountEntity::class,
        DailyAccountingRecordEntity::class,
        EmployeeAttendanceEntity::class,
        FinancialComplaintEntity::class,
        MerchantReviewEntity::class,
        InvoiceEntity::class,
        ProjectTaskEntity::class,
        ShiftHandoverEntity::class,
        DynamicExcelTemplateEntity::class,
        DynamicExcelCustomFieldEntity::class,
        DynamicExcelRowRecordEntity::class,
        BusinessProfileEntity::class,
        PayrollAdjustmentEntity::class,
        EmployeeLeaveEntity::class
    ],
    version = 28,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun contactDao(): ContactDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun chatAccountDao(): ChatAccountDao
    abstract fun workGroupDao(): WorkGroupDao
    abstract fun projectDao(): ProjectDao
    abstract fun transactionDao(): TransactionDao
    abstract fun goldPurchaseDao(): GoldPurchaseDao
    abstract fun traderRequestDao(): TraderRequestDao
    abstract fun adminConfigDao(): AdminConfigDao
    abstract fun projectMemberDao(): ProjectMemberDao
    abstract fun projectAuditLogDao(): ProjectAuditLogDao
    abstract fun immutableLedgerDao(): ImmutableLedgerDao
    abstract fun clearingSettlementDao(): ClearingSettlementDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun subscriptionPlanDao(): SubscriptionPlanDao
    abstract fun subscriptionRequestDao(): SubscriptionRequestDao
    abstract fun promoAnnouncementDao(): PromoAnnouncementDao
    abstract fun merchantStoreDao(): MerchantStoreDao
    abstract fun merchantReviewDao(): MerchantReviewDao
    abstract fun priceAlertDao(): PriceAlertDao
    abstract fun hiddenProfileDao(): HiddenProfileDao
    abstract fun hiddenSubAccountDao(): HiddenSubAccountDao
    abstract fun dailyAccountingRecordDao(): DailyAccountingRecordDao
    abstract fun employeeAttendanceDao(): EmployeeAttendanceDao
    abstract fun financialComplaintDao(): FinancialComplaintDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun projectTaskDao(): ProjectTaskDao
    abstract fun shiftHandoverDao(): ShiftHandoverDao
    abstract fun dynamicExcelDao(): DynamicExcelDao
    abstract fun businessHrDao(): BusinessHrDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN hashtagId TEXT")
                db.execSQL("ALTER TABLE invoices ADD COLUMN hashtagTag TEXT")
                db.execSQL("ALTER TABLE invoices ADD COLUMN chatId TEXT")
                db.execSQL("ALTER TABLE invoices ADD COLUMN paidAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE invoices ADD COLUMN isExternalBusiness INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE invoices ADD COLUMN sourceType TEXT NOT NULL DEFAULT 'DIRECT'")

                db.execSQL("ALTER TABLE hashtag_accounts ADD COLUMN projectId TEXT")
                db.execSQL("ALTER TABLE hashtag_accounts ADD COLUMN projectName TEXT")

                db.execSQL("ALTER TABLE hashtag_entries ADD COLUMN linkedInvoiceId TEXT")
                db.execSQL("ALTER TABLE hashtag_entries ADD COLUMN projectId TEXT")
                db.execSQL("ALTER TABLE hashtag_entries ADD COLUMN projectName TEXT")
            }
        }

        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE merchant_products ADD COLUMN viewsCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `business_profiles` (
                        `id` TEXT NOT NULL,
                        `ownerId` TEXT NOT NULL,
                        `companyName` TEXT NOT NULL,
                        `activityType` TEXT NOT NULL,
                        `customActivityLabel` TEXT NOT NULL,
                        `commercialRegistration` TEXT NOT NULL,
                        `taxNumber` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `address` TEXT NOT NULL,
                        `currency` TEXT NOT NULL,
                        `workStartTime` TEXT NOT NULL,
                        `workEndTime` TEXT NOT NULL,
                        `hqLatitude` REAL,
                        `hqLongitude` REAL,
                        `geofenceRadiusMeters` INTEGER NOT NULL,
                        `companyGroupId` TEXT NOT NULL,
                        `companyGroupName` TEXT NOT NULL,
                        `isSetupCompleted` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `payroll_adjustments` (
                        `id` TEXT NOT NULL,
                        `employeeId` TEXT NOT NULL,
                        `employeeName` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `reason` TEXT NOT NULL,
                        `effectiveDate` TEXT NOT NULL,
                        `monthKey` TEXT NOT NULL,
                        `createdByName` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `employee_leaves` (
                        `id` TEXT NOT NULL,
                        `employeeId` TEXT NOT NULL,
                        `employeeName` TEXT NOT NULL,
                        `leaveType` TEXT NOT NULL,
                        `startDate` TEXT NOT NULL,
                        `endDate` TEXT NOT NULL,
                        `daysCount` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `approvedByName` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("ALTER TABLE hidden_sub_accounts ADD COLUMN department TEXT NOT NULL DEFAULT 'العمليات'")
                db.execSQL("ALTER TABLE hidden_sub_accounts ADD COLUMN baseSalary REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE hidden_sub_accounts ADD COLUMN salaryCurrency TEXT NOT NULL DEFAULT 'EGP'")
                db.execSQL("ALTER TABLE hidden_sub_accounts ADD COLUMN geofenceEnforced INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE hidden_sub_accounts ADD COLUMN permManageBusiness INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE hidden_sub_accounts ADD COLUMN permManageStaff INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE hidden_sub_accounts ADD COLUMN permManagePayroll INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE hidden_sub_accounts ADD COLUMN permManageProjects INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE hidden_sub_accounts ADD COLUMN permAssignTasks INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE hidden_sub_accounts ADD COLUMN permManageAccounting INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE hidden_sub_accounts ADD COLUMN permApproveDiscounts INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE hidden_sub_accounts ADD COLUMN permDirectChat INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE hidden_sub_accounts ADD COLUMN isActiveEmployee INTEGER NOT NULL DEFAULT 1")

                db.execSQL("ALTER TABLE project_tasks ADD COLUMN timingMode TEXT NOT NULL DEFAULT 'HOURS'")
                db.execSQL("ALTER TABLE project_tasks ADD COLUMN durationValue INTEGER NOT NULL DEFAULT 3")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gold_chat_database"
                )
                .addMigrations(MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28)
                .fallbackToDestructiveMigrationFrom(*(1..24).toList().toIntArray())
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

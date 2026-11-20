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
        DynamicExcelRowRecordEntity::class
    ],
    version = 27,
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

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gold_chat_database"
                )
                .addMigrations(MIGRATION_25_26, MIGRATION_26_27)
                .fallbackToDestructiveMigrationFrom(*(1..24).toList().toIntArray())
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.AdminConfigEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMigration25To28Test {

    @Test
    fun migrate25To28_preservesExistingRowsAndCreatesNewColumnsAndTables() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val dbName = "migration_25_28_test.db"
        context.deleteDatabase(dbName)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(25) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `invoices` (
                                `id` TEXT NOT NULL PRIMARY KEY,
                                `invoiceNumber` TEXT NOT NULL,
                                `title` TEXT NOT NULL,
                                `totalAmount` REAL NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `hashtag_accounts` (
                                `id` TEXT NOT NULL PRIMARY KEY,
                                `chatId` TEXT NOT NULL,
                                `accountId` TEXT NOT NULL,
                                `tag` TEXT NOT NULL,
                                `creatorId` TEXT NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `hashtag_entries` (
                                `id` TEXT NOT NULL PRIMARY KEY,
                                `hashtagId` TEXT NOT NULL,
                                `accountId` TEXT NOT NULL,
                                `description` TEXT NOT NULL,
                                `amount` REAL NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `merchant_products` (
                                `id` TEXT NOT NULL PRIMARY KEY,
                                `merchantId` TEXT NOT NULL,
                                `title` TEXT NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `hidden_sub_accounts` (
                                `id` TEXT NOT NULL PRIMARY KEY,
                                `username` TEXT NOT NULL,
                                `displayName` TEXT NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `project_tasks` (
                                `id` TEXT NOT NULL PRIMARY KEY,
                                `projectId` TEXT NOT NULL,
                                `projectName` TEXT NOT NULL,
                                `title` TEXT NOT NULL
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )

        val db = helper.writableDatabase
        db.execSQL("INSERT INTO invoices (id, invoiceNumber, title, totalAmount) VALUES ('inv-1', 'INV-001', 'سبيكة ذهب', 25000.0)")
        db.execSQL("INSERT INTO hashtag_accounts (id, chatId, accountId, tag, creatorId) VALUES ('ht-1', 'chat-1', 'acc-1', '#ذهب', 'u-1')")
        db.execSQL("INSERT INTO hashtag_entries (id, hashtagId, accountId, description, amount) VALUES ('he-1', 'ht-1', 'acc-1', 'دفعة', 5000.0)")
        db.execSQL("INSERT INTO merchant_products (id, merchantId, title) VALUES ('prod-1', 'm-1', 'خاتم عيار 21')")
        db.execSQL("INSERT INTO hidden_sub_accounts (id, username, displayName) VALUES ('sub-1', 'cashier_1', 'الكاشير')")
        db.execSQL("INSERT INTO project_tasks (id, projectId, projectName, title) VALUES ('task-1', 'proj-1', 'الورشة', 'جرد')")

        AppDatabase.MIGRATION_25_26.migrate(db)
        AppDatabase.MIGRATION_26_27.migrate(db)
        AppDatabase.MIGRATION_27_28.migrate(db)

        db.query("SELECT invoiceNumber, paidAmount, sourceType FROM invoices WHERE id = 'inv-1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("INV-001", c.getString(0))
            assertEquals(0.0, c.getDouble(1), 0.001)
            assertEquals("DIRECT", c.getString(2))
        }

        db.query("SELECT viewsCount FROM merchant_products WHERE id = 'prod-1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0, c.getInt(0))
        }

        db.query("SELECT department, salaryCurrency, isActiveEmployee FROM hidden_sub_accounts WHERE id = 'sub-1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("العمليات", c.getString(0))
            assertEquals("EGP", c.getString(1))
            assertEquals(1, c.getInt(2))
        }

        db.query("SELECT timingMode, durationValue FROM project_tasks WHERE id = 'task-1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("HOURS", c.getString(0))
            assertEquals(3, c.getInt(1))
        }

        val createdTables = mutableSetOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { c ->
            while (c.moveToNext()) {
                createdTables.add(c.getString(0))
            }
        }
        assertTrue(createdTables.contains("business_profiles"))
        assertTrue(createdTables.contains("payroll_adjustments"))
        assertTrue(createdTables.contains("employee_leaves"))

        helper.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun clearLegacyGeminiApiKey_scrubsStoredKeyWithoutTouchingOtherAdminConfigFields() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val roomDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = roomDb.adminConfigDao()
            dao.saveConfig(
                AdminConfigEntity(
                    id = 1,
                    appName = "OOZOX Gold",
                    geminiApiKey = "AIzaSyLegacyClientKeyShouldBeCleared",
                    geminiModelName = "gemini-2.5-flash"
                )
            )
            val before = dao.getConfigSync()
            assertNotNull(before?.geminiApiKey)

            dao.clearLegacyGeminiApiKey()

            val after = dao.getConfigSync()
            assertNotNull(after)
            assertNull(after?.geminiApiKey)
            assertEquals("OOZOX Gold", after?.appName)
            assertEquals("gemini-2.5-flash", after?.geminiModelName)
        } finally {
            roomDb.close()
        }
    }
}

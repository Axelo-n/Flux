package com.example.flux.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TransactionEntity::class, ParserRule::class, FinanceConfig::class, BudgetPolicy::class, BalanceAdjustment::class, NotificationRecord::class], version = 3, exportSchema = false)
abstract class TransactionDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun parserRuleDao(): ParserRuleDao
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: TransactionDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN refund REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN refundNote TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN source TEXT NOT NULL DEFAULT 'manual'")
                db.execSQL("ALTER TABLE parser_rules ADD COLUMN blocked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE parser_rules ADD COLUMN enabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("CREATE TABLE finance_config (id INTEGER NOT NULL PRIMARY KEY, startDay INTEGER NOT NULL, timezone TEXT NOT NULL, openingBalance INTEGER NOT NULL, activatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE budget_policies (effectiveDay INTEGER NOT NULL PRIMARY KEY, amounts TEXT NOT NULL)")
                db.execSQL("CREATE TABLE balance_adjustments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, target TEXT NOT NULL, amount INTEGER NOT NULL, note TEXT NOT NULL, date INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE notification_records (eventId TEXT NOT NULL PRIMARY KEY, fingerprint TEXT NOT NULL, postedAt INTEGER NOT NULL, title TEXT NOT NULL, text TEXT NOT NULL, status TEXT NOT NULL, detail TEXT NOT NULL, occurrenceId TEXT NOT NULL)")
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `parser_rules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `keyword` TEXT NOT NULL,
                        `targetCategory` TEXT NOT NULL,
                        `targetNote` TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): TransactionDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    TransactionDatabase::class.java,
                    "flux_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}

package com.example.flux

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TransactionEntity::class, ParserRule::class], version = 2, exportSchema = false)
abstract class TransactionDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun parserRuleDao(): ParserRuleDao

    companion object {
        @Volatile
        private var INSTANCE: TransactionDatabase? = null

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
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TransactionDatabase::class.java,
                    "flux_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
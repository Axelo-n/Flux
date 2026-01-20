package com.example.flux

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Pastikan TransactionEntity sudah ada di project kamu
@Database(entities = [TransactionEntity::class], version = 1, exportSchema = false)
abstract class TransactionDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: TransactionDatabase? = null

        // INI FUNGSI YANG DICARI SAMA SERVICE KAMU
        fun getDatabase(context: Context): TransactionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TransactionDatabase::class.java,
                    "flux_database"
                )
                    // .fallbackToDestructiveMigration() // Buka komen ini kalau nanti error version mismatch
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
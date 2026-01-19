package com.example.flux

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Daftar semua tabel (Entity) disini
@Database(entities = [TransactionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // Pintu masuk ke DAO
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Fungsi buat bikin database (Singleton biar hemat memori)
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flux_database" // Nama file database di HP
                )
                    // .fallbackToDestructiveMigration() // Uncoment ini kalau males migrasi pas nambah kolom (data bakal riset)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
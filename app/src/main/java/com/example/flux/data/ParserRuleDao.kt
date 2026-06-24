package com.example.flux.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ParserRuleDao {
    @Query("SELECT * FROM parser_rules ORDER BY id DESC")
    fun getAllRules(): Flow<List<ParserRule>>

    @Query("SELECT * FROM parser_rules")
    suspend fun getAllRulesSync(): List<ParserRule>

    @Insert
    suspend fun insert(rule: ParserRule)

    @Delete
    suspend fun delete(rule: ParserRule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<ParserRule>)

    @Query("DELETE FROM parser_rules")
    suspend fun clearAll()
}

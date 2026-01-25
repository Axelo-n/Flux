package com.example.flux

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "parser_rules")
data class ParserRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val keyword: String,
    val targetCategory: String,
    val targetNote: String? = null
)

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
}
package com.example.flux.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parser_rules")
data class ParserRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val keyword: String,
    val targetCategory: String,
    val targetNote: String? = null,
    val blocked: Boolean = false,
    val enabled: Boolean = true
)

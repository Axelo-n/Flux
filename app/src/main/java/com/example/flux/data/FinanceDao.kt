package com.example.flux.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM finance_config WHERE id = 1")
    fun observeConfig(): Flow<FinanceConfig?>
    @Query("SELECT * FROM finance_config WHERE id = 1")
    suspend fun config(): FinanceConfig?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: FinanceConfig)
    @Query("SELECT * FROM budget_policies ORDER BY effectiveDay")
    fun observePolicies(): Flow<List<BudgetPolicy>>
    @Query("SELECT * FROM budget_policies ORDER BY effectiveDay")
    suspend fun policies(): List<BudgetPolicy>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addPolicy(policy: BudgetPolicy)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFuturePolicy(policy: BudgetPolicy)
    @Query("DELETE FROM budget_policies WHERE effectiveDay = :day")
    suspend fun deletePolicy(day: Long)
    @Query("SELECT * FROM balance_adjustments ORDER BY date DESC, id DESC")
    fun observeAdjustments(): Flow<List<BalanceAdjustment>>
    @Query("SELECT * FROM balance_adjustments")
    suspend fun adjustments(): List<BalanceAdjustment>
    @Insert
    suspend fun addAdjustment(adjustment: BalanceAdjustment)
    @Delete
    suspend fun deleteAdjustment(adjustment: BalanceAdjustment)
    @Query("SELECT * FROM notification_records ORDER BY postedAt DESC LIMIT 100")
    fun observeNotifications(): Flow<List<NotificationRecord>>
    @Query("SELECT COUNT(*) FROM notification_records WHERE eventId = :eventId")
    suspend fun hasEvent(eventId: String): Int
    @Query("SELECT COUNT(*) FROM notification_records WHERE fingerprint = :fingerprint AND status = 'Tercatat' AND ABS(postedAt - :postedAt) <= 120000")
    suspend fun similarRecorded(fingerprint: String, postedAt: Long): Int
    @Query("SELECT COUNT(*) FROM notification_records WHERE occurrenceId = :occurrenceId AND status = 'Tercatat'")
    suspend fun recordedOccurrence(occurrenceId: String): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addNotification(record: NotificationRecord): Long
    @Query("DELETE FROM finance_config") suspend fun clearConfig()
    @Query("DELETE FROM budget_policies") suspend fun clearPolicies()
    @Query("DELETE FROM balance_adjustments") suspend fun clearAdjustments()
    @Query("DELETE FROM notification_records") suspend fun clearNotifications()
}

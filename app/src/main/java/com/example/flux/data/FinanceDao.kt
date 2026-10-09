package com.example.flux.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM monthly_pockets ORDER BY id")
    fun observeMonthlyPockets(): Flow<List<MonthlyPocket>>
    @Query("SELECT * FROM monthly_pockets ORDER BY id")
    suspend fun monthlyPockets(): List<MonthlyPocket>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMonthlyPocket(pocket: MonthlyPocket)
    @Delete suspend fun deleteMonthlyPocket(pocket: MonthlyPocket)
    @Query("DELETE FROM monthly_pockets") suspend fun clearMonthlyPockets()

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
    @Query("SELECT * FROM notification_records WHERE eventId = :id")
    suspend fun notification(id: String): NotificationRecord?
    @Query("SELECT * FROM notification_records WHERE status = 'Cashback perlu ditautkan' OR (status = 'Perlu diperiksa' AND (LOWER(title) LIKE '%cashback%' OR LOWER(text) LIKE '%cashback%')) ORDER BY postedAt DESC")
    fun observePendingCashbacks(): Flow<List<NotificationRecord>>
    @Query("UPDATE notification_records SET status = 'Diabaikan', detail = 'Cashback duplikat sudah diproses' WHERE eventId != :id AND status != 'Tercatat' AND (occurrenceId = :occurrence OR (fingerprint = :fingerprint AND ABS(postedAt - :time) <= 120000))")
    suspend fun closeCashbackDuplicates(id: String, occurrence: String, fingerprint: String, time: Long)
    @Update
    suspend fun updateNotification(record: NotificationRecord)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addNotification(record: NotificationRecord): Long
    @Query("DELETE FROM finance_config") suspend fun clearConfig()
    @Query("DELETE FROM budget_policies") suspend fun clearPolicies()
    @Query("DELETE FROM balance_adjustments") suspend fun clearAdjustments()
    @Query("DELETE FROM notification_records") suspend fun clearNotifications()
}

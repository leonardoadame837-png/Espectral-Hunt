package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpectraDao {
    @Query("SELECT * FROM audit_reports ORDER BY timestamp DESC")
    fun getAllAuditReports(): Flow<List<AuditReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditReport(report: AuditReportEntity): Long

    @Query("DELETE FROM audit_reports WHERE id = :id")
    suspend fun deleteAuditReport(id: Long)

    @Query("DELETE FROM audit_reports")
    suspend fun clearAllAuditReports()

    @Query("SELECT * FROM intercepted_signals ORDER BY capturedAt DESC")
    fun getAllInterceptedSignals(): Flow<List<InterceptedSignalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterceptedSignal(signal: InterceptedSignalEntity): Long

    @Query("DELETE FROM intercepted_signals WHERE id = :id")
    suspend fun deleteInterceptedSignal(id: Long)

    @Query("DELETE FROM intercepted_signals")
    suspend fun clearAllSignals()

    // Threat Signatures Database
    @Query("SELECT * FROM threat_signatures ORDER BY threatLevel DESC, name ASC")
    fun getAllThreatSignatures(): Flow<List<ThreatSignatureEntity>>

    @Query("SELECT * FROM threat_signatures WHERE isEnabled = 1")
    suspend fun getEnabledThreatSignaturesSync(): List<ThreatSignatureEntity>

    @Query("SELECT COUNT(*) FROM threat_signatures")
    suspend fun getSignatureCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreatSignature(signature: ThreatSignatureEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreatSignatures(signatures: List<ThreatSignatureEntity>)

    @Query("UPDATE threat_signatures SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun toggleSignatureEnabled(id: String, isEnabled: Boolean)

    @Query("DELETE FROM threat_signatures WHERE id = :id")
    suspend fun deleteThreatSignature(id: String)

    // Threat Alerts
    @Query("SELECT * FROM threat_alerts ORDER BY timestamp DESC")
    fun getAllThreatAlerts(): Flow<List<ThreatAlertEntity>>

    @Query("SELECT * FROM threat_alerts WHERE status = 'ACTIVE' ORDER BY timestamp DESC")
    fun getActiveThreatAlerts(): Flow<List<ThreatAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreatAlert(alert: ThreatAlertEntity): Long

    @Query("UPDATE threat_alerts SET status = :status WHERE id = :id")
    suspend fun updateThreatAlertStatus(id: Long, status: String)

    @Query("DELETE FROM threat_alerts WHERE id = :id")
    suspend fun deleteThreatAlert(id: Long)

    @Query("DELETE FROM threat_alerts")
    suspend fun clearAllThreatAlerts()
}

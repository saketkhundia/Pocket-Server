package com.saketkhundia.pocketserver.data.repository

import com.saketkhundia.pocketserver.data.database.ServerLogDao
import com.saketkhundia.pocketserver.data.database.ServerLogEntity
import com.saketkhundia.pocketserver.domain.model.LogEntry
import com.saketkhundia.pocketserver.domain.model.ServerState
import com.saketkhundia.pocketserver.domain.model.ServerStats
import com.saketkhundia.pocketserver.domain.model.ServerStatus
import com.saketkhundia.pocketserver.domain.repository.ServerStateRepository
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private fun LogEntry.toEntity() = ServerLogEntity(0, timestampMs, clientIp, method, path, status, message)
private fun ServerLogEntity.toModel() = LogEntry(id, timestampMs, clientIp, method, path, status, message)

/** Max UI-visible stats publish rate: 5 Hz. Totals stay exact (see below). */
private const val STATS_FLUSH_MS = 200L
/** trim() is a full-table DELETE..NOT IN — enforce the cap every N inserts, not per insert. */
private const val TRIM_EVERY_INSERTS = 50

/** In-memory state + Room-backed log persistence. No sensitive data is logged. */
class AppServerStateRepository(
    private val dao: ServerLogDao?,
    private val scope: CoroutineScope
) : ServerStateRepository {
    private val _state = MutableStateFlow(ServerState(status = ServerStatus.STOPPED))
    private val _stats = MutableStateFlow(ServerStats())
    private val _memLogs = MutableStateFlow<List<LogEntry>>(emptyList())

    override val state: Flow<ServerState> = _state.asStateFlow()
    override val stats: Flow<ServerStats> = _stats.asStateFlow()
    override val logs: Flow<List<LogEntry>> = combine(
        _memLogs,
        dao?.observe() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    ) { mem, persisted ->
        // Merge: in-memory tail + persisted DB. The same event exists in both
        // (mem id 0, then re-arrives with a DB id), so dedupe on content,
        // newest first, cap 300.
        (mem + persisted.map { it.toModel() })
            .distinctBy { e -> e.timestampMs to e.method to e.path to e.status to e.clientIp }
            .sortedByDescending { it.timestampMs }
            .take(300)
    }

    override suspend fun setState(s: ServerState) { _state.value = s }

    // ── Stats coalescing ─────────────────────────────────────────
    // onRequest fires per HTTP request, so a photo-gallery load (hundreds of
    // thumbnail requests in seconds) would publish hundreds of _stats
    // emissions, each recomposing every stats/logs collector on Main.
    // Transforms are composed (totals stay bit-exact) but _stats publishes at
    // most every 200ms — up to 20x fewer recomposition cascades per burst.
    private val statsMutex = Mutex()
    private var pendingStats: ((ServerStats) -> ServerStats)? = null
    private var lastStatsFlushMs = 0L
    private var statsFlushScheduled = false

    override suspend fun updateStats(t: (ServerStats) -> ServerStats) {
        var immediate: ((ServerStats) -> ServerStats)? = null
        var scheduleDelayMs = 0L
        statsMutex.withLock {
            val prev = pendingStats
            pendingStats = if (prev == null) t else ({ s: ServerStats -> t(prev(s)) })
            val now = System.currentTimeMillis()
            if (now - lastStatsFlushMs >= STATS_FLUSH_MS) {
                immediate = pendingStats
                pendingStats = null
                lastStatsFlushMs = now
            } else if (!statsFlushScheduled) {
                statsFlushScheduled = true
                scheduleDelayMs = STATS_FLUSH_MS - (now - lastStatsFlushMs)
            }
        }
        immediate?.let { _stats.update(it) }
        if (scheduleDelayMs > 0) {
            val d = scheduleDelayMs
            scope.launch {
                delay(d)
                val toFlush: ((ServerStats) -> ServerStats)? = statsMutex.withLock {
                    val p = pendingStats
                    pendingStats = null
                    lastStatsFlushMs = System.currentTimeMillis()
                    statsFlushScheduled = false
                    p
                }
                toFlush?.let { _stats.update(it) }
            }
        }
    }

    private val logInsertCount = AtomicInteger(0)

    override suspend fun addLog(e: LogEntry) {
        // Never persist tokens/passwords — callers must sanitize
        _memLogs.update { (listOf(e) + it).take(300) }
        try {
            dao?.insert(e.toEntity())
            // trim() scans the whole table; per-insert during bursts is heavy
            // write amplification. Cap still enforced every 50 inserts, and
            // observe() LIMIT 300 keeps the UI capped regardless.
            if (logInsertCount.incrementAndGet() % TRIM_EVERY_INSERTS == 0) dao?.trim()
        } catch (_: Exception) { /* logging must never crash server */ }
    }
    override suspend fun clearLogs() {
        _memLogs.value = emptyList()
        try { dao?.clear() } catch (_: Exception) {}
    }
}

package com.saketkhundia.pocketserver.server.mdns

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.saketkhundia.pocketserver.BuildConfig
import com.saketkhundia.pocketserver.domain.model.MdnsStatus
import com.saketkhundia.pocketserver.network.LocalIpProvider
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

const val MDNS_SERVICE_TYPE = "_http._tcp.local."
const val MDNS_SERVICE_NAME = "Pocket Server"
const val MDNS_BASE_HOST = "pocketserver"
private const val MDNS_VERIFY_TIMEOUT_MS = 5_000L
private const val MDNS_REGISTER_TIMEOUT_MS = 20_000L
private const val MDNS_LOCK_TAG = "pocketserver-mdns"

/** http://<host>.local[:port] — port omitted only for :80. Tolerates FQDN input. */fun mdnsHostnameUrl(host: String, port: Int): String {
    var h = stripTrailingDot(host.trim()).lowercase()
    h = h.removeSuffix(".local")
    require(h.isNotEmpty()) { "empty mDNS host" }
    return if (port == 80) "http://$h.local" else "http://$h.local:$port"
}

fun stripTrailingDot(name: String): String = name.removeSuffix(".")

/**
 * On-device preview URL. Android has no mDNS resolver, so a .local name can
 * NEVER load in an on-device browser (NXDOMAIN by design) — "Open Server"
 * must use loopback. The server binds 0.0.0.0, so this always reaches it,
 * even with Wi-Fi off.
 */
fun loopbackUrl(port: Int): String = "http://127.0.0.1:$port"
/**
 * Sanitizes a candidate mDNS host label: lowercase alphanumerics + hyphen,
 * max 63 chars (RFC 1035). Falls back to [MDNS_BASE_HOST] when empty.
 */
fun sanitizeMdnsHost(raw: String): String {
    val cleaned = raw.lowercase()
        .map { c -> if (c in 'a'..'z' || c in '0'..'9' || c == '-') c else '-' }
        .joinToString("")
        .split('-')
        .filter { it.isNotEmpty() }
        .joinToString("-")
        .take(63)
    return cleaned.ifBlank { MDNS_BASE_HOST }
}

private fun d(msg: String) {
    if (BuildConfig.DEBUG) Log.d("MdnsManager", msg)
}

/**
 * Local-network hostname advertiser (JmDNS mDNS responder).
 *
 * Why JmDNS and not Android NSD: NsdManager only registers a *service
 * instance* under the device's own mDNS hostname — it cannot claim a stable
 * `pocketserver.local` name. JmDNS runs a real responder that announces and
 * DEFENDS A records for our hostname, renaming on conflict
 * (pocketserver-2, …). The UI only ever shows the VERIFIED defended name.
 *
 * Threading: everything blocking runs on Dispatchers.IO. Registration
 * failure NEVER throws out — it reports UNAVAILABLE so the IP fallback
 * keeps working. All public methods are idempotent and mutex-guarded.
 */
class MdnsManager(
    private val context: Context,
    private val appScope: CoroutineScope
) {
    private val mutex = Mutex()
    private var jmdns: JmDNS? = null
    private var lock: WifiManager.MulticastLock? = null
    private var boundPort: Int? = null
    private var boundIp: String? = null

    private val _status = MutableStateFlow(MdnsStatus.IDLE)
    val status: StateFlow<MdnsStatus> = _status.asStateFlow()

    /** Verified friendly URL, or null unless AVAILABLE. */
    private val _hostnameUrl = MutableStateFlow<String?>(null)
    val hostnameUrl: StateFlow<String?> = _hostnameUrl.asStateFlow()

    /**
     * Advertise `http://<host>.local:<port>`. Safe to call repeatedly;
     * re-registering the same port+IP is a no-op. [bindIp] pins the exact
     * interface address (the one shown in the UI) so the A record can never
     * disagree with the working IP URL. Returns the verified URL or
     * null when mDNS is unusable on this network (server keeps running on IP).
     */
    suspend fun register(port: Int, bindIp: String? = null, baseHost: String = MDNS_BASE_HOST): String? = mutex.withLock {
        if (jmdns != null && boundPort == port && (bindIp == null || bindIp == boundIp) &&
            _status.value == MdnsStatus.AVAILABLE
        ) {
            return _hostnameUrl.value
        }
        unregisterLocked()
        _status.value = MdnsStatus.REGISTERING
        _hostnameUrl.value = null
        d("mDNS registration started (port=$port ip=${bindIp ?: "auto"})")

        val url = withTimeoutOrNull(MDNS_REGISTER_TIMEOUT_MS) {
            withContext(Dispatchers.IO) { registerInternal(port, sanitizeMdnsHost(baseHost), bindIp) }
        }
        if (url != null) {
            _hostnameUrl.value = url
            _status.value = MdnsStatus.AVAILABLE
            boundPort = port
            d("mDNS registration successful: $url")
        } else {
            // Honest teardown: never advertise what we couldn't verify.
            unregisterLocked()
            _status.value = MdnsStatus.UNAVAILABLE
            d("mDNS registration failed — IP fallback only")
        }
        url
    }

    /** Restart advertisement (network/IP change). No-op when not registered. */
    suspend fun restart(port: Int, bindIp: String? = null): String? = mutex.withLock {
        if (jmdns == null && _status.value == MdnsStatus.IDLE) return null
        unregisterLocked()
        _status.value = MdnsStatus.REGISTERING
        _hostnameUrl.value = null
        d("mDNS re-registering after network change (port=$port ip=${bindIp ?: "auto"})")
        val url = withTimeoutOrNull(MDNS_REGISTER_TIMEOUT_MS) {
            withContext(Dispatchers.IO) { registerInternal(port, MDNS_BASE_HOST, bindIp) }
        }
        if (url != null) {
            _hostnameUrl.value = url
            _status.value = MdnsStatus.AVAILABLE
            boundPort = port
            d("mDNS registration successful: $url")
        } else {
            unregisterLocked()
            _status.value = MdnsStatus.UNAVAILABLE
            d("mDNS re-registration failed — IP fallback only")
        }
        url
    }

    suspend fun unregister() = mutex.withLock { unregisterLocked() }

    /** Fire-and-forget cleanup for onDestroy paths (uses appScope, never Main). */
    fun unregisterAsync() {
        appScope.launch(Dispatchers.IO) {
            try { unregister() } catch (_: Exception) {}
        }
    }

    private fun unregisterLocked() {
        try { jmdns?.unregisterAllServices() } catch (_: Exception) {}
        try { jmdns?.close() } catch (_: Exception) {}
        jmdns = null
        boundPort = null
        boundIp = null
        try {
            if (lock?.isHeld == true) lock?.release()
        } catch (_: Exception) {}
        lock = null
        if (_status.value != MdnsStatus.IDLE) {
            _status.value = MdnsStatus.IDLE
            _hostnameUrl.value = null
            d("mDNS service unregistered")
        }
    }

    /**
     * Blocking mDNS work — MUST run on Dispatchers.IO. Binds the exact
     * [bindIp] when given (the working UI address); otherwise enumerates.
     * Returns the verified friendly URL, or null. Never throws.
     */
    private fun registerInternal(port: Int, host: String, bindIp: String?): String? {
        var created: JmDNS? = null
        var held: WifiManager.MulticastLock? = null
        try {
            val ip = bindIp?.takeIf { it.isNotBlank() }
                ?: LocalIpProvider.getLocalIpv4(context) ?: run {
                    d("mDNS skipped: no local IP")
                    return null
                }
            val wifi = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as? WifiManager
            held = wifi?.createMulticastLock(MDNS_LOCK_TAG)?.apply {
                setReferenceCounted(true)
                acquire()
            }
            val addr = InetAddress.getByName(ip)
            created = JmDNS.create(addr, host)
            val info = ServiceInfo.create(
                MDNS_SERVICE_TYPE, MDNS_SERVICE_NAME, port, 0, 0,
                mapOf("path" to "/")
            )
            created.registerService(info)
            // Verify our own announcement (catches broken multicast RX).
            // Note: JmDNS.create() already probed/defended the hostname, so
            // one lookup suffices — a second would only double worst-case
            // time while holding the mutex and stall stop().
            val resolved = created.getServiceInfo(MDNS_SERVICE_TYPE, info.name, MDNS_VERIFY_TIMEOUT_MS)
            val fqdn = (resolved?.server ?: created.hostName)?.let(::stripTrailingDot)
            if (fqdn.isNullOrBlank()) {
                d("mDNS unverifiable: no server name in announcement")
                try { created.unregisterAllServices() } catch (_: Exception) {}
                try { created.close() } catch (_: Exception) {}
                try { if (held?.isHeld == true) held?.release() } catch (_: Exception) {}
                return null
            }
            if (!fqdn.equals("$host.local", ignoreCase = true)) {
                d("mDNS hostname conflict resolved as $fqdn")
            }
            jmdns = created
            lock = held
            created = null // ownership transferred — don't close below
            held = null
            boundIp = ip
            return mdnsHostnameUrl(fqdn.removeSuffix(".local"), port)
        } catch (e: Exception) {
            d("mDNS error: ${e.javaClass.simpleName}")
            return null
        } finally {
            try { created?.close() } catch (_: Exception) {}
            try { if (held?.isHeld == true) held?.release() } catch (_: Exception) {}
        }
    }
}

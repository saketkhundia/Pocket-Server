package com.saketkhundia.pocketserver.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import java.net.Inet4Address
import java.net.NetworkInterface

object LocalIpProvider {

    /** Returns the best site-local IPv4, or null. Tries ConnectivityManager → WifiManager → NetworkInterface. */
    fun getLocalIpv4(ctx: Context? = null): String? {
        // 1) ConnectivityManager LinkProperties (most reliable on modern Android)
        try {
            if (ctx != null) {
                val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val net = cm.activeNetwork
                if (net != null) {
                    val props = cm.getLinkProperties(net)
                    if (props != null) {
                        for (addr in props.linkAddresses) {
                            val inet = addr.address
                            if (inet is Inet4Address && !inet.isLoopbackAddress && !inet.isLinkLocalAddress) {
                                val host = inet.hostAddress ?: continue
                                // Prefer site-local (192.168/10./172.16) but accept any valid private
                                if (inet.isSiteLocalAddress) return host
                            }
                        }
                        // Second pass: any non-loopback IPv4 from active network
                        for (addr in props.linkAddresses) {
                            val inet = addr.address
                            if (inet is Inet4Address && !inet.isLoopbackAddress && !inet.isLinkLocalAddress) {
                                return inet.hostAddress
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 2) WifiManager as fallback (deprecated ipAddress still useful)
        try {
            if (ctx != null) {
                @Suppress("DEPRECATION")
                val wifi = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                @Suppress("DEPRECATION")
                val ipInt = wifi?.connectionInfo?.ipAddress ?: 0
                if (ipInt != 0) {
                    // ipAddress is little-endian
                    val ip = String.format(
                        "%d.%d.%d.%d",
                        ipInt and 0xff,
                        ipInt shr 8 and 0xff,
                        ipInt shr 16 and 0xff,
                        ipInt shr 24 and 0xff
                    )
                    if (ip != "0.0.0.0" && !ip.startsWith("127.")) return ip
                }
            }
        } catch (_: Exception) {}

        // 3) NetworkInterface enumeration (works on all devices, handles eth/wlan/rmnet filtering)
        try {
            val ifaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: return null
            val candidates = mutableListOf<Pair<Int, String>>()
            for (iface in ifaces) {
                if (!iface.isUp || iface.isLoopback || iface.isVirtual) continue
                val name = iface.name.lowercase()
                // Skip cellular rmnet if we have wifi, but keep as last resort
                val priority = when {
                    name.startsWith("wlan") -> 0
                    name.startsWith("eth") -> 1
                    name.startsWith("ap") -> 2  // hotspot
                    name.startsWith("p2p") -> 4
                    name.startsWith("rmnet") -> 10 // cellular last resort
                    else -> 5
                }
                for (addr in iface.inetAddresses.toList()) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        if (addr.isSiteLocalAddress) {
                            candidates.add(priority to host)
                        } else if (!addr.isLinkLocalAddress && host != "0.0.0.0") {
                            // Accept non-link-local even if not site-local (e.g., 10.x via tethering)
                            candidates.add((priority + 5) to host)
                        }
                    }
                }
            }
            // Also log all candidates for debugging
            val sorted = candidates.sortedBy { it.first }
            return sorted.firstOrNull()?.second
        } catch (_: Exception) { return null }
    }

    /** Returns all candidate IPv4s for diagnostic UI. */
    fun getAllCandidates(ctx: Context? = null): List<String> {
        val out = mutableListOf<String>()
        try {
            if (ctx != null) {
                val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                cm.getLinkProperties(cm.activeNetwork)?.linkAddresses?.forEach {
                    val inet = it.address
                    if (inet is Inet4Address && !inet.isLoopbackAddress) inet.hostAddress?.let { h -> out.add("linkProps:$h") }
                }
            }
        } catch (_: Exception) {}
        try {
            if (ctx != null) {
                @Suppress("DEPRECATION")
                val wifi = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                @Suppress("DEPRECATION")
                val ipInt = wifi?.connectionInfo?.ipAddress ?: 0
                if (ipInt != 0) {
                    val ip = String.format("%d.%d.%d.%d", ipInt and 0xff, ipInt shr 8 and 0xff, ipInt shr 16 and 0xff, ipInt shr 24 and 0xff)
                    if (ip != "0.0.0.0") out.add("wifiMgr:$ip")
                }
            }
        } catch (_: Exception) {}
        try {
            NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { iface ->
                if (!iface.isUp || iface.isLoopback) return@forEach
                for (addr in iface.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) addr.hostAddress?.let { out.add("${iface.name}:$it") }
                }
            }
        } catch (_: Exception) {}
        return out
    }

    fun describeNetwork(ctx: Context): String {
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val net = cm.activeNetwork ?: return "No network"
            val caps = cm.getNetworkCapabilities(net) ?: return "No network"
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data (may not be reachable from laptop)"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN (may block local access)"
                else -> "Local network"
            }
        } catch (_: Exception) { "Unknown" }
    }

    fun hasLocalNetwork(ctx: Context): Boolean {
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        } catch (_: Exception) { false }
    }

    /** Quick reachability hint for troubleshooting. */
    fun troubleshootingHint(ctx: Context): String {
        val desc = describeNetwork(ctx)
        return when {
            desc == "No network" -> "Connect phone to same Wi-Fi as laptop. Enable Wi-Fi, disable Mobile data."
            desc.contains("Mobile data") -> "Phone is on mobile data. Switch to Wi-Fi — mobile IPs aren't reachable on LAN."
            desc.contains("VPN") -> "VPN is active. Disable VPN or split-tunnel — it blocks local HTTP."
            else -> "Ensure laptop and phone are on same Wi-Fi, AP isolation is OFF, and firewall allows port."
        }
    }
}

package com.example.carebrief.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.carebrief.core.ui.components.OfflineIndicator

/** Pure copy mapping. The app is local-first, so offline is reassurance, not an error. */
fun offlineBannerText(online: Boolean): String = if (online) {
    "Offline-ready · demo data stored on device"
} else {
    "You're offline · everything keeps working"
}

private fun currentOnline(context: Context): Boolean {
    val manager = context.getSystemService(ConnectivityManager::class.java) ?: return true
    val network = manager.activeNetwork ?: return false
    val caps = manager.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

/** Live connectivity state via NetworkCallback. Starts online, corrects on first callback. */
@Composable
fun rememberOnlineState(): Boolean {
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    var online by remember { mutableStateOf(currentOnline(appContext)) }
    DisposableEffect(appContext) {
        val manager = appContext.getSystemService(ConnectivityManager::class.java)
        if (manager == null) {
            onDispose {}
        } else {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    online = currentOnline(appContext)
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    online = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                }

                override fun onLost(network: Network) {
                    online = currentOnline(appContext)
                }
            }
            manager.registerDefaultNetworkCallback(callback)
            onDispose { manager.unregisterNetworkCallback(callback) }
        }
    }
    return online
}

/** State-aware offline banner. Drop-in replacement for the static OfflineIndicator. */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    val online = rememberOnlineState()
    OfflineIndicator(text = offlineBannerText(online), modifier = modifier)
}

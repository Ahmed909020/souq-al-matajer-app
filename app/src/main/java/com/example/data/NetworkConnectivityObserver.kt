package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkConnectivityObserver(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkIsOnlineNow())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = checkIsOnlineNow()
        }

        override fun onLost(network: Network) {
            _isOnline.value = checkIsOnlineNow()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            _isOnline.value = hasInternetCapability(networkCapabilities)
        }
    }

    init {
        try {
            connectivityManager?.registerDefaultNetworkCallback(networkCallback)
        } catch (_: Exception) {
            // Keep initial check value if callback registration is unavailable in test environments
        }
    }

    fun checkIsOnlineNow(): Boolean {
        val cm = connectivityManager ?: return true
        return try {
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            hasInternetCapability(capabilities)
        } catch (_: Exception) {
            false
        }
    }

    private fun hasInternetCapability(capabilities: NetworkCapabilities): Boolean {
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun unregister() {
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }
    }
}

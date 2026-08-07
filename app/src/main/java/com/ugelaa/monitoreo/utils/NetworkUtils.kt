package com.ugelaa.monitoreo.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// Esta función se queda "escuchando" los cambios de internet del teléfono
fun observeConnectivityAsFlow(context: Context): Flow<Boolean> = callbackFlow {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { trySend(true) }
        override fun onLost(network: Network) { trySend(false) }
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            trySend(hasInternet)
        }
    }

    // Leemos el estado la primera vez que se abre
    val currentNetwork = connectivityManager.activeNetwork
    val currentCapabilities = connectivityManager.getNetworkCapabilities(currentNetwork)
    val isCurrentlyConnected = currentCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    trySend(isCurrentlyConnected)

    // Nos registramos para seguir escuchando
    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
    connectivityManager.registerNetworkCallback(request, callback)

    // Cerramos el canal si la pantalla desaparece
    awaitClose {
        connectivityManager.unregisterNetworkCallback(callback)
    }
}
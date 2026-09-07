package com.example.meuapp.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

/**
 * Procura na rede local o serviço mDNS que o Android anuncia quando a tela
 * "Depuração sem fio → Parear dispositivo com código de pareamento" está aberta
 * (serviço _adb-tls-pairing._tcp). Assim o app descobre IP e porta sozinho,
 * sem o usuário precisar digitá-los manualmente.
 */
class AdbPairingDiscovery(context: Context) {

    private val nsdManager =
        context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun procurar(
        aoEncontrar: (host: String, porta: Int) -> Unit,
        aoFalhar: (mensagem: String) -> Unit
    ) {
        pararBusca()

        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                aoFalhar("Falha ao resolver o serviço encontrado (código $errorCode)")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val host = serviceInfo.host?.hostAddress
                if (host != null) {
                    aoEncontrar(host, serviceInfo.port)
                } else {
                    aoFalhar("Serviço encontrado, mas sem endereço IP válido")
                }
            }
        }

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Busca iniciada: $serviceType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType.contains("_adb-tls-pairing")) {
                    nsdManager.resolveService(serviceInfo, resolveListener)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Serviço perdido: ${serviceInfo.serviceName}")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Busca parada: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                aoFalhar("Não foi possível iniciar a busca (código $errorCode)")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.d(TAG, "Falha ao parar a busca (código $errorCode)")
            }
        }

        discoveryListener = listener
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    fun pararBusca() {
        val listener = discoveryListener ?: return
        try {
            nsdManager.stopServiceDiscovery(listener)
        } catch (e: IllegalArgumentException) {
            Log.d(TAG, "Busca já estava parada")
        }
        discoveryListener = null
    }

    companion object {
        private const val TAG = "AdbPairingDiscovery"
        private const val SERVICE_TYPE = "_adb-tls-pairing._tcp."
    }
}

package com.example.meuapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.meuapp.adb.AdbConnectionManager
import com.example.meuapp.adb.AdbPairingDiscovery
import com.example.meuapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var pairingDiscovery: AdbPairingDiscovery

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            binding.textView.text = getString(R.string.hello_clicked)
        }

        pairingDiscovery = AdbPairingDiscovery(this)
        configurarTelaDePareamento()
    }

    override fun onDestroy() {
        pairingDiscovery.pararBusca()
        super.onDestroy()
    }

    private fun configurarTelaDePareamento() {
        binding.buttonProcurar.setOnClickListener {
            binding.textViewStatusPareamento.text = getString(R.string.status_procurando)
            pairingDiscovery.procurar(
                aoEncontrar = { host, porta ->
                    runOnUiThread {
                        binding.editTextHost.setText(host)
                        binding.editTextPorta.setText(porta.toString())
                        binding.textViewStatusPareamento.text =
                            getString(R.string.status_encontrado, host, porta)
                    }
                },
                aoFalhar = {
                    runOnUiThread {
                        binding.textViewStatusPareamento.text =
                            getString(R.string.status_procurando_falhou)
                    }
                }
            )
        }

        binding.buttonParear.setOnClickListener {
            val host = binding.editTextHost.text.toString().trim()
            val portaTexto = binding.editTextPorta.text.toString().trim()
            val codigo = binding.editTextCodigoPareamento.text.toString().trim()

            if (host.isEmpty() || portaTexto.isEmpty() || codigo.isEmpty()) {
                binding.textViewStatusPareamento.text =
                    getString(R.string.erro_preencha_ip_porta_codigo)
                return@setOnClickListener
            }

            val porta = portaTexto.toIntOrNull()
            if (porta == null) {
                binding.textViewStatusPareamento.text =
                    getString(R.string.erro_preencha_ip_porta_codigo)
                return@setOnClickListener
            }

            binding.textViewStatusPareamento.text = getString(R.string.status_pareando)

            Thread {
                try {
                    val pareado = AdbConnectionManager.getInstance().pair(host, porta, codigo)
                    runOnUiThread {
                        binding.textViewStatusPareamento.text = if (pareado) {
                            getString(R.string.status_pareado_sucesso)
                        } else {
                            getString(R.string.status_pareado_falha, "código ou dados incorretos")
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        binding.textViewStatusPareamento.text =
                            getString(R.string.status_pareado_falha, e.message ?: "erro desconhecido")
                    }
                }
            }.start()
        }
    }
}

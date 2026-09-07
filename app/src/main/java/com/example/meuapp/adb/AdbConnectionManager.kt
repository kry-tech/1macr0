package com.example.meuapp.adb

import android.os.Build
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import sun.security.x509.AlgorithmId
import sun.security.x509.CertificateAlgorithmId
import sun.security.x509.CertificateExtensions
import sun.security.x509.CertificateIssuerName
import sun.security.x509.CertificateSerialNumber
import sun.security.x509.CertificateSubjectName
import sun.security.x509.CertificateValidity
import sun.security.x509.CertificateVersion
import sun.security.x509.CertificateX509Key
import sun.security.x509.KeyIdentifier
import sun.security.x509.PrivateKeyUsageExtension
import sun.security.x509.SubjectKeyIdentifierExtension
import sun.security.x509.X500Name
import sun.security.x509.X509CertImpl
import sun.security.x509.X509CertInfo
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.util.Date
import java.util.Random

/**
 * Implementação concreta de AbsAdbConnectionManager (biblioteca libadb-android).
 *
 * Gera um par de chaves RSA + certificado autoassinado (exigido pelo protocolo
 * de autenticação do ADB) seguindo exatamente o exemplo da documentação oficial:
 * https://github.com/MuntashirAkon/libadb-android
 *
 * IMPORTANTE: se o nome do pacote da classe AbsAdbConnectionManager mudar em uma
 * versão futura da biblioteca, ajuste apenas o import acima — o restante do
 * arquivo não depende disso.
 */
class AdbConnectionManager private constructor() : AbsAdbConnectionManager() {

    private lateinit var chavePrivada: PrivateKey
    private lateinit var certificadoInterno: Certificate

    init {
        setApi(Build.VERSION.SDK_INT)
        gerarParDeChaves()
    }

    private fun gerarParDeChaves() {
        val tamanhoChave = 2048
        val geradorDeChaves = KeyPairGenerator.getInstance("RSA")
        geradorDeChaves.initialize(tamanhoChave, SecureRandom.getInstance("SHA1PRNG"))
        val parDeChaves = geradorDeChaves.generateKeyPair()
        val chavePublica: PublicKey = parDeChaves.public
        chavePrivada = parDeChaves.private

        val assunto = "CN=MeuApp"
        val nomeAlgoritmo = "SHA512withRSA"
        val dataExpiracao = System.currentTimeMillis() + 86_400_000L // 24 horas

        val extensoesCertificado = CertificateExtensions()
        extensoesCertificado.set(
            "SubjectKeyIdentifier",
            SubjectKeyIdentifierExtension(KeyIdentifier(chavePublica).identifier)
        )

        val x500Name = X500Name(assunto)
        val validoDe = Date()
        val validoAte = Date(dataExpiracao)
        extensoesCertificado.set("PrivateKeyUsage", PrivateKeyUsageExtension(validoDe, validoAte))
        val validadeCertificado = CertificateValidity(validoDe, validoAte)

        val infoCertificado = X509CertInfo()
        infoCertificado.set("version", CertificateVersion(2))
        infoCertificado.set(
            "serialNumber",
            CertificateSerialNumber(Random().nextInt() and Integer.MAX_VALUE)
        )
        infoCertificado.set("algorithmID", CertificateAlgorithmId(AlgorithmId.get(nomeAlgoritmo)))
        infoCertificado.set("subject", CertificateSubjectName(x500Name))
        infoCertificado.set("key", CertificateX509Key(chavePublica))
        infoCertificado.set("validity", validadeCertificado)
        infoCertificado.set("issuer", CertificateIssuerName(x500Name))
        infoCertificado.set("extensions", extensoesCertificado)

        val certificadoImpl = X509CertImpl(infoCertificado)
        certificadoImpl.sign(chavePrivada, nomeAlgoritmo)
        certificadoInterno = certificadoImpl
    }

    override fun getPrivateKey(): PrivateKey = chavePrivada

    override fun getCertificate(): Certificate = certificadoInterno

    override fun getDeviceName(): String = "MeuApp"

    companion object {
        @Volatile
        private var instancia: AdbConnectionManager? = null

        @JvmStatic
        fun getInstance(): AdbConnectionManager =
            instancia ?: synchronized(this) {
                instancia ?: AdbConnectionManager().also { instancia = it }
            }
    }
}

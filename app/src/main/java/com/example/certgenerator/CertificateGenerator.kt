package com.example.certgenerator

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.*
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.Date
import java.util.Calendar

object CertificateGenerator {
    
    fun generateSelfSignedCert(ipAddress: String): Triple<String, String, String> {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPair: KeyPair = keyPairGenerator.generateKeyPair()

        val subjectName = X500Name("C=CN, O=MySelfHost, CN=$ipAddress")
        val serialNumber = BigInteger(64, SecureRandom())
        
        // 10年有效期
        val notBefore = Date()
        val calendar = Calendar.getInstance()
        calendar.time = notBefore
        calendar.add(Calendar.YEAR, 10)
        val notAfter = calendar.time

        val certBuilder: JcaX509v3CertificateBuilder = JcaX509v3CertificateBuilder(
            subjectName, serialNumber, notBefore, notAfter, subjectName, keyPair.public
        )

        // 核心配置 - 对应 OpenSSL 配置
        certBuilder.addExtension(
            Extension.basicConstraints,
            true,
            BasicConstraints(true)
        )
        
        certBuilder.addExtension(
            Extension.keyUsage,
            true,
            KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyEncipherment or KeyUsage.keyCertSign)
        )
        
        certBuilder.addExtension(
            Extension.extendedKeyUsage,
            false,
            ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth)
        )
        
        val generalName = GeneralName(GeneralName.iPAddress, ipAddress)
        val generalNames = GeneralNames(generalName)
        certBuilder.addExtension(
            Extension.subjectAlternativeName,
            false,
            generalNames
        )

        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        val holder = certBuilder.build(signer)
        val certificate: X509Certificate = org.bouncycastle.cert.jcajce.JcaX509CertificateConverter()
            .getCertificate(holder)

        val certPem = convertToPem(certificate.encoded, "CERTIFICATE")
        val keyPem = convertToPem(keyPair.private.encoded, "PRIVATE KEY")
        val sha256 = calculateSHA256(certificate.encoded)

        return Triple(certPem, keyPem, sha256)
    }

    private fun convertToPem(encoded: ByteArray, type: String): String {
        val encoder = Base64.getMimeEncoder(64, byteArrayOf('\n'.toByte()))
        val base64Str = encoder.encodeToString(encoded)
        return "-----BEGIN $type-----\n$base64Str\n-----END $type-----"
    }

    fun calculateSHA256(data: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }.uppercase()
    }
}

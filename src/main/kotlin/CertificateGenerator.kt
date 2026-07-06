import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.*
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*

object CertificateGenerator {
    fun generateSelfSignedCert(ipAddress: String): Map<String, String> {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPair: KeyPair = keyPairGenerator.generateKeyPair()

        val subjectName = X500Name("C=CN, O=MySelfHost, CN=$ipAddress")
        val serialNumber = BigInteger(64, SecureRandom())
        val notBefore = Date()
        val notAfter = Date(notBefore.time + 365L * 24 * 60 * 60 * 1000)

        val certBuilder: X509v3CertificateBuilder = JcaX509v3CertificateBuilder(
            subjectName, serialNumber, notBefore, notAfter, subjectName, keyPair.public
        )

        certBuilder.addExtension(Extension.basicConstraints, true, BasicConstraints(true))
        certBuilder.addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyEncipherment or KeyUsage.keyCertSign))
        certBuilder.addExtension(Extension.extendedKeyUsage, false, ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth))

        val generalName = GeneralName(GeneralName.iPAddress, ipAddress)
        val generalNamesSequence = DERSequence(generalName)
        certBuilder.addExtension(Extension.subjectAnObject, false, generalNamesSequence)

        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        val holder = certBuilder.build(signer)
        val certificate: X509Certificate = JcaX509CertificateConverter().getCertificate(holder)

        return mapOf(
            "cert" to convertToPem(certificate.encoded, "CERTIFICATE"),
            "key" to convertToPem(keyPair.private.encoded, "PRIVATE KEY")
        )
    }

    private fun convertToPem(encoded: ByteArray, type: String): String {
        val encoder = Base64.getMimeEncoder(64, byteArrayOf('\n'.toByte()))
        val base64Str = encoder.encodeToString(encoded)
        return "-----BEGIN $type-----\n$base64Str\n-----END $type-----"
    }
}

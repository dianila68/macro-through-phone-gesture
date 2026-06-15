package io.github.dianila68.gesturemacro.android.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import io.github.dianila68.gesturemacro.core.security.IntegritySealer
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * ticket-023: Android Keystore–backed [IntegritySealer] implementation, quarantined
 * to the `.android.security` package. The [IntegritySealer] interface stays in
 * `core.security` (no android.* imports).
 *
 * Key never leaves the Keystore; only the MAC operation uses it (threat T5).
 */
class HmacSealer(private val key: SecretKey) : IntegritySealer {
    override fun seal(payload: String): String {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(key)
        return Base64.getEncoder().encodeToString(mac.doFinal(payload.toByteArray(Charsets.UTF_8)))
    }

    override fun verify(payload: String, sealValue: String?): Boolean {
        if (sealValue == null) return false
        val expected = seal(payload)
        return MessageDigest.isEqual(expected.toByteArray(Charsets.UTF_8), sealValue.toByteArray(Charsets.UTF_8))
    }

    companion object {
        const val ALGORITHM = "HmacSHA256"
    }
}

object KeystoreSealerFactory {
    private const val ALIAS = "macro_integrity_hmac"

    fun create(): IntegritySealer? = runCatching { HmacSealer(getOrCreateKey()) }.getOrNull()

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN).build(),
        )
        return generator.generateKey()
    }
}

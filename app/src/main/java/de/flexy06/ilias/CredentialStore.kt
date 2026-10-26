package de.flexy06.ilias

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class Credentials(val username: String, val password: String)

/**
 * Stores the ILIAS credentials on the device. The password is encrypted with an
 * AES-GCM key that lives in the Android Keystore and never leaves the secure hardware.
 */
class CredentialStore(context: Context) {
    private val prefs = context.getSharedPreferences("credentials", Context.MODE_PRIVATE)

    fun load(): Credentials? {
        val user = prefs.getString(KEY_USER, null) ?: return null
        val blob = prefs.getString(KEY_PASS, null) ?: return null
        return runCatching { Credentials(user, decrypt(blob)) }.getOrNull()
    }

    fun save(c: Credentials) {
        prefs.edit()
            .putString(KEY_USER, c.username)
            .putString(KEY_PASS, encrypt(c.password))
            .apply()
    }

    fun username(): String? = prefs.getString(KEY_USER, null)

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + ct, Base64.NO_WRAP)
    }

    private fun decrypt(blob: String): String {
        val bytes = Base64.decode(blob, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, 12)
        val ct = bytes.copyOfRange(12, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ct), Charsets.UTF_8)
    }

    private companion object {
        const val ALIAS = "ilias_credentials"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_USER = "user"
        const val KEY_PASS = "pass"
    }
}

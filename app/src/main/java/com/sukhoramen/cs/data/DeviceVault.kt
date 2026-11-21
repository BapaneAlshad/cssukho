package com.sukhoramen.cs.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

sealed interface UnlockResult {
    data class Success(val token: String) : UnlockResult
    data class Wrong(val remaining: Int) : UnlockResult
    data class Cooldown(val until: Long) : UnlockResult
    data object Damaged : UnlockResult
}

/** The backend credential is encrypted with both the app PIN and a non-exportable device key. */
class DeviceVault(context: Context, private val now: () -> Long = System::currentTimeMillis) {
    private val prefs = context.applicationContext.getSharedPreferences("sukho_live_v1", Context.MODE_PRIVATE)
    private val alias = "sukho_live_device_key_v1"
    private val random = SecureRandom()

    fun hasCredential(): Boolean = prefs.contains("credential")

    fun store(token: String, pin: String) {
        require(pin.matches(Regex("[0-9]{6}")))
        val salt = ByteArray(16).also(random::nextBytes)
        val pinCipher = Cipher.getInstance("AES/GCM/NoPadding")
        pinCipher.init(Cipher.ENCRYPT_MODE, derive(pin, salt))
        val inner = pinCipher.iv + pinCipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val deviceCipher = Cipher.getInstance("AES/GCM/NoPadding")
        deviceCipher.init(Cipher.ENCRYPT_MODE, deviceKey())
        val payload = salt + deviceCipher.iv + deviceCipher.doFinal(inner)
        check(prefs.edit().putString("credential", Base64.encodeToString(payload, Base64.NO_WRAP))
            .putInt("failures", 0).putLong("blocked_until", 0).commit()) { "Akses perangkat gagal disimpan." }
    }

    fun unlock(pin: String): UnlockResult {
        val until = prefs.getLong("blocked_until", 0)
        if (now() < until) return UnlockResult.Cooldown(until)
        if (!pin.matches(Regex("[0-9]{6}"))) return wrong()
        val saved = prefs.getString("credential", null) ?: return UnlockResult.Damaged
        val inner: ByteArray
        val salt: ByteArray
        try {
            val payload = Base64.decode(saved, Base64.NO_WRAP)
            if (payload.size < 56) return UnlockResult.Damaged
            salt = payload.copyOfRange(0, 16)
            val outerIv = payload.copyOfRange(16, 28)
            val outer = payload.copyOfRange(28, payload.size)
            val deviceCipher = Cipher.getInstance("AES/GCM/NoPadding")
            deviceCipher.init(Cipher.DECRYPT_MODE, deviceKey(), GCMParameterSpec(128, outerIv))
            inner = deviceCipher.doFinal(outer)
            if (inner.size < 29) return UnlockResult.Damaged
        } catch (_: Exception) {
            return UnlockResult.Damaged
        }
        return try {
            val pinCipher = Cipher.getInstance("AES/GCM/NoPadding")
            pinCipher.init(Cipher.DECRYPT_MODE, derive(pin, salt), GCMParameterSpec(128, inner.copyOfRange(0, 12)))
            val token = pinCipher.doFinal(inner.copyOfRange(12, inner.size)).toString(Charsets.UTF_8)
            prefs.edit().putInt("failures", 0).putLong("blocked_until", 0).commit()
            UnlockResult.Success(token)
        } catch (_: Exception) {
            wrong()
        }
    }

    fun clear() {
        prefs.edit().clear().commit()
    }

    private fun wrong(): UnlockResult {
        val failures = prefs.getInt("failures", 0) + 1
        val cooldown = if (failures >= 5) (30_000L * (1L shl (failures - 5).coerceAtMost(5))).coerceAtMost(900_000L) else 0L
        val until = if (cooldown > 0) now() + cooldown else 0L
        prefs.edit().putInt("failures", failures).putLong("blocked_until", until).commit()
        return if (until > 0) UnlockResult.Cooldown(until) else UnlockResult.Wrong(5 - failures)
    }

    private fun derive(pin: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 150_000, 256)
        return try {
            SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
        } finally { spec.clearPassword() }
    }

    private fun deviceKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build())
        return generator.generateKey()
    }
}


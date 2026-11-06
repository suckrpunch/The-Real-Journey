package com.newjourney.core.journal

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The secret that unlocks the journal database.
 *
 * A random 32-byte passphrase is created once. It is stored only in encrypted form, sealed with
 * an AES key that lives inside Android Keystore (hardware-backed on most phones) and can never
 * be read out of it. Copying the app's files to another device therefore does not reveal the journal.
 */
internal class JournalKey(private val context: Context) {

    fun passphrase(): ByteArray {
        val file = File(context.noBackupFilesDir, FILE_NAME)
        if (file.exists()) return decrypt(file.readBytes())
        val secret = ByteArray(32).also { SecureRandom().nextBytes(it) }
        file.writeBytes(encrypt(secret))
        return secret
    }

    private fun keystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    /** Stored as: 12-byte IV, then the sealed passphrase. */
    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        return cipher.iv + cipher.doFinal(plain)
    }

    private fun decrypt(stored: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, stored.copyOfRange(0, IV_SIZE)))
        return cipher.doFinal(stored.copyOfRange(IV_SIZE, stored.size))
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "journal_database_key"
        const val FILE_NAME = "journal_key.bin"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
    }
}

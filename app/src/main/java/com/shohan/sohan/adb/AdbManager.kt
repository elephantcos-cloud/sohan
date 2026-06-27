package com.shohan.sohan.adb

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.security.Key
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAKeyGenParameterSpec
import java.security.spec.RSAPublicKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

object AdbManager {

    private const val KEYSTORE_PROVIDER  = "AndroidKeyStore"
    private const val AES_KEY_ALIAS      = "sohan_adb_aes_key"
    private const val TRANSFORMATION     = "AES/GCM/NoPadding"
    private const val GCM_IV_SIZE        = 12
    private const val GCM_TAG_SIZE_BITS  = 128
    private const val PREFS_NAME         = "sohan_adb_secure"
    private const val PREF_ENCRYPTED_KEY = "enc_rsa_private"

    private var dadb: Dadb? = null
    val isConnected: Boolean get() = dadb != null

    suspend fun connect(host: String, port: Int, context: Context): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                dadb?.close()
                dadb = null

                val connection = try {
                    val keyPair = loadOrCreateKeyPair(context)
                    Dadb.create(host, port, keyPair)
                } catch (_: Exception) {
                    Dadb.create(host, port)
                }

                val probe = connection.shell("echo sohan_ok")
                if (!probe.output.contains("sohan_ok")) {
                    connection.close()
                    return@withContext Result.failure(
                        Exception("Connected but shell test failed. Try restarting Wireless Debugging.")
                    )
                }

                dadb = connection
                Result.success(Unit)
            } catch (e: ConnectException) {
                Result.failure(Exception("Connection refused — is Wireless Debugging enabled? Port: \${port}"))
            } catch (e: SocketTimeoutException) {
                Result.failure(Exception("Connection timed out. Check the port number."))
            } catch (e: Exception) {
                Result.failure(Exception(e.message ?: "Unknown ADB error"))
            }
        }

    fun disconnect() {
        dadb?.close()
        dadb = null
    }

    suspend fun shell(command: String): Result<String> =
        withContext(Dispatchers.IO) {
            val conn = dadb
                ?: return@withContext Result.failure(Exception("Not connected to ADB."))
            try {
                val response = conn.shell(command)
                Result.success(response.output)
            } catch (e: Exception) {
                dadb = null
                Result.failure(Exception("Shell failed: \${e.message}"))
            }
        }

    private fun loadOrCreateKeyPair(context: Context): AdbKeyPair {
        val prefs  = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val aesKey = getOrCreateAesKey()

        val privateKeyBytes: ByteArray = loadEncryptedKey(prefs, aesKey)
            ?: run {
                val kg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA)
                kg.initialize(RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4))
                val rawPrivate = kg.generateKeyPair().private.encoded
                saveEncryptedKey(prefs, aesKey, rawPrivate)
                rawPrivate
            }

        val privateKey = KeyFactory.getInstance("RSA")
            .generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes)) as RSAPrivateKey
        val publicKey = KeyFactory.getInstance("RSA")
            .generatePublic(RSAPublicKeySpec(privateKey.modulus, RSAKeyGenParameterSpec.F4))

        val keyDir = File(context.filesDir, "adb_keys").also { it.mkdirs() }
        val privFile = File(keyDir, "sohan_key")
        val pubFile  = File(keyDir, "sohan_key.pub")
        privFile.writeBytes(privateKeyBytes)
        pubFile.writeBytes(publicKey.encoded)

        return AdbKeyPair.read(privFile, pubFile)
    }

    private fun getOrCreateAesKey(): Key {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).also { it.load(null) }
        return ks.getKey(AES_KEY_ALIAS, null) ?: run {
            val spec = KeyGenParameterSpec.Builder(
                AES_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
                .apply { init(spec) }
                .generateKey()
        }
    }

    private fun saveEncryptedKey(prefs: SharedPreferences, aesKey: Key, plaintext: ByteArray) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, aesKey) }
        val iv         = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        val blob       = ByteArray(iv.size + ciphertext.size)
        iv.copyInto(blob)
        ciphertext.copyInto(blob, iv.size)
        prefs.edit { putString(PREF_ENCRYPTED_KEY, Base64.encodeToString(blob, Base64.NO_WRAP)) }
    }

    private fun loadEncryptedKey(prefs: SharedPreferences, aesKey: Key): ByteArray? {
        val b64 = prefs.getString(PREF_ENCRYPTED_KEY, null) ?: return null
        return try {
            val blob       = Base64.decode(b64, Base64.NO_WRAP)
            val iv         = blob.copyOfRange(0, GCM_IV_SIZE)
            val ciphertext = blob.copyOfRange(GCM_IV_SIZE, blob.size)
            val spec       = GCMParameterSpec(GCM_TAG_SIZE_BITS, iv)
            Cipher.getInstance(TRANSFORMATION)
                .apply { init(Cipher.DECRYPT_MODE, aesKey, spec) }
                .doFinal(ciphertext)
        } catch (_: Exception) {
            null
        }
    }
}

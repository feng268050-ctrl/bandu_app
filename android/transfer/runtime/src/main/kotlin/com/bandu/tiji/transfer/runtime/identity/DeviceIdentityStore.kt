package com.bandu.tiji.transfer.runtime.identity

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.bandu.tiji.core.storage.preferences.DevicePreferencesStore
import com.bandu.tiji.transfer.protocol.identity.IdentityProof
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.first

data class LocalDeviceIdentity(
    val deviceId: String,
    val displayName: String,
    val keyAlias: String,
    val signingPublicKey: ByteArray,
    val publicKeyFingerprint: String,
)

class DeviceIdentityStore(
    private val preferences: DevicePreferencesStore,
    private val keys: IdentityKeyStore = AndroidIdentityKeyStore(),
    private val createUuid: () -> String = { UUID.randomUUID().toString() },
) {
    private val mutex = Mutex()

    suspend fun getOrCreate(defaultDisplayName: String): LocalDeviceIdentity = mutex.withLock {
        val current = preferences.data.first()
        val existing = current.identityKeyAlias
            .takeIf { current.deviceId.isNotBlank() && it.isNotBlank() }
            ?.let(keys::load)
        if (existing != null) {
            return@withLock current.toIdentity(existing.public)
        }
        createIdentity(defaultDisplayName, deleteAlias = current.identityKeyAlias.ifBlank { null })
    }

    suspend fun reset(displayName: String): LocalDeviceIdentity = mutex.withLock {
        val current = preferences.data.first()
        createIdentity(displayName, deleteAlias = current.identityKeyAlias.ifBlank { null })
    }

    fun signingPrivateKey(identity: LocalDeviceIdentity): PrivateKey =
        requireNotNull(keys.load(identity.keyAlias)?.private) {
            "Device identity key is unavailable"
        }

    private suspend fun createIdentity(
        displayName: String,
        deleteAlias: String?,
    ): LocalDeviceIdentity {
        require(displayName.isNotBlank()) { "Device display name must not be blank" }
        deleteAlias?.let(keys::delete)
        val deviceId = createUuid()
        val alias = "$KEY_ALIAS_PREFIX${deviceId.replace("-", "")}"
        val keyPair = keys.create(alias)
        val current = preferences.data.first()
        preferences.replace(
            current.copy(
                deviceId = deviceId,
                deviceDisplayName = displayName,
                identityKeyAlias = alias,
                trustedPeerRecords = emptySet(),
            ),
        )
        return preferences.data.first().toIdentity(keyPair.public)
    }

    private fun com.bandu.tiji.core.storage.preferences.DevicePreferences.toIdentity(
        publicKey: PublicKey,
    ): LocalDeviceIdentity {
        val encoded = publicKey.encoded
        return LocalDeviceIdentity(
            deviceId = deviceId,
            displayName = deviceDisplayName,
            keyAlias = identityKeyAlias,
            signingPublicKey = encoded,
            publicKeyFingerprint = IdentityProof.fingerprint(encoded),
        )
    }

    companion object {
        const val KEY_ALIAS_PREFIX = "bandu_tiji_device_identity_v1_"
    }
}

interface IdentityKeyStore {
    fun create(alias: String): KeyPair

    fun load(alias: String): KeyPair?

    fun delete(alias: String)
}

class AndroidIdentityKeyStore : IdentityKeyStore {
    override fun create(alias: String): KeyPair {
        require(alias.startsWith(DeviceIdentityStore.KEY_ALIAS_PREFIX))
        delete(alias)
        return KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            ANDROID_KEYSTORE,
        ).run {
            initialize(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
                )
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build(),
            )
            generateKeyPair()
        }
    }

    override fun load(alias: String): KeyPair? {
        if (alias.isBlank()) return null
        val store = keyStore()
        val privateKey = store.getKey(alias, null) as? PrivateKey ?: return null
        val publicKey = store.getCertificate(alias)?.publicKey ?: return null
        return KeyPair(publicKey, privateKey)
    }

    override fun delete(alias: String) {
        if (alias.isBlank()) return
        keyStore().let { store ->
            if (store.containsAlias(alias)) store.deleteEntry(alias)
        }
    }

    private fun keyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }
}

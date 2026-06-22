package com.bandu.tiji.transfer.runtime.identity

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bandu.tiji.core.storage.preferences.StoragePreferencesFactory
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceIdentityStoreInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val root = File(context.filesDir, "runtime-identity-test-${System.nanoTime()}")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preferences = StoragePreferencesFactory.createDevice(root, scope)
    private val aliases = mutableSetOf<String>()

    @After
    fun tearDown() {
        val keys = AndroidIdentityKeyStore()
        aliases.forEach(keys::delete)
        scope.coroutineContext[Job]?.cancel()
        root.deleteRecursively()
    }

    @Test
    fun keystoreIdentitySurvivesReopenAndResetRotatesIt() = runBlocking {
        val firstStore = DeviceIdentityStore(preferences)
        val first = firstStore.getOrCreate("Instrumented device").also { aliases += it.keyAlias }
        val reopened = DeviceIdentityStore(preferences).getOrCreate("Ignored")

        assertThat(reopened.deviceId).isEqualTo(first.deviceId)
        assertThat(reopened.signingPublicKey).isEqualTo(first.signingPublicKey)
        assertThat(reopened.publicKeyFingerprint).isEqualTo(first.publicKeyFingerprint)

        val reset = DeviceIdentityStore(preferences)
            .reset("Reset device")
            .also { aliases += it.keyAlias }
        assertThat(reset.deviceId).isNotEqualTo(first.deviceId)
        assertThat(reset.signingPublicKey).isNotEqualTo(first.signingPublicKey)
    }
}

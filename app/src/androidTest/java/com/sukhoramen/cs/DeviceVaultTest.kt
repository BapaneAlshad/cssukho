package com.sukhoramen.cs

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sukhoramen.cs.data.DeviceVault
import com.sukhoramen.cs.data.UnlockResult
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceVaultTest {
    @Test fun pinProtectsCredentialAndThrottlesWrongAttempts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val vault = DeviceVault(context)
        vault.clear()
        try {
            vault.store("test-device-token-only", "123456")
            assertTrue(vault.hasCredential())
            assertTrue(vault.unlock("000000") is UnlockResult.Wrong)
            val unlocked = vault.unlock("123456")
            assertEquals("test-device-token-only", (unlocked as UnlockResult.Success).token)
            repeat(4) { assertTrue(vault.unlock("000000") is UnlockResult.Wrong) }
            assertTrue(vault.unlock("000000") is UnlockResult.Cooldown)
            assertTrue(vault.unlock("123456") is UnlockResult.Cooldown)
        } finally { vault.clear() }
    }
}


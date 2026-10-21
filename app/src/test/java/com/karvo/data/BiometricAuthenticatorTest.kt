package com.karvo.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BiometricAuthenticatorTest {
    @Test fun detachedAuthenticatorReturnsFalse() = runTest {
        val auth = BiometricAuthenticator(ApplicationProvider.getApplicationContext<Context>())
        assertFalse(auth.authenticate("Show earnings"))
    }
}

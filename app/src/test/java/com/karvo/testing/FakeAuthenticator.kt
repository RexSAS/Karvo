package com.karvo.testing

import com.karvo.data.Authenticator

class FakeAuthenticator(var available: Boolean = true, var succeeds: Boolean = true) : Authenticator {
    var calls = 0
        private set

    override fun isAvailable(): Boolean = available

    override suspend fun authenticate(title: String): Boolean {
        calls++
        return succeeds
    }
}

/*
 * Copyright 2021-Present Okta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okta.testing.network

import com.okta.authfoundation.client.Cache
import com.okta.authfoundation.client.OidcClient
import com.okta.authfoundation.client.OidcConfiguration
import com.okta.authfoundation.client.OidcEndpoints
import com.okta.authfoundation.events.EventCoordinator
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.tls.HandshakeCertificates
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.net.Proxy
import kotlin.coroutines.EmptyCoroutineContext

class NetworkRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return MockWebServerStatement(base, OktaMockWebServer.dispatcher, description)
    }

    fun enqueue(vararg requestMatcher: RequestMatcher, responseFactory: (MockResponse) -> Unit) {
        OktaMockWebServer.dispatcher.enqueue(*requestMatcher) { response ->
            responseFactory(response)
        }
    }

    fun mockedUrl(): HttpUrl {
        return OktaMockWebServer.mockWebServer.url("")
    }

    fun okHttpClient(): OkHttpClient {
        val clientBuilder = OkHttpClient.Builder()
        // This prevents Charles proxy from messing our mock responses.
        clientBuilder.proxy(Proxy.NO_PROXY)

        val handshakeCertificates = HandshakeCertificates.Builder()
            .addTrustedCertificate(OktaMockWebServer.localhostCertificate.certificate)
            .build()
        clientBuilder.sslSocketFactory(
            handshakeCertificates.sslSocketFactory(),
            handshakeCertificates.trustManager
        )

        clientBuilder.addInterceptor(OktaMockWebServer.interceptor)

        return clientBuilder.build()
    }

    val clock: TestClock = TestClock()

    val configuration: OidcConfiguration = OidcConfiguration(
        clientId = "test",
        defaultScope = "openid email profile offline_access",
        okHttpClientFactory = { okHttpClient() },
        eventCoordinator = EventCoordinator(emptyList()),
        clock = clock,
        idTokenValidator = { _, _, _ -> },
        accessTokenValidator = { _, _, _ -> },
        deviceSecretValidator = { _, _, _ -> },
        ioDispatcher = EmptyCoroutineContext,
        computeDispatcher = EmptyCoroutineContext,
        cache = NoOpCache(),
    )

    fun createOidcClient(urlBuilder: HttpUrl.Builder = mockedUrl().newBuilder()): OidcClient {
        val endpoints = OidcEndpoints(
            issuer = urlBuilder.encodedPath("/oauth2/default").build(),
            authorizationEndpoint = urlBuilder.encodedPath("/oauth2/default/v1/authorize").build(),
            tokenEndpoint = urlBuilder.encodedPath("/oauth2/default/v1/token").build(),
            userInfoEndpoint = urlBuilder.encodedPath("/oauth2/default/v1/userinfo").build(),
            jwksUri = null,
            introspectionEndpoint = urlBuilder.encodedPath("/oauth2/default/v1/introspect").build(),
            revocationEndpoint = urlBuilder.encodedPath("/oauth2/default/v1/revoke").build(),
            endSessionEndpoint = urlBuilder.encodedPath("/oauth2/default/v1/logout").build(),
            deviceAuthorizationEndpoint = null,
        )
        return OidcClient.create(configuration, endpoints)
    }
}

private class NoOpCache : Cache {
    override fun set(key: String, value: String) {
    }

    override fun get(key: String): String? {
        return null
    }
}

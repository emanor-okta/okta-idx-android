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
package com.okta.idx.android

import android.app.Application
import com.okta.authfoundation.AuthFoundationDefaults
import com.okta.authfoundation.client.OidcClient
import com.okta.authfoundation.client.OidcConfiguration
import com.okta.authfoundation.client.SharedPreferencesCache
import com.okta.authfoundation.credential.CredentialDataSource.Companion.createCredentialDataSource
import com.okta.authfoundationbootstrap.CredentialBootstrap
import okhttp3.HttpUrl.Companion.toHttpUrl
import timber.log.Timber

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Add AuthFoundation for Token storage/refresh capability
        val context = applicationContext
        AuthFoundationDefaults.cache = SharedPreferencesCache.create(context)
        val oidcConfiguration = OidcConfiguration(
            clientId = BuildConfig.CLIENT_ID,
            defaultScope = "openid email profile offline_access",
        )
        val oidcClient = OidcClient.createFromDiscoveryUrl(
            oidcConfiguration,
            "${BuildConfig.ISSUER}/.well-known/openid-configuration".toHttpUrl(),
        )
        CredentialBootstrap.initialize(oidcClient.createCredentialDataSource(context))
        // End Add AuthFoundation


        Timber.plant(Timber.DebugTree())
    }
}

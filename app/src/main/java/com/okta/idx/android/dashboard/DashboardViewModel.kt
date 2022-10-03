/*
 * Copyright 2022-Present Okta, Inc.
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
package com.okta.idx.android.dashboard

import androidx.lifecycle.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.okta.authfoundation.client.OidcClientResult
import com.okta.authfoundation.credential.Token
import com.okta.authfoundationbootstrap.CredentialBootstrap
import com.okta.idx.android.TokenViewModel
import com.okta.idx.android.network.Network
import com.okta.idx.sdk.api.model.TokenType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import okhttp3.Request
import timber.log.Timber
import java.io.IOException

internal class DashboardViewModel : ViewModel() {
    private val _logoutStateLiveData = MutableLiveData<LogoutState>(LogoutState.Idle)
    val logoutStateLiveData: LiveData<LogoutState> = _logoutStateLiveData

    private val _userInfoLiveData = MutableLiveData<Map<String, String>>(emptyMap())
    val userInfoLiveData: LiveData<Map<String, String>> = _userInfoLiveData

    init {
        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                //test AuthFoundation
            if (TokenViewModel.tokenResponse != null) {
                val token = Token(
                    TokenViewModel.tokenResponse.tokenType,
                    TokenViewModel.tokenResponse.expiresIn,
                    TokenViewModel.tokenResponse.accessToken,
                    "openid profile email offline_access",
                    TokenViewModel.tokenResponse.refreshToken,
                    TokenViewModel.tokenResponse.idToken,
                    null,
                    null
                )
                CredentialBootstrap.defaultCredential().storeToken(token)
                userInfo()
            }
//                //end test
//
//                getClaims()?.let { _userInfoLiveData.postValue(it) }
//            } catch (e: IOException) {
//                Timber.e(e, "User info request failed.")
//            }
        }

    }

    // Test Authfoundation
    fun userInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = CredentialBootstrap.defaultCredential().getUserInfo()) {
                is OidcClientResult.Error -> {
                    Timber.e(result.exception, "User info request failed.")
                }
                is OidcClientResult.Success -> {
                    val successResult = result.result

                    val map = mutableMapOf<String, String>()
                    for (entry in successResult.deserializeClaims(JsonObject.serializer()).entries) {
                        map[entry.key] = entry.value.toString()
                    }

                    if (!TokenViewModel.tokenResponse.accessToken.equals(CredentialBootstrap.defaultCredential().token?.accessToken)) {
                        TokenViewModel.tokenResponse.expiresIn = CredentialBootstrap.defaultCredential().token!!.expiresIn
                        TokenViewModel.tokenResponse.accessToken = CredentialBootstrap.defaultCredential().token!!.accessToken
                        TokenViewModel.tokenResponse.refreshToken = CredentialBootstrap.defaultCredential().token?.refreshToken
                        TokenViewModel.tokenResponse.idToken = CredentialBootstrap.defaultCredential().token!!.idToken
                    }
                    _userInfoLiveData.postValue(map)

                }
            }
        }
    }

    fun logout() {
        _logoutStateLiveData.value = LogoutState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {

                if (TokenViewModel.tokenResponse.refreshToken != null) {
                    // Revoking the refresh token revokes both!
//                    Network.authenticationWrapper().revokeToken(
//                        TokenType.REFRESH_TOKEN,
//                        TokenViewModel.tokenResponse.refreshToken
//                    )
                } else {
                    Network.authenticationWrapper().revokeToken(
                        TokenType.ACCESS_TOKEN,
                        TokenViewModel.tokenResponse.accessToken
                    )
                }

                TokenViewModel._tokenResponse = null

                _logoutStateLiveData.postValue(LogoutState.Success)
            } catch (e: Exception) {
                _logoutStateLiveData.postValue(LogoutState.Failed)
            }
        }
    }

    // Test Authfoundation - Remove old getClaims
//    private fun getClaims(): Map<String, String>? {
//        val accessToken = TokenViewModel.tokenResponse.accessToken
//        val request = Request.Builder()
//            .addHeader("authorization", "Bearer $accessToken")
//            .url("${Network.baseUrl}/v1/userinfo")
//            .build()
//        val response = Network.okHttpClient().newCall(request).execute()
//        if (response.isSuccessful) {
//            val parser = ObjectMapper().createParser(response.body?.byteStream())
//            val json = parser.readValueAsTree<JsonNode>()
//            val map = mutableMapOf<String, String>()
//            for (entry in json.fields()) {
//                map[entry.key] = entry.value.asText()
//            }
//            return map
//        } else {
//            println("Refresh Fail: " + response.code)
//        }
//
//        return null
//    }

    fun acknowledgeLogoutSuccess() {
        _logoutStateLiveData.value = LogoutState.Idle
    }

    sealed class LogoutState {
        object Idle : LogoutState()
        object Loading : LogoutState()
        object Success : LogoutState()
        object Failed : LogoutState()
    }
}

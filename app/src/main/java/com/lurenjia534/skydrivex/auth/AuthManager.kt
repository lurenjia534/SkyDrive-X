package com.lurenjia534.skydrivex.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import com.lurenjia534.skydrivex.auth.AuthConfigDefaults.ACCOUNT_MODE
import com.lurenjia534.skydrivex.auth.AuthConfigDefaults.AUDIENCE_TYPE
import com.lurenjia534.skydrivex.auth.AuthConfigDefaults.AUTHORITY_TYPE
import com.lurenjia534.skydrivex.auth.AuthConfigDefaults.DEFAULT_TENANT_ID
import com.lurenjia534.skydrivex.auth.AuthConfigDefaults.REDIRECT_URI
import com.lurenjia534.skydrivex.data.repository.AuthConfig
import com.lurenjia534.skydrivex.data.repository.AuthConfigRepository
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.Prompt
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authConfigRepository: AuthConfigRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var singleAccountApp: ISingleAccountPublicClientApplication? = null
    private val initializationState = MutableStateFlow<Boolean?>(null)

    init {
        scope.launch {
            authConfigRepository.configFlow.collectLatest { config ->
                singleAccountApp = null
                initializationState.value = null

                if (config == null) {
                    initializationState.value = false
                    return@collectLatest
                }

                initializeMsal(config)
            }
        }
    }

    suspend fun awaitInitialization(): Boolean = initializationState
        .filterNotNull()
        .first()

    fun signIn(activity: Activity, scopes: Array<String>, callback: AuthenticationCallback) {
        val parameters = AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(activity)
            .withScopes(scopes.toList())
            .withPrompt(Prompt.SELECT_ACCOUNT)
            .withCallback(callback)
            .build()
        singleAccountApp?.acquireToken(parameters)
    }

    fun acquireToken(activity: Activity, scopes: Array<String>, callback: AuthenticationCallback) {
        val parameters = AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(activity)
            .withScopes(scopes.toList())
            .withCallback(callback)
            .build()
        singleAccountApp?.acquireToken(parameters)
    }

    fun acquireTokenSilent(account: IAccount, scopes: Array<String>, callback: SilentAuthenticationCallback) {
        val tenantAuthority = account.authority

        val parameters = AcquireTokenSilentParameters.Builder()
            .forAccount(account)
            .fromAuthority(tenantAuthority)
            .withScopes(scopes.toList())
            .withCallback(callback)
            .build()
        singleAccountApp?.acquireTokenSilentAsync(parameters)
    }

    fun signOut(callback: ISingleAccountPublicClientApplication.SignOutCallback) {
        singleAccountApp?.signOut(callback)
    }

    fun getCurrentAccount(callback: ISingleAccountPublicClientApplication.CurrentAccountCallback) {
        singleAccountApp?.getCurrentAccountAsync(callback)
    }

    private suspend fun initializeMsal(config: AuthConfig) {
        val configFile = writeRuntimeConfig(config)
        withContext(Dispatchers.Main) {
            PublicClientApplication.createSingleAccountPublicClientApplication(
                context,
                configFile,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
                        singleAccountApp = application
                        initializationState.value = true
                    }

                    override fun onError(exception: MsalException) {
                        Log.e("AuthManager", "MSAL initialization failed", exception)
                        initializationState.value = false
                    }
                }
            )
        }
    }

    private suspend fun writeRuntimeConfig(config: AuthConfig): File = withContext(Dispatchers.IO) {
        val json = buildJsonConfig(config.clientId)
        val file = File(context.filesDir, AUTH_CONFIG_FILE_NAME)
        file.writeText(json)
        file
    }

    private fun buildJsonConfig(clientId: String): String = """
        {
          "client_id": "$clientId",
          "redirect_uri": "$REDIRECT_URI",
          "account_mode": "$ACCOUNT_MODE",
          "authorities": [
            {
              "type": "$AUTHORITY_TYPE",
              "audience": {
                "type": "$AUDIENCE_TYPE",
                "tenant_id": "$DEFAULT_TENANT_ID"
              },
              "default": true
            }
          ]
        }
    """.trimIndent()

    companion object {
        private const val AUTH_CONFIG_FILE_NAME = "auth_config_runtime.json"
    }
}

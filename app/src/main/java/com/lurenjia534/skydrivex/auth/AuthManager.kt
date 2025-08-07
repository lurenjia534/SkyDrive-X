package com.lurenjia534.skydrivex.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import com.lurenjia534.skydrivex.R
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.Prompt
import kotlinx.coroutines.CompletableDeferred
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(@ApplicationContext context: Context) {

    private var singleAccountApp: ISingleAccountPublicClientApplication? = null
    private val initializationDeferred = CompletableDeferred<Boolean>()

    init {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            R.raw.auth_config,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    singleAccountApp = application
                    initializationDeferred.complete(true)
                }

                override fun onError(exception: MsalException) {
                    Log.e("AuthManager", "MSAL initialization failed", exception)
                    initializationDeferred.complete(false)
                }
            }
        )
    }

    suspend fun awaitInitialization(): Boolean = initializationDeferred.await()

    fun signIn(activity: Activity, scopes: Array<String>, callback: AuthenticationCallback) {
        val parameters = AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(activity)
            .withScopes(scopes.toList())
            .withPrompt(Prompt.SELECT_ACCOUNT) // 显式触发登录/选择账号
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

        //val authority = "https://login.microsoftonline.com/common"
        // 依据当前账户租户 ID 构造租户专属 authority
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
}


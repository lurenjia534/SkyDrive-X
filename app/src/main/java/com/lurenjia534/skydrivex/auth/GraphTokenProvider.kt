package com.lurenjia534.skydrivex.auth

import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class GraphTokenProvider @Inject constructor(
    private val authManager: AuthManager
) {

    suspend fun getAccessToken(): String {
        val initialized = authManager.awaitInitialization()
        if (!initialized) throw IllegalStateException("MSAL initialization failed")
        val account = currentAccount()
            ?: throw IllegalStateException("No cached account for silent auth")
        return acquireTokenSilent(account)
    }

    private suspend fun currentAccount(): IAccount? = suspendCancellableCoroutine { cont ->
        authManager.getCurrentAccount(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                if (cont.isActive) cont.resume(activeAccount)
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                if (cont.isActive) cont.resume(currentAccount)
            }

            override fun onError(exception: MsalException) {
                if (cont.isActive) cont.resumeWithException(exception)
            }
        })
    }

    private suspend fun acquireTokenSilent(account: IAccount): String = suspendCancellableCoroutine { cont ->
        authManager.acquireTokenSilent(
            account = account,
            scopes = MsalScopes.DEFAULT,
            callback = object : SilentAuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    if (cont.isActive) cont.resume(authenticationResult.accessToken)
                }

                override fun onError(exception: MsalException) {
                    if (cont.isActive) cont.resumeWithException(exception)
                }
            }
        )
    }
}

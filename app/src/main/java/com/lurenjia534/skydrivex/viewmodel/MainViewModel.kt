package com.lurenjia534.skydrivex.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import com.lurenjia534.skydrivex.auth.AuthManager
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    companion object {
        private val SCOPES = arrayOf("User.Read")
    }

    private val _account = MutableStateFlow<IAccount?>(null)
    val account: StateFlow<IAccount?> = _account.asStateFlow()

    init {
        authManager.getCurrentAccount(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                _account.value = activeAccount
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                _account.value = currentAccount
            }

            override fun onError(exception: MsalException) {
                Log.e("MainViewModel", "Load account error", exception)
            }
        })
    }

    fun signIn(activity: Activity) {
        authManager.signIn(activity, SCOPES, object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                _account.value = authenticationResult.account
            }

            override fun onError(exception: MsalException) {
                Log.e("MainViewModel", "Sign in error", exception)
            }

            override fun onCancel() {
                Log.d("MainViewModel", "Sign in cancelled")
            }
        })
    }

    fun acquireToken(activity: Activity) {
        authManager.acquireToken(activity, SCOPES, object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                _account.value = authenticationResult.account
            }

            override fun onError(exception: MsalException) {
                Log.e("MainViewModel", "Acquire token error", exception)
            }

            override fun onCancel() {
                Log.d("MainViewModel", "Acquire token cancelled")
            }
        })
    }

    fun acquireTokenSilent() {
        authManager.acquireTokenSilent(SCOPES, object : SilentAuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                _account.value = authenticationResult.account
            }

            override fun onError(exception: MsalException) {
                Log.e("MainViewModel", "Silent token error", exception)
            }
        })
    }

    fun signOut() {
        authManager.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() {
                _account.value = null
            }

            override fun onError(exception: MsalException) {
                Log.e("MainViewModel", "Sign out error", exception)
            }
        })
    }
}


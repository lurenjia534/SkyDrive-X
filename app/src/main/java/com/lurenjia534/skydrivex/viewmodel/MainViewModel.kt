package com.lurenjia534.skydrivex.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurenjia534.skydrivex.auth.AuthManager
import com.lurenjia534.skydrivex.data.local.ThemePreferenceRepository
import com.lurenjia534.skydrivex.data.repository.UserRepository
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val themePreferenceRepository: ThemePreferenceRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        private val SCOPES = arrayOf("User.Read")
    }

    private val _account = MutableStateFlow<IAccount?>(null)
    val account: StateFlow<IAccount?> = _account.asStateFlow()

    private val _userState = MutableStateFlow(UserUiState(data = null, isLoading = false, error = null))
    val userState: StateFlow<UserUiState> = _userState.asStateFlow()

    private var lastToken: String? = null

    val isDarkMode = themePreferenceRepository.isDarkMode.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        false
    )

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
                loadUser(authenticationResult.accessToken)
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
                loadUser(authenticationResult.accessToken)
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
                loadUser(authenticationResult.accessToken)
            }

            override fun onError(exception: MsalException) {
                Log.e("MainViewModel", "Silent token error", exception)
                _userState.value = UserUiState(data = null, isLoading = false, error = exception.message)
            }
        })
    }

    fun signOut() {
        authManager.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() {
                _account.value = null
                _userState.value = UserUiState(data = null, isLoading = false, error = null)
            }

            override fun onError(exception: MsalException) {
                Log.e("MainViewModel", "Sign out error", exception)
            }
        })
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            themePreferenceRepository.setDarkMode(enabled)
        }
    }

    private fun loadUser(token: String) {
        lastToken = token
        viewModelScope.launch {
            _userState.value = UserUiState(data = null, isLoading = true, error = null)
            try {
                val user = userRepository.getUser("Bearer $token")
                _userState.value = UserUiState(data = user, isLoading = false, error = null)
            } catch (e: Exception) {
                _userState.value = UserUiState(data = null, isLoading = false, error = e.message)
            }
        }
    }

    fun retry() {
        lastToken?.let { loadUser(it) }
    }
}


package com.lurenjia534.skydrivex.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val authManager: AuthManager,
    private val themePreferenceRepository: ThemePreferenceRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        private val SCOPES = arrayOf("User.Read", "Files.Read")
    }

    private val _account = MutableStateFlow<IAccount?>(null)
    val account: StateFlow<IAccount?> = _account.asStateFlow()

    private val _userState = MutableStateFlow(UserUiState(data = null, isLoading = false, error = null))
    val userState: StateFlow<UserUiState> = _userState.asStateFlow()

    private val _driveState = MutableStateFlow(DriveUiState(data = null, isLoading = false, error = null))
    val driveState: StateFlow<DriveUiState> = _driveState.asStateFlow()

    private var lastToken: String? = null
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    val isDarkMode = themePreferenceRepository.isDarkMode.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        false
    )

    private val _areNotificationsEnabled = MutableStateFlow(false)
    val areNotificationsEnabled: StateFlow<Boolean> = _areNotificationsEnabled.asStateFlow()

    init {
        checkNotificationStatus()
        viewModelScope.launch {
            val initialized = authManager.awaitInitialization()
            if (initialized) {
                authManager.getCurrentAccount(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
                    override fun onAccountLoaded(activeAccount: IAccount?) {
                        _account.value = activeAccount
                        if (activeAccount != null) {
                            acquireTokenSilent()
                        }
                    }

                    override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                        _account.value = currentAccount
                        if (currentAccount != null) {
                            acquireTokenSilent()
                        } else {
                            _userState.value = UserUiState(data = null, isLoading = false, error = null)
                            _driveState.value = DriveUiState(data = null, isLoading = false, error = null)
                        }
                    }

                    override fun onError(exception: MsalException) {
                        Log.e("MainViewModel", "Load account error", exception)
                    }
                })
            } else {
                _userState.value = UserUiState(data = null, isLoading = false, error = "MSAL initialization failed")
                _driveState.value = DriveUiState(data = null, isLoading = false, error = "MSAL initialization failed")
            }
        }
    }

    fun signIn(activity: Activity) {
        authManager.signIn(activity, SCOPES, object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                _account.value = authenticationResult.account
                loadData(authenticationResult.accessToken)
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
                loadData(authenticationResult.accessToken)
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
        _userState.value = UserUiState(data = null, isLoading = true, error = null)
        _driveState.value = DriveUiState(data = null, isLoading = true, error = null)
        viewModelScope.launch {
            if (authManager.awaitInitialization()) {
                authManager.acquireTokenSilent(SCOPES, object : SilentAuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        _account.value = authenticationResult.account
                        loadData(authenticationResult.accessToken)
                    }

                    override fun onError(exception: MsalException) {
                        Log.e("MainViewModel", "Silent token error", exception)
                        _userState.value = UserUiState(
                            data = null,
                            isLoading = false,
                            error = exception.message ?: "Failed to acquire token",
                        )
                        _driveState.value = DriveUiState(
                            data = null,
                            isLoading = false,
                            error = exception.message ?: "Failed to acquire token",
                        )
                    }
                })
            } else {
                _userState.value = UserUiState(
                    data = null,
                    isLoading = false,
                    error = "MSAL initialization failed",
                )
                _driveState.value = DriveUiState(
                    data = null,
                    isLoading = false,
                    error = "MSAL initialization failed",
                )
            }
        }
    }

    fun signOut() {
        authManager.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() {
                _account.value = null
                _userState.value = UserUiState(data = null, isLoading = false, error = null)
                _driveState.value = DriveUiState(data = null, isLoading = false, error = null)
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

    fun checkNotificationStatus() {
        _areNotificationsEnabled.value = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun openNotificationSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun loadData(token: String) {
        lastToken = token
        _token.value = token
        loadUser(token)
        loadDrive(token)
    }

    private fun loadUser(token: String) {
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

    private fun loadDrive(token: String) {
        viewModelScope.launch {
            _driveState.value = DriveUiState(data = null, isLoading = true, error = null)
            try {
                val drive = userRepository.getDrive("Bearer $token")
                _driveState.value = DriveUiState(data = drive, isLoading = false, error = null)
            } catch (e: Exception) {
                _driveState.value = DriveUiState(data = null, isLoading = false, error = e.message)
            }
        }
    }

    fun retry() {
        lastToken?.let { loadData(it) }
    }
}


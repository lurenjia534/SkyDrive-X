package com.lurenjia534.skydrivex.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurenjia534.skydrivex.auth.AuthManager
import com.lurenjia534.skydrivex.data.local.ActiveAccountPreferenceRepository
import com.lurenjia534.skydrivex.data.local.ThemePreferenceRepository
import com.lurenjia534.skydrivex.data.local.DownloadPreferenceRepository
import com.lurenjia534.skydrivex.data.local.DownloadLocationPreference
import com.lurenjia534.skydrivex.data.repository.UserRepository
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import dagger.hilt.android.lifecycle.HiltViewModel
import com.lurenjia534.skydrivex.ui.state.UserUiState
import com.lurenjia534.skydrivex.ui.state.DriveUiState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.lurenjia534.skydrivex.auth.MsalScopes

/**
 * 主界面 ViewModel：
 * - 负责管理账户列表、激活账户及 MSAL 令牌生命周期
 * - 拉取 OneDrive 用户与驱动器信息并暴露 UI 状态
 * - 同时维护主题、下载目录、通知权限等用户偏好
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val authManager: AuthManager,
    private val activeAccountPreferenceRepository: ActiveAccountPreferenceRepository,
    private val themePreferenceRepository: ThemePreferenceRepository,
    private val downloadPreferenceRepository: DownloadPreferenceRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _accounts = MutableStateFlow<List<IAccount>>(emptyList())
    val accounts: StateFlow<List<IAccount>> = _accounts.asStateFlow()

    private val _activeAccount = MutableStateFlow<IAccount?>(null)
    val activeAccount: StateFlow<IAccount?> = _activeAccount.asStateFlow()

    private val _isAccountInitialized = MutableStateFlow(false)
    val isAccountInitialized: StateFlow<Boolean> = _isAccountInitialized.asStateFlow()

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

    val downloadPreference = downloadPreferenceRepository.downloadPreference.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        DownloadLocationPreference()
    )

    private val _areNotificationsEnabled = MutableStateFlow(false)
    val areNotificationsEnabled: StateFlow<Boolean> = _areNotificationsEnabled.asStateFlow()

    init {
        // 初始化时预先检测通知权限，并等待 MSAL 准备完成
        checkNotificationStatus()
        viewModelScope.launch {
            val initialized = authManager.awaitInitialization()
            if (initialized) {
                refreshAccounts()
            } else {
                _userState.value = UserUiState(data = null, isLoading = false, error = "MSAL initialization failed")
                _driveState.value = DriveUiState(data = null, isLoading = false, error = "MSAL initialization failed")
                _isAccountInitialized.value = true
            }
        }
    }

    /**
     * 重新拉取本地缓存的账户列表，并恢复/更新激活账户。
     * triggerTokenRefresh 控制是否在切换账户时自动拉取新令牌。
     */
    fun refreshAccounts(triggerTokenRefresh: Boolean = true) {
        viewModelScope.launch {
            val initialized = authManager.awaitInitialization()
            if (!initialized) {
                _accounts.value = emptyList()
                setActiveAccountInternal(null, triggerTokenRefresh = false)
                _userState.value = UserUiState(data = null, isLoading = false, error = "MSAL initialization failed")
                _driveState.value = DriveUiState(data = null, isLoading = false, error = "MSAL initialization failed")
                _isAccountInitialized.value = true
                return@launch
            }

            val list = authManager.loadAccounts()
            _accounts.value = list
            val savedId = activeAccountPreferenceRepository.getActiveAccountId()
            val candidate = list.find { it.id == savedId } ?: list.firstOrNull()
            setActiveAccountInternal(candidate, triggerTokenRefresh)
            _isAccountInitialized.value = true
        }
    }

    /**
     * 触发交互式登录；成功后立即刷新账户列表并拉取用户/驱动器数据。
     */
    fun signIn(activity: Activity) {
        authManager.signIn(activity, MsalScopes.DEFAULT, object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                viewModelScope.launch {
                    setActiveAccountInternal(authenticationResult.account, triggerTokenRefresh = false)
                    refreshAccounts(triggerTokenRefresh = false)
                    loadData(authenticationResult.accessToken)
                }
            }

            override fun onError(exception: MsalException) {
                Log.e("MainViewModel", "Sign in error", exception)
            }

            override fun onCancel() {
                Log.d("MainViewModel", "Sign in cancelled")
            }
        })
    }

    /**
     * 交互式拉取新令牌（无需强制选择账户），用于手动刷新。
     */
    fun acquireToken(activity: Activity) {
        authManager.acquireToken(activity, MsalScopes.DEFAULT, object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                viewModelScope.launch {
                    setActiveAccountInternal(authenticationResult.account, triggerTokenRefresh = false)
                    refreshAccounts(triggerTokenRefresh = false)
                    loadData(authenticationResult.accessToken)
                }
            }

            override fun onError(exception: MsalException) {
                Log.e("MainViewModel", "Acquire token error", exception)
            }

            override fun onCancel() {
                Log.d("MainViewModel", "Acquire token cancelled")
            }
        })
    }

    /**
     * 基于当前激活账户静默获取令牌，若无账户则清空 UI 数据。
     */
    fun acquireTokenSilent() {
        val account = _activeAccount.value
        if (account != null) {
            acquireTokenSilentInternal(account)
        } else {
            clearUserData(message = "No active account")
            clearDriveData(message = "No active account")
        }
    }

    fun setActiveAccount(account: IAccount) {
        viewModelScope.launch { setActiveAccountInternal(account, triggerTokenRefresh = true) }
    }

    fun removeAccount(account: IAccount) {
        viewModelScope.launch {
            val removed = authManager.removeAccount(account)
            if (!removed) {
                Log.e("MainViewModel", "Remove account failed")
            }
            refreshAccounts(triggerTokenRefresh = true)
        }
    }

    /**
     * 真正执行静默令牌刷新，期间将用户与驱动器状态标记为加载中。
     */
    private fun acquireTokenSilentInternal(account: IAccount) {
        _userState.value  = UserUiState(data = null, isLoading = true, error = null)
        _driveState.value = DriveUiState(data = null, isLoading = true, error = null)

        viewModelScope.launch {
            if (authManager.awaitInitialization()) {
                authManager.acquireTokenSilent(account, MsalScopes.DEFAULT, object : SilentAuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        viewModelScope.launch {
                            setActiveAccountInternal(authenticationResult.account, triggerTokenRefresh = false)
                        }
                        loadData(authenticationResult.accessToken)
                    }

                    override fun onError(exception: MsalException) {
                        Log.e("MainViewModel", "Silent token error", exception)
                        clearUserData(message = exception.message)
                        clearDriveData(message = exception.message)
                    }
                })
            } else {
                clearUserData(message = "MSAL initialization failed")
                clearDriveData(message = "MSAL initialization failed")
            }
        }
    }

    /**
     * 移除激活账户或直接清理数据，退出登录。
     */
    fun signOut() {
        _activeAccount.value?.let { removeAccount(it) } ?: run {
            clearUserData()
            clearDriveData()
        }
    }

    /**
     * 更新激活账户及本地存储；可按需触发静默拉取新令牌。
     */
    private suspend fun setActiveAccountInternal(account: IAccount?, triggerTokenRefresh: Boolean) {
        _activeAccount.value = account
        if (account == null) {
            activeAccountPreferenceRepository.clearActiveAccountId()
            _token.value = null
            lastToken = null
            clearUserData()
            clearDriveData()
            return
        }

        activeAccountPreferenceRepository.setActiveAccountId(account.id)

        if (triggerTokenRefresh) {
            acquireTokenSilentInternal(account)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            themePreferenceRepository.setDarkMode(enabled)
        }
    }

    fun setDownloadToSystem() {
        viewModelScope.launch {
            downloadPreferenceRepository.setSystemDownloads()
        }
    }

    fun setDownloadModeCustom() {
        viewModelScope.launch {
            downloadPreferenceRepository.setCustomMode()
        }
    }

    fun setDownloadToCustom(treeUriString: String) {
        viewModelScope.launch {
            downloadPreferenceRepository.setCustomTree(treeUriString)
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

    private fun clearUserData(message: String? = null) {
        _userState.value = UserUiState(data = null, isLoading = false, error = message)
    }

    private fun clearDriveData(message: String? = null) {
        _driveState.value = DriveUiState(data = null, isLoading = false, error = message)
    }

    /**
     * 将令牌写入状态并并行加载用户和驱动器信息。
     */
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

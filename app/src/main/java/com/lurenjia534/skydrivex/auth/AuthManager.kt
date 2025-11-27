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
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 负责创建并管理 MSAL 多账户客户端的中枢类：
 * - 监听授权配置的变化，按需重建 PublicClientApplication
 * - 提供交互式/静默获取令牌、账户查询与移除等能力
 * - 通过 [initializationState] 暴露初始化成功与否，外部需先等待初始化完成
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authConfigRepository: AuthConfigRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var multipleAccountApp: IMultipleAccountPublicClientApplication? = null
    private val initializationState = MutableStateFlow<Boolean?>(null)

    init {
        // 监听配置流：配置变更时重新构建 MSAL 客户端，并重置初始化状态
        scope.launch {
            authConfigRepository.configFlow.collectLatest { config ->
                multipleAccountApp = null
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

    /**
     * 打开账户选择器进行交互式登录，适合首次登录或切换账户。
     */
    fun signIn(activity: Activity, scopes: Array<String>, callback: AuthenticationCallback) {
        val parameters = AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(activity)
            .withScopes(scopes.toList())
            .withPrompt(Prompt.SELECT_ACCOUNT)
            .withCallback(callback)
            .build()
        multipleAccountApp?.acquireToken(parameters)
    }

    /**
     * 交互式获取令牌（不强制弹出账户选择），可用于刷新过期令牌。
     */
    fun acquireToken(activity: Activity, scopes: Array<String>, callback: AuthenticationCallback) {
        val parameters = AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(activity)
            .withScopes(scopes.toList())
            .withCallback(callback)
            .build()
        multipleAccountApp?.acquireToken(parameters)
    }

    /**
     * 静默获取令牌：优先复用缓存，失败再交给回调决定是否走交互式流程。
     */
    fun acquireTokenSilent(account: IAccount, scopes: Array<String>, callback: SilentAuthenticationCallback) {
        val tenantAuthority = account.authority

        val parameters = AcquireTokenSilentParameters.Builder()
            .forAccount(account)
            .fromAuthority(tenantAuthority)
            .withScopes(scopes.toList())
            .withCallback(callback)
            .build()
        multipleAccountApp?.acquireTokenSilentAsync(parameters)
    }

    suspend fun hasCachedAccount(): Boolean = loadAccounts().isNotEmpty()

    suspend fun loadAccounts(): List<IAccount> {
        val initialized = awaitInitialization()
        if (!initialized) return emptyList()
        val app = multipleAccountApp ?: return emptyList()

        return suspendCancellableCoroutine { continuation ->
            app.getAccounts(object : IPublicClientApplication.LoadAccountsCallback {
                override fun onTaskCompleted(result: List<IAccount>) {
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                override fun onError(exception: MsalException) {
                    if (continuation.isActive) {
                        continuation.resume(emptyList())
                    }
                }
            })
        }
    }

    suspend fun getAccountById(homeAccountId: String): IAccount? {
        val initialized = awaitInitialization()
        if (!initialized) return null
        val app = multipleAccountApp ?: return null

        return suspendCancellableCoroutine { continuation ->
            app.getAccount(homeAccountId, object : IMultipleAccountPublicClientApplication.GetAccountCallback {
                override fun onTaskCompleted(result: IAccount?) {
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                override fun onError(exception: MsalException) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            })
        }
    }

    suspend fun removeAccount(account: IAccount): Boolean {
        val initialized = awaitInitialization()
        if (!initialized) return false
        val app = multipleAccountApp ?: return false

        return suspendCancellableCoroutine { continuation ->
            app.removeAccount(account, object : IMultipleAccountPublicClientApplication.RemoveAccountCallback {
                override fun onRemoved() {
                    if (continuation.isActive) {
                        continuation.resume(true)
                    }
                }

                override fun onError(exception: MsalException) {
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            })
        }
    }

    /**
     * 用最新配置创建 MSAL 客户端。必须在主线程调用。
     */
    private suspend fun initializeMsal(config: AuthConfig) {
        val configFile = writeRuntimeConfig(config)
        withContext(Dispatchers.Main) {
            PublicClientApplication.createMultipleAccountPublicClientApplication(
                context,
                configFile,
                object : IPublicClientApplication.IMultipleAccountApplicationCreatedListener {
                    override fun onCreated(application: IMultipleAccountPublicClientApplication) {
                        multipleAccountApp = application
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

    /**
     * 根据存储的客户端 ID 动态生成配置文件，写入应用私有目录。
     */
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

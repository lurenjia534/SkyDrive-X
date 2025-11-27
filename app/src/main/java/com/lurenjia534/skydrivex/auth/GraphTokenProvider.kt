package com.lurenjia534.skydrivex.auth

import com.lurenjia534.skydrivex.data.local.ActiveAccountPreferenceRepository
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 用于向 Graph API 提供访问令牌的简单封装：
 * - 依赖 [ActiveAccountPreferenceRepository] 提取当前激活账户
 * - 统一走静默获取流程，外部只需调用 [getAccessToken]
 */
@Singleton
class GraphTokenProvider @Inject constructor(
    private val authManager: AuthManager,
    private val activeAccountPreferenceRepository: ActiveAccountPreferenceRepository
) {

    /**
     * 返回当前激活账户的访问令牌，若状态异常则抛出具体原因。
     */
    suspend fun getAccessToken(): String {
        val initialized = authManager.awaitInitialization()
        if (!initialized) throw IllegalStateException("MSAL initialization failed")
        val activeId = activeAccountPreferenceRepository.getActiveAccountId()
            ?: throw IllegalStateException("No active account for silent auth")
        val account = authManager.getAccountById(activeId)
            ?: throw IllegalStateException("Active account not found")
        return acquireTokenSilent(account)
    }

    /**
     * 调用 MSAL 静默接口，失败时直接向上抛出异常以便调用方决定后续动作。
     */
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

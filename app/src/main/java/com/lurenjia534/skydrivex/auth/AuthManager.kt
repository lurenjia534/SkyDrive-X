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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(@ApplicationContext context: Context) {

    private var singleAccountApp: ISingleAccountPublicClientApplication? = null
    private val authority = "https://login.microsoftonline.com/common"

    init {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            R.raw.auth_config,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    singleAccountApp = application
                }

                override fun onError(exception: MsalException) {
                    Log.e("AuthManager", "MSAL initialization failed", exception)
                }
            }
        )
    }

    fun signIn(activity: Activity, scopes: Array<String>, callback: AuthenticationCallback) {
        singleAccountApp?.signIn(activity, null, scopes, callback)
    }

    fun acquireToken(activity: Activity, scopes: Array<String>, callback: AuthenticationCallback) {
        singleAccountApp?.acquireToken(activity, scopes, callback)
    }

    fun acquireTokenSilent(scopes: Array<String>, callback: SilentAuthenticationCallback) {
        singleAccountApp?.acquireTokenSilentAsync(scopes, authority, callback)
    }

    fun signOut(callback: ISingleAccountPublicClientApplication.SignOutCallback) {
        singleAccountApp?.signOut(callback)
    }

    fun getCurrentAccount(callback: ISingleAccountPublicClientApplication.CurrentAccountCallback) {
        singleAccountApp?.getCurrentAccountAsync(callback)
    }
}


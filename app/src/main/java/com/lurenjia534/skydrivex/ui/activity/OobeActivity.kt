package com.lurenjia534.skydrivex.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lurenjia534.skydrivex.auth.AuthConfigDefaults
import com.lurenjia534.skydrivex.ui.theme.SkyDriveXTheme
import com.lurenjia534.skydrivex.ui.viewmodel.OobeEvent
import com.lurenjia534.skydrivex.ui.viewmodel.OobeUiState
import com.lurenjia534.skydrivex.ui.viewmodel.OobeViewModel
import dagger.hilt.android.AndroidEntryPoint

enum class OobeMode { INITIAL, UPDATE }

@AndroidEntryPoint
class OobeActivity : ComponentActivity() {

    private val viewModel: OobeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val mode = intent.getStringExtra(EXTRA_MODE)?.let { value ->
            runCatching { OobeMode.valueOf(value) }.getOrNull()
        } ?: OobeMode.INITIAL

        setContent {
            SkyDriveXTheme {
                OobeRoute(
                    viewModel = viewModel,
                    mode = mode,
                    onCompleted = { shouldLogin ->
                        when (mode) {
                            OobeMode.INITIAL -> {
                                val intent = Intent(this, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    putExtra(MainActivity.EXTRA_SKIP_TOKEN_CHECK, true)
                                    putExtra(MainActivity.EXTRA_REQUEST_SIGN_IN, shouldLogin)
                                }
                                startActivity(intent)
                                finish()
                            }

                            OobeMode.UPDATE -> {
                                if (shouldLogin) {
                                    val intent = Intent(this, MainActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        putExtra(MainActivity.EXTRA_SKIP_TOKEN_CHECK, true)
                                        putExtra(MainActivity.EXTRA_REQUEST_SIGN_IN, true)
                                    }
                                    startActivity(intent)
                                }
                                finish()
                            }
                        }
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_MODE = "mode"
    }
}

@Composable
fun OobeRoute(
    viewModel: OobeViewModel = hiltViewModel(),
    mode: OobeMode,
    onCompleted: (shouldLogin: Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is OobeEvent.Completed) {
                onCompleted(event.shouldLogin)
            }
        }
    }

    OobeScreen(
        uiState = uiState,
        mode = mode,
        onClientIdChanged = viewModel::onClientIdChanged,
        onSubmitLogin = viewModel::submitAndLogin,
        onSubmitOnly = viewModel::submitOnly
    )
}

@Composable
fun OobeScreen(
    uiState: OobeUiState,
    mode: OobeMode,
    onClientIdChanged: (String) -> Unit,
    onSubmitLogin: () -> Unit,
    onSubmitOnly: () -> Unit
) {
    val scrollState = rememberScrollState()
    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (mode == OobeMode.INITIAL) "首次启动配置" else "更新登录配置",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "请填写来自 Azure 门户的应用程序 Client ID。其他字段为此应用固定值，需与 Azure 应用注册保持一致。",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = uiState.clientId,
                onValueChange = onClientIdChanged,
                label = { Text("Application (client) ID") },
                placeholder = { Text("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx") },
                isError = uiState.clientIdError != null,
                supportingText = {
                    uiState.clientIdError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = AuthConfigDefaults.REDIRECT_URI,
                onValueChange = {},
                label = { Text("Redirect URI（固定）") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = AuthConfigDefaults.ACCOUNT_MODE,
                onValueChange = {},
                label = { Text("Account Mode（固定）") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = AuthConfigDefaults.AUDIENCE_TYPE,
                onValueChange = {},
                label = { Text("Audience（固定）") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = AuthConfigDefaults.DEFAULT_TENANT_ID,
                onValueChange = {},
                label = { Text("Tenant ID（固定）") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (mode) {
                OobeMode.INITIAL -> {
                    Button(
                        onClick = onSubmitLogin,
                        enabled = !uiState.isSaving,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(text = if (uiState.isSaving) "保存中…" else "登陆")
                    }
                }
                OobeMode.UPDATE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onSubmitOnly,
                            enabled = !uiState.isSaving
                        ) {
                            Text(text = "更新配置")
                        }

                        Button(
                            onClick = onSubmitLogin,
                            enabled = !uiState.isSaving
                        ) {
                            Text(text = if (uiState.isSaving) "保存中…" else "更新配置并登陆")
                        }
                    }
                }
            }
        }
    }
}

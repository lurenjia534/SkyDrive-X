package com.lurenjia534.skydrivex.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HeaderSection(mode = mode)

            CredentialForm(
                uiState = uiState,
                onClientIdChanged = onClientIdChanged
            )

            FixedFieldsSection()

            ActionSection(
                mode = mode,
                isSaving = uiState.isSaving,
                onSubmitLogin = onSubmitLogin,
                onSubmitOnly = onSubmitOnly
            )
        }
    }
}

@Composable
private fun HeaderSection(mode: OobeMode) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = if (mode == OobeMode.INITIAL) Icons.Outlined.Lock else Icons.Outlined.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Column {
                Text(
                    text = if (mode == OobeMode.INITIAL) "首次启动配置" else "更新登录配置",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = if (mode == OobeMode.INITIAL) "请准备好在 Azure 门户注册的应用程序 Client ID，以完成首启绑定。" else "修改 Client ID 后可立即保存或再次登陆，便于切换不同的 MSAL 应用。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "必须先在 Azure AD 中登记本应用的 Redirect URI，确保登陆回调能够成功。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun CredentialForm(
    uiState: OobeUiState,
    onClientIdChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Azure 应用信息",
            style = MaterialTheme.typography.titleMedium
        )
        OutlinedTextField(
            value = uiState.clientId,
            onValueChange = onClientIdChanged,
            label = { Text("Application (client) ID") },
            placeholder = { Text("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx") },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Key, contentDescription = null)
            },
            isError = uiState.clientIdError != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            modifier = Modifier.fillMaxWidth()
        )
        AnimatedVisibility(visible = uiState.clientIdError != null) {
            Text(
                text = uiState.clientIdError.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun FixedFieldsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "固定参数（只读）",
            style = MaterialTheme.typography.titleMedium
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
    }
}

@Composable
private fun ActionSection(
    mode: OobeMode,
    isSaving: Boolean,
    onSubmitLogin: () -> Unit,
    onSubmitOnly: () -> Unit
) {
    when (mode) {
        OobeMode.INITIAL -> {
            FilledTonalButton(
                onClick = onSubmitLogin,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text("保存并登陆")
                }
            }
        }
        OobeMode.UPDATE -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onSubmitOnly,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("仅更新配置")
                }
                FilledTonalButton(
                    onClick = onSubmitLogin,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text("更新配置并登陆")
                    }
                }
            }
        }
    }
}

package com.lurenjia534.skydrivex.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

@AndroidEntryPoint
class OobeActivity : ComponentActivity() {

    private val viewModel: OobeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SkyDriveXTheme {
                OobeRoute(
                    viewModel = viewModel,
                    onCompleted = {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun OobeRoute(
    viewModel: OobeViewModel = hiltViewModel(),
    onCompleted: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is OobeEvent.Completed) {
                onCompleted()
            }
        }
    }

    OobeScreen(
        uiState = uiState,
        onClientIdChanged = viewModel::onClientIdChanged,
        onSubmit = viewModel::submit
    )
}

@Composable
fun OobeScreen(
    uiState: OobeUiState,
    onClientIdChanged: (String) -> Unit,
    onSubmit: () -> Unit
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
                text = "首次启动配置",
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

            Button(
                onClick = onSubmit,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .align(Alignment.End)
            ) {
                Text(
                    text = when {
                        uiState.isSaving -> "保存中…"
                        uiState.hasExistingConfig -> "更新配置"
                        else -> "保存并继续"
                    }
                )
            }
        }
    }
}

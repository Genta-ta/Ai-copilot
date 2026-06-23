package com.genta.copilot.ui

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.Window
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.genta.copilot.providers.ProviderFactory

fun showSettingsDialog(
    activity: Activity,
    getStr: (String, String) -> String,
    putStr: (String, String) -> Unit
) {
    val dialog = Dialog(activity)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    val composeView = ComposeView(activity).apply {
        setViewTreeLifecycleOwner(activity as? androidx.lifecycle.LifecycleOwner)
        setViewTreeSavedStateRegistryOwner(activity as? androidx.savedstate.SavedStateRegistryOwner)

        setContent {
            MaterialTheme {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    SettingsPanel(
                        getStr = getStr,
                        putStr = putStr,
                        onDismiss = { dialog.dismiss() }
                    )
                }
            }
        }
    }

    dialog.setContentView(composeView)
    dialog.window?.setLayout(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
    dialog.show()
}

@Composable
fun SettingsPanel(
    getStr: (String, String) -> String,
    putStr: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var activeProvider by remember {
        mutableStateOf(getStr("active_provider", "Claude"))
    }

    // map provider → state API key
    val apiKeys = ProviderFactory.ALL_PROVIDERS.associateWith { p ->
        remember { mutableStateOf(getStr("api_key_$p", "")) }
    }
    val showPass = ProviderFactory.ALL_PROVIDERS.associateWith { _ ->
        remember { mutableStateOf(false) }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AI Copilot Settings", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onDismiss) { Text("✕") }
        }

        Spacer(Modifier.height(12.dp))
        Text("Provider Aktif", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))

        ProviderFactory.ALL_PROVIDERS.forEach { p ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = activeProvider == p,
                    onClick = {
                        activeProvider = p
                        putStr("active_provider", p)
                    }
                )
                Text(p)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("API Keys", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))

        ProviderFactory.ALL_PROVIDERS.forEach { p ->
            val keyState = apiKeys[p]!!
            val showState = showPass[p]!!

            OutlinedTextField(
                value = keyState.value,
                onValueChange = {
                    keyState.value = it
                    putStr("api_key_$p", it)
                },
                label = { Text("$p API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showState.value)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showState.value = !showState.value }) {
                        Text(if (showState.value) "Hide" else "Show")
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Simpan & Tutup")
        }
    }
}
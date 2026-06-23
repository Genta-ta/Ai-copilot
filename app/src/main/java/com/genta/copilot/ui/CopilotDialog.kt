package com.genta.copilot.ui

import android.app.Activity
import android.app.Dialog
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.Window
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.genta.copilot.providers.AiProvider
import com.genta.copilot.providers.ProviderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun showCopilotDialog(
    activity: Activity,
    scope: CoroutineScope,
    getProvider: () -> AiProvider?
) {
    // ambil teks dari clipboard sebagai code context
    val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val codeContext = clipboard.primaryClip
        ?.getItemAt(0)?.text?.toString() ?: ""

    val dialog = Dialog(activity)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    val composeView = ComposeView(activity).apply {
        // lifecycle owner agar Compose bisa jalan di luar Fragment/Activity
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
                    CopilotPanel(
                        codeContext = codeContext,
                        scope = scope,
                        getProvider = getProvider,
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
fun CopilotPanel(
    codeContext: String,
    scope: CoroutineScope,
    getProvider: () -> AiProvider?,
    onDismiss: () -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val systemPrompt = """
        Kamu adalah AI copilot untuk code editor.
        Jawab singkat dan langsung ke poin.
        Kalau ada kode, format dengan blok kode.
    """.trimIndent()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AI Copilot", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onDismiss) { Text("✕") }
        }

        // Code context preview
        if (codeContext.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Context (dari clipboard):",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = codeContext.take(300).let {
                        if (codeContext.length > 300) "$it..." else it
                    },
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(8.dp)
                        .horizontalScroll(rememberScrollState())
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Prompt input
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Tanya AI...") },
            placeholder = { Text("Contoh: explain ini, refactor, bikin unit test") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4
        )

        Spacer(Modifier.height(8.dp))

        // Send button
        Button(
            onClick = {
                if (prompt.isBlank()) return@Button
                val provider = getProvider()
                if (provider == null) {
                    error = "Set API key dulu di Settings extension!"
                    return@Button
                }
                loading = true
                error = ""
                result = ""
                scope.launch {
                    try {
                        val userMsg = if (codeContext.isNotEmpty())
                            "Code:\n```\n$codeContext\n```\n\n$prompt"
                        else prompt
                        result = provider.complete(systemPrompt, userMsg)
                    } catch (e: Exception) {
                        error = e.message ?: "Unknown error"
                    } finally {
                        loading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading && prompt.isNotBlank()
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Thinking...")
            } else {
                Text("Kirim ke ${getProvider()?.name ?: "AI"}")
            }
        }

        // Error
        if (error.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Result
        if (result.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(8.dp))
            Text("Response:", style = MaterialTheme.typography.labelSmall)
            SelectionContainer {
                Text(
                    text = result,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }
}
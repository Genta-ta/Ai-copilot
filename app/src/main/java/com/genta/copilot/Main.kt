package com.genta.copilot

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.Keep
import com.genta.copilot.providers.AiProvider
import com.genta.copilot.providers.ProviderFactory
import com.genta.copilot.ui.showCopilotDialog
import com.genta.copilot.ui.showSettingsDialog
import com.rk.extension.ExtensionAPI
import com.rk.extension.ExtensionContext

@Keep
@Suppress("unused")
class Main(context: ExtensionContext) : ExtensionAPI(context) {

    private var floatingView: View? = null

    // ambil provider sekarang dari settings
    private fun currentProvider(): AiProvider? {
        val name = context.settings.getString("active_provider", "Claude")
        val key = context.settings.getString("api_key_$name", "")
        if (key.isBlank()) return null
        return try {
            ProviderFactory.create(name, key)
        } catch (e: Exception) {
            null
        }
    }

    override fun onExtensionLoaded() {
        context.logInfo("AI Copilot loaded")
    }

    override fun afterActivityCreated(activity: Activity) {
        injectFloatingButtons(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        floatingView = null
    }

    private fun injectFloatingButtons(activity: Activity) {
        val rootView = activity.window.decorView
            .findViewById<FrameLayout>(android.R.id.content)
            ?: return

        // Container buat 2 tombol: Copilot + Settings
        val container = android.widget.LinearLayout(activity).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                bottomMargin = 200  // hindari navbar
                rightMargin = 32
            }
        }

        // Tombol Settings
        val settingsBtn = android.widget.Button(activity).apply {
            text = "⚙"
            textSize = 16f
            setBackgroundColor(Color.parseColor("#555555"))
            setTextColor(Color.WHITE)
            layoutParams = android.widget.LinearLayout.LayoutParams(120, 80)
            setOnClickListener {
                showSettingsDialog(
                    activity = activity,
                    getStr = { key, def -> context.settings.getString(key, def) },
                    putStr = { key, value -> context.settings.putString(key, value) }
                )
            }
        }

        // Tombol Copilot
        val copilotBtn = android.widget.Button(activity).apply {
            text = "AI"
            textSize = 14f
            setBackgroundColor(Color.parseColor("#7C3AED"))
            setTextColor(Color.WHITE)
            layoutParams = android.widget.LinearLayout.LayoutParams(120, 80)
            setOnClickListener {
                val provider = currentProvider()
                if (provider == null) {
                    Toast.makeText(
                        activity,
                        "Set API key dulu! Tekan ⚙",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                showCopilotDialog(
                    activity = activity,
                    scope = context.scope,
                    getProvider = { currentProvider() }
                )
            }
        }

        container.addView(settingsBtn)
        container.addView(copilotBtn)

        activity.runOnUiThread {
            rootView.addView(container)
        }

        floatingView = container
    }
}
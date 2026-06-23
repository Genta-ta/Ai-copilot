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
// Ubah nama parameter konstruktor menjadi extContext agar bebas dari bentrokan
class Main(private val extContext: ExtensionContext) : ExtensionAPI(extContext) {

    private var floatingView: View? = null

    // Ambil provider sekarang dari settings
    private fun currentProvider(): AiProvider? {
        val name: String = extContext.settings.getString("active_provider", "Claude") ?: "Claude"
        val key: String = extContext.settings.getString("api_key_$name", "") ?: ""
        
        if (key.isBlank()) return null
        return try {
            ProviderFactory.create(name, key)
        } catch (e: Exception) {
            null
        }
    }

    // ==========================================
    // FUNGSI SIKLUS HIDUP EKSTENSI
    // ==========================================

    override fun onExtensionLoaded() {
        extContext.logInfo("AI Copilot loaded")
    }

    override fun onInstalled() {
        // Kosongkan jika tidak ada logika khusus
    }

    override fun onUpdated() {
        // Kosongkan jika tidak ada logika khusus
    }

    override fun onUninstalled() {
        // Kosongkan jika tidak ada logika khusus
    }

    // ==========================================
    // FUNGSI SIKLUS HIDUP ACTIVITY
    // ==========================================

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        injectFloatingButtons(activity)
    }

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        floatingView = null
    }

    // ==========================================
    // LOGIKA INJEKSI TOMBOL FLOATING
    // ==========================================

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
                    getStr = { key, def -> extContext.settings.getString(key, def) ?: def },
                    putStr = { key, value -> extContext.settings.putString(key, value) }
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
                    scope = extContext.scope, // Memakai extContext yang aman
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

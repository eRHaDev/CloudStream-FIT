package com.github.erhadev.onlinefit

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lagradost.api.Log
import com.lagradost.cloudstream3.CommonActivity.showToast
import kotlinx.coroutines.runBlocking
import androidx.core.content.edit
import kotlin.random.Random


/**
 * A simple [Fragment] subclass.
 */
class SettingsFragment(
    plugin: OnlineFITPlugin,
    private val sharedPref: SharedPreferences,
) : BottomSheetDialogFragment() {
    private val res = plugin.resources ?: throw Exception("Unable to read resources")

    private fun <T : View> View.findView(name: String): T {
        val id = res.getIdentifier(name, "id", BuildConfig.LIBRARY_PACKAGE_NAME)
        return this.findViewById(id)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val layout = res.getLayout(res.getIdentifier("settings_fragment", "layout", BuildConfig.LIBRARY_PACKAGE_NAME))
        return inflater.inflate(layout, container, false)
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isDraggable = false
        }
    }


    @SuppressLint("SetJavaScriptEnabled", "SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val statusText = view.findView<TextView>("statusText")
        val loginButton = view.findView<Button>("loginButton")
        val resetButton = view.findView<Button>("resetButton")
        val webView = view.findView<WebView>("authWebView")

        val document = runBlocking {
            OnlineFITPlugin.fetch(OnlineFITPlugin.mainURL).document.toString()
        }

        if (!document.contains("401 Invalid Access Token")) {
            statusText.text = "Status: OK\nUsername: ${sharedPref.getString("oauth_username", null)}"
        }

        setupWebView(webView)

        loginButton.setOnClickListener {
            webView.visibility = View.VISIBLE
            webView.loadUrl(OnlineFITPlugin.mainURL + "?" + Random.nextInt(1000000))
        }

        resetButton.setOnClickListener {
            sharedPref.edit(commit = true) { clear() }

            restartApp()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: WebView) {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.userAgentString =
            "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.90 Mobile Safari/537.36"

        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                val cookiesString = CookieManager.getInstance().getCookie(url)

                if (cookiesString != null && cookiesString.contains("oauth_refresh_token")) {
                    val cookies = mutableMapOf<String, String>()

                    cookiesString.split(";")
                        .map { it.trim() }
                        .forEach {
                            cookies[it.split("=", limit=2)[0]] = it.split("=", limit=2)[1]
                        }

                    activity?.runOnUiThread {
                        sharedPref.edit(commit = true) {
                            for ((k, v) in cookies) {
                                putString(k, v)
                            }
                        }

                        restartApp()
                    }
                }
            }
        }
    }

    private fun restartApp() {
        val context = requireContext().applicationContext
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component

        if (componentName != null) {
            val restartIntent = Intent.makeRestartActivityTask(componentName)
            context.startActivity(restartIntent)
            Runtime.getRuntime().exit(0)
        }
    }
}
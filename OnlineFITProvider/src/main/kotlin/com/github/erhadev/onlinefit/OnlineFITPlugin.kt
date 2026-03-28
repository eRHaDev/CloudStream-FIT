package com.github.erhadev.onlinefit

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.github.erhadev.onlinefit.providers.OnlineFITProvider
import com.github.erhadev.onlinefit.providers.OnlineFITStreamProvider
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.nicehttp.NiceResponse

@CloudstreamPlugin
class OnlineFITPlugin: Plugin() {
    var sharedPref: SharedPreferences? = null;

    companion object {
        var instance: OnlineFITPlugin? = null
        val mainURL = "https://online.fit.cvut.cz/"

        suspend fun fetch(url: String): NiceResponse {
            var response = app.get(
                url,
                headers = mapOf("Cookie" to getCookie())
            )

            if (response.document.toString().contains("401 Invalid Access Token")) {
                val refreshResponse = app.get(
                    url,
                    headers = mapOf("Cookie" to getCookie(false))
                )

                val accessToken = refreshResponse.headers["Set-Cookie"]

                if (accessToken != null && accessToken.contains("oauth_access_token=")) {
                    instance?.sharedPref?.edit(commit = true) {
                        putString("oauth_access_token", accessToken.split(";")[0].split("=", limit=2)[1])
                    }

                    response = app.get(
                        url,
                        headers = mapOf("Cookie" to getCookie())
                    )
                } else {
                    CommonActivity.showToast("Login expired, check OnlineFIT settings!")
                }
            }

            return response
        }

        fun getCookie(access: Boolean = true): String {
            val accessToken = instance?.sharedPref?.getString("oauth_access_token", null)
            val refreshToken = instance?.sharedPref?.getString("oauth_refresh_token", null)
            val username = instance?.sharedPref?.getString("oauth_username", null)

            if (accessToken == "" || refreshToken == "" || username == "") {
                return ""
            }

            var result = "oauth_refresh_token=${refreshToken}; oauth_username=${username}"

            if (access) {
                result += "; oauth_access_token=${accessToken}"
            }

            return result
        }
    }

    override fun load(context: Context) {
        instance = this

        val sharedPref = context.getSharedPreferences("OnlineFIT", Context.MODE_PRIVATE)
        this.sharedPref = sharedPref

        registerMainAPI(OnlineFITProvider())
        registerMainAPI(OnlineFITStreamProvider())

        openSettings = { ctx ->
            val activity = ctx as AppCompatActivity
            val frag = SettingsFragment(this, sharedPref)
            frag.show(activity.supportFragmentManager, "Frag")
        }
    }
}
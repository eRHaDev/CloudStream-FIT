package com.github.erhadev.onlinefit.providers

import com.github.erhadev.onlinefit.OnlineFITPlugin
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newLiveSearchResponse
import com.lagradost.cloudstream3.newLiveStreamLoadResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.newExtractorLink

class OnlineFITStreamProvider : MainAPI() {
    override var mainUrl = OnlineFITPlugin.mainURL
    override var name = "OnlineFIT - Stream"
    override val supportedTypes = setOf(TvType.Live)

    override var lang = "cs"

    override val hasMainPage = true
    override val mainPage = mainPageOf(
        Pair("1", "Stream z místností")
    )

    override suspend fun search(query: String): List<SearchResponse> {
        return OnlineFITPlugin.fetch(
            mainUrl
        ).document.select("body > div > div:nth-child(2) table > tbody > tr").mapNotNull {
            val name = it.selectFirst("th")?.text() ?: return@mapNotNull null
            val link = it.selectFirst("td > a")?.attr("href") ?: return@mapNotNull null

            if (!name.contains(query, ignoreCase = true)) return@mapNotNull null

            newLiveSearchResponse(
                name,
                mainUrl + link,
            ) {
                this.posterUrl =
                    "https://fit.cvut.cz/fakulta/budovy/4393/image-thumb__4393__Block2Image/budova-ntk-prednaskovka.92f2ec57.avif"
            }
        }
    }

    override suspend fun getMainPage(
        page: Int, request: MainPageRequest
    ): HomePageResponse {
        if (page != 1) return newHomePageResponse(request.name, emptyList())

        val response = OnlineFITPlugin.fetch(
            mainUrl
        ).document.select("body > div > div:nth-child(2) table > tbody > tr").mapNotNull {
            val name = it.selectFirst("th")?.text() ?: return@mapNotNull null
            val link = it.selectFirst("td > a")?.attr("href") ?: return@mapNotNull null

            newLiveSearchResponse(
                name, mainUrl + link
            ) {
                this.posterUrl =
                    "https://fit.cvut.cz/fakulta/budovy/4393/image-thumb__4393__Block2Image/budova-ntk-prednaskovka.92f2ec57.avif"
            }
        }

        return newHomePageResponse(request.name, response)
    }

    override suspend fun load(url: String): LoadResponse {
        val document = OnlineFITPlugin.fetch(
            url
        ).document

        var episodeURL = document.select("table > tbody > tr > td > a")?.attr("href")
        episodeURL = url.substring(0, url.lastIndexOf('/')) + "/" + episodeURL

        return newLiveStreamLoadResponse(
            document.selectFirst(".card-title")?.text() ?: "Unknown", url, episodeURL
        ) {
            this.posterUrl =
                "https://www.shutterstock.com/image-illustration/high-school-graduate-hat-masters-260nw-1818847574.jpg"
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        var url = Regex("source: \"(.*)\",").find(
            OnlineFITPlugin.fetch(
                data
            ).document.toString()
        )?.groupValues[1] ?: return false;
        url = data.substring(0, data.lastIndexOf('/')) + "/" + url

        callback.invoke(
            newExtractorLink(
                name, name, url
            ) {
                this.headers = mapOf("Cookie" to OnlineFITPlugin.getCookie())
            })

        return true
    }
}
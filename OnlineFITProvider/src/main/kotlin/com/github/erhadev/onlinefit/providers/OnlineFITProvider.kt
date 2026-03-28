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
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.newExtractorLink
import kotlin.collections.mapOf

class OnlineFITProvider : MainAPI() {
    override var mainUrl = OnlineFITPlugin.mainURL
    override var name = "OnlineFIT - Záznamy"
    override val supportedTypes = setOf(TvType.TvSeries)

    override var lang = "cs"

    override val hasMainPage = true
    override val mainPage = mainPageOf(
        Pair("1", "Předměty v aktivním semestru"),
        Pair("2", "Předměty v minulém semestru")
    )

    override suspend fun search(query: String): List<SearchResponse> {
        val response = mutableListOf<SearchResponse>()

        for (i in 1..4) {
            val semesterURL = getSemesterURL(i) ?: return emptyList()

            OnlineFITPlugin.fetch(
                semesterURL
            ).document
                .select("body table > tbody > tr")
                .forEach {
                    val name = it.selectFirst("th")?.text() ?: return@forEach
                    val link = it.selectFirst("td > a")?.attr("href") ?: return@forEach

                    if (!name.contains(query, ignoreCase = true)) return@forEach

                    response.add(
                        newTvSeriesSearchResponse(
                            "$name (${parseSemesterName(semesterURL)})",
                            semesterURL + link
                        ) {
                            this.posterUrl = "https://www.shutterstock.com/image-illustration/high-school-graduate-hat-masters-260nw-1818847574.jpg"
                        }
                    )
                }
        }

        return response
    }

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        if (page != 1) return newHomePageResponse(request.name, emptyList())

        val semesterURL = getSemesterURL(request.data.toInt()) ?: return newHomePageResponse(request.name, emptyList())

        val response = OnlineFITPlugin.fetch(
            semesterURL
        ).document
            .select("body table > tbody > tr")
            .mapNotNull {
                val name = it.selectFirst("th")?.text() ?: return@mapNotNull null
                val link = it.selectFirst("td > a")?.attr("href") ?: return@mapNotNull null

                newTvSeriesSearchResponse(
                    "$name (${parseSemesterName(semesterURL)})",
                    semesterURL + link,
                    TvType.TvSeries
                ) {
                    this.posterUrl = "https://www.shutterstock.com/image-illustration/high-school-graduate-hat-masters-260nw-1818847574.jpg"
                }
            }

        return newHomePageResponse(request.name, response.sortedBy { it.name })
    }

    override suspend fun load(url: String): LoadResponse {
        val document = OnlineFITPlugin.fetch(
            url
        ).document

        return newTvSeriesLoadResponse(
            document.selectFirst(".card-title")?.text() ?: "Unknown",
            url,
            TvType.TvSeries,
            document.select("table > tbody > tr").mapNotNull {
                var episodeURL = it.select("td > a")?.attr("href");
                episodeURL = url.substring(0, url.lastIndexOf('/')) + "/" + episodeURL

                newEpisode(
                    episodeURL
                ) {
                    this.name = it.select("th").text()
                    this.posterUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/58/CTU_-_New_Building_Dejvice%2C_an_entrance.jpg/250px-CTU_-_New_Building_Dejvice%2C_an_entrance.jpg"
                }
            }
        ) {
            this.posterUrl = "https://www.shutterstock.com/image-illustration/high-school-graduate-hat-masters-260nw-1818847574.jpg"
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        var url = Regex("source: \"(.*)\",").find(OnlineFITPlugin.fetch(data).document.toString())?.groupValues[1] ?: return false;
        url = data.substring(0, data.lastIndexOf('/')) + "/" + url

        callback.invoke(
            newExtractorLink(
                name,
                name,
                url
            ) {
                this.headers = mapOf("Cookie" to OnlineFITPlugin.getCookie())
            }
        )

        return true
    }

    suspend fun getSemesterURL(index: Int): String? {
        val semesterURL = OnlineFITPlugin.fetch(
            mainUrl
        ).document
            .selectFirst("body > div > div:nth-child(3) table > tbody > tr:nth-last-child($index) > td > a")?.attr("href")
            ?: return null

        return mainUrl + semesterURL.replace("index.html", "")
    }

    fun parseSemesterName(url: String): String {
        return url.split("/").reversed()[1]
    }
}
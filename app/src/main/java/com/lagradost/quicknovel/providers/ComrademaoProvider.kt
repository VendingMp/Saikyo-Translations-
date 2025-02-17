package com.lagradost.quicknovel.providers

import com.lagradost.quicknovel.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class ComrademaoProvider : MainAPI() {
    override val name = "Comrademao"
    override val mainUrl = "https://comrademao.com"

    override fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query&post_type=novel"
        val response = khttp.get(url)
        val document = Jsoup.parse(response.text)
        val items = document.select(".bs")
        return items.mapNotNull {
            val poster = it.selectFirst("img")?.attr("src")
            val titleHolder = it.selectFirst("a")
            val title = titleHolder?.text() ?: return@mapNotNull null
            val href = titleHolder.attr("href")
            SearchResponse(title, href, poster, null, null, this.name)
        }
    }

    override fun loadHtml(url: String): String? {
        val response = khttp.get(url)
        val document = Jsoup.parse(response.text)
        return document.selectFirst("div[readability]")?.html()
            ?.replace("(end of this chapter)", "", ignoreCase = true)
    }

    override fun load(url: String): LoadResponse {
        val response = khttp.get(url)
        val document = Jsoup.parse(response.text)
        val novelInfo = document.selectFirst("div.thumb > img")
        val mainDivs = document.select("div.infox")

        val title = novelInfo.attr("title").replace(" – Comrade Mao", "")
        val poster = novelInfo.attr("src")

        val descript = document.select("div.wd-full p")?.lastOrNull()?.text()
        var genres: ArrayList<String>? = null
        var tags: ArrayList<String>? = null
        var author: String? = null
        var status: String? = null

        fun handleType(element: Element) {
            val txt = element.text()
            println("HANDLE TAG $txt")
            when {
                txt.contains("Genre") -> {
                    genres = ArrayList(element.select("a").map { it.text() })
                }
                txt.contains("Tag") -> {
                    tags = ArrayList(element.select("a").map { it.text() })
                }
                txt.contains("Publisher") -> {
                    author = element.selectFirst("a")?.text()
                }
                txt.contains("Status") -> {
                    status = element.selectFirst("a")?.text()
                }
            }
        }

        mainDivs.select(".wd-full").forEach(::handleType)

        if (genres == null) {
            genres = tags
        } else {
            genres?.addAll(tags ?: listOf())
        }

        val statusInt = when (status) {
            "On-going" -> STATUS_ONGOING
            "Complete" -> STATUS_COMPLETE
            else -> STATUS_NULL
        }

        val chapters = document.select("li[data-num]").map {
            val name = it.select(".chapternum").text()
            val date = it.select(".chapterdate").text()
            val chapUrl = it.select("a").attr("href")
            ChapterData(name, chapUrl, date, null)
        }.reversed()

        return LoadResponse(
            url,
            title,
            chapters,
            author,
            poster,
            null,
            null,
            null,
            descript,
            genres,
            statusInt,
        )
    }
}
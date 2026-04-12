package com.sarmaya.app.network.rss

import android.util.Xml
import com.sarmaya.app.data.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

class GoogleRssParser {
    companion object {
        private val RSS_URL = "https://news.google.com/rss/search?q=pakistan+stock+exchange+OR+PSX&hl=en-PK&gl=PK&ceid=PK:en"
        private val DATE_FORMAT = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)
    }

    suspend fun fetchAndParse(): List<NewsArticle> = withContext(Dispatchers.IO) {
        val articles = mutableListOf<NewsArticle>()
        try {
            val url = URL(RSS_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.readTimeout = 10000
            conn.connectTimeout = 15000
            conn.requestMethod = "GET"
            conn.doInput = true
            conn.connect()

            val stream = conn.inputStream
            articles.addAll(parseXml(stream))
            stream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        articles
    }

    private fun parseXml(inputStream: InputStream): List<NewsArticle> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)
        parser.nextTag()

        val articles = mutableListOf<NewsArticle>()
        var insideChannel = false

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                if (parser.name == "channel") {
                    insideChannel = true
                } else if (insideChannel && parser.name == "item") {
                    val article = parseItem(parser)
                    if (article != null) {
                        articles.add(article)
                    }
                }
            }
            parser.next()
        }
        return articles
    }

    private fun parseItem(parser: XmlPullParser): NewsArticle? {
        var title = ""
        var link = ""
        var description = ""
        var pubDate = 0L
        var source = "Google News"

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "title" -> title = readText(parser)
                "link" -> link = readText(parser)
                "description" -> description = readText(parser)
                "pubDate" -> {
                    val dateString = readText(parser)
                    try {
                        pubDate = DATE_FORMAT.parse(dateString)?.time ?: 0L
                    } catch (e: Exception) { }
                }
                "source" -> source = readText(parser)
                else -> skip(parser)
            }
        }
        
        if (title.isBlank() || link.isBlank()) return null
        
        return NewsArticle(
            id = link,
            title = title,
            link = link,
            description = description,
            pubDate = pubDate,
            source = source,
            category = "Market",
            relatedSector = null,
            cachedAt = System.currentTimeMillis()
        )
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) return
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}

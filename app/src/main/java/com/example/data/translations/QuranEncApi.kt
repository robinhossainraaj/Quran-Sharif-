package com.example.data.translations

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

/** One translation edition from the QuranEnc.com catalogue. */
data class RemoteTranslation(
    val key: String,
    val languageCode: String,
    val direction: String,
    val version: String,
    val title: String,
    val description: String,
    val lastUpdate: Long
) {
    /** Id used inside the app database. */
    val id: String get() = "qe_$key"
}

data class SuraAyah(val aya: Int, val text: String)

/** Small client for the public QuranEnc API (https://quranenc.com/en/home/api/). */
object QuranEncApi {

    private const val BASE = "https://quranenc.com/api/v1"
    const val SOURCE_NAME = "QuranEnc.com"
    const val SOURCE_URL = "https://quranenc.com"

    /** Whole catalogue, all languages. */
    fun fetchCatalog(): List<RemoteTranslation> {
        val root = JSONObject(httpGet("$BASE/translations/list/?localization=en"))
        val arr = root.optJSONArray("translations") ?: JSONArray()
        val out = ArrayList<RemoteTranslation>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val key = o.optString("key")
            val lang = o.optString("language_iso_code")
            if (key.isBlank() || lang.isBlank()) continue
            out.add(
                RemoteTranslation(
                    key = key,
                    languageCode = lang,
                    direction = o.optString("direction", "ltr"),
                    version = o.optString("version", ""),
                    title = o.optString("title", key),
                    description = o.optString("description", ""),
                    lastUpdate = o.optLong("last_update", 0L)
                )
            )
        }
        return out
    }

    /** All ayahs of one surah for one translation. */
    fun fetchSura(key: String, sura: Int): List<SuraAyah> {
        val body = httpGet("$BASE/translation/sura/$key/$sura")
        val trimmed = body.trim()
        val arr: JSONArray = if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            val root = JSONObject(trimmed)
            root.optJSONArray("result") ?: firstArray(root) ?: JSONArray()
        }
        val out = ArrayList<SuraAyah>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val aya = o.optString("aya").trim().toIntOrNull() ?: continue
            val text = o.optString("translation").trim()
            out.add(SuraAyah(aya, text))
        }
        return out
    }

    private fun firstArray(root: JSONObject): JSONArray? {
        val names = root.keys()
        while (names.hasNext()) {
            val v = root.opt(names.next())
            if (v is JSONArray) return v
        }
        return null
    }

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "QuranSharif-Android")
            val code = conn.responseCode
            if (code != 200) throw IOException("HTTP $code")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}

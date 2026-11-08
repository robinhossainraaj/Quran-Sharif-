package com.example.audio.catalog

import android.content.Context
import android.util.Log
import org.json.JSONObject

object ReciterCatalogLoader {
    private const val TAG = "ReciterCatalogLoader"
    private const val ASSET_PATH = "audio/reciters.json"

    private var cachedCatalog: ReciterCatalog? = null

    fun load(context: Context): ReciterCatalog {
        cachedCatalog?.let { return it }
        val jsonString = try {
            context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read $ASSET_PATH from assets", e)
            ""
        }
        val catalog = parseJson(jsonString)
        cachedCatalog = catalog
        return catalog
    }

    fun parseJson(jsonString: String): ReciterCatalog {
        if (jsonString.isBlank()) {
            return ReciterCatalog(
                cdnBase = "https://cdn.islamic.network/quran/audio",
                reciters = listOf(ReciterCatalog.fallbackReciter)
            )
        }

        try {
            val root = JSONObject(jsonString)
            var cdnBase = root.optString("cdnBase", "https://cdn.islamic.network/quran/audio").trim()
            if (!cdnBase.startsWith("https://", ignoreCase = true)) {
                Log.w(TAG, "cdnBase is not HTTPS: $cdnBase. Falling back to default secure CDN.")
                cdnBase = "https://cdn.islamic.network/quran/audio"
            }
            // Strip trailing slash
            cdnBase = cdnBase.removeSuffix("/")

            val recitersArray = root.optJSONArray("reciters")
            val reciters = mutableListOf<Reciter>()
            val seenIds = mutableSetOf<String>()

            if (recitersArray != null) {
                for (i in 0 until recitersArray.length()) {
                    val obj = recitersArray.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "").trim()
                    val edition = obj.optString("edition", "").trim()
                    val nameEn = obj.optString("nameEn", "").trim()
                    val nameBn = obj.optString("nameBn", "").trim()
                    val style = obj.optString("style", "Murattal").trim()
                    val bitrate = obj.optInt("bitrate", 128)
                    val isDef = obj.optBoolean("default", false)

                    if (id.isEmpty() || edition.isEmpty() || nameEn.isEmpty()) {
                        Log.w(TAG, "Skipping invalid reciter at index $i: missing required fields")
                        continue
                    }
                    if (seenIds.contains(id.lowercase())) {
                        Log.w(TAG, "Skipping duplicate reciter id '$id' at index $i")
                        continue
                    }
                    seenIds.add(id.lowercase())

                    reciters.add(
                        Reciter(
                            id = id,
                            edition = edition,
                            nameEn = nameEn,
                            nameBn = nameBn,
                            style = style,
                            bitrate = bitrate,
                            isDefault = isDef
                        )
                    )
                }
            }

            if (reciters.isEmpty()) {
                reciters.add(ReciterCatalog.fallbackReciter)
            }

            // Ensure exactly one default
            val defaultsCount = reciters.count { it.isDefault }
            val sanitizedReciters = when {
                defaultsCount == 1 -> reciters
                defaultsCount > 1 -> {
                    var firstDefaultFound = false
                    reciters.map { r ->
                        if (r.isDefault) {
                            if (!firstDefaultFound) {
                                firstDefaultFound = true
                                r
                            } else {
                                r.copy(isDefault = false)
                            }
                        } else r
                    }
                }
                else -> {
                    // No default found, mark first as default
                    reciters.mapIndexed { idx, r ->
                        if (idx == 0) r.copy(isDefault = true) else r
                    }
                }
            }

            return ReciterCatalog(cdnBase = cdnBase, reciters = sanitizedReciters)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing reciters JSON", e)
            return ReciterCatalog(
                cdnBase = "https://cdn.islamic.network/quran/audio",
                reciters = listOf(ReciterCatalog.fallbackReciter)
            )
        }
    }
}

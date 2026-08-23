package com.trucdecomptable.lumen

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Vérification de mise à jour via GitHub Releases (repo public, pas de clé).
 * Comparaison versionName locale (BuildConfig) vs tag distant.
 */
object UpdateChecker {

    const val REPO = "ClawFabriceH92/lumen-horloge"
    private const val API_LATEST = "https://api.github.com/repos/$REPO/releases/latest"

    data class UpdateInfo(
        val tag: String,       // ex: "v1.1"
        val apkUrl: String,    // lien direct .apk
        val isNewer: Boolean
    )

    fun latest(): UpdateInfo? {
        var conn: HttpURLConnection? = null
        return try {
            conn = URL(API_LATEST).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            if (conn.responseCode != 200) return null

            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val tag = json.optString("tag_name", "")
            if (tag.isBlank()) return null

            var apk = "https://github.com/$REPO/releases/download/$tag/app-debug.apk"
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name", "").endsWith(".apk")) {
                        apk = a.optString("browser_download_url", apk)
                        break
                    }
                }
            }
            UpdateInfo(tag, apk, isNewerVersion(tag, BuildConfig.VERSION_NAME))
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    /** true si `remote` (ex: "v1.2") est strictement plus haut que `local` (ex: "1.1") */
    fun isNewerVersion(remote: String, local: String): Boolean {
        val r = remote.removePrefix("v").split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
        val l = local.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrNull(i) ?: 0
            val b = l.getOrNull(i) ?: 0
            if (a != b) return a > b
        }
        return false
    }
}

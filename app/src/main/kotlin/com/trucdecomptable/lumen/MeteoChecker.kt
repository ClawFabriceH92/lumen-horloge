package com.trucdecomptable.lumen

// Récupère la météo via Open-Meteo (gratuit, sans clé API, géolocalisation IP).
// https://open-meteo.com/ — réponse JSON minimaliste.
object MeteoChecker {

    data class Meso(
        val temperature: Double,  // °C
        val condition: String     // libellé FR
    )

    private fun conditionFR(code: Int): String = when (code) {
        0 -> "Dégagé"
        1 -> "Peu nuageux"
        2 -> "Nuageux"
        3 -> "Couché nuageux"
        45, 48 -> "Brouillard"
        51, 53, 55 -> "Bruine"
        56, 57 -> "Bruine verglacante"
        61 -> "Pluie faible"
        63 -> "Pluie modérée"
        65 -> "Pluie forte"
        66, 67 -> "Pluie verglacante"
        71 -> "Neige faible"
        73 -> "Neige modérée"
        75 -> "Neige forte"
        77 -> "Grains de neige"
        80, 81, 82 -> "Averses"
        85, 86 -> "Averses de neige"
        95 -> "Orage"
        96, 99 -> "Orage + grêle"
        else -> ""
    }

    suspend fun latest(): Meso? {
        return try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val client = java.net.URL("https://open-meteo.com/v1/forecast?current=temperature_2m,weather_code").openConnection() as java.net.HttpURLConnection
                client.connectTimeout = 8000
                client.readTimeout = 8000
                client.instanceFollowRedirects = true
                if (client.responseCode != 200) return@withContext null
                val json = client.inputStream.bufferedReader().use { it.readText() }
                client.disconnect()
                val tempMatch = Regex("\"temperature_2m\":(-?\\d+\\.?\\d*)").find(json) ?: return@withContext null
                val codeMatch = Regex("\"weather_code\":(\\d+)").find(json) ?: return@withContext null
                Meso(
                    temperature = tempMatch.groupValues[1].toDouble(),
                    condition = conditionFR(codeMatch.groupValues[1].toInt())
                )
            }
        } catch (_: Exception) {
            null
        }
    }
}

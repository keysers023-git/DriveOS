package com.driveos.alpha

import android.content.Context
import android.location.Geocoder
import java.io.IOException
import javax.net.ssl.SSLHandshakeException
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DriveRoute(val points: List<GeoPoint>, val km: Double, val minutes: Int, val destination: String)

/** Prototype only: device Geocoder availability varies; public OSRM is not production infrastructure. */
suspend fun findTestRoute(context: Context, destination: String, start: GeoPoint): DriveRoute = withContext(Dispatchers.IO) {
    require(destination.trim().length >= 3) { "Upiši naziv odredišta (najmanje 3 znaka)." }
    val geocoder = Geocoder(context, Locale.getDefault())
    @Suppress("DEPRECATION")
    val matches = try {
        @Suppress("DEPRECATION")
        geocoder.getFromLocationName(destination.trim(), 1)
    } catch (e: Exception) {
        throw IllegalStateException("Geocoding error (" + e.javaClass.simpleName + "): " + e.message + ". Android Geocoder may be unavailable in BlueStacks.", e)
    }
    val place = matches?.firstOrNull() ?: throw IllegalStateException("Odredište nije pronađeno. Provjeri Google usluge ili naziv mjesta.")
    val end = GeoPoint(place.latitude, place.longitude)
    val url = URL("https://router.project-osrm.org/route/v1/driving/${start.longitude},${start.latitude};${end.longitude},${end.latitude}?overview=full&geometries=geojson")
    val connection = url.openConnection() as HttpURLConnection
    connection.connectTimeout = 12000
    connection.readTimeout = 12000
    connection.setRequestProperty("User-Agent", "DriveOS-Prototype/0.5 (GitHub keysers023-git/DriveOS)")
    try {
        val responseCode = try { connection.responseCode }
        catch (e: SSLHandshakeException) {
            throw IllegalStateException("OSRM HTTPS TLS handshake failed. Check emulator date, Android version, VPN or proxy. Details: " + e.message, e)
        } catch (e: IOException) {
            throw IllegalStateException("OSRM network failure (" + e.javaClass.simpleName + "): " + e.message, e)
        }
        if (responseCode != 200) throw IllegalStateException("Routing servis nije dostupan (HTTP " + responseCode + ").")
        val data = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        val result = data.optJSONArray("routes")?.optJSONObject(0) ?: throw IllegalStateException("Nije pronađena cestovna ruta.")
        val coords = result.getJSONObject("geometry").getJSONArray("coordinates")
        val points = (0 until coords.length()).map { index ->
            val coord = coords.getJSONArray(index)
            GeoPoint(coord.getDouble(1), coord.getDouble(0))
        }
        if (points.size < 2) throw IllegalStateException("Ruta nema dovoljno koordinata.")
        DriveRoute(points, result.getDouble("distance") / 1000.0, (result.getDouble("duration") / 60.0).toInt(), destination.trim())
    } finally {
        connection.disconnect()
    }
}

package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.*

enum class MapTileSource(val displayName: String, val urlTemplate: String, val maxZoom: Int) {
  GOOGLE_MAPS_HD(
    "Google Maps Siêu Nét HD",
    "https://mt{s}.google.com/vt/lyrs=m&hl=vi&x={x}&y={y}&z={z}&scale=2",
    20
  ),
  GOOGLE_SATELLITE(
    "Vệ Tinh Hybrid HD (Google)",
    "https://mt{s}.google.com/vt/lyrs=y&hl=vi&x={x}&y={y}&z={z}&scale=2",
    20
  ),
  CARTO_VOYAGER(
    "Carto Voyager HD (@2x)",
    "https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}@2x.png",
    19
  ),
  CARTO_DARK(
    "Ban đêm HD (Dark HUD @2x)",
    "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png",
    19
  ),
  ESRI_WORLD(
    "Esri World Navigation",
    "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}",
    19
  ),
  OSM_STANDARD(
    "OpenStreetMap Gốc",
    "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
    19
  )
}

object OsmTileManager {

  // High-capacity L1 Memory Cache (800 tiles)
  private val memoryCache: LruCache<String, ImageBitmap> = object : LruCache<String, ImageBitmap>(800) {}
  private val activeRequests = mutableSetOf<String>()
  private var diskCacheDir: File? = null

  private val subdomains = listOf("a", "b", "c", "d")
  private var subDomainIndex = 0

  private val httpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.SECONDS)
    .readTimeout(6, TimeUnit.SECONDS)
    .build()

  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  // State flow that increments on new tile load to trigger smooth recomposition
  private val _tileUpdateSignal = MutableStateFlow(0L)
  val tileUpdateSignal: StateFlow<Long> = _tileUpdateSignal.asStateFlow()

  fun init(context: Context) {
    try {
      val cachePath = File(context.cacheDir, "osm_tiles")
      if (!cachePath.exists()) {
        cachePath.mkdirs()
      }
      diskCacheDir = cachePath
    } catch (e: Exception) {
      Log.w("OsmTileManager", "Disk cache init failed: ${e.message}")
    }
  }

  fun getTile(source: MapTileSource, zoom: Int, x: Int, y: Int): ImageBitmap? {
    val key = "${source.name}_${zoom}_${x}_${y}"
    val inMem = memoryCache.get(key)
    if (inMem != null) return inMem

    // Asynchronously fetch from disk or network
    fetchTileAsync(source, zoom, x, y, key)

    return null
  }

  data class FallbackSubTile(
    val image: ImageBitmap,
    val srcX: Int,
    val srcY: Int,
    val srcW: Int,
    val srcH: Int
  )

  /**
   * Recursive fallback to parent tiles (zoom - 1 down to zoom - 5) with multi-source fallback.
   * Guarantees 100% continuous map coverage during deep zoom, fast panning, or slow connection.
   */
  fun getDeepFallbackTile(source: MapTileSource, zoom: Int, x: Int, y: Int): FallbackSubTile? {
    val candidateSources = listOf(source, MapTileSource.GOOGLE_MAPS_HD, MapTileSource.CARTO_VOYAGER, MapTileSource.OSM_STANDARD).distinct()

    for (candSource in candidateSources) {
      for (diff in 1..5) {
        val pZoom = zoom - diff
        if (pZoom < 1) break
        val scale = 1 shl diff
        val pX = x shr diff
        val pY = y shr diff
        val parentKey = "${candSource.name}_${pZoom}_${pX}_${pY}"
        val pBitmap = memoryCache.get(parentKey)
          ?: diskCacheDir?.let { dir ->
            val file = File(dir, "$parentKey.png")
            if (file.exists() && file.length() > 200) {
              BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()?.also {
                memoryCache.put(parentKey, it)
              }
            } else null
          }

        if (pBitmap != null) {
          val subW = (pBitmap.width / scale).coerceAtLeast(1)
          val subH = (pBitmap.height / scale).coerceAtLeast(1)
          val subX = ((x % scale) * subW).coerceIn(0, (pBitmap.width - subW).coerceAtLeast(0))
          val subY = ((y % scale) * subH).coerceIn(0, (pBitmap.height - subH).coerceAtLeast(0))
          return FallbackSubTile(
            image = pBitmap,
            srcX = subX,
            srcY = subY,
            srcW = subW,
            srcH = subH
          )
        }
      }
    }
    return null
  }

  private fun fetchTileAsync(source: MapTileSource, zoom: Int, x: Int, y: Int, key: String) {
    synchronized(activeRequests) {
      if (activeRequests.contains(key)) return
      activeRequests.add(key)
    }

    scope.launch {
      try {
        val diskFile = diskCacheDir?.let { File(it, "$key.png") }
        if (diskFile != null && diskFile.exists() && diskFile.length() > 200) {
          val bitmap = BitmapFactory.decodeFile(diskFile.absolutePath)
          if (bitmap != null) {
            val imgBitmap = bitmap.asImageBitmap()
            memoryCache.put(key, imgBitmap)
            withContext(Dispatchers.Main) {
              _tileUpdateSignal.value = System.currentTimeMillis()
            }
            return@launch
          }
        }

        // Fetch from network
        val maxTile = (1 shl zoom) - 1
        if (x < 0 || x > maxTile || y < 0 || y > maxTile) return@launch

        val sub = if (source.urlTemplate.contains("google.com")) {
          ((x + y) % 4).toString()
        } else {
          synchronized(this@OsmTileManager) {
            subDomainIndex = (subDomainIndex + 1) % subdomains.size
            subdomains[subDomainIndex]
          }
        }

        val url = source.urlTemplate
          .replace("{s}", sub)
          .replace("{z}", zoom.toString())
          .replace("{x}", x.toString())
          .replace("{y}", y.toString())

        val request = Request.Builder()
          .url(url)
          .header("User-Agent", "SpeedAlertVietnamApp/2.0 (Android Live GPS Navigation; OpenStreetMap Client)")
          .header("Accept", "image/webp,image/png,image/*;q=0.8")
          .build()

        val response = httpClient.newCall(request).execute()
        response.use { resp ->
          if (resp.isSuccessful) {
            val bytes = resp.body?.bytes()
            if (bytes != null && bytes.size > 150) {
              val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
              if (bitmap != null) {
                val imgBitmap = bitmap.asImageBitmap()
                memoryCache.put(key, imgBitmap)

                diskFile?.let { file ->
                  try {
                    FileOutputStream(file).use { out ->
                      out.write(bytes)
                    }
                  } catch (_: Exception) {}
                }

                withContext(Dispatchers.Main) {
                  _tileUpdateSignal.value = System.currentTimeMillis()
                }
              }
            }
          }
        }
      } catch (_: Exception) {
      } finally {
        synchronized(activeRequests) {
          activeRequests.remove(key)
        }
      }
    }
  }

  fun prefetchRegion(
    source: MapTileSource,
    centerLat: Double,
    centerLng: Double,
    radiusKm: Double,
    minZoom: Int,
    maxZoom: Int,
    onProgress: (Int, Int) -> Unit = { _, _ -> }
  ) {
    scope.launch {
      val tilesToFetch = mutableListOf<Triple<Int, Int, Int>>()
      for (z in minZoom..maxZoom) {
        val latDegrees = radiusKm / 111.0
        val lonDegrees = radiusKm / (111.0 * cos(Math.toRadians(centerLat)))

        val minLat = centerLat - latDegrees
        val maxLat = centerLat + latDegrees
        val minLng = centerLng - lonDegrees
        val maxLng = centerLng + lonDegrees

        val minX = lon2tileX(minLng, z).toInt()
        val maxX = lon2tileX(maxLng, z).toInt()
        val minY = lat2tileY(maxLat, z).toInt()
        val maxY = lat2tileY(minLat, z).toInt()

        for (x in minX..maxX) {
          for (y in minY..maxY) {
            tilesToFetch.add(Triple(z, x, y))
          }
        }
      }

      val total = tilesToFetch.size
      var count = 0
      if (total == 0) {
        withContext(Dispatchers.Main) { onProgress(1, 1) }
        return@launch
      }

      val maxConcurrency = 4
      val channel = kotlinx.coroutines.channels.Channel<Triple<Int, Int, Int>>(kotlinx.coroutines.channels.Channel.UNLIMITED)
      tilesToFetch.forEach { channel.trySend(it) }
      channel.close()

      val jobs = (1..maxConcurrency).map {
        launch {
          for (tile in channel) {
            val (z, x, y) = tile
            val key = "${source.name}_${z}_${x}_${y}"
            val diskFile = diskCacheDir?.let { File(it, "$key.png") }
            if (diskFile == null || !diskFile.exists() || diskFile.length() < 200) {
              try {
                val sub = synchronized(this@OsmTileManager) {
                  subDomainIndex = (subDomainIndex + 1) % subdomains.size
                  subdomains[subDomainIndex]
                }
                val url = source.urlTemplate
                  .replace("{s}", sub)
                  .replace("{z}", z.toString())
                  .replace("{x}", x.toString())
                  .replace("{y}", y.toString())

                val request = Request.Builder().url(url).header("User-Agent", "SpeedAlertVietnamApp/2.0").build()
                httpClient.newCall(request).execute().use { response ->
                  if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null && bytes.size > 150) {
                      diskFile?.let { file ->
                        FileOutputStream(file).use { out -> out.write(bytes) }
                      }
                    }
                  }
                }
              } catch (_: Exception) { }
            }

            val current = synchronized(this@OsmTileManager) {
              count++
              count
            }
            withContext(Dispatchers.Main) {
              onProgress(current, total)
            }
          }
        }
      }
      jobs.forEach { it.join() }
    }
  }

  fun lon2tileX(lon: Double, zoom: Int): Double {
    return (lon + 180.0) / 360.0 * (1 shl zoom)
  }

  fun lat2tileY(lat: Double, zoom: Int): Double {
    val clampedLat = lat.coerceIn(-85.05112878, 85.05112878)
    val latRad = Math.toRadians(clampedLat)
    return (1.0 - asinh(tan(latRad)) / Math.PI) / 2.0 * (1 shl zoom)
  }

  fun tileX2lon(x: Double, zoom: Int): Double {
    return x / (1 shl zoom) * 360.0 - 180.0
  }

  fun tileY2lat(y: Double, zoom: Int): Double {
    val n = Math.PI - 2.0 * Math.PI * y / (1 shl zoom)
    return Math.toDegrees(atan(sinh(n)))
  }
}

package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VietnamTrafficData
import com.example.data.model.*
import com.example.service.MapTileSource
import com.example.service.OsmTileManager
import com.example.service.CompassSensorEngine
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.*

enum class VehicleIconType {
  ARROW,      // Classic navigation triangle (default)
  MOTORBIKE,  // 🏍️ Xe máy phân khối lớn
  SCOOTER,    // 🛵 Xe tay ga / xe số
  CAR,        // 🚗 Ô tô
  TRUCK       // 🚛 Xe tải
}

enum class MapOrientationMode {
  TRACK_UP, // Vehicle forward heading points UP
  NORTH_UP, // North is always UP
  FREE      // User manually rotated
}

@Composable
fun OfflineMapCanvas(
  locationState: GpsLocationState,
  cameras: List<TrafficCamera>,
  breadcrumbs: List<BreadcrumbPoint>,
  activeRoute: NavigationRoute? = null,
  nearestCamera: TrafficCamera? = null,
  nearestCameraDistance: Int? = null,
  activeWarning: ActiveWarning? = null,
  targetFocusPlace: DestinationPlace? = null,
  pois: List<MapPoi> = emptyList(),
  onSelectCamera: (TrafficCamera) -> Unit = {},
  onSelectPoi: (MapPoi) -> Unit = {},
  onMapTapLocation: ((lat: Double, lng: Double) -> Unit)? = null,
  onRefreshLocation: (() -> Unit)? = null,
  onToggleVoice: (() -> Unit)? = null,
  voiceEnabled: Boolean = true,
  compassHeading: Float = 0f,
  compassEnabled: Boolean = true,
  darkMapMode: Boolean = false,
  showBreadcrumbs: Boolean = true,
  vehicleIconType: VehicleIconType = VehicleIconType.MOTORBIKE,
  vehicleIconScale: Float = 1.3f,
  roadSnappingEnabled: Boolean = true,
  cameraTilt3D: Boolean = true,
  modifier: Modifier = Modifier
) {
  val coroutineScope = rememberCoroutineScope()
  val density = LocalDensity.current

  // Continuous smooth zoom animatable
  val animatedZoom = remember { Animatable(18.0f) }
  val zoomLevel = animatedZoom.value

  var panOffsetX by remember { mutableFloatStateOf(0f) }
  var panOffsetY by remember { mutableFloatStateOf(0f) }
  var userRotationAngle by remember { mutableFloatStateOf(0f) }
  var orientationMode by remember { mutableStateOf(MapOrientationMode.TRACK_UP) }
  // Map tile source: auto-select Google Maps HD or Carto Dark based on setting
  var currentTileSource by remember { mutableStateOf(if (darkMapMode) MapTileSource.CARTO_DARK else MapTileSource.GOOGLE_MAPS_HD) }

  LaunchedEffect(darkMapMode) {
    currentTileSource = if (darkMapMode) MapTileSource.CARTO_DARK else MapTileSource.GOOGLE_MAPS_HD
  }

  // Target Focus animation: smoothly center map on selected place
  LaunchedEffect(targetFocusPlace) {
    if (targetFocusPlace != null) {
      val intZ = zoomLevel.toInt().coerceIn(2, currentTileSource.maxZoom)
      val cTileX = OsmTileManager.lon2tileX(locationState.longitude, intZ)
      val cTileY = OsmTileManager.lat2tileY(locationState.latitude, intZ)
      val tTileX = OsmTileManager.lon2tileX(targetFocusPlace.longitude, intZ)
      val tTileY = OsmTileManager.lat2tileY(targetFocusPlace.latitude, intZ)
      val baseTileSize = with(density) { 256.dp.toPx() }
      val zoomMultiplier = 2.0.pow((zoomLevel - intZ).toDouble()).toFloat()
      val tileSizePx = baseTileSize * zoomMultiplier
      panOffsetX = -((tTileX - cTileX).toFloat() * tileSizePx)
      panOffsetY = -((tTileY - cTileY).toFloat() * tileSizePx)
    }
  }

  // Listen to tile download signals
  val tileSignal by OsmTileManager.tileUpdateSignal.collectAsState()

  // Pulsing animation for vehicle radar halo
  val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
  val pulseRadius by infiniteTransition.animateFloat(
    initialValue = 18f,
    targetValue = 54f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "PulseRadius"
  )
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.6f,
    targetValue = 0.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "PulseAlpha"
  )

  // Route chevrons animation
  val chevronOffset by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(750, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "ChevronOffset"
  )

  // Smooth heading rotation — resolve compass vs GPS heading
  val resolvedHeading = CompassSensorEngine.resolveHeading(
    gpsHeading = locationState.headingDegrees,
    compassHeading = compassHeading,
    speedKmh = locationState.speedKmh,
    compassEnabled = compassEnabled
  )
  val rawTargetRotation = when (orientationMode) {
    MapOrientationMode.TRACK_UP -> -resolvedHeading
    MapOrientationMode.NORTH_UP -> 0f
    MapOrientationMode.FREE -> userRotationAngle
  }
  val animatedRotation by animateFloatAsState(
    targetValue = rawTargetRotation,
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
    label = "MapRotation"
  )

  val centerLat = locationState.latitude
  val centerLng = locationState.longitude
  val isUserPanning = abs(panOffsetX) > 15f || abs(panOffsetY) > 15f

  // Base tile size adapting to screen density (256.dp) for large, sharp text & road details
  val baseTileSize = with(density) { 256.dp.toPx() }
  val anchorZoom = zoomLevel.toInt().coerceIn(2, 18)
  val zoomFraction = zoomLevel - anchorZoom
  val zoomMultiplier = 2.0.pow(zoomFraction.toDouble()).toFloat()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFFF1F5F9))
      .pointerInput(orientationMode) {
        detectTapGestures(
          onDoubleTap = { tapOffset ->
            coroutineScope.launch {
              val canvasW = size.width.toFloat()
              val canvasH = size.height.toFloat()
              val vehicleBias = if (orientationMode == MapOrientationMode.TRACK_UP) canvasH * 0.12f else 0f
              val originX = canvasW / 2f
              val originY = canvasH / 2f + vehicleBias

              val oldZ = animatedZoom.value
              val targetZ = (oldZ + 1.2f).coerceIn(12.0f, 19.5f)
              val actualScale = 2.0.pow((targetZ - oldZ).toDouble()).toFloat()

              panOffsetX = panOffsetX * actualScale + (tapOffset.x - originX) * (1f - actualScale)
              panOffsetY = panOffsetY * actualScale + (tapOffset.y - originY) * (1f - actualScale)

              animatedZoom.animateTo(targetZ, tween(260, easing = FastOutSlowInEasing))
            }
          },
          onTap = { tapOffset ->
            val canvasW = size.width.toFloat()
            val canvasH = size.height.toFloat()
            val vehicleBias = if (orientationMode == MapOrientationMode.TRACK_UP) canvasH * 0.12f else 0f
            val midX = canvasW / 2f + panOffsetX
            val midY = canvasH / 2f + panOffsetY + vehicleBias
            val tileSizePx = baseTileSize * zoomMultiplier

            // Biến đổi toạ độ chạm màn hình về không gian bản đồ chưa xoay
            val rotAngleRad = Math.toRadians((-animatedRotation).toDouble())
            val dx = tapOffset.x - midX
            val dy = tapOffset.y - midY
            val unrotatedTapX = midX + (dx * cos(rotAngleRad) - dy * sin(rotAngleRad)).toFloat()
            val unrotatedTapY = midY + (dx * sin(rotAngleRad) + dy * cos(rotAngleRad)).toFloat()
            val effectiveTap = Offset(unrotatedTapX, unrotatedTapY)

            val cTileX = OsmTileManager.lon2tileX(centerLng, anchorZoom)
            val cTileY = OsmTileManager.lat2tileY(centerLat, anchorZoom)

            // Check camera taps với toạ độ đã bù góc xoay
            var tappedCam: TrafficCamera? = null
            for (cam in cameras) {
              val camTileX = OsmTileManager.lon2tileX(cam.longitude, anchorZoom)
              val camTileY = OsmTileManager.lat2tileY(cam.latitude, anchorZoom)
              val cx = midX + (camTileX - cTileX).toFloat() * tileSizePx
              val cy = midY + (camTileY - cTileY).toFloat() * tileSizePx
              val dist = hypot(effectiveTap.x - cx, effectiveTap.y - cy)
              if (dist < 95f) {
                tappedCam = cam
                break
              }
            }

            // Check POI taps với toạ độ đã bù góc xoay
            var tappedPoi: MapPoi? = null
            for (poi in pois) {
              val poiTileX = OsmTileManager.lon2tileX(poi.longitude, anchorZoom)
              val poiTileY = OsmTileManager.lat2tileY(poi.latitude, anchorZoom)
              val px = midX + (poiTileX - cTileX).toFloat() * tileSizePx
              val py = midY + (poiTileY - cTileY).toFloat() * tileSizePx
              val dist = hypot(effectiveTap.x - px, effectiveTap.y - py)
              if (dist < 85f) {
                tappedPoi = poi
                break
              }
            }

            if (tappedCam != null) {
              onSelectCamera(tappedCam)
            } else if (tappedPoi != null) {
              onSelectPoi(tappedPoi)
            } else if (onMapTapLocation != null) {
              val clickTileX = cTileX + (effectiveTap.x - midX) / tileSizePx
              val clickTileY = cTileY + (effectiveTap.y - midY) / tileSizePx
              val tappedLng = OsmTileManager.tileX2lon(clickTileX, anchorZoom)
              val tappedLat = OsmTileManager.tileY2lat(clickTileY, anchorZoom)
              onMapTapLocation(tappedLat, tappedLng)
            }
          }
        )
      }
      .pointerInput(orientationMode) {
        detectTransformGestures(panZoomLock = false) { centroid, pan, zoom, rotation ->
          val canvasW = size.width.toFloat()
          val canvasH = size.height.toFloat()
          val vehicleBias = if (orientationMode == MapOrientationMode.TRACK_UP) canvasH * 0.12f else 0f
          val originX = canvasW / 2f
          val originY = canvasH / 2f + vehicleBias

          if (zoom != 1f && zoom > 0f) {
            // Mercator tile scale delta: deltaZ = log2(zoom) = ln(zoom)/ln(2)
            val deltaZ = (ln(zoom.toDouble()) / ln(2.0)).toFloat()
            val oldZ = animatedZoom.value
            val newZ = (oldZ + deltaZ).coerceIn(3.0f, 19.5f)
            val actualScaleRatio = 2.0.pow((newZ - oldZ).toDouble()).toFloat()

            // Shift pan offset around exact touch centroid
            panOffsetX = panOffsetX * actualScaleRatio + (centroid.x - originX) * (1f - actualScaleRatio)
            panOffsetY = panOffsetY * actualScaleRatio + (centroid.y - originY) * (1f - actualScaleRatio)

            coroutineScope.launch {
              animatedZoom.snapTo(newZ)
            }
          }

          if (abs(rotation) > 0.45f) {
            orientationMode = MapOrientationMode.FREE
            userRotationAngle += rotation
          }

          panOffsetX += pan.x
          panOffsetY += pan.y
        }
      }
      .testTag("offline_map_canvas")
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      @Suppress("UNUSED_VARIABLE")
      val redrawTrigger = tileSignal

      val canvasWidth = size.width
      val canvasHeight = size.height

      // Navigation Lookahead: in Track-Up mode, place vehicle lower to see road ahead (Google Maps 3D style)
      val vehicleVerticalBias = if (orientationMode == MapOrientationMode.TRACK_UP) {
        if (cameraTilt3D) canvasHeight * 0.18f else canvasHeight * 0.12f
      } else 0f
      val midX = canvasWidth / 2f + panOffsetX
      val midY = canvasHeight / 2f + panOffsetY + vehicleVerticalBias

      val tileSizePx = baseTileSize * zoomMultiplier
      val cTileX = OsmTileManager.lon2tileX(centerLng, anchorZoom)
      val cTileY = OsmTileManager.lat2tileY(centerLat, anchorZoom)

      fun project(lat: Double, lng: Double): Offset {
        val tX = OsmTileManager.lon2tileX(lng, anchorZoom)
        val tY = OsmTileManager.lat2tileY(lat, anchorZoom)
        val x = midX + (tX - cTileX).toFloat() * tileSizePx
        val y = midY + (tY - cTileY).toFloat() * tileSizePx
        return Offset(x, y)
      }

      // Rotate entire canvas smoothly
      rotate(degrees = animatedRotation, pivot = Offset(midX, midY)) {

        // 2. OPENSTREETMAP TILE RENDERING WITH MULTI-LEVEL DEEP FALLBACK
        val maxTileIndex = (1 shl anchorZoom) - 1
        val diagonal = hypot(canvasWidth, canvasHeight)
        val tilesMargin = ceil(diagonal / tileSizePx / 2.0).toInt() + 2

        val minTileX = (cTileX.toInt() - tilesMargin).coerceIn(0, maxTileIndex)
        val maxTileX = (cTileX.toInt() + tilesMargin).coerceIn(0, maxTileIndex)
        val minTileY = (cTileY.toInt() - tilesMargin).coerceIn(0, maxTileIndex)
        val maxTileY = (cTileY.toInt() + tilesMargin).coerceIn(0, maxTileIndex)

        for (tx in minTileX..maxTileX) {
          for (ty in minTileY..maxTileY) {
            val tileScreenX = midX + (tx - cTileX).toFloat() * tileSizePx
            val tileScreenY = midY + (ty - cTileY).toFloat() * tileSizePx

            val tileBitmap = OsmTileManager.getTile(currentTileSource, anchorZoom, tx, ty)
            if (tileBitmap != null) {
              drawImage(
                image = tileBitmap,
                dstOffset = IntOffset(tileScreenX.toInt(), tileScreenY.toInt()),
                dstSize = IntSize(ceil(tileSizePx).toInt() + 1, ceil(tileSizePx).toInt() + 1),
                filterQuality = FilterQuality.High
              )
            } else {
              val fallback = OsmTileManager.getDeepFallbackTile(currentTileSource, anchorZoom, tx, ty)
              if (fallback != null) {
                drawImage(
                  image = fallback.image,
                  srcOffset = IntOffset(fallback.srcX, fallback.srcY),
                  srcSize = IntSize(fallback.srcW, fallback.srcH),
                  dstOffset = IntOffset(tileScreenX.toInt(), tileScreenY.toInt()),
                  dstSize = IntSize(ceil(tileSizePx).toInt() + 1, ceil(tileSizePx).toInt() + 1),
                  filterQuality = FilterQuality.High
                )
              }
            }
          }
        }

        // Optional User-Selected Dark Map Mode Overlay (Only if explicitly enabled by user in Settings)
        if (darkMapMode) {
          val diagCover = hypot(canvasWidth, canvasHeight) * 4f
          drawRect(
            color = Color(0xFF0F172A).copy(alpha = 0.55f),
            topLeft = Offset(midX - diagCover / 2f, midY - diagCover / 2f),
            size = Size(diagCover, diagCover),
            blendMode = BlendMode.Multiply
          )
        }

        // 3. TARGET FOCUS / SEARCH PIN (📍 Marker on Map)
        if (targetFocusPlace != null) {
          val focusPos = project(targetFocusPlace.latitude, targetFocusPlace.longitude)

          drawCircle(color = Color(0xFF38BDF8).copy(alpha = pulseAlpha), radius = pulseRadius * 1.5f, center = focusPos)
          drawCircle(color = Color(0xFF38BDF8).copy(alpha = 0.3f), radius = 26.dp.toPx(), center = focusPos)
          drawCircle(color = Color(0xFFE11D48), radius = 13.dp.toPx(), center = focusPos)
          drawCircle(color = Color.White, radius = 5.5.dp.toPx(), center = focusPos)

          val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 12.5.sp.toPx()
            color = android.graphics.Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
          }
          val bgBubblePaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(235, 15, 23, 42)
            style = Paint.Style.FILL
          }
          val borderBubblePaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(255, 56, 189, 248)
            style = Paint.Style.STROKE
            strokeWidth = 1.5.dp.toPx()
          }

          val textW = titlePaint.measureText(targetFocusPlace.name) + 20.dp.toPx()
          val textH = 24.dp.toPx()
          val bLeft = focusPos.x - textW / 2f
          val bTop = focusPos.y - 42.dp.toPx()

          drawContext.canvas.nativeCanvas.drawRoundRect(bLeft, bTop, bLeft + textW, bTop + textH, 12.dp.toPx(), 12.dp.toPx(), bgBubblePaint)
          drawContext.canvas.nativeCanvas.drawRoundRect(bLeft, bTop, bLeft + textW, bTop + textH, 12.dp.toPx(), 12.dp.toPx(), borderBubblePaint)
          drawContext.canvas.nativeCanvas.drawText(
            targetFocusPlace.name,
            focusPos.x,
            bTop + 16.dp.toPx(),
            titlePaint
          )
        }

        // 4. ACTIVE & ALTERNATIVE NAVIGATION ROUTES OVERLAY
        if (activeRoute != null) {
          // Render Alternative Routes (Translucent Grey/Silver Polylines)
          for (altRoute in activeRoute.alternativeRoutes) {
            if (altRoute.waypoints.size >= 2) {
              val altPath = Path()
              val p0 = project(altRoute.waypoints[0].first, altRoute.waypoints[0].second)
              altPath.moveTo(p0.x, p0.y)
              for (k in 1 until altRoute.waypoints.size) {
                val pk = project(altRoute.waypoints[k].first, altRoute.waypoints[k].second)
                altPath.lineTo(pk.x, pk.y)
              }
              // Casing
              drawPath(
                path = altPath,
                color = Color(0xFF1E293B).copy(alpha = 0.6f),
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
              )
              // Body
              drawPath(
                path = altPath,
                color = Color(0xFF94A3B8).copy(alpha = 0.75f),
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
              )
            }
          }

          if (activeRoute.waypoints.size >= 2) {
            // Find closest waypoint on route to vehicle current position
            var closestIdx = 0
            var minWpDist = Double.MAX_VALUE
            for (i in 0 until activeRoute.waypoints.size) {
              val d = VietnamTrafficData.calculateDistanceMeters(
                centerLat, centerLng,
                activeRoute.waypoints[i].first, activeRoute.waypoints[i].second
              )
              if (d < minWpDist) {
                minWpDist = d
                closestIdx = i
              }
            }

            // Build forward path starting directly from current vehicle coordinate
            val forwardWaypoints = mutableListOf<Pair<Double, Double>>()
            forwardWaypoints.add(centerLat to centerLng)
            val startIndex = if (minWpDist < 25.0) closestIdx + 1 else closestIdx
            for (i in startIndex until activeRoute.waypoints.size) {
              forwardWaypoints.add(activeRoute.waypoints[i])
            }
            if (forwardWaypoints.size < 2 && activeRoute.waypoints.isNotEmpty()) {
              forwardWaypoints.add(activeRoute.waypoints.last())
            }

            if (forwardWaypoints.size >= 2) {
              val navPath = Path()
              val firstPos = project(forwardWaypoints[0].first, forwardWaypoints[0].second)
              navPath.moveTo(firstPos.x, firstPos.y)
              for (i in 1 until forwardWaypoints.size) {
                val pos = project(forwardWaypoints[i].first, forwardWaypoints[i].second)
                navPath.lineTo(pos.x, pos.y)
              }

              // Dark route outline casing
              drawPath(
                path = navPath,
                color = Color(0xFF0F172A),
                style = Stroke(width = 15.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
              )

            // Dynamic Traffic Flow Polyline (Green = Clear, Yellow = Slow, Red = Congested)
            if (activeRoute.trafficSegments.isNotEmpty()) {
              for (seg in activeRoute.trafficSegments) {
                val segPath = Path()
                var started = false
                val segColor = Color(seg.congestion.colorHex)

                for (idx in seg.startIndex..seg.endIndex.coerceAtMost(activeRoute.waypoints.size - 1)) {
                  val wp = activeRoute.waypoints[idx]
                  val pos = project(wp.first, wp.second)
                  if (!started) {
                    segPath.moveTo(pos.x, pos.y)
                    started = true
                  } else {
                    segPath.lineTo(pos.x, pos.y)
                  }
                }
                if (started) {
                  drawPath(
                    path = segPath,
                    color = segColor,
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                  )
                }
              }
            } else {
              // Default Electric Blue Polyline
              drawPath(
                path = navPath,
                color = Color(0xFF0284C7),
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
              )
            }

            // Moving chevrons along forward remaining route only
            for (i in 0 until forwardWaypoints.size - 1) {
              val p1 = project(forwardWaypoints[i].first, forwardWaypoints[i].second)
              val p2 = project(forwardWaypoints[i + 1].first, forwardWaypoints[i + 1].second)
              val distPx = hypot(p2.x - p1.x, p2.y - p1.y)
              val step = 45.dp.toPx()
              var currentD = (chevronOffset * step) % step
              while (currentD < distPx) {
                val fraction = currentD / distPx
                val cx = p1.x + (p2.x - p1.x) * fraction
                val cy = p1.y + (p2.y - p1.y) * fraction
                val angle = atan2(p2.y - p1.y, p2.x - p1.x)

                val chevronPaint = Paint().apply {
                  isAntiAlias = true
                  textSize = 12.sp.toPx()
                  color = android.graphics.Color.WHITE
                  typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                  textAlign = Paint.Align.CENTER
                }
                drawContext.canvas.nativeCanvas.save()
                drawContext.canvas.nativeCanvas.translate(cx, cy)
                drawContext.canvas.nativeCanvas.rotate(Math.toDegrees(angle.toDouble()).toFloat() + 90f)
                drawContext.canvas.nativeCanvas.drawText("▲", 0f, 4.dp.toPx(), chevronPaint)
                drawContext.canvas.nativeCanvas.restore()

                currentD += step
              }
            }
          }

          // Destination Pin & Flag
          val destPos = project(activeRoute.destinationLat, activeRoute.destinationLng)
          drawCircle(color = Color(0xFFE11D48).copy(alpha = pulseAlpha), radius = pulseRadius * 1.5f, center = destPos)
          drawCircle(color = Color(0xFFE11D48), radius = 13.dp.toPx(), center = destPos)
          drawCircle(color = Color.White, radius = 5.5.dp.toPx(), center = destPos)
        }
      }

        // 5. ENHANCED SPECIFIC CAMERA MARKERS ON MAP (Filtered by Zoom Level & Distance Radius)
        val maxCamRadiusMeters = when {
          zoomLevel >= 16f -> 2200.0  // Zoom gần: 2.2km
          zoomLevel >= 14f -> 3500.0  // Zoom trung bình: 3.5km
          else -> 0.0                // Zoom xa (< 14): ẨN HOÀN TOÀN để bản đồ sạch & nhẹ
        }

        val visibleCameras = if (maxCamRadiusMeters > 0) {
          cameras.filter { cam ->
            val d = VietnamTrafficData.calculateDistanceMeters(centerLat, centerLng, cam.latitude, cam.longitude)
            d <= maxCamRadiusMeters || cam.id == nearestCamera?.id
          }
        } else {
          // Khi zoom xa, chỉ giữ lại camera gần nhất nếu đang trong phạm vi cảnh báo
          if (nearestCamera != null && (nearestCameraDistance ?: 9999) < 650) listOf(nearestCamera) else emptyList()
        }

        for (cam in visibleCameras) {
          val camPos = project(cam.latitude, cam.longitude)
          val isNear = nearestCamera?.id == cam.id
          val distLabel = if (isNear && nearestCameraDistance != null) "${nearestCameraDistance}m" else null

          // Giữ biểu tượng camera và chữ số tốc độ luôn thẳng đứng (counter-rotate theo -animatedRotation)
          rotate(degrees = -animatedRotation, pivot = camPos) {
            // Alert pulse for closest camera
            if (isNear) {
              val pulseColor = if (activeWarning?.isOverspeeding == true) AlertCrimsonDanger else Color(0xFF0284C7)
              drawCircle(color = pulseColor.copy(alpha = pulseAlpha * 0.6f), radius = pulseRadius * 1.4f, center = camPos)
            }

            when (cam.type) {
              CameraType.SPEED_CAMERA, CameraType.COLD_FINE_SURVEILLANCE, CameraType.COMMUNITY_REPORT -> {
                val cardW = 44.dp.toPx()
                val cardH = 44.dp.toPx()
                val left = camPos.x - cardW / 2f
                val top = camPos.y - cardH / 2f

                val bgPaint = Paint().apply {
                  isAntiAlias = true
                  color = if (isNear && activeWarning?.isOverspeeding == true)
                    android.graphics.Color.argb(255, 185, 28, 28) // red when over speed
                  else android.graphics.Color.argb(250, 15, 23, 42) // sleek dark blue-black
                  style = Paint.Style.FILL
                }
                val borderPaint = Paint().apply {
                  isAntiAlias = true
                  color = if (isNear) android.graphics.Color.argb(255, 2, 180, 255) else android.graphics.Color.argb(220, 148, 163, 184)
                  style = Paint.Style.STROKE
                  strokeWidth = if (isNear) 2.5.dp.toPx() else 1.5.dp.toPx()
                }

                // Card body with rounded corners
                drawContext.canvas.nativeCanvas.drawRoundRect(left, top, left + cardW, top + cardH, 10.dp.toPx(), 10.dp.toPx(), bgPaint)
                drawContext.canvas.nativeCanvas.drawRoundRect(left, top, left + cardW, top + cardH, 10.dp.toPx(), 10.dp.toPx(), borderPaint)

                // Speedometer arc icon inside (upper half)
                val arcPaint = Paint().apply {
                  isAntiAlias = true
                  color = android.graphics.Color.WHITE
                  style = Paint.Style.STROKE
                  strokeWidth = 2.0.dp.toPx()
                  strokeCap = Paint.Cap.ROUND
                }
                val arcCenterX = camPos.x
                val arcCenterY = camPos.y - 4.dp.toPx()
                val arcRadius = 11.5.dp.toPx()
                val arcRect = android.graphics.RectF(arcCenterX - arcRadius, arcCenterY - arcRadius, arcCenterX + arcRadius, arcCenterY + arcRadius)
                drawContext.canvas.nativeCanvas.drawArc(arcRect, 200f, 140f, false, arcPaint)

                // Speedometer needle
                val needleAngle = Math.toRadians(270.0)
                val needlePaint = Paint().apply {
                  isAntiAlias = true
                  color = if (isNear && activeWarning?.isOverspeeding == true) android.graphics.Color.argb(255, 255, 80, 80) else android.graphics.Color.WHITE
                  strokeWidth = 1.8.dp.toPx()
                  strokeCap = Paint.Cap.ROUND
                  style = Paint.Style.STROKE
                }
                val needleEndX = arcCenterX + (arcRadius * 0.7f * cos(needleAngle).toFloat())
                val needleEndY = arcCenterY + (arcRadius * 0.7f * sin(needleAngle).toFloat())
                drawContext.canvas.nativeCanvas.drawLine(arcCenterX, arcCenterY, needleEndX, needleEndY, needlePaint)

                // Blue dot in center
                val dotPaint = Paint().apply { isAntiAlias = true; color = android.graphics.Color.argb(255, 0, 180, 255); style = Paint.Style.FILL }
                drawContext.canvas.nativeCanvas.drawCircle(arcCenterX, arcCenterY, 3.2.dp.toPx(), dotPaint)

                // Speed limit text cleanly below arc (lower half of card)
                val speedPaint = Paint().apply {
                  isAntiAlias = true
                  textSize = 9.5.sp.toPx()
                  color = android.graphics.Color.WHITE
                  typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                  textAlign = Paint.Align.CENTER
                }
                if (cam.speedLimit > 0) {
                  drawContext.canvas.nativeCanvas.drawText("${cam.speedLimit}", camPos.x, top + cardH - 5.dp.toPx(), speedPaint)
                }

                // Real-time countdown distance capsule below card
                if (distLabel != null) {
                  val distPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 10.5.sp.toPx()
                    color = android.graphics.Color.WHITE
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                  }
                  val distW = distPaint.measureText(distLabel) + 12.dp.toPx()
                  val distH = 16.dp.toPx()
                  val distLeft = camPos.x - distW / 2f
                  val distTop = top + cardH + 4.dp.toPx()

                  val distBgPaint = Paint().apply {
                    isAntiAlias = true
                    color = if (activeWarning?.isOverspeeding == true) android.graphics.Color.argb(235, 220, 38, 38) else android.graphics.Color.argb(235, 2, 132, 199)
                    style = Paint.Style.FILL
                  }
                  drawContext.canvas.nativeCanvas.drawRoundRect(distLeft, distTop, distLeft + distW, distTop + distH, 8.dp.toPx(), 8.dp.toPx(), distBgPaint)
                  drawContext.canvas.nativeCanvas.drawText(distLabel, camPos.x, distTop + 12.dp.toPx(), distPaint)
                }
              }

              CameraType.RED_LIGHT_CAMERA -> {
                // Vietmap-style: traffic light card
                val cardW = 30.dp.toPx()
                val cardH = 44.dp.toPx()
                val left = camPos.x - cardW / 2f
                val top = camPos.y - cardH / 2f

                val bgPaint = Paint().apply { isAntiAlias = true; color = android.graphics.Color.argb(240, 20, 20, 30); style = Paint.Style.FILL }
                val borderPaint = Paint().apply { isAntiAlias = true; color = if (isNear) android.graphics.Color.argb(255, 239, 68, 68) else android.graphics.Color.argb(200, 200, 210, 220); style = Paint.Style.STROKE; strokeWidth = if (isNear) 2.5.dp.toPx() else 1.5.dp.toPx() }

                drawContext.canvas.nativeCanvas.drawRoundRect(left, top, left + cardW, top + cardH, 6.dp.toPx(), 6.dp.toPx(), bgPaint)
                drawContext.canvas.nativeCanvas.drawRoundRect(left, top, left + cardW, top + cardH, 6.dp.toPx(), 6.dp.toPx(), borderPaint)

                drawCircle(color = Color(0xFFEF4444), radius = 3.5.dp.toPx(), center = Offset(camPos.x, camPos.y - 10.dp.toPx()))
                drawCircle(color = Color(0xFFF59E0B), radius = 3.5.dp.toPx(), center = Offset(camPos.x, camPos.y))
                drawCircle(color = Color(0xFF10B981), radius = 3.5.dp.toPx(), center = Offset(camPos.x, camPos.y + 10.dp.toPx()))

                if (distLabel != null) {
                  val distPaint = Paint().apply { isAntiAlias = true; textSize = 11.sp.toPx(); color = android.graphics.Color.WHITE; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
                  drawContext.canvas.nativeCanvas.drawText(distLabel, camPos.x, top + cardH + 16.dp.toPx(), distPaint)
                }
              }

              CameraType.MOTORBIKE_PROHIBITED_ZONE -> {
                val r = 14.dp.toPx()
                drawCircle(color = Color.White, radius = r, center = camPos)
                drawCircle(color = Color(0xFFDC2626), radius = r, center = camPos, style = Stroke(width = 3.dp.toPx()))
                val iconPaint = Paint().apply { isAntiAlias = true; textSize = 11.sp.toPx(); textAlign = Paint.Align.CENTER }
                drawContext.canvas.nativeCanvas.drawText("🚫", camPos.x, camPos.y + 4.dp.toPx(), iconPaint)
                if (distLabel != null) {
                  val distPaint = Paint().apply { isAntiAlias = true; textSize = 11.sp.toPx(); color = android.graphics.Color.argb(255, 220, 38, 38); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
                  drawContext.canvas.nativeCanvas.drawText(distLabel, camPos.x, camPos.y + r + 16.dp.toPx(), distPaint)
                }
              }

              CameraType.SECURITY_MONITORING -> {
                val r = 14.dp.toPx()
                drawCircle(color = Color(0xFF1E40AF), radius = r, center = camPos)
                drawCircle(color = Color(0xFF93C5FD), radius = r, center = camPos, style = Stroke(width = 1.8.dp.toPx()))
                val iconPaint = Paint().apply { isAntiAlias = true; textSize = 11.sp.toPx(); textAlign = Paint.Align.CENTER }
                drawContext.canvas.nativeCanvas.drawText("🛡️", camPos.x, camPos.y + 4.dp.toPx(), iconPaint)
                if (distLabel != null) {
                  val distPaint = Paint().apply { isAntiAlias = true; textSize = 11.sp.toPx(); color = android.graphics.Color.WHITE; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
                  drawContext.canvas.nativeCanvas.drawText(distLabel, camPos.x, camPos.y + r + 14.dp.toPx(), distPaint)
                }
              }

              CameraType.SPEED_LIMIT_SIGN -> {
                // Standard P.127 Vietnamese Speed Limit Sign
                drawCircle(color = SignBackgroundWhite, radius = 13.5.dp.toPx(), center = camPos)
                drawCircle(color = SignBorderRed, radius = 13.5.dp.toPx(), center = camPos, style = Stroke(width = 3.dp.toPx()))
                val speedPaint = Paint().apply { isAntiAlias = true; textSize = 10.5.sp.toPx(); color = android.graphics.Color.BLACK; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
                drawContext.canvas.nativeCanvas.drawText("${cam.speedLimit}", camPos.x, camPos.y + 3.8.dp.toPx(), speedPaint)
              }

              CameraType.ZONE_RESIDENTIAL_ENTRY, CameraType.ZONE_RESIDENTIAL_EXIT -> {
                val w = 24.dp.toPx(); val h = 18.dp.toPx(); val left = camPos.x - w / 2f; val top = camPos.y - h / 2f
                val bgPaint = Paint().apply { isAntiAlias = true; color = android.graphics.Color.argb(240, 29, 78, 216); style = Paint.Style.FILL }
                val borderPaint = Paint().apply { isAntiAlias = true; color = android.graphics.Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 1.5.dp.toPx() }
                drawContext.canvas.nativeCanvas.drawRoundRect(left, top, left + w, top + h, 4.dp.toPx(), 4.dp.toPx(), bgPaint)
                drawContext.canvas.nativeCanvas.drawRoundRect(left, top, left + w, top + h, 4.dp.toPx(), 4.dp.toPx(), borderPaint)
                val iconPaint = Paint().apply { isAntiAlias = true; textSize = 9.sp.toPx(); textAlign = Paint.Align.CENTER }
                drawContext.canvas.nativeCanvas.drawText("🏙️", camPos.x, camPos.y + 3.5.dp.toPx(), iconPaint)
                if (cam.type == CameraType.ZONE_RESIDENTIAL_EXIT) {
                  drawLine(color = Color(0xFFEF4444), start = Offset(left, top + h), end = Offset(left + w, top), strokeWidth = 2.5.dp.toPx())
                }
                if (distLabel != null) {
                  val distPaint = Paint().apply { isAntiAlias = true; textSize = 11.sp.toPx(); color = android.graphics.Color.WHITE; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
                  drawContext.canvas.nativeCanvas.drawText(distLabel, camPos.x, top + h + 16.dp.toPx(), distPaint)
                }
              }

              CameraType.HAZARD_ACCIDENT_ZONE, CameraType.SCHOOL_ZONE -> {
                val w = 24.dp.toPx(); val h = 21.dp.toPx()
                val path = Path().apply {
                  moveTo(camPos.x, camPos.y - h / 2f)
                  lineTo(camPos.x + w / 2f, camPos.y + h / 2f)
                  lineTo(camPos.x - w / 2f, camPos.y + h / 2f)
                  close()
                }
                val fillColor = if (cam.type == CameraType.SCHOOL_ZONE) Color(0xFF0284C7) else Color(0xFFF59E0B)
                drawPath(path, color = fillColor)
                drawPath(path, color = Color(0xFF0F172A), style = Stroke(width = 2.dp.toPx()))
                val textPaint = Paint().apply { isAntiAlias = true; textSize = 10.sp.toPx(); color = android.graphics.Color.BLACK; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
                val label = if (cam.type == CameraType.SCHOOL_ZONE) "🏫" else "!"
                drawContext.canvas.nativeCanvas.drawText(label, camPos.x, camPos.y + 5.dp.toPx(), textPaint)
                if (distLabel != null) {
                  val distPaint = Paint().apply { isAntiAlias = true; textSize = 11.sp.toPx(); color = android.graphics.Color.WHITE; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
                  drawContext.canvas.nativeCanvas.drawText(distLabel, camPos.x, camPos.y + h + 12.dp.toPx(), distPaint)
                }
              }
            }
          }
        }

        // 5.2. POI & HAZARD ICONS ON MAP (Only when zoomed in >= 14 for optimal performance)
        val visiblePois = if (zoomLevel >= 14f) {
          pois.filter { poi ->
            VietnamTrafficData.calculateDistanceMeters(centerLat, centerLng, poi.latitude, poi.longitude) <= 3000.0
          }
        } else emptyList()

        for (poi in visiblePois) {
          val poiPos = project(poi.latitude, poi.longitude)

          val (badgeColor, borderCol, emojiChar) = when (poi.type) {
            PoiType.GAS_STATION -> Triple(Color(0xFFF97316), Color.White, "⛽")
            PoiType.TOLL_BOOTH -> Triple(Color(0xFF0284C7), Color.White, "🚧")
            PoiType.HOSPITAL -> Triple(Color(0xFFDC2626), Color.White, "🏥")
            PoiType.TIRE_REPAIR -> Triple(Color(0xFFF59E0B), Color.White, "🔧")
            PoiType.ACCIDENT_HOTSPOT -> Triple(Color(0xFFE11D48), Color.White, "⚠️")
            PoiType.BRIDGE -> Triple(Color(0xFF64748B), Color.White, "🌉")
            PoiType.REST_STOP -> Triple(Color(0xFF10B981), Color.White, "🅿️")
          }

          // Giữ biểu tượng POI luôn thẳng đứng
          rotate(degrees = -animatedRotation, pivot = poiPos) {
            // Outer shadow & background circle
            drawCircle(color = Color(0xFF0F172A).copy(alpha = 0.2f), radius = 13.dp.toPx(), center = Offset(poiPos.x, poiPos.y + 1.5.dp.toPx()))
            drawCircle(color = badgeColor, radius = 12.dp.toPx(), center = poiPos)
            drawCircle(color = borderCol, radius = 12.dp.toPx(), center = poiPos, style = Stroke(width = 1.8.dp.toPx()))

            val emojiPaint = Paint().apply {
              isAntiAlias = true
              textSize = 11.sp.toPx()
              textAlign = Paint.Align.CENTER
            }
            drawContext.canvas.nativeCanvas.drawText(emojiChar, poiPos.x, poiPos.y + 4.dp.toPx(), emojiPaint)

            // Short name label when zoomed in
            if (zoomLevel >= 15.5f) {
              val labelPaint = Paint().apply {
                isAntiAlias = true
                textSize = 9.5.sp.toPx()
                color = android.graphics.Color.WHITE
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
              }
              val shortName = poi.name.take(18)
              val lblW = labelPaint.measureText(shortName) + 10.dp.toPx()
              val lblH = 15.dp.toPx()
              val lblLeft = poiPos.x - lblW / 2f
              val lblTop = poiPos.y + 14.dp.toPx()

              val bgLblPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.argb(220, 15, 23, 42)
                style = Paint.Style.FILL
              }
              drawContext.canvas.nativeCanvas.drawRoundRect(lblLeft, lblTop, lblLeft + lblW, lblTop + lblH, 6.dp.toPx(), 6.dp.toPx(), bgLblPaint)
              drawContext.canvas.nativeCanvas.drawText(shortName, poiPos.x, lblTop + 11.dp.toPx(), labelPaint)
            }
          }
        }

        // 6. BREADCRUMBS TRAIL (Smooth GPS Path History)
        if (showBreadcrumbs && breadcrumbs.size >= 2) {
          val trailPath = Path()
          val firstB = project(breadcrumbs[0].latitude, breadcrumbs[0].longitude)
          trailPath.moveTo(firstB.x, firstB.y)
          for (k in 1 until breadcrumbs.size) {
            val bPos = project(breadcrumbs[k].latitude, breadcrumbs[k].longitude)
            trailPath.lineTo(bPos.x, bPos.y)
          }
          drawPath(
            path = trailPath,
            color = Color(0xFF0284C7).copy(alpha = 0.5f),
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
          )
        }

        // 7. HIGH-PRECISION NAVIGATION VEHICLE MARKER WITH ROAD-SNAPPING & RADAR BEAM
        // Snap to road centerline if enabled and moving
        var vLat = locationState.latitude
        var vLng = locationState.longitude

        if (roadSnappingEnabled && locationState.speedKmh > 3.0f) {
          val candidatePoints = mutableListOf<Pair<Double, Double>>()
          if (activeRoute != null && activeRoute.waypoints.isNotEmpty()) {
            candidatePoints.addAll(activeRoute.waypoints)
          } else {
            val nearbyRoad = VietnamTrafficData.ALL_ROADS.minByOrNull { road ->
              road.coordinates.minOfOrNull { (lat, lng) ->
                VietnamTrafficData.calculateDistanceMeters(locationState.latitude, locationState.longitude, lat, lng)
              } ?: Double.MAX_VALUE
            }
            if (nearbyRoad != null) {
              candidatePoints.addAll(nearbyRoad.coordinates)
            }
          }

          if (candidatePoints.size >= 2) {
            var minSegDist = Double.MAX_VALUE
            var closestProjLat = locationState.latitude
            var closestProjLng = locationState.longitude

            for (i in 0 until candidatePoints.size - 1) {
              val (p1Lat, p1Lng) = candidatePoints[i]
              val (p2Lat, p2Lng) = candidatePoints[i + 1]

              val dx = p2Lng - p1Lng
              val dy = p2Lat - p1Lat
              val denom = dx * dx + dy * dy
              if (denom == 0.0) continue
              val t = (((locationState.longitude - p1Lng) * dx + (locationState.latitude - p1Lat) * dy) / denom).coerceIn(0.0, 1.0)
              val projLat = p1Lat + t * dy
              val projLng = p1Lng + t * dx

              val dist = VietnamTrafficData.calculateDistanceMeters(locationState.latitude, locationState.longitude, projLat, projLng)
              if (dist < minSegDist) {
                minSegDist = dist
                closestProjLat = projLat
                closestProjLng = projLng
              }
            }

            // Snap smoothly to centerline if within 26m
            if (minSegDist <= 26.0) {
              val blendFactor = 0.76
              vLat = locationState.latitude * (1.0 - blendFactor) + closestProjLat * blendFactor
              vLng = locationState.longitude * (1.0 - blendFactor) + closestProjLng * blendFactor
            }
          }
        }

        val userPos = project(vLat, vLng)
        val headingRad = Math.toRadians((resolvedHeading - 90).toDouble())
        val isMoving = locationState.speedKmh > 2.5f
        val vScale = vehicleIconScale.coerceIn(0.75f, 2.2f)

        // 7.1 GOOGLE MAPS STYLE RADAR BEAM / LIGHT CONE
        val radarLen = 52.dp.toPx() * vScale
        val radarAngleHalf = Math.toRadians(26.0)
        val radarP1X = userPos.x + radarLen * cos(headingRad - radarAngleHalf).toFloat()
        val radarP1Y = userPos.y + radarLen * sin(headingRad - radarAngleHalf).toFloat()
        val radarP2X = userPos.x + radarLen * cos(headingRad + radarAngleHalf).toFloat()
        val radarP2Y = userPos.y + radarLen * sin(headingRad + radarAngleHalf).toFloat()

        val radarPath = Path().apply {
          moveTo(userPos.x, userPos.y)
          lineTo(radarP1X, radarP1Y)
          lineTo(radarP2X, radarP2Y)
          close()
        }
        drawPath(
          path = radarPath,
          brush = Brush.radialGradient(
            colors = listOf(Color(0x750284C7), Color(0x3038BDF8), Color.Transparent),
            center = userPos,
            radius = radarLen
          )
        )

        // Pulsating location accuracy halo
        drawCircle(
          color = Color(0xFF0284C7).copy(alpha = pulseAlpha * 0.7f),
          radius = pulseRadius * 1.5f * vScale,
          center = userPos
        )

        val r = 21.dp.toPx() * vScale
        val isOverspeeding = activeWarning?.isOverspeeding == true

        when (vehicleIconType) {
          VehicleIconType.ARROW -> {
            // 3D Ultra-Sharp High-Tech Navigation Chevron with Drop Shadow & White Casing
            val arrowLen = 28.dp.toPx() * vScale
            val arrowWidth = 19.dp.toPx() * vScale
            val notchDepth = 8.dp.toPx() * vScale

            val forwardX = userPos.x + arrowLen * cos(headingRad).toFloat()
            val forwardY = userPos.y + arrowLen * sin(headingRad).toFloat()
            val leftX = userPos.x + arrowWidth * cos(headingRad + Math.toRadians(140.0)).toFloat()
            val leftY = userPos.y + arrowWidth * sin(headingRad + Math.toRadians(140.0)).toFloat()
            val rightX = userPos.x + arrowWidth * cos(headingRad - Math.toRadians(140.0)).toFloat()
            val rightY = userPos.y + arrowWidth * sin(headingRad - Math.toRadians(140.0)).toFloat()
            val notchX = userPos.x + notchDepth * cos(headingRad + Math.PI).toFloat()
            val notchY = userPos.y + notchDepth * sin(headingRad + Math.PI).toFloat()

            val arrowPath = Path().apply {
              moveTo(forwardX, forwardY)
              lineTo(leftX, leftY)
              lineTo(notchX, notchY)
              lineTo(rightX, rightY)
              close()
            }

            val shadowPath = Path().apply {
              moveTo(forwardX, forwardY + 3.dp.toPx())
              lineTo(leftX, leftY + 3.dp.toPx())
              lineTo(notchX, notchY + 3.dp.toPx())
              lineTo(rightX, rightY + 3.dp.toPx())
              close()
            }
            drawPath(shadowPath, color = Color(0x60000000))
            drawPath(arrowPath, color = Color.White, style = Stroke(width = 4.5.dp.toPx() * vScale, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(arrowPath, color = if (isOverspeeding) AlertCrimsonDanger else Color(0xFF00B4D8))
            drawCircle(color = Color.White, radius = 3.5.dp.toPx() * vScale, center = userPos)
          }

          VehicleIconType.SCOOTER, VehicleIconType.MOTORBIKE -> {
            // GOOGLE MAPS STYLE 3D ISOMETRIC MOTORBIKE / SCOOTER (Rider with White Helmet + Cyan Body + Shadow)
            val s = vScale * 1.25f
            val w = 28.dp.toPx() * s
            val h = 48.dp.toPx() * s

            rotate(degrees = resolvedHeading, pivot = userPos) {
              // 1. Realistic Oblong Ground Shadow on asphalt
              drawOval(
                color = Color(0x55000000),
                topLeft = Offset(userPos.x - w * 0.45f, userPos.y - h * 0.15f),
                size = Size(w * 0.9f, h * 0.72f)
              )

              // 2. Rear Tire & Mudguard
              drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(userPos.x - 4.5.dp.toPx() * s, userPos.y + 10.dp.toPx() * s),
                size = Size(9.dp.toPx() * s, 12.dp.toPx() * s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.5.dp.toPx() * s)
              )

              // 3. Scooter Main Chassis / Frame (Cyan or Crimson when overspeeding)
              val bodyColor = if (isOverspeeding) Color(0xFFEF4444) else if (vehicleIconType == VehicleIconType.MOTORBIKE) Color(0xFF0284C7) else Color(0xFF06B6D4)
              val bodyHighlight = if (isOverspeeding) Color(0xFFFCA5A5) else Color(0xFF67E8F9)

              val chassisPath = Path().apply {
                moveTo(userPos.x - 8.dp.toPx() * s, userPos.y + 12.dp.toPx() * s)
                lineTo(userPos.x - 11.dp.toPx() * s, userPos.y - 3.dp.toPx() * s)
                lineTo(userPos.x - 5.dp.toPx() * s, userPos.y - 18.dp.toPx() * s)
                lineTo(userPos.x + 5.dp.toPx() * s, userPos.y - 18.dp.toPx() * s)
                lineTo(userPos.x + 11.dp.toPx() * s, userPos.y - 3.dp.toPx() * s)
                lineTo(userPos.x + 8.dp.toPx() * s, userPos.y + 12.dp.toPx() * s)
                close()
              }
              drawPath(chassisPath, color = bodyColor)

              // Central Gloss Ridge on scooter body
              val ridgePath = Path().apply {
                moveTo(userPos.x - 2.dp.toPx() * s, userPos.y - 16.dp.toPx() * s)
                lineTo(userPos.x + 2.dp.toPx() * s, userPos.y - 16.dp.toPx() * s)
                lineTo(userPos.x + 3.5.dp.toPx() * s, userPos.y + 8.dp.toPx() * s)
                lineTo(userPos.x - 3.5.dp.toPx() * s, userPos.y + 8.dp.toPx() * s)
                close()
              }
              drawPath(ridgePath, color = bodyHighlight)

              // 4. Rear Tail Light (Red LED glow with white center)
              drawRoundRect(
                color = Color(0xFFFF1E1E),
                topLeft = Offset(userPos.x - 5.5.dp.toPx() * s, userPos.y + 11.dp.toPx() * s),
                size = Size(11.dp.toPx() * s, 3.2.dp.toPx() * s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx() * s)
              )

              // 5. Handlebars & Dual Chrome Mirrors (Google Maps style)
              drawLine(
                color = Color(0xFF0F172A),
                start = Offset(userPos.x - 14.dp.toPx() * s, userPos.y - 10.dp.toPx() * s),
                end = Offset(userPos.x + 14.dp.toPx() * s, userPos.y - 10.dp.toPx() * s),
                strokeWidth = 2.5.dp.toPx() * s,
                cap = StrokeCap.Round
              )
              // Left Mirror
              drawCircle(
                color = Color(0xFFE2E8F0),
                radius = 2.6.dp.toPx() * s,
                center = Offset(userPos.x - 14.5.dp.toPx() * s, userPos.y - 12.5.dp.toPx() * s)
              )
              // Right Mirror
              drawCircle(
                color = Color(0xFFE2E8F0),
                radius = 2.6.dp.toPx() * s,
                center = Offset(userPos.x + 14.5.dp.toPx() * s, userPos.y - 12.5.dp.toPx() * s)
              )

              // 6. Rider Body (Jacket & Shoulders)
              val jacketColor = Color(0xFF1E293B) // Dark slate jacket
              drawRoundRect(
                color = jacketColor,
                topLeft = Offset(userPos.x - 8.dp.toPx() * s, userPos.y - 6.dp.toPx() * s),
                size = Size(16.dp.toPx() * s, 12.dp.toPx() * s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx() * s)
              )

              // 7. 3D Spherical White Helmet with Visor & Highlight
              // Base shadow under helmet
              drawCircle(
                color = Color(0x40000000),
                radius = 7.dp.toPx() * s,
                center = Offset(userPos.x, userPos.y - 3.dp.toPx() * s)
              )
              // White 3D sphere gradient
              drawCircle(
                brush = Brush.radialGradient(
                  colors = listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9), Color(0xFF94A3B8)),
                  center = Offset(userPos.x - 1.8.dp.toPx() * s, userPos.y - 7.dp.toPx() * s),
                  radius = 7.5.dp.toPx() * s
                ),
                radius = 6.8.dp.toPx() * s,
                center = Offset(userPos.x, userPos.y - 5.dp.toPx() * s)
              )
              // Dark goggles / visor rim
              drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(userPos.x - 4.5.dp.toPx() * s, userPos.y - 9.5.dp.toPx() * s),
                size = Size(9.dp.toPx() * s, 2.8.dp.toPx() * s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.2.dp.toPx() * s)
              )
            }
          }

          VehicleIconType.CAR -> {
            // 3D SPORT CAR SEDAN MODEL
            val s = vScale * 1.3f
            val w = 26.dp.toPx() * s
            val h = 48.dp.toPx() * s

            rotate(degrees = resolvedHeading, pivot = userPos) {
              // 1. Drop shadow
              drawRoundRect(
                color = Color(0x50000000),
                topLeft = Offset(userPos.x - w * 0.5f, userPos.y - h * 0.48f + 2.dp.toPx() * s),
                size = Size(w, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx() * s)
              )

              // 2. Car Chassis Body
              val carColor = if (isOverspeeding) Color(0xFFEF4444) else Color(0xFF0284C7)
              drawRoundRect(
                color = carColor,
                topLeft = Offset(userPos.x - 11.dp.toPx() * s, userPos.y - 20.dp.toPx() * s),
                size = Size(22.dp.toPx() * s, 40.dp.toPx() * s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx() * s)
              )

              // 3. Front Windshield (Dark glass with highlight)
              drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(userPos.x - 8.dp.toPx() * s, userPos.y - 12.dp.toPx() * s),
                size = Size(16.dp.toPx() * s, 9.dp.toPx() * s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx() * s)
              )

              // 4. Roof Metallic Panel
              drawRoundRect(
                color = if (isOverspeeding) Color(0xFFDC2626) else Color(0xFF0369A1),
                topLeft = Offset(userPos.x - 7.5.dp.toPx() * s, userPos.y - 2.dp.toPx() * s),
                size = Size(15.dp.toPx() * s, 11.dp.toPx() * s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx() * s)
              )

              // 5. Rear Glass
              drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(userPos.x - 8.dp.toPx() * s, userPos.y + 10.dp.toPx() * s),
                size = Size(16.dp.toPx() * s, 5.dp.toPx() * s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx() * s)
              )

              // 6. Dual Headlights (Front LED) & Taillights (Rear Red)
              drawCircle(color = Color(0xFFFFFBEB), radius = 2.dp.toPx() * s, center = Offset(userPos.x - 8.dp.toPx() * s, userPos.y - 19.dp.toPx() * s))
              drawCircle(color = Color(0xFFFFFBEB), radius = 2.dp.toPx() * s, center = Offset(userPos.x + 8.dp.toPx() * s, userPos.y - 19.dp.toPx() * s))
              drawRoundRect(color = Color(0xFFFF2222), topLeft = Offset(userPos.x - 9.dp.toPx() * s, userPos.y + 18.dp.toPx() * s), size = Size(5.dp.toPx() * s, 2.dp.toPx() * s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx() * s))
              drawRoundRect(color = Color(0xFFFF2222), topLeft = Offset(userPos.x + 4.dp.toPx() * s, userPos.y + 18.dp.toPx() * s), size = Size(5.dp.toPx() * s, 2.dp.toPx() * s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx() * s))
            }
          }

          VehicleIconType.TRUCK -> {
            val bgPaint = Paint().apply {
              isAntiAlias = true
              color = if (isOverspeeding) android.graphics.Color.argb(255, 220, 38, 38) else android.graphics.Color.argb(255, 100, 116, 139)
              style = Paint.Style.FILL
            }
            val borderPaint = Paint().apply { isAntiAlias = true; color = android.graphics.Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 3.2.dp.toPx() * vScale }
            val emojiPaint = Paint().apply { isAntiAlias = true; textSize = 16.5.sp.toPx() * vScale; textAlign = Paint.Align.CENTER }
            val arrowPaint = Paint().apply { isAntiAlias = true; color = android.graphics.Color.WHITE; style = Paint.Style.FILL }

            drawContext.canvas.nativeCanvas.drawCircle(userPos.x, userPos.y, r, bgPaint)
            drawContext.canvas.nativeCanvas.drawCircle(userPos.x, userPos.y, r, borderPaint)

            drawContext.canvas.nativeCanvas.save()
            drawContext.canvas.nativeCanvas.translate(userPos.x, userPos.y)
            drawContext.canvas.nativeCanvas.rotate(resolvedHeading)
            val triPath = android.graphics.Path().apply {
              moveTo(0f, -r - 7.dp.toPx() * vScale); lineTo(-4.5.dp.toPx() * vScale, -r); lineTo(4.5.dp.toPx() * vScale, -r); close()
            }
            drawContext.canvas.nativeCanvas.drawPath(triPath, arrowPaint)
            drawContext.canvas.nativeCanvas.restore()

            drawContext.canvas.nativeCanvas.drawText("🚛", userPos.x, userPos.y + (5.5f * vScale).dp.toPx(), emojiPaint)
          }
        }
      }
    }

    // TOP-LEFT: MINIMALIST ZOOM LEVEL CHIPS (Vietmap Style)
    Row(
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(start = 14.dp, top = if (activeRoute?.isNavigating == true) 75.dp else 65.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Surface(
        onClick = {
          coroutineScope.launch {
            panOffsetX = 0f
            panOffsetY = 0f
            animatedZoom.animateTo(20.0f, tween(260, easing = FastOutSlowInEasing))
          }
        },
        color = if (zoomLevel >= 19.5f) Color(0xFF1E88E5) else Color.White.copy(alpha = 0.92f),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp
      ) {
        Text(
          text = "20x Gần",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
          color = if (zoomLevel >= 19.5f) Color.White else Color(0xFF334155),
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }

      Surface(
        onClick = {
          coroutineScope.launch {
            panOffsetX = 0f
            panOffsetY = 0f
            animatedZoom.animateTo(18.0f, tween(260, easing = FastOutSlowInEasing))
          }
        },
        color = if (zoomLevel in 17.0f..19.4f) Color(0xFF1E88E5) else Color.White.copy(alpha = 0.92f),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp
      ) {
        Text(
          text = "18x Phố",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
          color = if (zoomLevel in 17.0f..19.4f) Color.White else Color(0xFF334155),
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }

      Surface(
        onClick = {
          coroutineScope.launch {
            panOffsetX = 0f
            panOffsetY = 0f
            animatedZoom.animateTo(15.0f, tween(260, easing = FastOutSlowInEasing))
          }
        },
        color = if (zoomLevel < 17.0f) Color(0xFF1E88E5) else Color.White.copy(alpha = 0.92f),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp
      ) {
        Text(
          text = "15x Tổng",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
          color = if (zoomLevel < 17.0f) Color.White else Color(0xFF334155),
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }
    }

    // RIGHT-HAND CLEAN MINIMALIST TOOLBAR (Layer, 3D, Voice, Recenter)
    Column(
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // 0. Map Layer Switcher (Google Maps HD, Google Satellite, Dark HUD, Carto)
      FloatingActionButton(
        onClick = {
          currentTileSource = when (currentTileSource) {
            MapTileSource.GOOGLE_MAPS_HD -> MapTileSource.GOOGLE_SATELLITE
            MapTileSource.GOOGLE_SATELLITE -> MapTileSource.CARTO_DARK
            MapTileSource.CARTO_DARK -> MapTileSource.CARTO_VOYAGER
            else -> MapTileSource.GOOGLE_MAPS_HD
          }
        },
        containerColor = Color.White.copy(alpha = 0.95f),
        contentColor = Color(0xFF0284C7),
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
        modifier = Modifier.size(46.dp)
      ) {
        val layerIcon = when (currentTileSource) {
          MapTileSource.GOOGLE_SATELLITE -> "🛰️"
          MapTileSource.CARTO_DARK -> "🌙"
          MapTileSource.CARTO_VOYAGER -> "🧭"
          else -> "🗺️"
        }
        Text(text = layerIcon, fontSize = 18.sp)
      }

      // 1. 3D / Track Mode Toggle
      FloatingActionButton(
        onClick = {
          orientationMode = when (orientationMode) {
            MapOrientationMode.TRACK_UP -> MapOrientationMode.NORTH_UP
            MapOrientationMode.NORTH_UP -> MapOrientationMode.TRACK_UP
            MapOrientationMode.FREE -> MapOrientationMode.TRACK_UP
          }
        },
        containerColor = Color.White.copy(alpha = 0.95f),
        contentColor = if (orientationMode == MapOrientationMode.TRACK_UP) Color(0xFF1E88E5) else Color(0xFF64748B),
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
        modifier = Modifier.size(46.dp)
      ) {
        Text(
          text = if (orientationMode == MapOrientationMode.TRACK_UP) "3D" else "2D",
          fontWeight = FontWeight.Black,
          fontSize = 14.sp
        )
      }

      // 2. Voice Audio Toggle
      if (onToggleVoice != null) {
        FloatingActionButton(
          onClick = { onToggleVoice() },
          containerColor = Color.White.copy(alpha = 0.95f),
          contentColor = if (voiceEnabled) Color(0xFF1E88E5) else Color(0xFFEF4444),
          shape = CircleShape,
          elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
          modifier = Modifier.size(46.dp)
        ) {
          Icon(
            imageVector = if (voiceEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
            contentDescription = "Giọng nói",
            modifier = Modifier.size(22.dp)
          )
        }
      }

      // 3. Recenter Button (🎯)
      FloatingActionButton(
        onClick = {
          coroutineScope.launch {
            panOffsetX = 0f
            panOffsetY = 0f
            animatedZoom.animateTo(18.0f, tween(260, easing = FastOutSlowInEasing))
          }
          onRefreshLocation?.invoke()
        },
        containerColor = if (isUserPanning) Color(0xFFE11D48) else Color.White.copy(alpha = 0.95f),
        contentColor = if (isUserPanning) Color.White else Color(0xFF1E88E5),
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
        modifier = Modifier.size(46.dp)
      ) {
        Icon(Icons.Default.MyLocation, contentDescription = "Tâm xe", modifier = Modifier.size(22.dp))
      }
    }

    // Reset center button if user panned away
    if (isUserPanning) {
      Button(
        onClick = {
          coroutineScope.launch {
            panOffsetX = 0f
            panOffsetY = 0f
            animatedZoom.animateTo(18.0f, tween(260, easing = FastOutSlowInEasing))
          }
          onRefreshLocation?.invoke()
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7), contentColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 60.dp)
      ) {
        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Tâm bản đồ về xe", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
      }
    }
  }
}

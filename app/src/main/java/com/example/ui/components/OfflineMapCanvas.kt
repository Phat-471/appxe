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
  // Map tile source: auto-select Carto Voyager or Carto Dark based on setting
  var currentTileSource by remember { mutableStateOf(if (darkMapMode) MapTileSource.CARTO_DARK else MapTileSource.CARTO_VOYAGER) }

  LaunchedEffect(darkMapMode) {
    currentTileSource = if (darkMapMode) MapTileSource.CARTO_DARK else MapTileSource.CARTO_VOYAGER
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

            val cTileX = OsmTileManager.lon2tileX(centerLng, anchorZoom)
            val cTileY = OsmTileManager.lat2tileY(centerLat, anchorZoom)

            // Check camera taps
            var tappedCam: TrafficCamera? = null
            for (cam in cameras) {
              val camTileX = OsmTileManager.lon2tileX(cam.longitude, anchorZoom)
              val camTileY = OsmTileManager.lat2tileY(cam.latitude, anchorZoom)
              val cx = midX + (camTileX - cTileX).toFloat() * tileSizePx
              val cy = midY + (camTileY - cTileY).toFloat() * tileSizePx
              val dist = hypot(tapOffset.x - cx, tapOffset.y - cy)
              if (dist < 48f) {
                tappedCam = cam
                break
              }
            }

            // Check POI taps
            var tappedPoi: MapPoi? = null
            for (poi in pois) {
              val poiTileX = OsmTileManager.lon2tileX(poi.longitude, anchorZoom)
              val poiTileY = OsmTileManager.lat2tileY(poi.latitude, anchorZoom)
              val px = midX + (poiTileX - cTileX).toFloat() * tileSizePx
              val py = midY + (poiTileY - cTileY).toFloat() * tileSizePx
              val dist = hypot(tapOffset.x - px, tapOffset.y - py)
              if (dist < 42f) {
                tappedPoi = poi
                break
              }
            }

            if (tappedCam != null) {
              onSelectCamera(tappedCam)
            } else if (tappedPoi != null) {
              onSelectPoi(tappedPoi)
            } else if (onMapTapLocation != null) {
              val clickTileX = cTileX + (tapOffset.x - midX) / tileSizePx
              val clickTileY = cTileY + (tapOffset.y - midY) / tileSizePx
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

      // Navigation Lookahead: in Track-Up mode, place vehicle lower to see road ahead
      val vehicleVerticalBias = if (orientationMode == MapOrientationMode.TRACK_UP) canvasHeight * 0.12f else 0f
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
                filterQuality = FilterQuality.Medium
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
                  filterQuality = FilterQuality.Medium
                )
              }
            }
          }
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

          val labelText = "📍 ${targetFocusPlace.name}"
          val textWidth = titlePaint.measureText(labelText)
          val bubbleH = 26.dp.toPx()
          val bubbleW = textWidth + 22.dp.toPx()
          val bubbleLeft = focusPos.x - bubbleW / 2f
          val bubbleTop = focusPos.y - 46.dp.toPx()

          drawContext.canvas.nativeCanvas.drawRoundRect(
            bubbleLeft, bubbleTop, bubbleLeft + bubbleW, bubbleTop + bubbleH,
            12.dp.toPx(), 12.dp.toPx(), bgBubblePaint
          )
          drawContext.canvas.nativeCanvas.drawText(
            labelText,
            focusPos.x,
            bubbleTop + 17.dp.toPx(),
            titlePaint
          )
        }

        // 4. ACTIVE NAVIGATION ROUTE OVERLAY (Dynamically shortens as vehicle advances)
        if (activeRoute != null && activeRoute.waypoints.size >= 2) {
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

            // Dark blue route outline casing
            drawPath(
              path = navPath,
              color = Color(0xFF0C4A6E),
              style = Stroke(width = 15.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Electric Blue Polyline
            drawPath(
              path = navPath,
              color = Color(0xFF0284C7),
              style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

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

        // 5. ENHANCED SPECIFIC CAMERA MARKERS ON MAP
        for (cam in cameras) {
          val camPos = project(cam.latitude, cam.longitude)
          val isNear = nearestCamera?.id == cam.id

          // Outer beacon alert glow for nearby cameras
          if (isNear) {
            drawCircle(
              color = (if (activeWarning?.isOverspeeding == true) AlertCrimsonDanger else Color(0xFF0284C7)).copy(alpha = pulseAlpha),
              radius = pulseRadius * 1.1f,
              center = camPos
            )
            drawCircle(
              color = Color(0xFF0284C7).copy(alpha = 0.25f),
              radius = 22.dp.toPx(),
              center = camPos
            )
          }

          when (cam.type) {
            CameraType.SPEED_CAMERA, CameraType.SPEED_LIMIT_SIGN -> {
              // Standard Vietnamese P.127 Speed Limit Sign
              drawCircle(color = Color(0xFF0F172A).copy(alpha = 0.15f), radius = 14.5.dp.toPx(), center = Offset(camPos.x, camPos.y + 2.dp.toPx()))
              drawCircle(color = SignBackgroundWhite, radius = 13.5.dp.toPx(), center = camPos)
              drawCircle(
                color = SignBorderRed,
                radius = 13.5.dp.toPx(),
                center = camPos,
                style = Stroke(width = 3.dp.toPx())
              )

              val speedPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10.5.sp.toPx()
                color = android.graphics.Color.BLACK
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
              }
              drawContext.canvas.nativeCanvas.drawText("${cam.speedLimit}", camPos.x, camPos.y + 3.8.dp.toPx(), speedPaint)

              // Video camera lens badge on top right for speed cameras
              if (cam.type == CameraType.SPEED_CAMERA) {
                val tagCenter = Offset(camPos.x + 9.5.dp.toPx(), camPos.y - 9.5.dp.toPx())
                drawCircle(color = Color(0xFF0284C7), radius = 5.dp.toPx(), center = tagCenter)
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = tagCenter, style = Stroke(width = 1.dp.toPx()))
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = tagCenter)
              }
            }

            CameraType.RED_LIGHT_CAMERA -> {
              // Traffic Light Post Icon
              val w = 15.dp.toPx()
              val h = 25.dp.toPx()
              val left = camPos.x - w / 2f
              val top = camPos.y - h / 2f

              val bgPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.argb(240, 15, 23, 42)
                style = Paint.Style.FILL
              }
              val borderPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.argb(255, 100, 116, 139)
                style = Paint.Style.STROKE
                strokeWidth = 1.5.dp.toPx()
              }

              drawContext.canvas.nativeCanvas.drawRoundRect(left, top, left + w, top + h, 6.dp.toPx(), 6.dp.toPx(), bgPaint)
              drawContext.canvas.nativeCanvas.drawRoundRect(left, top, left + w, top + h, 6.dp.toPx(), 6.dp.toPx(), borderPaint)

              // 3 lights: Red, Yellow, Green
              drawCircle(color = Color(0xFFEF4444), radius = 2.8.dp.toPx(), center = Offset(camPos.x, camPos.y - 6.5.dp.toPx()))
              drawCircle(color = Color(0xFFF59E0B), radius = 2.8.dp.toPx(), center = Offset(camPos.x, camPos.y))
              drawCircle(color = Color(0xFF10B981), radius = 2.8.dp.toPx(), center = Offset(camPos.x, camPos.y + 6.5.dp.toPx()))
            }

            CameraType.COLD_FINE_SURVEILLANCE -> {
              // Cold fine dome / blue surveillance shield
              val sizePx = 22.dp.toPx()
              val left = camPos.x - sizePx / 2f
              val top = camPos.y - sizePx / 2f

              val bgPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.argb(240, 2, 132, 199)
                style = Paint.Style.FILL
              }
              val borderPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 1.8.dp.toPx()
              }

              drawContext.canvas.nativeCanvas.drawRoundRect(left, top, left + sizePx, top + sizePx, 6.dp.toPx(), 6.dp.toPx(), bgPaint)
              drawContext.canvas.nativeCanvas.drawRoundRect(left, top, left + sizePx, top + sizePx, 6.dp.toPx(), 6.dp.toPx(), borderPaint)

              val iconPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10.sp.toPx()
                textAlign = Paint.Align.CENTER
              }
              drawContext.canvas.nativeCanvas.drawText("📷", camPos.x, camPos.y + 3.5.dp.toPx(), iconPaint)
            }

            CameraType.SECURITY_MONITORING -> {
              // Security & Surveillance Dome (Royal Blue + Shield)
              val sizePx = 22.dp.toPx()
              drawCircle(color = Color(0xFF1E40AF), radius = sizePx / 2f, center = camPos)
              drawCircle(color = Color(0xFF93C5FD), radius = sizePx / 2f, center = camPos, style = Stroke(width = 1.8.dp.toPx()))

              val iconPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10.sp.toPx()
                textAlign = Paint.Align.CENTER
              }
              drawContext.canvas.nativeCanvas.drawText("🛡️", camPos.x, camPos.y + 3.5.dp.toPx(), iconPaint)
            }

            CameraType.MOTORBIKE_PROHIBITED_ZONE -> {
              // Prohibited Motorbike Red Warning Circle
              val r = 13.5.dp.toPx()
              drawCircle(color = SignBackgroundWhite, radius = r, center = camPos)
              drawCircle(color = Color(0xFFDC2626), radius = r, center = camPos, style = Stroke(width = 3.dp.toPx()))
              val iconPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10.sp.toPx()
                textAlign = Paint.Align.CENTER
              }
              drawContext.canvas.nativeCanvas.drawText("🚫", camPos.x, camPos.y + 3.5.dp.toPx(), iconPaint)
            }

            CameraType.ZONE_RESIDENTIAL_ENTRY, CameraType.ZONE_RESIDENTIAL_EXIT -> {
              // Residential Zone Blue Sign (R.420 / R.421)
              val w = 24.dp.toPx()
              val h = 18.dp.toPx()
              val left = camPos.x - w / 2f
              val top = camPos.y - h / 2f

              val bgPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.argb(240, 29, 78, 216)
                style = Paint.Style.FILL
              }
              val borderPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 1.5.dp.toPx()
              }

              drawContext.canvas.nativeCanvas.drawRoundRect(left, top, left + w, top + h, 4.dp.toPx(), 4.dp.toPx(), bgPaint)
              drawContext.canvas.nativeCanvas.drawRoundRect(left, top, left + w, top + h, 4.dp.toPx(), 4.dp.toPx(), borderPaint)

              val iconPaint = Paint().apply {
                isAntiAlias = true
                textSize = 9.sp.toPx()
                textAlign = Paint.Align.CENTER
              }
              drawContext.canvas.nativeCanvas.drawText("🏙️", camPos.x, camPos.y + 3.5.dp.toPx(), iconPaint)

              if (cam.type == CameraType.ZONE_RESIDENTIAL_EXIT) {
                drawLine(color = Color(0xFFEF4444), start = Offset(left, top + h), end = Offset(left + w, top), strokeWidth = 2.5.dp.toPx())
              }
            }

            CameraType.HAZARD_ACCIDENT_ZONE -> {
              val w = 24.dp.toPx()
              val h = 21.dp.toPx()
              val path = Path().apply {
                moveTo(camPos.x, camPos.y - h / 2f)
                lineTo(camPos.x + w / 2f, camPos.y + h / 2f)
                lineTo(camPos.x - w / 2f, camPos.y + h / 2f)
                close()
              }
              drawPath(path, color = Color(0xFFF59E0B))
              drawPath(path, color = Color(0xFF0F172A), style = Stroke(width = 2.dp.toPx()))

              val textPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10.sp.toPx()
                color = android.graphics.Color.BLACK
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
              }
              drawContext.canvas.nativeCanvas.drawText("!", camPos.x, camPos.y + 5.dp.toPx(), textPaint)
            }

            else -> {
              drawCircle(color = SignBackgroundWhite, radius = 12.dp.toPx(), center = camPos)
              drawCircle(color = AlertAmberPrimary, radius = 12.dp.toPx(), center = camPos, style = Stroke(width = 2.dp.toPx()))
              val numberPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10.sp.toPx()
                color = android.graphics.Color.BLACK
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
              }
              drawContext.canvas.nativeCanvas.drawText("${cam.speedLimit}", camPos.x, camPos.y + 3.5.dp.toPx(), numberPaint)
            }
          }

          // Proximity floating distance bubble above nearest camera
          if (isNear && nearestCameraDistance != null) {
            val distLabel = "${nearestCameraDistance}m"
            val tagPaint = Paint().apply {
              isAntiAlias = true
              textSize = 10.5.sp.toPx()
              color = android.graphics.Color.WHITE
              typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
              textAlign = Paint.Align.CENTER
            }
            val tagW = tagPaint.measureText(distLabel) + 14.dp.toPx()
            val tagH = 18.dp.toPx()
            val tagLeft = camPos.x - tagW / 2f
            val tagTop = camPos.y - 32.dp.toPx()

            val tagBgPaint = Paint().apply {
              isAntiAlias = true
              color = if (activeWarning?.isOverspeeding == true) android.graphics.Color.argb(240, 225, 29, 72) else android.graphics.Color.argb(240, 15, 23, 42)
              style = Paint.Style.FILL
            }

            drawContext.canvas.nativeCanvas.drawRoundRect(tagLeft, tagTop, tagLeft + tagW, tagTop + tagH, 8.dp.toPx(), 8.dp.toPx(), tagBgPaint)
            drawContext.canvas.nativeCanvas.drawText(distLabel, camPos.x, tagTop + 13.dp.toPx(), tagPaint)
          }
        }

        // 5.2. POI & HAZARD ICONS ON MAP (Gas, Toll, Hospital, Tire Rescue, Blackspots)
        for (poi in pois) {
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

        // 7. HIGH-PRECISION NAVIGATION VEHICLE CHEVRON (Vietmap 3D Style with Compass Sensor)
        val userPos = project(locationState.latitude, locationState.longitude)
        val headingRad = Math.toRadians((resolvedHeading - 90).toDouble())

        // Pulsating location accuracy halo
        drawCircle(
          color = Color(0xFF0284C7).copy(alpha = pulseAlpha),
          radius = pulseRadius * 1.3f,
          center = userPos
        )
        drawCircle(
          color = Color(0xFF0284C7).copy(alpha = 0.25f),
          radius = 24.dp.toPx(),
          center = userPos
        )

        // 3D Navigation Vehicle Chevron
        val arrowLen = 22.dp.toPx()
        val arrowWidth = 15.dp.toPx()
        val forwardX = userPos.x + arrowLen * cos(headingRad).toFloat()
        val forwardY = userPos.y + arrowLen * sin(headingRad).toFloat()
        val leftX = userPos.x + arrowWidth * cos(headingRad + Math.toRadians(142.0)).toFloat()
        val leftY = userPos.y + arrowWidth * sin(headingRad + Math.toRadians(142.0)).toFloat()
        val rightX = userPos.x + arrowWidth * cos(headingRad - Math.toRadians(142.0)).toFloat()
        val rightY = userPos.y + arrowWidth * sin(headingRad - Math.toRadians(142.0)).toFloat()

        val arrowPath = Path().apply {
          moveTo(forwardX, forwardY)
          lineTo(leftX, leftY)
          lineTo(userPos.x, userPos.y)
          lineTo(rightX, rightY)
          close()
        }

        drawPath(
          path = arrowPath,
          color = Color.White,
          style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
          path = arrowPath,
          color = if (activeWarning?.isOverspeeding == true) AlertCrimsonDanger else Color(0xFF00B4D8)
        )
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

    // RIGHT-HAND CLEAN MINIMALIST TOOLBAR (Vietmap Live Style: 3D, Voice, Recenter)
    Column(
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

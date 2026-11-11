package com.autoworktracker.ui.settings

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autoworktracker.domain.model.AppLanguage
import com.autoworktracker.domain.model.Workplace
import com.autoworktracker.domain.util.AppText
import com.autoworktracker.location.LocationPermissions
import com.autoworktracker.ui.common.CollectUserEvents
import com.autoworktracker.ui.theme.Emerald
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.TilesOverlay

private data class PlaceHit(val title: String, val latitude: Double, val longitude: Double)

@Composable
fun WorkplaceMapScreen(
    initialLatitude: Double?,
    initialLongitude: Double?,
    initialRadius: Float,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val labels = AppText.of(state.settings.language)
    val snackbar = remember { SnackbarHostState() }
    CollectUserEvents(viewModel.events, snackbar, text = labels)
    val scope = rememberCoroutineScope()
    val hasPoint = initialLatitude != null && initialLongitude != null
    val start = remember(initialLatitude, initialLongitude) {
        GeoPoint(initialLatitude ?: FALLBACK_LATITUDE, initialLongitude ?: FALLBACK_LONGITUDE)
    }
    var radius by remember(initialRadius) {
        mutableFloatStateOf(initialRadius.coerceIn(Workplace.MIN_RADIUS_METERS, Workplace.MAX_RADIUS_METERS))
    }
    var center by remember(start) { mutableStateOf(start) }
    var query by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<PlaceHit>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var satellite by remember { mutableStateOf(false) }
    val radiusNow by rememberUpdatedState(radius)
    val context = androidx.compose.ui.platform.LocalContext.current
    var tracking by remember { mutableStateOf(LocationPermissions.hasForeground(context)) }
    val map = remember {
        prepareMapConfig(context)
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            setBackgroundColor(android.graphics.Color.rgb(232, 232, 228))
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 14.0
            maxZoomLevel = 19.0
            controller.setZoom(18.0)
            controller.setCenter(start)
        }
    }
    val labelProvider = remember {
        MapTileProviderBasic(context, EsriTiles.labels()).apply {
            tileRequestCompleteHandlers.clear()
        }
    }
    val labelOverlay = remember {
        TilesOverlay(labelProvider, context).apply {
            loadingBackgroundColor = android.graphics.Color.TRANSPARENT
            loadingLineColor = android.graphics.Color.TRANSPARENT
            isEnabled = false
        }
    }
    val circle = remember {
        Polygon(map).apply {
            points = Polygon.pointsAsCircle(start, radius.toDouble())
            fillPaint.color = android.graphics.Color.argb(48, 30, 230, 164)
            outlinePaint.color = android.graphics.Color.rgb(30, 230, 164)
            outlinePaint.strokeWidth = 3f * context.resources.displayMetrics.density
            outlinePaint.isAntiAlias = true
            fillPaint.isAntiAlias = true
        }
    }
    val me = remember {
        Marker(map).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = BitmapDrawable(context.resources, blueLocationDot(context))
            infoWindow = null
            isEnabled = false
        }
    }

    DisposableEffect(map) {
        map.overlays.add(labelOverlay)
        map.overlays.add(circle)
        map.overlays.add(me)
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                moveCircleToCenter()
                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                moveCircleToCenter()
                return true
            }

            private fun moveCircleToCenter() {
                val point = map.mapCenter as? GeoPoint ?: return
                circle.points = Polygon.pointsAsCircle(point, radiusNow.toDouble())
                center = point
                map.invalidate()
            }
        }
        map.addMapListener(listener)
        map.onResume()
        onDispose {
            map.removeMapListener(listener)
            labelProvider.detach()
            map.onPause()
            map.onDetach()
        }
    }
    LaunchedEffect(tracking) {
        if (!tracking) return@LaunchedEffect
        val precise = viewModel.awaitPrecisePoint()
        var centered = false
        if (precise != null) {
            val point = GeoPoint(precise.first, precise.second)
            me.position = point
            me.isEnabled = true
            focus(map, precise.first, precise.second)
            map.invalidate()
            centered = true
        }
        viewModel.deviceFixes().collect { location ->
            val point = GeoPoint(location.latitude, location.longitude)
            me.position = point
            me.isEnabled = true
            map.invalidate()
            if (!centered) {
                centered = true
                focus(map, location.latitude, location.longitude)
            }
        }
    }
    LaunchedEffect(radius) {
        val point = map.mapCenter as? GeoPoint ?: center
        circle.points = Polygon.pointsAsCircle(point, radius.toDouble())
        map.invalidate()
    }
    fun jumpToMe() {
        if (me.isEnabled) {
            focus(map, me.position.latitude, me.position.longitude)
            return
        }
        viewModel.previewCurrentLocation { lat, lng ->
            if (lat != null && lng != null) focus(map, lat, lng)
        }
    }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            tracking = true
            jumpToMe()
        }
    }

    fun search() {
        val text = query.trim()
        if (text.length < 3) return
        scope.launch {
            searching = true
            hits = searchPlaces(text, center.latitude, center.longitude, state.settings.language)
            searching = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { map }, modifier = Modifier.fillMaxSize())
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = labels.pinCenter,
            tint = Emerald,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-22).dp)
                .size(44.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapChip(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = labels.back, tint = Color(0xFF1A1A1A))
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(labels.searchHint) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF5F6368)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { search() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color(0xFF1A1A1A),
                        unfocusedTextColor = Color(0xFF1A1A1A),
                        cursorColor = Color(0xFF1A73E8),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                )
            }
            if (searching || hits.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 6.dp,
                ) {
                    if (searching) {
                        Text(
                            labels.searching,
                            modifier = Modifier.padding(14.dp),
                            color = Color(0xFF1A1A1A),
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                            items(hits) { hit ->
                                Surface(
                                    onClick = {
                                        hits = emptyList()
                                        query = hit.title.substringBefore(",")
                                        focus(map, hit.latitude, hit.longitude)
                                    },
                                    color = Color.White,
                                ) {
                                    Text(
                                        hit.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        color = Color(0xFF1A1A1A),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MapChip(onClick = {
                satellite = !satellite
                if (satellite) {
                    map.setTileSource(EsriTiles.imagery())
                    labelOverlay.isEnabled = true
                } else {
                    map.setTileSource(TileSourceFactory.MAPNIK)
                    labelOverlay.isEnabled = false
                }
                map.invalidate()
            }) {
                Text(
                    if (satellite) labels.scheme else labels.satellite,
                    color = Color(0xFF1A1A1A),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            MapChip(onClick = { map.controller.zoomIn() }) {
                Icon(Icons.Filled.Add, contentDescription = labels.zoomIn, tint = Color(0xFF1A1A1A))
            }
            MapChip(onClick = { map.controller.zoomOut() }) {
                Icon(Icons.Filled.Remove, contentDescription = labels.zoomOut, tint = Color(0xFF1A1A1A))
            }
            MapChip(
                onClick = {
                    if (state.permissions.foregroundLocation || LocationPermissions.hasForeground(context)) {
                        tracking = true
                        jumpToMe()
                    } else {
                        locationPermission.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    }
                },
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = labels.myLocation, tint = Color(0xFF1A73E8))
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(labels.radiusMeters(radius.roundToInt()), style = MaterialTheme.typography.titleMedium)
                Text(
                    labels.mapHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = radius,
                    onValueChange = {
                        radius = (it / 10f).roundToInt().times(10).toFloat()
                            .coerceIn(Workplace.MIN_RADIUS_METERS, Workplace.MAX_RADIUS_METERS)
                    },
                    valueRange = Workplace.MIN_RADIUS_METERS..Workplace.MAX_RADIUS_METERS,
                    steps = 94,
                )
                Button(
                    onClick = {
                        viewModel.saveMapPoint(center.latitude, center.longitude, radius, onBack)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(labels.saveThisPoint)
                }
                Text(
                    labels.mapAttribution,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 210.dp),
        )
    }
}

@Composable
private fun MapChip(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 4.dp,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

private fun focus(map: MapView, latitude: Double, longitude: Double) {
    map.controller.setZoom(18.0)
    map.controller.animateTo(GeoPoint(latitude, longitude))
}

private fun prepareMapConfig(context: Context) {
    val config = Configuration.getInstance()
    val prefs = context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
    config.load(context, prefs)
    config.userAgentValue = "AutoWorkTracker/1.2"
    val base = File(context.cacheDir, "osmdroid")
    base.mkdirs()
    config.osmdroidBasePath = base
    config.osmdroidTileCache = File(base, "tiles").apply { mkdirs() }
}

private fun blueLocationDot(context: Context): Bitmap {
    val size = (28f * context.resources.displayMetrics.density).toInt().coerceAtLeast(28)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.color = android.graphics.Color.rgb(26, 115, 232)
    canvas.drawCircle(size / 2f, size / 2f, size / 2.6f, paint)
    return bitmap
}

private suspend fun searchPlaces(
    query: String,
    nearLatitude: Double,
    nearLongitude: Double,
    language: AppLanguage,
): List<PlaceHit> {
    return withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        val left = nearLongitude - 0.6
        val right = nearLongitude + 0.6
        val top = nearLatitude + 0.4
        val bottom = nearLatitude - 0.4
        val url = URL(
            "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=5&q=$encoded" +
                "&viewbox=$left,$top,$right,$bottom&bounded=0",
        )
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "AutoWorkTracker/1.2 (personal shift tracker)")
            connection.setRequestProperty("Accept-Language", language.acceptLanguage)
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            if (connection.responseCode != 200) return@withContext emptyList()
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val array = JSONArray(body)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val latitude = item.optString("lat").toDoubleOrNull() ?: continue
                    val longitude = item.optString("lon").toDoubleOrNull() ?: continue
                    add(
                        PlaceHit(
                            title = item.optString("display_name"),
                            latitude = latitude,
                            longitude = longitude,
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }
}

private class EsriTiles(
    name: String,
    vararg bases: String,
) : OnlineTileSourceBase(name, 0, 19, 256, "", bases) {
    override fun getTileURLString(mapTileIndex: Long): String {
        return baseUrl +
            MapTileIndex.getZoom(mapTileIndex) + "/" +
            MapTileIndex.getY(mapTileIndex) + "/" +
            MapTileIndex.getX(mapTileIndex)
    }

    companion object {
        fun imagery(): EsriTiles = EsriTiles(
            "EsriWorldImagery",
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/",
        )

        fun labels(): EsriTiles = EsriTiles(
            "EsriReference",
            "https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Reference_Overlay/MapServer/tile/",
        )
    }
}

private const val FALLBACK_LATITUDE = 49.82
private const val FALLBACK_LONGITUDE = 15.48

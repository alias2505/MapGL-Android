package ru.dublgis.dgismobile.mapsdk

import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.fragment.app.Fragment
import ru.dublgis.dgismobile.mapsdk.location.LocationProviderFactory


/**
 * Class used for drawing a 2gis map.
 */
class MapFragment : Fragment() {
    /**
     * Callback for the result when map is ready.
     */
    var mapReadyCallback: MapReadyCallback = { /* noop */ }

    private lateinit var bridge: PlatformBridge
    private var webView: WebView? = null

    /**
     * Called to do initial creation of a MapFragment.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    /** @suppress */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context?.let {
            bridge = PlatformBridge(
                it.packageName ?: "",
                this::evaluateJavaScript,
                this::onMapReady,
                LocationProviderFactory(it),
                this
            )
        }
    }

    /** @suppress */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_dgismap, null)

        webView = view.findViewById<WebView>(R.id.webview)?.apply {
            settings.javaScriptEnabled = true
            webViewClient = bridge
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    if (consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                        val logMessage =
                            "${consoleMessage.message()} -- line: ${consoleMessage.lineNumber()} of ${consoleMessage.sourceId()}"
                        Log.e(
                            TAG, logMessage
                        )
                    }

                    return super.onConsoleMessage(consoleMessage)
                }
            }
            setBackgroundColor(Color.parseColor("#f7f3df"))
            addJavascriptInterface(bridge, "bridge")
            loadData(loadIndexHtml(), "text/html", "base64")
        }
        return view
    }

    /**
     * Called to set initial values of the map.
     *
     * @param apiKey The private key map.
     * @param center Map center in geographical coordinates ([longitude, latitude]).
     * @param maxZoom Maximum map zoom.
     * @param minZoom Minimum map zoom.
     * @param zoom Map zoom.
     * @param maxPitch Maximum map pitch in degrees.
     * @param minPitch Minimum map pitch in degrees.
     * @param pitch Map pitch in degrees.
     * @param rotation Map rotation in degrees.
     * @param controls Whether a zoom control should be added during the map initialization.
     */
    fun setup(
        apiKey: String,
        center: LonLat = LonLat(37.618317, 55.750574),
        maxZoom: Double = 18.0,
        minZoom: Double = 2.0,
        zoom: Double = 16.0,
        maxPitch: Double = 45.0,
        minPitch: Double = 0.0,
        pitch: Double = 0.0,
        rotation: Double = 0.0,
        controls: Boolean = false

    ) = bridge.setup(
        apiKey,
        center,
        maxZoom, minZoom, zoom,
        maxPitch, minPitch, pitch,
        rotation,
        controls
    )

    // ------------------------------------------------------

    private fun onMapReady(map: Map?) {
        mapReadyCallback(map)
    }

    private fun evaluateJavaScript(script: String) {
        webView?.evaluateJavascript(script, null)
    }

    private fun loadIndexHtml(): String {
        val ctx = context ?: return ""

        ctx.assets.open("mapgl/index.html").let {
            return Base64.encodeToString(it.readBytes(), Base64.NO_PADDING or Base64.NO_WRAP)
        }
    }
}
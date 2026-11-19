package com.elbeialy.shorts

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.progressindicator.LinearProgressIndicator

class MainActivity : AppCompatActivity() {

    companion object {
        const val HOME_URL = "https://elbeialy-shorts.vercel.app/"
        const val HOST = "elbeialy-shorts.vercel.app"
    }

    private lateinit var webView: WebView
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var progress: LinearProgressIndicator
    private lateinit var offlineView: View
    private lateinit var pattern: View
    private lateinit var mainContent: View
    private lateinit var fullscreenContainer: FrameLayout

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var lastBack = 0L
    private var splashDone = false

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        fileCallback?.onReceiveValue(
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        )
        fileCallback = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !splashDone }
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        swipe = findViewById(R.id.swipe)
        progress = findViewById(R.id.progress)
        offlineView = findViewById(R.id.offlineView)
        pattern = findViewById(R.id.pattern)
        mainContent = findViewById(R.id.mainContent)
        fullscreenContainer = findViewById(R.id.fullscreenContainer)

        swipe.setColorSchemeColors(getColor(R.color.gold))
        swipe.setProgressBackgroundColorSchemeColor(getColor(R.color.emerald_800))
        swipe.setOnRefreshListener { webView.reload() }
        // السحب للتحديث فقط عندما تكون الصفحة في أعلاها
        swipe.setOnChildScrollUpCallback { _, _ -> webView.scrollY > 0 }

        findViewById<ImageButton>(R.id.btnHome).setOnClickListener { webView.loadUrl(HOME_URL) }
        findViewById<ImageButton>(R.id.btnShare).setOnClickListener { shareCurrent() }
        findViewById<TextView>(R.id.btnRetry).setOnClickListener { retry() }

        webView.setBackgroundColor(Color.parseColor("#042A23"))
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            allowFileAccess = false
            setSupportZoom(false)
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = "$userAgentString ElbeialyShortsApp/1.0"
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                val host = uri.host ?: return false
                // روابط الموقع ومشغّلات الفيديو تفتح داخل التطبيق، والباقي في المتصفح/التطبيق المناسب
                val inApp = host == HOST || host.endsWith("youtube.com") ||
                        host.endsWith("youtube-nocookie.com") || host.endsWith("vercel.app")
                if (inApp && (uri.scheme == "https" || uri.scheme == "http")) return false
                openExternal(uri); return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                swipe.isRefreshing = false
                splashDone = true
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) showOffline(true)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress.setProgressCompat(newProgress, true)
                progress.visibility = if (newProgress < 100) View.VISIBLE else View.INVISIBLE
                if (newProgress > 60) splashDone = true
            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) { callback.onCustomViewHidden(); return }
                customView = view; customViewCallback = callback
                fullscreenContainer.addView(view)
                fullscreenContainer.visibility = View.VISIBLE
                mainContent.visibility = View.GONE
                toggleFullscreen(true)
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            }

            override fun onHideCustomView() { exitFullscreen() }

            override fun onShowFileChooser(
                view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                return try { filePicker.launch(params.createIntent()); true }
                catch (e: Exception) { fileCallback = null; false }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    customView != null -> exitFullscreen()
                    webView.canGoBack() -> webView.goBack()
                    System.currentTimeMillis() - lastBack < 2000 -> finish()
                    else -> {
                        lastBack = System.currentTimeMillis()
                        Toast.makeText(this@MainActivity, R.string.press_back_again, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

        val startUrl = intent?.data?.toString() ?: HOME_URL
        if (savedInstanceState != null) webView.restoreState(savedInstanceState)
        else if (isOnline()) webView.loadUrl(startUrl) else showOffline(true)

        // لا تُبقِ شاشة البداية أكثر من ٣ ثوانٍ مهما حدث
        webView.postDelayed({ splashDone = true }, 3000)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { webView.loadUrl(it.toString()) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onPause() { super.onPause(); webView.onPause() }
    override fun onResume() { super.onResume(); webView.onResume() }

    private fun retry() {
        if (isOnline()) {
            showOffline(false)
            if (webView.url.isNullOrEmpty()) webView.loadUrl(HOME_URL) else webView.reload()
        } else Toast.makeText(this, R.string.offline_msg, Toast.LENGTH_SHORT).show()
    }

    private fun showOffline(show: Boolean) {
        splashDone = true
        swipe.isRefreshing = false
        offlineView.visibility = if (show) View.VISIBLE else View.GONE
        pattern.visibility = if (show) View.VISIBLE else View.GONE
        mainContent.visibility = if (show) View.INVISIBLE else View.VISIBLE
    }

    private fun exitFullscreen() {
        val v = customView ?: return
        fullscreenContainer.removeView(v)
        fullscreenContainer.visibility = View.GONE
        mainContent.visibility = View.VISIBLE
        customView = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        toggleFullscreen(false)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    private fun toggleFullscreen(on: Boolean) {
        WindowCompat.setDecorFitsSystemWindows(window, !on)
        val c = WindowInsetsControllerCompat(window, window.decorView)
        if (on) {
            c.hide(WindowInsetsCompat.Type.systemBars())
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else c.show(WindowInsetsCompat.Type.systemBars())
    }

    private fun shareCurrent() {
        val url = webView.url ?: HOME_URL
        val title = webView.title ?: getString(R.string.app_name)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "$title\n$url")
        }, getString(R.string.share)))
    }

    private fun openExternal(uri: Uri) {
        try { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        catch (_: Exception) { }
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

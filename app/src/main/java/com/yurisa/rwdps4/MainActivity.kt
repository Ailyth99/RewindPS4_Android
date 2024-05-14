package com.yurisa.rwdps4

import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.Html
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Collections
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.ColorFilterTransformation
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import android.text.method.LinkMovementMethod
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.text.style.URLSpan
import android.text.Spanned
import yurisa.Yurisa //Golang

class MainActivity : AppCompatActivity() {

    private lateinit var mode1Layout: LinearLayout
    private lateinit var mode2Layout: LinearLayout
    private lateinit var textViewStatus: TextView
    private lateinit var textViewServerInfo: TextView
    private lateinit var textViewIPPort: TextView
    private lateinit var serverInfoContainer: RelativeLayout

    private var selectedMode: Long = 0
    private var isProxyRunning = false
    private var localIpAddress: String? = null
    private var port: String = "8080"
    private var userjsonUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Extend the background to the top, full-screen background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        mode1Layout = findViewById<LinearLayout>(R.id.mode1Layout)
        mode2Layout = findViewById<LinearLayout>(R.id.mode2Layout)
        textViewStatus = findViewById<TextView>(R.id.textViewStatus)
        textViewServerInfo = findViewById<TextView>(R.id.textViewServerInfo)
        serverInfoContainer = findViewById<RelativeLayout>(R.id.serverInfoContainer)
        textViewIPPort = findViewById<TextView>(R.id.textViewIPPort)

        // Simple animation for the IP display area
        val blinkAnimation = AnimationUtils.loadAnimation(this, R.anim.shine)
        textViewIPPort.startAnimation(blinkAnimation)

        val buttonStart = findViewById<Button>(R.id.buttonStart)


        //default bg img
        val mainLayout = findViewById<RelativeLayout>(R.id.mainLayout)
        Glide.with(this)
            .load(R.drawable.ran)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(1, 3)))
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    mainLayout.background = resource
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    mainLayout.setBackgroundColor(getResources().getColor(R.color.default_background_color))
                }
            })

        //mode1 区域选中状态
        mode1Layout.setOnClickListener {
            if (isProxyRunning) {
                Toast.makeText(this, getString(R.string.toast_server_running), Toast.LENGTH_SHORT)
                    .show()
            } else {
                selectedMode = 1L
                textViewStatus.text =
                    getString(R.string.info_version_locked)
                showJsonInputDialog()
                highlightSelectedMode(mode1Layout, mode2Layout)
                showModeStripe(1)
            }
        }

        //
        mode2Layout.setOnClickListener {
            if (isProxyRunning) {
                Toast.makeText(this, getString(R.string.toast_server_running), Toast.LENGTH_SHORT)
                    .show()
            } else {
                selectedMode = 2L
                highlightSelectedMode(mode2Layout, mode1Layout)
                showModeStripe(2)
                textViewStatus.text =
                    getString(R.string.info_update_blocked)
                textViewStatus.visibility = View.VISIBLE

                findViewById<LinearLayout>(R.id.mode1GameInfo).visibility = View.GONE
                updateStatusAndVisibility()
            }
        }

        buttonStart.setOnClickListener {
            if (selectedMode == 0L) {
                Toast.makeText(this, getString(R.string.toast_select_mode), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Update the mapping LIST or specify whether to block the update server through Yurisa.setMode
            when (selectedMode) {
                1L -> {
                    //val jsonUrl = findViewById<EditText>(R.id.editTextJson).text.toString().trim()
                    if (Yurisa.isValidJSONURL(userjsonUrl)) {
                        Yurisa.setMode(1, userjsonUrl)
                    } else {
                        Snackbar.make(
                            findViewById(R.id.buttonStart),
                            getString(R.string.toast_invalid_json),
                            Snackbar.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }
                }

                2L -> Yurisa.setMode(2, "")
            }

            //CLICK start proxy btn
            if (!isProxyRunning) {
                localIpAddress = getLocalIpAddress()
                if (localIpAddress != null) {
                    if (Yurisa.checkPort(port.toInt().toLong())) {
                        showPortInputDialog()
                    } else {
                        if (selectedMode == 1L && userjsonUrl != null) {
                            Yurisa.setMode(1, userjsonUrl)
                        } else if (selectedMode == 2L) {
                            Yurisa.setMode(2, "")
                        }
                        Yurisa.startProxy(port)
                        isProxyRunning = true

                        //server running info
                        val modeRunningText = when (selectedMode) {
                            1L -> getString(R.string.mode1)
                            2L -> getString(R.string.mode2)
                            else -> getString(R.string.unknown_mode)
                        }
                        val server_info = "$modeRunningText " + getString(R.string.proxy_running) + "\n"
                        val ipPortText = getString(R.string.ip_addr) + localIpAddress + getString(R.string.port) + port
                        textViewServerInfo.text = server_info
                        textViewIPPort.text = ipPortText

                        // Set button background
                        Glide.with(this)
                            .asGif()
                            .load(R.drawable.shiraui)
                            .into(findViewById<ImageView>(R.id.gifBackground))
                        buttonStart.background = null
                        buttonStart.text = getString(R.string.button_stop)
                        buttonStart.setTextColor(ContextCompat.getColor(this, R.color.tsukikazu))
                        showModeStripe(0)
                        mode1Layout.visibility = View.GONE
                        mode2Layout.visibility = View.GONE
                        serverInfoContainer.visibility = View.VISIBLE
                        textViewServerInfo.setTextColor(
                            ContextCompat.getColor(
                                this,
                                R.color.tsukikazu
                            )
                        )

                        Log.d("START ProxyServer", "with mode: $selectedMode")
                    }
                } else {
                    Snackbar.make(
                        findViewById(R.id.buttonStart),
                        getString(R.string.toast_connect_wifi),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                //click STOP SERVER btn
            } else { //isRunning
                Yurisa.stopProxy()
                selectedMode = 0L
                isProxyRunning = false
                findViewById<ImageView>(R.id.gifBackground).setImageResource(0)
                buttonStart.setBackgroundResource(R.drawable.rounded_transparent_background)
                buttonStart.text = getString(R.string.button_start)
                buttonStart.setTextColor(ContextCompat.getColor(this, R.color.white))
                buttonStart.setBackgroundResource(R.drawable.rounded_transparent_background)
                highlightSelectedMode(null, null)
                textViewStatus.visibility = View.GONE
                findViewById<LinearLayout>(R.id.mode1GameInfo).visibility = View.GONE
                serverInfoContainer.visibility = View.GONE
                mode1Layout.visibility = View.VISIBLE
                mode2Layout.visibility = View.VISIBLE
                showModeStripe(0)
                findViewById<TextView>(R.id.mode1Description).setTextColor(ContextCompat.getColor(this, R.color.text_color_default))
                findViewById<TextView>(R.id.mode2Description).setTextColor(ContextCompat.getColor(this, R.color.text_color_default))
                Log.d("STOP ProxyServer", "with mode: $selectedMode")
            }
        }

        val infoIcon = findViewById<ImageView>(R.id.infoIcon)
        infoIcon.setOnClickListener {
            showAppInfoDialog()
        }

        // mode 1 stripe bg
        val mode1GifImageView = findViewById<ImageView>(R.id.mode1gif)
        Glide.with(this)
            .asGif()
            .load(R.drawable.hoshijiro)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(mode1GifImageView)

        // mode 2 stripe bg
        val mode2GifImageView = findViewById<ImageView>(R.id.mode2gif)
        Glide.with(this)
            .asGif()
            .load(R.drawable.hoshijiro)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(mode2GifImageView)

    }

    //TITLE BAR ICON
    private fun showAppInfoDialog() {
        val messageText = SpannableString("\nRewindPS4 for Android 1.0\n\nGitHub_Page")
        val linkText = "GitHub_Page"
        val start = messageText.indexOf(linkText)
        if (start == -1) {
            Log.e("DialogError", "The link text does not exist in the message text.")
            return
        }
        val end = start + linkText.length

        messageText.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        messageText.setSpan(URLSpan("https://github.com/Ailyth99/RewindPS4_Android"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val appInfoDialog = AlertDialog.Builder(this)
            .setTitle("App Information")
            .setMessage(messageText)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()

        appInfoDialog.show()
        // Make the link clickable
        appInfoDialog.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethod.getInstance()
    }

    // Handling JSON input dialog
    private fun showJsonInputDialog() {
        val dialogView = layoutInflater.inflate(R.layout.json_input_dialog, null)
        val editTextJson = dialogView.findViewById<EditText>(R.id.editTextJson)
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(Html.fromHtml("<font color='#FFFFFF'>" + getString(R.string.input_patch_json) + "</font>"))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.button_submit), null)
            //click cancel
            .setNegativeButton(getString(R.string.button_cancel)) { dialog, _ ->
                findViewById<TextView>(R.id.textViewModeSelected).text = getString(R.string.no_mode_selected)
                highlightSelectedMode(null, null) // Reset selection
                selectedMode = 0L // Reset selected mode
                showModeStripe(0)
                textViewStatus.visibility = View.GONE
                findViewById<TextView>(R.id.mode1Description).setTextColor(ContextCompat.getColor(this, R.color.text_color_default))
                dialog.cancel()
            }
            .create()
        alertDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_bg)
        alertDialog.setOnShowListener {
            val button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)

            //click submit
            button.setOnClickListener {
                val jsonLink = editTextJson.text.toString().trim()
                if (Yurisa.isValidJSONURL(jsonLink)) {
                    userjsonUrl = jsonLink
                    val details = Yurisa.details(jsonLink)
                    val detailsJson = JSONObject(details)
                    val gameID = detailsJson.getString("CUSA")
                    val gameName = detailsJson.getString("GameName")
                    val lastVersion = detailsJson.getString("LastVersion")
                    val region = detailsJson.getString("Region")
                    val downgradeVersion = Yurisa.extractVersion(jsonLink)

                    //分别处理CJK字符和西文字母，用于game name的缩略
                    val maxLength = if (containsCJK(gameName)) 10 else 23
                    val gameNameEllipsized = ellipsizeText(gameName, maxLength)
                    val statusText = """
                    $gameID
                    $gameNameEllipsized
                    $region
                    $lastVersion
                    $downgradeVersion
                """.trimIndent()

                    findViewById<TextView>(R.id.rightViewValue).text = statusText
                    findViewById<LinearLayout>(R.id.mode1GameInfo).visibility = View.VISIBLE
                    findViewById<TextView>(R.id.textViewStatus).visibility = View.GONE
                    updateStatusAndVisibility()
                    alertDialog.dismiss()
                    // Update background picture
                    val imageUrl = Yurisa.titleMetadataInfo(jsonLink)
                    Log.d("GAME ICON URL", " $imageUrl")
                    updateBackgroundImage(imageUrl)

                } else {
                    Snackbar.make(
                        dialogView,
                        getString(R.string.toast_invalid_json),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
        alertDialog.show()
    }

    // Handling port input dialog when port is occupied
    private fun showPortInputDialog() {
        val dialogView = layoutInflater.inflate(R.layout.port_input_dialog, null)
        val editTextPort = dialogView.findViewById<EditText>(R.id.editTextPort)
        val buttonStart = findViewById<Button>(R.id.buttonStart)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.port_in_use))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.button_submit)) { dialog, _ ->
                val port = editTextPort.text.toString().trim()
                val localIpAddress = getLocalIpAddress()
                if (localIpAddress != null) {
                    Yurisa.startProxy(port)
                    val modeRunningText = when (selectedMode) {
                        1L -> getString(R.string.mode1)
                        2L -> getString(R.string.mode2)
                        else -> getString(R.string.unknown_mode)
                    }
                    val server_info = "$modeRunningText " + getString(R.string.proxy_running) + "\n"
                    val ipPortText = getString(R.string.port) + localIpAddress + getString(R.string.port) + port
                    textViewServerInfo.text = server_info
                    textViewIPPort.text = ipPortText
                    buttonStart.text = getString(R.string.button_stop)
                    isProxyRunning = true

                    mode1Layout.visibility = View.GONE
                    mode2Layout.visibility = View.GONE
                    serverInfoContainer.visibility = View.VISIBLE
                    findViewById<TextView>(R.id.textViewServerInfo).visibility = View.GONE
                    dialog.dismiss()
                } else {
                    Snackbar.make(
                        findViewById(R.id.buttonStart),
                        getString(R.string.toast_connect_wifi),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton(getString(R.string.button_cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun updateStatusAndVisibility() {
        textViewStatus.visibility =
            if (textViewStatus.text.isNotEmpty()) View.VISIBLE else View.GONE
    }

    // Handling mode selection area
    private fun highlightSelectedMode(selected: LinearLayout?, other: LinearLayout?) {
        if (selected != null && other != null) {
            selected.setBackgroundResource(R.drawable.selected_background)
            other.setBackgroundResource(R.drawable.rounded_transparent_background)
        } else {
            // Reset both layouts to normal background if both parameters are null
            mode1Layout.setBackgroundResource(R.drawable.rounded_transparent_background)
            mode2Layout.setBackgroundResource(R.drawable.rounded_transparent_background)
        }
        val modeText = when (selected?.id) {
            R.id.mode1Layout -> getString(R.string.mode1_selected)
            R.id.mode2Layout -> getString(R.string.mode2_selected)
            else -> getString(R.string.no_mode_selected)
        }
        findViewById<TextView>(R.id.textViewModeSelected).text = modeText
    }



    fun containsCJK(text: String): Boolean {
        val cjkRanges = listOf(
            0x4E00..0x9FFF,
        )

        for (char in text) {
            val code = char.toInt()
            if (cjkRanges.any { code in it }) {
                return true
            }
        }
        return false
    }

    // Abbreviating text
    private fun ellipsizeText(text: String, maxLength: Int): String {
        return if (text.length > maxLength) text.substring(0, maxLength) + "..." else text
    }

    // Handling CJK text abbreviation
    private fun ellipsizeTextForCJK(text: String, maxWidth: Float, paint: Paint): String {
        if (paint.measureText(text) <= maxWidth) {
            return text
        } else {
            var ellipsizedText = text
            while (paint.measureText("$ellipsizedText...") > maxWidth) {
                if (ellipsizedText.isEmpty()) break
                ellipsizedText = ellipsizedText.dropLast(1)
            }
            return "$ellipsizedText..."
        }
    }

    // Updating background image from PS TMDB
    private fun updateBackgroundImage(imageUrl: String?) {
        val imageView = findViewById<ImageView>(R.id.backgroundImage)
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .apply(
                    RequestOptions()
                        .transform(
                            MultiTransformation(
                                BlurTransformation(1, 3),
                                ColorFilterTransformation(
                                    Color.argb(
                                        180,
                                        50,
                                        50,
                                        50
                                    )
                                ) // Darken the image with a filter
                            )
                        )
                )
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        imageView.setImageBitmap(resource)
                        analyzeImageAndAdjustTextColor(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        imageView.setBackgroundColor(getResources().getColor(R.color.default_background_color))
                    }
                })
        } else {
            imageView.setBackgroundColor(getResources().getColor(R.color.dark))
        }
    }

    // Analyzing image color to adjust text color
    private fun analyzeImageAndAdjustTextColor(bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette ->
            palette?.dominantSwatch?.rgb?.let { color ->
                val textColor = if (isColorDark(color)) R.color.white else R.color.black
                //
            }


        }
    }

    private fun isColorDark(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }
    //
    // Getting local IP addr
    private fun getLocalIpAddress(): String? {
        try {
            val networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in networkInterfaces) {
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.hostAddress
                    }
                }
            }
        } catch (ex: SocketException) {
            ex.printStackTrace()
        }
        return null
    }

    
    //
    private fun showModeStripe(selectedMode: Int) {
        val mode1Gif = findViewById<ImageView>(R.id.mode1gif)
        val mode2Gif = findViewById<ImageView>(R.id.mode2gif)

        when (selectedMode) {
            1 -> {
                mode1Gif.visibility = View.VISIBLE
                mode2Gif.visibility = View.GONE
                Glide.with(this).load(R.drawable.hoshijiro).into(mode1Gif)
                findViewById<TextView>(R.id.mode1Description).setTextColor(ContextCompat.getColor(this, R.color.muzuku))
                findViewById<TextView>(R.id.mode2Description).setTextColor(ContextCompat.getColor(this, R.color.text_color_default))
            }
            2 -> {
                mode1Gif.visibility = View.GONE
                mode2Gif.visibility = View.VISIBLE
                Glide.with(this).load(R.drawable.hoshijiro).into(mode2Gif)
                findViewById<TextView>(R.id.mode1Description).setTextColor(ContextCompat.getColor(this, R.color.text_color_default))
                findViewById<TextView>(R.id.mode2Description).setTextColor(ContextCompat.getColor(this, R.color.muzuku))
            }
            else -> {
                mode1Gif.visibility = View.GONE
                mode2Gif.visibility = View.GONE
            }
        }
    }

}
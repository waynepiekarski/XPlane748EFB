// ---------------------------------------------------------------------
//
// XPlane748EFB
//
// Copyright (C) 2018 Wayne Piekarski
// wayne@tinmith.net http://tinmith.net/wayne
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
// ---------------------------------------------------------------------

package net.waynepiekarski.xplane748efb

import android.app.Activity
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.net.InetAddress
import android.widget.EditText
import android.app.AlertDialog
import android.content.Context
import android.os.*
import android.text.InputType
import android.view.SoundEffectConstants
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Exception
import java.net.UnknownHostException




class MainActivity : Activity(), TCPClient.OnTCPEvent, TCPBitmapClient.OnTCPBitmapEvent, MulticastReceiver.OnReceiveMulticast {

    private var becn_listener: MulticastReceiver? = null
    private var tcp_extplane: TCPClient? = null
    private var tcp_texture: TCPBitmapClient? = null
    private var connectAddress: String? = null
    private var manualAddress: String = ""
    private var manualInetAddress: InetAddress? = null
    private var connectSupported = false
    private var connectActiveDescrip: String = ""
    private var connectWorking = false
    private var connectTexturing = false
    private var connectShutdown = false
    private var connectFailures = 0
    private lateinit var overlayCanvas: Canvas
    private lateinit var sourceBitmap: Bitmap
    private var overlayOutlines = false
    private var lastLayoutLeft   = -1
    private var lastLayoutTop    = -1
    private var lastLayoutRight  = -1
    private var lastLayoutBottom = -1
    private var window1Idx = -1
    private var windowNames: ArrayList<String> = ArrayList(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(Const.TAG, "onCreate()")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Reset the layout cache since there might be old values from before the last Activity was terminated
        lastLayoutLeft = -1
        lastLayoutTop = -1
        lastLayoutRight = -1
        lastLayoutBottom = -1
        window1Idx = -1
        windowNames.clear()

        // Reset the Definitions
        Definitions.reset()

        // Add the compiled-in BuildConfig values to the about text
        aboutText.text = aboutText.getText().toString().replace("__VERSION__", "Version: " + Const.getBuildVersion() + " " + BuildConfig.BUILD_TYPE + " build " + Const.getBuildId() + " " + "\nBuild date: " + Const.getBuildDateTime())

        Toast.makeText(this, "Click the panel screws to bring up help and usage information.\nClick the terminal screen or connection status to specify a manual hostname.", Toast.LENGTH_LONG).show()

        // Miscellaneous counters that also need reset
        connectFailures = 0

        efbImage.setOnTouchListener { _view, motionEvent ->
            if (backgroundThread == null) {
                // It seems possible for onTouch events to arrive after onPause, but the background
                // thread is now null, and I've observed null exceptions in doBgThread. So avoid handling
                // any events here if the app is not running.
                Log.w(Const.TAG, "onTouch event ignored after onPause()")
            } else if (motionEvent.action == MotionEvent.ACTION_UP) {
                // Compute touch location relative to the original image size
                val ix = ((motionEvent.x * efbImage.getDrawable().intrinsicWidth) / efbImage.width).toInt()
                val iy = ((motionEvent.y * efbImage.getDrawable().intrinsicHeight) / efbImage.height).toInt()
                Log.d(Const.TAG, "ImageClick = ${ix},${iy}, RawClick = ${motionEvent.x},${motionEvent.y} from Image ${efbImage.getDrawable().intrinsicWidth},${efbImage.getDrawable().intrinsicHeight} -> ${efbImage.width},${efbImage.height}")

                // If the help is visible, hide it on any kind of click
                if (aboutText.visibility == View.VISIBLE) {
                    aboutText.visibility = View.INVISIBLE
                    overlayOutlines = false
                    refreshOverlay()
                    return@setOnTouchListener true
                }

                // Find the click inside the definitions
                for (entry in Definitions.buttons) {
                    if ((ix >= entry.value.x1) && (ix <= entry.value.x2) && (iy >= entry.value.y1) && (iy <= entry.value.y2)) {
                        Log.d(Const.TAG, "Found click matches to key ${entry.key}")
                        if (entry.key.startsWith("internal_hostname")) {
                            // Pop up the host name changer for this item type
                            popupManualHostname()
                        } else if (entry.key.startsWith("internal_help")) {
                            // One of the many help buttons were pressed, they all map to the same action
                            if (aboutText.visibility == View.VISIBLE) {
                                aboutText.visibility = View.INVISIBLE
                                overlayOutlines = false
                                refreshOverlay()
                            } else {
                                aboutText.visibility = View.VISIBLE
                                overlayOutlines = true
                                refreshOverlay()
                            }
                        } else if (entry.key.startsWith("internal_")) {
                            Log.w(Const.TAG, "Unknown internal command ${entry.key} - ignored")
                        } else {
                            // We now have a button name. Execute this in the definition list to see
                            // what dataref changes come out
                            val outbound = Definitions.executeButton(entry.key)

                            if (outbound.length == 0) {
                                Log.d(Const.TAG, "After executing key ${entry.key}, no outbound network commands generated")
                            } else {
                                // Send the request on a separate thread, note the background operation
                                // happens later so we check if the TCP socket has changed later on
                                val tcpRef = tcp_extplane
                                doBgThread {
                                    if ((tcpRef != null) && (tcpRef == tcp_extplane) && connectWorking) {
                                        tcpRef.write(outbound)
                                    } else {
                                        Log.d(Const.TAG, "Ignoring outbound network commands since TCP connection is not available: $outbound")
                                    }
                                }
                            }

                            // Play sound effect on button press
                            efbImage.playSoundEffect(SoundEffectConstants.CLICK)
                        }
                    }
                }
            }
            return@setOnTouchListener true
        }

        efbImage.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if ((lastLayoutLeft == left) && (lastLayoutTop == top) && (lastLayoutRight == right) && (lastLayoutBottom == bottom)) {
                Log.d(Const.TAG, "Skipping layout change since it is identical to current layout")
                return@addOnLayoutChangeListener
            }
            lastLayoutLeft = left
            lastLayoutTop = top
            lastLayoutRight = right
            lastLayoutBottom = bottom
            layoutEfbImage()
        }

        connectText.setOnClickListener { popupManualHostname() }
    }

    private fun layoutEfbImage() {
        Log.d(Const.TAG, "Layout change: $lastLayoutLeft, $lastLayoutTop, $lastLayoutRight, $lastLayoutBottom")
        Log.d(Const.TAG, "EFB raw image = ${efbImage.getDrawable().intrinsicWidth}x${efbImage.getDrawable().intrinsicHeight}")
        Log.d(Const.TAG, "EFB scaled image = ${efbImage.width}x${efbImage.height}")
        Log.d(Const.TAG, "EFB raw help = ${efbHelp.getDrawable().intrinsicWidth}x${efbHelp.getDrawable().intrinsicHeight}")
        Log.d(Const.TAG, "EFB scaled help = ${efbHelp.width}x${efbHelp.height}")
        val scaleX = efbImage.width / efbImage.getDrawable().intrinsicWidth.toFloat()
        val scaleY = efbImage.height / efbImage.getDrawable().intrinsicHeight.toFloat()

        // Compute the dimensions of the text display in actual device pixels,
        // since the EFB ImageView has been stretched to fit this
        val efbOutline = Definitions.buttons["internal_display"]!!
        val pixelXLeft   = (efbOutline.x1 * scaleX).toInt()
        val pixelXRight  = (efbOutline.x2 * scaleX).toInt()
        val pixelYTop    = (efbOutline.y1 * scaleY).toInt()
        val pixelYBottom = (efbOutline.y2 * scaleY).toInt()
        val pixelWidth   = pixelXRight - pixelXLeft
        val pixelHeight  = pixelYBottom - pixelYTop

        // Set the padding in pixels according to where the texture display will be fit in to
        val lp1 = leftPaddingDisplay.getLayoutParams()
        lp1.width = pixelXLeft
        leftPaddingDisplay.setLayoutParams(lp1)
        val lp2 = topPaddingDisplay.getLayoutParams()
        lp2.height = pixelYTop
        topPaddingDisplay.setLayoutParams(lp2)

        // Resize the texture display
        val lpT = textureImage1.getLayoutParams()
        lpT.width = pixelWidth
        lpT.height = pixelHeight
        textureImage1.setLayoutParams(lpT)

        // Adjust the about box to the correct width to fit only over the texture display
        val lp3 = aboutText.getLayoutParams()
        lp3.width = pixelWidth
        aboutText.setLayoutParams(lp3)

        // Create a transparent overlay to draw key outlines and also any other indicators
        val bitmapDrawable = efbImage.getDrawable() as BitmapDrawable
        sourceBitmap = bitmapDrawable.getBitmap()
        val bitmapNew = Bitmap.createBitmap(sourceBitmap.width, sourceBitmap.height, Bitmap.Config.ARGB_8888)
        overlayCanvas = Canvas(bitmapNew)
        Log.d(Const.TAG, "Adding overlay bitmap of size ${bitmapNew.width}x${bitmapNew.height}")
        efbHelp.setImageBitmap(bitmapNew)

        // Refresh the overlay for the first time
        refreshOverlay()
    }

    companion object {
        private var backgroundThread: HandlerThread? = null

        fun doUiThread(code: () -> Unit) {
            Handler(Looper.getMainLooper()).post { code() }
        }

        fun doBgThread(code: () -> Unit) {
            Handler(backgroundThread!!.getLooper()).post { code() }
        }
    }

    // The user can click on the connectText and specify a X-Plane hostname manually
    private fun changeManualHostname(hostname: String) {
        if (hostname.isEmpty()) {
            Log.d(Const.TAG, "Clearing override X-Plane hostname for automatic mode, saving to prefs, restarting networking")
            manualAddress = hostname
            val sharedPref = getPreferences(Context.MODE_PRIVATE)
            with(sharedPref.edit()){
                putString("manual_address", manualAddress)
                commit()
            }
            restartNetworking()
        } else {
            Log.d(Const.TAG, "Setting override X-Plane hostname to $manualAddress")
            // Lookup the IP address on a background thread
            doBgThread {
                try {
                    manualInetAddress = InetAddress.getByName(hostname)
                } catch (e: UnknownHostException) {
                    // IP address was not valid, so ask for another one and exit this thread
                    doUiThread { popupManualHostname(error=true) }
                    return@doBgThread
                }

                // We got a valid IP address, so we can now restart networking on the UI thread
                doUiThread {
                    manualAddress = hostname
                    Log.d(Const.TAG, "Converted manual X-Plane hostname [$manualAddress] to ${manualInetAddress}, saving to prefs, restarting networking")
                    val sharedPref = getPreferences(Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("manual_address", manualAddress)
                        commit()
                    }
                    restartNetworking()
                }
            }
        }
    }

    private fun popupManualHostname(error: Boolean = false) {
        val builder = AlertDialog.Builder(this)
        if (error)
            builder.setTitle("Invalid entry! Specify X-Plane hostname or IP")
        else
            builder.setTitle("Specify X-Plane hostname or IP")

        val input = EditText(this)
        input.setText(manualAddress)
        input.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        builder.setView(input)
        builder.setPositiveButton("Manual Override") { dialog, which -> changeManualHostname(input.text.toString()) }
        builder.setNegativeButton("Revert") { dialog, which -> dialog.cancel() }
        builder.setNeutralButton("Automatic Multicast") { dialog, which -> changeManualHostname("") }
        builder.show()
    }

    fun refreshOverlay() {
        // Always clear the overlay first, this is not a super efficient process, but not used very often
        overlayCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        fun drawBox(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, paint: Paint) {
            canvas.drawLine(x1, y1, x2, y1, paint)
            canvas.drawLine(x2, y1, x2, y2, paint)
            canvas.drawLine(x2, y2, x1, y2, paint)
            canvas.drawLine(x1, y2, x1, y1, paint)
        }

        var fontSize = -1.0f
        var bounds = Rect()
        for ((_, item) in Definitions.buttons) {
            // Draw the label text in if this item has been flagged, otherwise use the text in the image
            if (item.drawLabel != null) {
                val paint = Paint()
                paint.color = Color.LTGRAY
                paint.textScaleX = 0.65f // Make the font a bit thinner than usual
                paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                paint.setAntiAlias(true)

                val maxWidth = item.x2 - item.x1
                val maxHeight = item.y2 - item.y1

                // If this is the first time we're handling a button, compute the font size. We assume all buttons
                // are the same size, so don't mix and match the sizes!
                if (fontSize < 0) {
                    // Compute the font size for a label XXXX XXXX that fits within one of the rectangle buttons
                    fontSize = 1.0f
                    val testBounds = Rect()
                    while (true) {
                        paint.textSize = fontSize
                        paint.getTextBounds("XXXX", 0, "XXXX".length, testBounds)
                        // Use 1.0f for the height, since we only have one row of text. Use 2.0f for two rows.
                        // Also, only use 75% of the max width, and 60% of the max height, do not use the whole button area
                        if ((bounds.width() < maxWidth * 0.75f) && (bounds.height()*1.0f < maxHeight * 0.6f)) {
                            fontSize += 0.5f
                            bounds = testBounds
                        } else {
                            break
                        }
                    }
                    // We now have fontSize set to the largest value possible
                    Log.d(Const.TAG, "Found font size ${fontSize} for EFB key overlay with height ${maxHeight}")
                }
                paint.textSize = fontSize

                fun centerText(text: String, x: Float, y: Float, paint: Paint, canvas: Canvas) {
                    val width = paint.measureText(text)
                    canvas.drawText(text, x - width/2.0f, y, paint)
                }

                // Deal with either one or two rows of text, but not more
                val lines = item.drawLabel!!.split(' ') // Split on spaces or newlines

                val yCenter = (item.y1 + maxHeight/2.0f)
                // paint.ascent() goes slightly higher than all-caps text, and paint.descent() goes slightly lower.
                // They are not exactly equal, but combining them gives the best vertical centering I've seen so far.
                val yHeight = paint.ascent() + paint.descent()

                if (lines.size == 1) {
                    centerText(lines[0], item.x1 + maxWidth/2.0f, (yCenter - yHeight/2.0f), paint, overlayCanvas)
                } else if (lines.size == 2) {
                    centerText(lines[0], item.x1 + maxWidth/2.0f, (yCenter - yHeight/2.0f) + yHeight/1.5f, paint, overlayCanvas)
                    centerText(lines[1], item.x1 + maxWidth/2.0f, (yCenter - yHeight/2.0f) - yHeight/1.5f, paint, overlayCanvas)
                } else {
                    Log.e(Const.TAG, "Found ${lines.size} in string [${item.drawLabel}] instead of expected 1 or 2 split on space")
                }
            }
        }

        // Draw the key outlines if they are active
        if (overlayOutlines) {
            val paint = Paint()
            paint.color = Color.RED

            for (entry in Definitions.buttons) {
                if (entry.value.x1 >= 0) {
                    overlayCanvas.drawText(entry.key, entry.value.x1.toFloat() + 3.0f, entry.value.y2.toFloat() - 3.0f, paint)
                    drawBox(overlayCanvas, entry.value.x1.toFloat(), entry.value.y1.toFloat(), entry.value.x2.toFloat(), entry.value.y2.toFloat(), paint)
                }
            }
        }

        // Notify the ImageView about the latest bitmap change
        efbHelp.invalidate()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Only implement full-screen in API >= 19, older Android brings them back on each click
        if (hasFocus && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    override fun onConfigurationChanged(config: Configuration) {
        Log.d(Const.TAG, "onConfigurationChanged ignored")
        super.onConfigurationChanged(config)
    }

    override fun onResume() {
        super.onResume()
        Log.d(Const.TAG, "onResume()")
        connectShutdown = false

        // Start up our background processing thread
        backgroundThread = HandlerThread("BackgroundThread")
        backgroundThread!!.start()

        // Retrieve the manual address from shared preferences
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val prefAddress = sharedPref.getString("manual_address", "")
        Log.d(Const.TAG, "Found preferences value for manual_address = [$prefAddress]")

        // Pass on whatever this string is, and will end up calling restartNetworking()
        changeManualHostname(prefAddress)
    }

    private fun setConnectionStatus(line1: String, line2: String, fixup: String, dest: String? = null, redraw: Boolean = true) {
        Log.d(Const.TAG, "Changing connection status to [$line1][$line2][$fixup] with destination [$dest]")
        var out = line1 + ". "
        if (line2.length > 0)
            out += "${line2}. "
        if (fixup.length > 0)
            out += "${fixup}. "
        if (dest != null)
            out += "${dest}."
        if (connectFailures > 0)
            out += "\nError #$connectFailures"

        connectText.text = out
    }

    private fun restartNetworking() {
        Log.d(Const.TAG, "restartNetworking()")
        setConnectionStatus("Closing down network", "", "Wait a few seconds")
        connectAddress = null
        connectTexturing = false
        connectWorking = false
        connectSupported = false
        connectActiveDescrip = ""
        if (tcp_extplane != null) {
            Log.d(Const.TAG, "Cleaning up any EXTPLANE TCP connections")
            tcp_extplane!!.stopListener()
            tcp_extplane = null
        }
        if (tcp_texture != null) {
            Log.d(Const.TAG, "Cleaning up any TEXTURE TCP connections")
            tcp_texture!!.stopListener()
            tcp_texture = null
        }
        if (becn_listener != null) {
            Log.w(Const.TAG, "Cleaning up the BECN listener, somehow it is still around?")
            becn_listener!!.stopListener()
            becn_listener = null
        }
        if (connectShutdown) {
            Log.d(Const.TAG, "Will not restart BECN listener since connectShutdown is set")
        } else {
            if (manualAddress.isEmpty()) {
                setConnectionStatus("Waiting for X-Plane", "BECN broadcast", "Touch to override")
                Log.d(Const.TAG, "Starting X-Plane BECN listener since connectShutdown is not set")
                becn_listener = MulticastReceiver(Const.BECN_ADDRESS, Const.BECN_PORT, this)
            } else {
                Log.d(Const.TAG, "Manual address $manualAddress specified, skipping any auto-detection")
                check(tcp_extplane == null)
                connectAddress = manualAddress
                setConnectionStatus("Manual TCP connect", "Find ExtPlane plugin", "Check Win firewall", "$connectAddress:${Const.TCP_EXTPLANE_PORT}")
                tcp_extplane = TCPClient(manualInetAddress!!, Const.TCP_EXTPLANE_PORT, this)
            }
        }
        Definitions.reset()
        textureImage1.setImageBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    }

    override fun onPause() {
        Log.d(Const.TAG, "onPause()")
        super.onPause()
        connectShutdown = true // Prevent new BECN listeners starting up in restartNetworking
        if (tcp_extplane != null) {
            Log.d(Const.TAG, "onPause(): Cancelling existing EXTPLANE TCP connection")
            tcp_extplane!!.stopListener()
            tcp_extplane = null
        }
        if (tcp_texture != null) {
            Log.d(Const.TAG, "onPause(): Cancelling existing TEXTURE TCP connection")
            tcp_texture!!.stopListener()
            tcp_texture = null
        }
        if (becn_listener != null) {
            Log.d(Const.TAG, "onPause(): Cancelling existing BECN listener")
            becn_listener!!.stopListener()
            becn_listener = null
        }
        backgroundThread!!.quit()
        backgroundThread = null
    }

    override fun onDestroy() {
        Log.d(Const.TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun onFailureMulticast(ref: MulticastReceiver) {
        if (ref != becn_listener)
            return
        connectFailures++
        setConnectionStatus("No network available", "Cannot listen for BECN", "Enable WiFi")
    }

    override fun onTimeoutMulticast(ref: MulticastReceiver) {
        if (ref != becn_listener)
            return
        Log.d(Const.TAG, "Received indication the multicast socket is not getting replies, will restart it and wait again")
        connectFailures++
        setConnectionStatus("Timeout waiting for", "BECN multicast", "Touch to override")
    }

    override fun onReceiveMulticast(buffer: ByteArray, source: InetAddress, ref: MulticastReceiver) {
        if (ref != becn_listener)
            return
        setConnectionStatus("Found BECN multicast", "Find ExtPlane plugin", "Check Win firewall", source.getHostAddress())
        connectAddress = source.toString().replace("/","")

        // The BECN listener will only reply once, so close it down and open the TCP connection
        becn_listener!!.stopListener()
        becn_listener = null

        check(tcp_extplane == null)
        Log.d(Const.TAG, "Making EXTPLANE connection to $connectAddress:${Const.TCP_EXTPLANE_PORT}")
        tcp_extplane = TCPClient(source, Const.TCP_EXTPLANE_PORT, this)
    }

    override fun onConnectTCP(tcpRef: TCPClient) {
        if (tcpRef != tcp_extplane)
            return
        // We will wait for EXTPLANE 1 in onReceiveTCP, so don't send the requests just yet
        setConnectionStatus("Established TCP", "Waiting for ExtPlane", "Needs ExtPlane plugin", "$connectAddress:${Const.TCP_EXTPLANE_PORT}")
    }

    override fun onDisconnectTCP(tcpRef: TCPClient) {
        if (tcpRef != tcp_extplane)
            return
        Log.d(Const.TAG, "onDisconnectTCP(): Closing down TCP connection and will restart")
        connectFailures++
        restartNetworking()
    }

    override fun onConnectTCP(tcpRef: TCPBitmapClient) {
        if (tcpRef != tcp_texture)
            return
        setConnectionStatus("Established TCP", "Waiting for XTE", "Needs XTE plugin", "$connectAddress:${Const.TCP_TEXTURE_PORT}")
    }

    override fun onDisconnectTCP(reason: String?, tcpRef: TCPBitmapClient) {
        if (tcpRef != tcp_texture)
            return
        Log.d(Const.TAG, "onDisconnectTCP(): Closing down TCP connection and will restart")
        if (reason != null) {
            Log.d(Const.TAG, "Network failed due to reason [$reason]")
            Toast.makeText(this, "Network failed - $reason", Toast.LENGTH_LONG).show()
        }
        connectFailures++
        restartNetworking()
    }

    override fun onReceiveTCPBitmap(windowId: Int, image: Bitmap, tcpRef: TCPBitmapClient) {
        // If the current connection does not match the incoming reference, it is out of date and should be ignored.
        // This is important otherwise we will try to transmit on the wrong socket, fail, and then try to restart.
        if (tcpRef != tcp_texture)
            return

        if (!connectTexturing) {
            // Everything is working with actual data coming back. This is the final step with ExtPlane and XTE both working!
            connectFailures = 0
            setConnectionStatus("X-Plane 748 EFB", connectActiveDescrip, "", "$connectAddress:${Const.TCP_EXTPLANE_PORT}+${Const.TCP_TEXTURE_PORT}")
            connectTexturing = true
        }

        // Store the image into the layout, which will resize it to fit the screen
        // Log.d(Const.TAG, "TCP returned window $windowId bitmap $image with ${image.width}x${image.height}, win1=$window1Idx")
        if (window1Idx == windowId)
            textureImage1.setImageBitmap(image)
    }

    override fun onReceiveTCP(line: String, tcpRef: TCPClient) {
        // If the current connection does not match the incoming reference, it is out of date and should be ignored.
        // This is important otherwise we will try to transmit on the wrong socket, fail, and then try to restart.
        if (tcpRef != tcp_extplane)
            return

        if (line.startsWith("EXTPLANE 1")) {
            Log.d(Const.TAG, "Found ExtPlane welcome message, will now make subscription requests for aircraft info")
            setConnectionStatus("Received EXTPLANE", "Sending acf subscribe", "Start your flight", "$connectAddress:${Const.TCP_EXTPLANE_PORT}")

            // Make requests for aircraft type messages so we can detect when a supported aircraft is available,
            // the datarefs do not exist until the aircraft is loaded and in use
            doBgThread {
                tcpRef.writeln("sub sim/aircraft/view/acf_descrip")
            }
        } else {
            // Log.d(Const.TAG, "Received TCP line [$line]")
            if (!connectWorking) {
                check(!connectSupported) { "connectSupported should not be set if connectWorking is not set" }
                // Everything is working with actual data coming back.
                // This is the last time we can put debug text on the EFB before it is overwritten
                connectFailures = 0
                setConnectionStatus("X-Plane EFB starting", "Waiting acf_descrip", "Must be Zibo/SSG", "$connectAddress:${Const.TCP_EXTPLANE_PORT}")
                connectWorking = true
            }

            val tokens = line.split(" ")
            if (tokens[0] == "ub") {
                val decoded = String(Base64.decode(tokens[2], Base64.DEFAULT))

                Log.d(Const.TAG, "Decoded byte array with [$decoded]=${decoded.length} for name [${tokens[1]}]")

                // We have received a change in acf_descrip. If we have never seen any aircraft before, then start
                // the subscriptions if it is either Zibo or SSG. If we have seen a previous aircraft, then reset
                // the network and UI to start fresh.
                if (tokens[1] == "sim/aircraft/view/acf_descrip") {
                    if (connectActiveDescrip == "") {
                        // No previous aircraft during this connection
                        connectActiveDescrip = decoded

                        // The aircraft description has actually changed from before, look for one of our supported aircraft
                        val SSG748I_DESCRIP = "SSG Boeing 748-i"
                        val SSG748F_DESCRIP = "SSG  Boeing 748 - Freighter" // Two spaces is a typo in the SSG aircraft
                        if (decoded.contains(SSG748I_DESCRIP) || decoded.contains(SSG748F_DESCRIP))
                        {
                            setConnectionStatus("X-Plane EFB starting", "Sub: ${connectActiveDescrip}", "Check latest plugin", "$connectAddress:${Const.TCP_EXTPLANE_PORT}")

                            // The aircraft has changed to a supported aircraft, so start the subscription process
                            Log.d(Const.TAG, "Sending subscriptions for SSG 748 I/F datarefs now that it is detected")
                            Definitions.setAircraft(Definitions.Aircraft.SSG)
                            Definitions.reset()
                            doBgThread {
                                for (entry in Definitions.EFBDatarefsSSG748) {
                                    val outbound = Definitions.subscribeDREF(entry.key)
                                    if (outbound.length > 0)
                                        tcpRef.writeln(outbound)
                                }
                            }
                        } else {
                            // acf_descrip contains an aircraft which we don't support
                            setConnectionStatus("X-Plane EFB failed", "Invalid ${connectActiveDescrip}", "Must be Zibo/SSG", "$connectAddress:${Const.TCP_EXTPLANE_PORT}")
                        }
                    } else if (connectActiveDescrip == decoded) {
                        // acf_descrip was sent to us with the same value. This can happen if a second device connects
                        // via ExtPlane, and it updates all listeners with the latest value. We can safely ignore this.
                        Log.d(Const.TAG, "Detected aircraft update which is the same [$connectActiveDescrip], but ignoring since nothing has changed")
                    } else {
                        // Currently handling another aircraft, so reset everything to keep the restart sequence simple
                        Log.d(Const.TAG, "Detected aircraft change from [$connectActiveDescrip] to [$decoded], so resetting display and connection")
                        restartNetworking()
                    }
                } else {
                    Log.d(Const.TAG, "Found unused result name [${tokens[1]}] with string [$decoded]")
                }
            } else if ((tokens[0] == "ud") || (tokens[0] == "uf") || (tokens[0] == "ui")) {
                val number = tokens[2].toDouble()
                // Log.d(Const.TAG, "Decoded number for name [${tokens[1]}] with value [$number]")
                val round = (tokens[0] == "ui")
                Definitions.incomingDREF(tokens[1], number, round)
            } else if ((tokens[0] == "uda") || (tokens[0] == "ufa") || (tokens[0] == "uia")) {
                var items = tokens[2]
                if (items.startsWith('[') && items.endsWith(']')) {
                    items = items.substring(1, items.length-1)
                    val numbers = items.split(',')
                    // Log.d(Const.TAG, "Decoded for name [${tokens[1]}] with values {$numbers} and ${numbers.size} items")
                    val round = (tokens[0] == "uia")
                    val doubles = numbers.map {it.toDouble() }.toDoubleArray()
                    Definitions.incomingDREF(tokens[1], doubles, round)
                } else {
                    Log.e(Const.TAG, "Did not find enclosing [ ] brackets in [${tokens[2]}]")
                }
            } else {
                Log.e(Const.TAG, "Unknown encoding type [${tokens[0]}] for name [${tokens[1]}]")
            }

            // Track when the last dataref we subscribed to arrives, once this has occurred then we know we are done.
            if (!connectSupported) {
                var unfinished = 0
                for (entry in Definitions.EFBDatarefsSSG748) {
                    if (!entry.value.init) {
                        unfinished++
                        // Log.d(Const.TAG, "Unfinished dataref ${entry.key}")
                    }
                }
                if (unfinished == 0) {
                    setConnectionStatus("X-Plane EFB ExtPlane", "${connectActiveDescrip}", "Starting XTE", "$connectAddress:${Const.TCP_EXTPLANE_PORT}", redraw = false)
                    connectSupported = true

                    // Now make the XTextureExtractor connection since the ExtPlane set up is done
                    tcp_texture = TCPBitmapClient(tcpRef.address, Const.TCP_TEXTURE_PORT, this)
                } else {
                    // Log.d(Const.TAG, "Waiting for $unfinished unfinished datarefs")
                }
            }
        }
    }

    private fun networkingFatal(reason: String) {
        Log.d(Const.TAG, "Network fatal error due to reason [$reason]")
        Toast.makeText(this, "Network error - $reason", Toast.LENGTH_LONG).show()
        restartNetworking()
    }

    override fun onReceiveTCPHeader(header: ByteArray, tcpRef: TCPBitmapClient) {
        var headerStr = String(header)
        headerStr = headerStr.substring(0, headerStr.indexOf(0x00.toChar()))
        Log.d(Const.TAG, "Received raw header [$headerStr] before PNG stream")
        try {
            val bufferedReader = BufferedReader(InputStreamReader(ByteArrayInputStream(header)))
            val version = bufferedReader.readLine().split(' ')[0]
            val aircraft = bufferedReader.readLine()
            val texture = bufferedReader.readLine().split(' ')
            val textureWidth = texture[0].toInt()
            val textureHeight = texture[1].toInt()
            Log.d(Const.TAG, "Plugin version [$version], aircraft [$aircraft], texture ${textureWidth}x${textureHeight}")
            var line: String?
            windowNames.clear()
            while (true) {
                line = bufferedReader.readLine()
                if (line == null || line.contains("__EOF__"))
                    break
                val window = line.split(' ')
                val name = window[0]
                windowNames.add(name)
                val l = window[1].toInt()
                val t = window[2].toInt()
                val r = window[3].toInt()
                val b = window[4].toInt()
                Log.d(Const.TAG, "Window [$name] = ($l,$t)->($r,$b)")
                if (name.contains("CFG") || name.contains("EFB")) {
                    window1Idx = windowNames.size-1
                    Log.d(Const.TAG, "Found suitable EFB texture window at index $window1Idx")
                }
            }
            // connectAircraft = "$aircraft ${textureWidth}x${textureHeight}"
            if (version != Const.TCP_PLUGIN_VERSION) {
                networkingFatal("Version [$version] is not expected [${Const.TCP_PLUGIN_VERSION}]")
                return
            }
            if (windowNames.size <= 0) {
                networkingFatal("No valid windows were sent")
                return
            }
        } catch (e: IOException) {
            Log.e(Const.TAG, "IOException processing header - $e")
            networkingFatal("Invalid header data")
            return
        } catch (e: Exception) {
            Log.e(Const.TAG, "Unknown exception processing header - $e")
            networkingFatal("Invalid header read")
            return
        }
    }

    override fun getWindow1Index(): Int { return window1Idx }
    override fun getWindow2Index(): Int { return window1Idx } // Use same as window 1
}

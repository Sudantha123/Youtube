package com.example.videoplayer

import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.videoplayer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var player: ExoPlayer? = null

    private val speeds = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    private var currentSpeedIndex = 2 // 1.0x

    private val resizeModes = arrayOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit",
        AspectRatioFrameLayout.RESIZE_MODE_FILL to "Fill",
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Zoom",
        AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH to "Fix W",
        AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT to "Fix H"
    )
    private var currentResizeIndex = 0

    private val selectVideoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { playUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initPlayer()
        setupListeners()
        handleIncomingIntent(intent)
    }

    private fun initPlayer() {
        if (player == null) {
            player = ExoPlayer.Builder(this).build().apply {
                binding.playerView.player = this
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> binding.loadingSpinner.visibility = View.VISIBLE
                            Player.STATE_READY -> binding.loadingSpinner.visibility = View.GONE
                            Player.STATE_ENDED -> binding.loadingSpinner.visibility = View.GONE
                            Player.STATE_IDLE -> binding.loadingSpinner.visibility = View.GONE
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        binding.loadingSpinner.visibility = View.GONE
                        Toast.makeText(this@MainActivity, "Playback Error: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                })
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun setupListeners() {
        binding.btnPickVideo.setOnClickListener {
            selectVideoLauncher.launch("video/*")
        }

        binding.btnStreamUrl.setOnClickListener {
            showStreamUrlDialog()
        }

        binding.btnSpeed.setOnClickListener {
            currentSpeedIndex = (currentSpeedIndex + 1) % speeds.size
            val selectedSpeed = speeds[currentSpeedIndex]
            player?.playbackParameters = PlaybackParameters(selectedSpeed)
            binding.btnSpeed.text = "${selectedSpeed}x"
        }

        binding.btnResize.setOnClickListener {
            currentResizeIndex = (currentResizeIndex + 1) % resizeModes.size
            val (mode, label) = resizeModes[currentResizeIndex]
            binding.playerView.resizeMode = mode
            binding.btnResize.text = label
        }

        binding.btnPip.setOnClickListener {
            enterPipMode()
        }

        // Demo sample video if nothing playing on first launch
        val defaultSample = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        playUri(Uri.parse(defaultSample))
    }

    private fun showStreamUrlDialog() {
        val input = EditText(this).apply {
            hint = "https://... (.mp4, .m3u8, etc.)"
        }
        AlertDialog.Builder(this)
            .setTitle("Play Stream URL")
            .setView(input)
            .setPositiveButton("Play") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) {
                    playUri(Uri.parse(url))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun playUri(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        player?.apply {
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            playUri(uri)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        } else {
            Toast.makeText(this, "Picture-in-Picture not supported on this device version", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            binding.topBar.visibility = View.GONE
            binding.playerView.useController = false
        } else {
            binding.topBar.visibility = View.VISIBLE
            binding.playerView.useController = true
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Auto PiP on home gesture if video is playing
        if (player?.isPlaying == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPipMode()
        }
    }

    override fun onStart() {
        super.onStart()
        initPlayer()
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}

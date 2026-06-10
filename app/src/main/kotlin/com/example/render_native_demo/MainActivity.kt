package com.example.render_native_demo

import android.app.Activity
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import com.example.render_native_demo.decode.AudioDecoder
import com.example.render_native_demo.decode.DecodeThread
import com.example.render_native_demo.render.RenderRenderer
import com.example.render_native_demo.runtime.Result
import com.example.render_native_demo.runtime.RuntimeController
import java.io.File
import java.io.FileOutputStream
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : Activity() {
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: RenderRenderer
    private lateinit var decodeThread: DecodeThread
    private lateinit var audioDecoder: AudioDecoder
    private lateinit var runtimeController: RuntimeController
    private var decoderSurface: Surface? = null
    private var pendingPrepare: String? = null
    private var isPlaying = false
    private var isPaused = false
    private var isReleased = false
    private var releaseStarted = false
    private var playbackEnded = false
    private var preparedFilePath: String? = null
    private var playbackPositionMs: Long = 0
    private var playbackProgressRunnable: Runnable? = null

    private lateinit var btnPlay: Button
    private lateinit var btnPause: Button
    private lateinit var btnSeekBack: Button
    private lateinit var btnSeekForward: Button

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        glSurfaceView = GLSurfaceView(this)
        glSurfaceView.setEGLContextClientVersion(2)

        decodeThread = DecodeThread()
        decodeThread.startThread()

        audioDecoder = AudioDecoder()

        runtimeController = RuntimeController()

        renderer = RenderRenderer()
        renderer.setSurfaceTextureListener { surfaceTexture ->
            Log.i(TAG, "===== SurfaceTexture available =====")
            decoderSurface = Surface(surfaceTexture)
            decodeThread.setSurface(decoderSurface!!)

            if (pendingPrepare != null) {
                Log.i(TAG, "Starting pending decoder: ${pendingPrepare!!}")
                prepareAndPlay(pendingPrepare!!)
                pendingPrepare = null
            }
        }

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                Log.i(TAG, "===== onSurfaceCreated =====")
                renderer.init()
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                Log.i(TAG, "onSurfaceChanged: $width x $height")
            }

            override fun onDrawFrame(gl: GL10?) {
                renderer.render()
            }
        })

        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        // 创建控制按钮
        btnPlay = Button(this).apply { text = "Play" }
        btnPause = Button(this).apply { text = "Pause" }
        btnSeekBack = Button(this).apply { text = "-10s" }
        btnSeekForward = Button(this).apply { text = "+10s" }

        val controlPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(btnPlay)
            addView(btnPause)
            addView(btnSeekBack)
            addView(btnSeekForward)
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(glSurfaceView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            ))
            addView(controlPanel)
        }

        setContentView(rootLayout)

        // 设置按钮点击事件
        btnPlay.setOnClickListener { onPlayClicked() }
        btnPause.setOnClickListener { onPauseClicked() }
        btnSeekBack.setOnClickListener { onSeekBackClicked() }
        btnSeekForward.setOnClickListener { onSeekForwardClicked() }

        glSurfaceView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                Log.i(TAG, "View attached to window")
            }

            override fun onViewDetachedFromWindow(v: View) {
                Log.i(TAG, "View detached from window")
                release()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()

        try {
            val videoFile = copyAssetToFile("test.mp4")
            if (videoFile != null) {
                if (preparedFilePath != null) {
                    Log.i(TAG, "Playback already prepared: $preparedFilePath")
                } else if (decoderSurface != null) {
                    Log.i(TAG, "Decoder surface ready, starting playback")
                    prepareAndPlay(videoFile.absolutePath)
                } else {
                    Log.i(TAG, "Decoder surface not ready yet, pending")
                    pendingPrepare = videoFile.absolutePath
                }
            } else {
                Log.e(TAG, "Failed to copy video file from assets")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing video", e)
        }
    }

    override fun onPause() {
        super.onPause()
        if (isPlaying && !isPaused && !releaseStarted) {
            pausePlayback()
        }
        glSurfaceView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        release()
    }

    private fun prepareAndPlay(filePath: String) {
        Log.i(TAG, "prepareAndPlay: $filePath")
        if (isReleased || releaseStarted) {
            Log.w(TAG, "prepareAndPlay ignored: activity is releasing/released")
            return
        }
        if (preparedFilePath != null) {
            Log.i(TAG, "prepareAndPlay ignored: already prepared with $preparedFilePath")
            return
        }

        val prepareResult = runtimeController.prepare(filePath)
        if (!prepareResult.success) {
            Log.e(TAG, "Runtime prepare failed: ${prepareResult.errorCode} - ${prepareResult.errorMessage}")
            return
        }

        startDecoder(filePath)

        val playResult = runtimeController.play()
        if (!playResult.success) {
            Log.e(TAG, "Runtime play failed: ${playResult.errorCode}")
            return
        }
        preparedFilePath = filePath
        isPlaying = true
        isPaused = false
        playbackEnded = false
        startPlaybackProgressUpdater()
    }

    private fun startDecoder(filePath: String) {
        Log.i(TAG, "Starting decoder: $filePath")
        decodeThread.prepare(filePath)

        Log.i(TAG, "Starting audio decoder: $filePath")
        audioDecoder.prepare(filePath)
        audioDecoder.start()
    }

    private fun onPlayClicked() {
        Log.i(TAG, "===== Play clicked =====")
        if (playbackEnded || decodeThread.isEOS()) {
            restartPlayback()
        } else if (isPaused) {
            resumePlayback()
        } else if (!isPlaying) {
            startPlayback()
        }
    }

    private fun onPauseClicked() {
        Log.i(TAG, "===== Pause clicked =====")
        pausePlayback()
    }

    private fun onSeekBackClicked() {
        Log.i(TAG, "===== Seek back clicked =====")
        val targetMs = maxOf(0L, playbackPositionMs - 10000)
        seekTo(targetMs)
    }

    private fun onSeekForwardClicked() {
        Log.i(TAG, "===== Seek forward clicked =====")
        val durationMs = decodeThread.getDuration() / 1000
        val targetMs = minOf(durationMs, playbackPositionMs + 10000)
        seekTo(targetMs)
    }

    private fun startPlayback() {
        Log.i(TAG, "===== Starting playback =====")
        if (isReleased || releaseStarted) return
        val playResult = runtimeController.play()
        if (!playResult.success) {
            Log.e(TAG, "Runtime play failed: ${playResult.errorCode}")
            return
        }
        decodeThread.resumeDecoder()
        audioDecoder.resume()

        isPlaying = true
        isPaused = false
        playbackEnded = false
        startPlaybackProgressUpdater()
    }

    private fun pausePlayback() {
        Log.i(TAG, "===== Pausing playback =====")
        if (isReleased || releaseStarted || isPaused) return
        isPaused = true

        runtimeController.pause()
        decodeThread.pauseDecoder()
        audioDecoder.pause()

        stopPlaybackProgressUpdater()
    }

    private fun resumePlayback() {
        Log.i(TAG, "===== Resuming playback =====")
        if (isReleased || releaseStarted) return
        val playResult = runtimeController.play()
        if (!playResult.success) {
            Log.e(TAG, "Runtime play failed: ${playResult.errorCode}")
            return
        }
        decodeThread.resumeDecoder()
        audioDecoder.resume()

        isPlaying = true
        isPaused = false
        playbackEnded = false
        startPlaybackProgressUpdater()
    }

    private fun seekTo(positionMs: Long) {
        Log.i(TAG, "===== Seeking to $positionMs ms =====")
        if (isReleased || releaseStarted || preparedFilePath == null) {
            Log.w(TAG, "seek ignored: not prepared or releasing")
            return
        }
        val positionUs = positionMs * 1000

        val seekResult = runtimeController.seek(positionUs)
        if (!seekResult.success) {
            Log.e(TAG, "Runtime seek failed: ${seekResult.errorCode}")
            return
        }
        decodeThread.seekTo(positionUs)
        audioDecoder.seekTo(positionUs)

        playbackPositionMs = positionMs
        playbackEnded = false
    }

    private fun restartPlayback() {
        Log.i(TAG, "===== Restarting playback =====")
        if (isReleased || releaseStarted) return
        playbackPositionMs = 0

        runtimeController.seek(0)

        decodeThread.seekTo(0)
        audioDecoder.seekTo(0)

        isPlaying = true
        isPaused = false
        playbackEnded = false

        startPlaybackProgressUpdater()
    }

    private fun startPlaybackProgressUpdater() {
        stopPlaybackProgressUpdater()
        val runnable = object : Runnable {
            override fun run() {
                if (!isPlaying || isPaused) return

                playbackPositionMs = decodeThread.getCurrentPosition() / 1000

                if (decodeThread.isEOS()) {
                    handlePlaybackEnded()
                    return
                }

                handler.postDelayed(this, 500)
            }
        }
        playbackProgressRunnable = runnable
        handler.post(runnable)
    }

    private fun stopPlaybackProgressUpdater() {
        playbackProgressRunnable?.let { handler.removeCallbacks(it) }
        playbackProgressRunnable = null
    }

    private fun release() {
        if (releaseStarted) {
            Log.d(TAG, "release ignored: already started")
            return
        }
        releaseStarted = true
        isReleased = true
        isPlaying = false
        isPaused = false
        playbackEnded = false
        Log.i(TAG, "===== RELEASE 8-STEP ORDER =====")

        // Step 1: Stop clock (stop playback progress updater)
        Log.i(TAG, "Step 1/8: Stop clock")
        stopPlaybackProgressUpdater()

        // Step 2: Stop render submit
        Log.i(TAG, "Step 2/8: Stop render submit")
        glSurfaceView.queueEvent {
            renderer.release()
        }

        // Step 3: Flush decoder
        Log.i(TAG, "Step 3/8: Flush decoder")
        runtimeController.stop()
        decodeThread.stopDecoder()
        audioDecoder.pause()

        // Step 4: Clear frame queue
        Log.i(TAG, "Step 4/8: Clear frame queue")
        // Already handled by pause

        // Step 5: Detach SurfaceTexture
        Log.i(TAG, "Step 5/8: Detach SurfaceTexture")
        decoderSurface?.release()
        decoderSurface = null

        // Step 6: Destroy GL resource
        Log.i(TAG, "Step 6/8: Destroy GL resource")
        // GL resources are destroyed on the GLSurfaceView render thread above.

        // Step 7: Destroy EGL (handled by GLSurfaceView)
        Log.i(TAG, "Step 7/8: Destroy EGL")
        glSurfaceView.onPause()

        // Step 8: Stop threads
        Log.i(TAG, "Step 8/8: Stop threads")
        decodeThread.releaseThread()
        audioDecoder.release()
        runtimeController.release()
        preparedFilePath = null

        Log.i(TAG, "===== Release completed =====")
    }

    private fun copyAssetToFile(assetName: String): File? {
        return try {
            val file = File(cacheDir, assetName)
            if (!file.exists()) {
                assets.open(assetName).use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            Log.i(TAG, "Copied asset to: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error copying asset file", e)
            null
        }
    }

    private fun handlePlaybackEnded() {
        if (playbackEnded) return

        Log.i(TAG, "===== Playback ended, entering EOS hold =====")
        playbackEnded = true
        isPlaying = false
        isPaused = false
        playbackPositionMs = decodeThread.getDuration() / 1000
        runtimeController.markEndOfStream()
    }
}

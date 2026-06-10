package com.example.render_native_demo.render

import android.graphics.SurfaceTexture
import android.opengl.*
import android.util.Log
import com.example.render_native_demo.runtime.FrameStats
import java.util.concurrent.atomic.AtomicInteger

class RenderRenderer {
    companion object {
        private const val TAG = "RenderRenderer"
    }

    private var oesTextureId = 0
    private var surfaceTexture: SurfaceTexture? = null
    private val renderGraph = RenderGraph()
    private val pendingFrames = AtomicInteger(0)
    private var frameCount = 0L
    private val frameStats = FrameStats()
    @Volatile
    private var released = false

    fun interface SurfaceTextureListener {
        fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture)
    }

    private var surfaceTextureListener: SurfaceTextureListener? = null

    fun setSurfaceTextureListener(listener: SurfaceTextureListener) {
        surfaceTextureListener = listener
    }

    fun init() {
        Log.i(TAG, "===== Initializing RenderRenderer =====")
        released = false
        initGLResources()
        renderGraph.configure()
    }

    fun render() {
        var framesToConsume = pendingFrames.getAndSet(0)
        while (framesToConsume > 0) {
            try {
                surfaceTexture?.updateTexImage()
                frameCount++
                Log.i(TAG, "===== renderFrame: frameAvailable is true, frameCount=$frameCount =====")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating texture image", e)
                pendingFrames.set(0)
                break
            }
            framesToConsume--
        }

        renderGraph.render(oesTextureId)
        frameStats.recordFrame()
    }

    fun release() {
        if (released) {
            Log.d(TAG, "release ignored: already released")
            return
        }
        released = true
        Log.i(TAG, "===== Releasing RenderRenderer =====")
        surfaceTexture?.release()
        surfaceTexture = null
        renderGraph.release()
        if (oesTextureId != 0) {
            val textures = intArrayOf(oesTextureId)
            GLES20.glDeleteTextures(1, textures, 0)
            oesTextureId = 0
        }
    }

    private fun initGLResources() {
        Log.i(TAG, "===== Initializing GL resources =====")

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        oesTextureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        Log.i(TAG, "Created OES texture: $oesTextureId")

        try {
            surfaceTexture = SurfaceTexture(oesTextureId)
            surfaceTexture?.setDefaultBufferSize(1920, 1080)
            surfaceTexture?.setOnFrameAvailableListener {
                val count = pendingFrames.incrementAndGet()
                Log.i(TAG, "===== Frame available callback, pending=$count =====")
            }

            if (surfaceTexture == null) {
                Log.e(TAG, "Failed to create SurfaceTexture!")
            } else {
                Log.i(TAG, "SurfaceTexture created successfully")
                surfaceTextureListener?.onSurfaceTextureAvailable(surfaceTexture!!)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating SurfaceTexture", e)
        }

    }

    fun getSurfaceTexture(): SurfaceTexture? {
        return surfaceTexture
    }
}

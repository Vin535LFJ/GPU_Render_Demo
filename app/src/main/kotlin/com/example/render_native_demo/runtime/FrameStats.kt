package com.example.render_native_demo.runtime

import android.util.Log

class FrameStats {
    companion object {
        private const val TAG = "FrameStats"
    }

    private var frameCount = 0L
    private var lastFrameTimeNs = 0L
    private var frameTimesMs = mutableListOf<Double>()

    fun recordFrame() {
        val currentTimeNs = System.nanoTime()
        if (lastFrameTimeNs != 0L) {
            val deltaMs = (currentTimeNs - lastFrameTimeNs) / 1_000_000.0
            frameTimesMs.add(deltaMs)
            if (frameTimesMs.size > 100) {
                frameTimesMs.removeAt(0)
            }
        }
        lastFrameTimeNs = currentTimeNs
        frameCount++

        if (frameCount % 60 == 0L) {
            logStats()
        }
    }

    private fun logStats() {
        if (frameTimesMs.isEmpty()) return
        val avg = frameTimesMs.average()
        val fps = 1000.0 / avg
        Log.d(TAG, "Frames: $frameCount, Avg frame time: ${"%.2f".format(avg)}ms, FPS: ${"%.2f".format(fps)}")
    }

    fun reset() {
        frameCount = 0L
        lastFrameTimeNs = 0L
        frameTimesMs.clear()
    }
}

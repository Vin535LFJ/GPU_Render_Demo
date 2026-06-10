package com.example.render_native_demo.decode

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import android.view.Surface

class DecodeThread : HandlerThread("DecodeThread") {
    companion object {
        private const val TAG = "DecodeThread"
        private const val TIMEOUT_US = 10000L
        private const val UNSET_TIME_US = Long.MIN_VALUE
        private const val MAX_SLEEP_MS = 20L
    }

    private var handler: Handler? = null
    private var mediaExtractor: MediaExtractor? = null
    private var mediaCodec: MediaCodec? = null
    @Volatile
    private var isRunning = false
    @Volatile
    private var isPaused = false
    private var surface: Surface? = null
    private var trackIndex = -1
    @Volatile
    private var inputDone = false
    @Volatile
    private var outputDone = false
    private var videoDurationUs: Long = 0
    private var shouldLoop = false // 临时禁用循环播放
    private var filePath: String? = null // 保存文件路径用于循环
    @Volatile
    private var lastRenderedPtsUs: Long = 0L
    private var firstVideoPtsUs: Long = UNSET_TIME_US
    private var playbackStartElapsedUs: Long = UNSET_TIME_US
    private var pauseStartedElapsedUs: Long = UNSET_TIME_US
    private var decodeLoopScheduled = false
    private var decodeLoopGeneration = 0

    fun startThread() {
        Log.d(TAG, "Starting DecodeThread")
        start()
        handler = Handler(looper)
        isRunning = true
    }

    fun setSurface(surface: Surface) {
        Log.d(TAG, "setSurface: $surface, isValid: ${surface.isValid}")
        this.surface = surface
    }

    fun prepare(filePath: String) {
        this.filePath = filePath // 保存文件路径
        Log.i(TAG, "===== prepare: $filePath =====")
        handler?.post {
            try {
                internalPrepare(filePath)
            } catch (e: Exception) {
                Log.e(TAG, "Error preparing decoder", e)
            }
        }
    }

    private fun internalPrepare(filePath: String) {
        releaseDecoder(clearSurface = false)
        resetDecodeState()

        Log.i(TAG, "Creating MediaExtractor")
        mediaExtractor = MediaExtractor()
        mediaExtractor?.setDataSource(filePath)
        Log.i(TAG, "MediaExtractor created successfully")

        trackIndex = -1
        Log.i(TAG, "Looking for video track...")
        for (i in 0 until mediaExtractor!!.trackCount) {
            val format = mediaExtractor!!.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            Log.d(TAG, "Track $i: $mime")
            if (mime?.startsWith("video/") == true) {
                trackIndex = i
                Log.i(TAG, "Found video track at index $trackIndex")
                break
            }
        }

        if (trackIndex < 0) {
            Log.e(TAG, "No video track found!")
            return
        }

        mediaExtractor?.selectTrack(trackIndex)
        val format = mediaExtractor!!.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)
        
        if (format.containsKey(MediaFormat.KEY_DURATION)) {
            videoDurationUs = format.getLong(MediaFormat.KEY_DURATION)
            Log.i(TAG, "Video duration: ${videoDurationUs / 1000} ms")
        }
        
        Log.i(TAG, "Video format: $mime, size: ${format.getInteger(MediaFormat.KEY_WIDTH)}x${format.getInteger(MediaFormat.KEY_HEIGHT)}")

        Log.i(TAG, "Creating MediaCodec for $mime")
        mediaCodec = MediaCodec.createDecoderByType(mime!!)
        Log.i(TAG, "MediaCodec created")
        
        val width = format.getInteger(MediaFormat.KEY_WIDTH)
        val height = format.getInteger(MediaFormat.KEY_HEIGHT)
        val currentSurface = surface
        Log.i(TAG, "Configuring codec with surface: $currentSurface, width: $width, height: $height")
        
        if (currentSurface == null || !currentSurface.isValid) {
            Log.e(TAG, "Surface is null or not valid!")
            return
        }
        
        try {
            mediaCodec?.configure(format, currentSurface, null, 0)
            Log.i(TAG, "Codec configured successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure codec: ${e.message}", e)
            mediaCodec?.release()
            mediaCodec = null
            return
        }
        
        try {
            mediaCodec?.start()
            Log.i(TAG, "Codec started successfully, starting decoding loop")
            startDecoding()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start codec: ${e.message}", e)
            mediaCodec?.stop()
            mediaCodec?.release()
            mediaCodec = null
        }
    }

    private fun startDecoding(force: Boolean = false) {
        if (decodeLoopScheduled && !force) {
            Log.d(TAG, "startDecoding ignored: decode loop already scheduled")
            return
        }
        if (force) {
            decodeLoopScheduled = false
        }
        decodeLoopScheduled = true
        val generation = ++decodeLoopGeneration
        Log.i(TAG, "===== startDecoding started =====")
        handler?.post(object : Runnable {
            private var iteration = 0

            override fun run() {
                if (generation != decodeLoopGeneration) {
                    Log.d(TAG, "startDecoding: stale generation, exiting")
                    return
                }
                iteration++
                Log.d(TAG, "startDecoding iteration $iteration: isRunning=$isRunning, isPaused=$isPaused, outputDone=$outputDone")
                
                if (!isRunning) {
                    Log.i(TAG, "startDecoding: thread stopped, exiting")
                    decodeLoopScheduled = false
                    return
                }
                
                if (isPaused) {
                    Log.d(TAG, "startDecoding: paused, rescheduling")
                    handler?.postDelayed(this, 50)
                    return
                }
                
                decodeLoop()
                
                // 检查是否需要循环播放
                if (isRunning && outputDone && shouldLoop) {
                    Log.i(TAG, "===== Looping video - resetting and seeking to start =====")
                    resetAndSeekToStart()
                }
                
                // 继续调度下一轮解码，除非明确停止
                if (isRunning && !outputDone) {
                    handler?.post(this)
                } else if (outputDone) {
                    Log.i(TAG, "startDecoding: output done, stopping decode loop after $iteration iterations")
                    decodeLoopScheduled = false
                } else {
                    Log.d(TAG, "startDecoding: isRunning=$isRunning, outputDone=$outputDone, will retry")
                    handler?.postDelayed(this, 10)
                }
            }
        })
    }

    private fun resetAndSeekToStart() {
        try {
            Log.i(TAG, "===== Resetting decoder for loop =====")
            
            // 重置状态
            resetDecodeState()
            
            // 先重置 MediaExtractor 到开头
            mediaExtractor?.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            
            // 刷新 MediaCodec，清除缓冲区
            mediaCodec?.flush()
            
            Log.i(TAG, "Decoder reset for loop complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting decoder for loop", e)
        }
    }

    private fun decodeLoop() {
        if (mediaCodec == null) {
            Log.w(TAG, "decodeLoop: mediaCodec is null")
            return
        }
        if (isPaused) {
            Log.d(TAG, "decodeLoop: paused, skipping")
            return
        }

        // Feed input
        if (!inputDone) {
            try {
                val inputBufferIndex = mediaCodec!!.dequeueInputBuffer(TIMEOUT_US)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = mediaCodec!!.getInputBuffer(inputBufferIndex)
                    if (inputBuffer != null) {
                        val sampleSize = mediaExtractor!!.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            Log.i(TAG, "===== End of input stream (EOS) =====")
                            mediaCodec!!.queueInputBuffer(
                                inputBufferIndex, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            val pts = mediaExtractor!!.sampleTime
                            mediaCodec!!.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                sampleSize,
                                pts,
                                0
                            )
                            mediaExtractor!!.advance()
                        }
                    } else {
                        Log.w(TAG, "Input buffer is null")
                    }
                } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Log.d(TAG, "No input buffer available")
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "IllegalStateException in dequeueInputBuffer", e)
                handleCodecError()
                return
            } catch (e: Exception) {
                Log.e(TAG, "Error in input processing", e)
                handleCodecError()
                return
            }
        }

        // Drain output
        if (!outputDone) {
            try {
                val bufferInfo = MediaCodec.BufferInfo()
                val outputBufferIndex = mediaCodec!!.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when (outputBufferIndex) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // No output ready yet
                    }
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val newFormat = mediaCodec!!.outputFormat
                        Log.d(TAG, "Output format changed: $newFormat")
                    }
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                        Log.d(TAG, "Output buffers changed")
                    }
                    else -> {
                        if (outputBufferIndex >= 0) {
                            val isEos = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            val shouldRender = bufferInfo.size > 0
                            if (shouldRender) {
                                waitUntilPresentationTime(bufferInfo.presentationTimeUs)
                                lastRenderedPtsUs = bufferInfo.presentationTimeUs
                            }

                            mediaCodec!!.releaseOutputBuffer(outputBufferIndex, shouldRender)

                            if (isEos) {
                                Log.d(TAG, "End of output stream (EOS)")
                                outputDone = true
                            }
                        }
                    }
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "IllegalStateException in dequeueOutputBuffer", e)
                handleCodecError()
                return
            } catch (e: Exception) {
                Log.e(TAG, "Error in output processing", e)
                handleCodecError()
                return
            }
        }
    }

    private fun handleCodecError() {
        Log.e(TAG, "Handling codec error, resetting decoder")
        outputDone = true
        // 不在这里处理，让外层循环处理循环播放逻辑
    }

    fun pauseDecoder() {
        Log.d(TAG, "pauseDecoder called")
        if (!isPaused) {
            pauseStartedElapsedUs = nowUs()
        }
        isPaused = true
    }

    fun resumeDecoder() {
        Log.d(TAG, "resumeDecoder called")
        if (isPaused && pauseStartedElapsedUs != UNSET_TIME_US && playbackStartElapsedUs != UNSET_TIME_US) {
            playbackStartElapsedUs += nowUs() - pauseStartedElapsedUs
        }
        pauseStartedElapsedUs = UNSET_TIME_US
        isPaused = false
        if (!isRunning) return

        handler?.post {
            if (!outputDone) {
                startDecoding(force = true)
            }
        }
    }

    fun seekTo(positionUs: Long) {
        Log.d(TAG, "seekTo: $positionUs us")
        inputDone = false
        outputDone = false
        lastRenderedPtsUs = positionUs
        handler?.post {
            try {
                resetDecodeState()
                lastRenderedPtsUs = positionUs

                mediaCodec?.flush()
                
                mediaExtractor?.seekTo(positionUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                
                Log.d(TAG, "Seek completed to position: ${positionUs / 1000} ms")
                
                if (!isRunning) return@post
                
                startDecoding()
            } catch (e: Exception) {
                Log.e(TAG, "Error during seek", e)
            }
        }
    }

    fun getCurrentPosition(): Long {
        return lastRenderedPtsUs.takeIf { it >= 0L } ?: (mediaExtractor?.sampleTime ?: 0L)
    }

    fun getDuration(): Long {
        return videoDurationUs
    }

    fun isPausedState(): Boolean = isPaused

    fun isEOS(): Boolean = outputDone

    fun stopDecoder() {
        Log.d(TAG, "stopDecoder")
        isPaused = true
        inputDone = true
        outputDone = true
        handler?.post {
            try {
                mediaCodec?.flush()
            } catch (e: Exception) {
                Log.w(TAG, "Ignoring codec flush failure during stop", e)
            }
            resetDecodeState()
            isPaused = true
        }
    }

    fun releaseThread() {
        Log.d(TAG, "releaseThread")
        isRunning = false
        handler?.post {
            decodeLoopScheduled = false
            releaseDecoder()
            quitSafely()
        }
    }

    private fun releaseDecoder() {
        releaseDecoder(clearSurface = true)
    }

    private fun releaseDecoder(clearSurface: Boolean) {
        Log.d(TAG, "Releasing decoder")
        try {
            mediaCodec?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Ignoring codec stop failure during release", e)
        }
        try {
            mediaCodec?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Ignoring codec release failure", e)
        }
        mediaCodec = null
        try {
            mediaExtractor?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Ignoring extractor release failure", e)
        }
        mediaExtractor = null
        if (clearSurface) {
            surface = null
        }
    }

    private fun waitUntilPresentationTime(presentationTimeUs: Long) {
        if (presentationTimeUs < 0) return

        if (firstVideoPtsUs == UNSET_TIME_US) {
            firstVideoPtsUs = presentationTimeUs
            playbackStartElapsedUs = nowUs()
            lastRenderedPtsUs = presentationTimeUs
            Log.i(TAG, "Playback clock anchored: firstPts=${presentationTimeUs / 1000} ms")
            return
        }

        val dueUs = playbackStartElapsedUs + (presentationTimeUs - firstVideoPtsUs)
        while (isRunning && !isPaused) {
            val delayUs = dueUs - nowUs()
            if (delayUs <= 0L) return
            Thread.sleep(minOf(MAX_SLEEP_MS, maxOf(1L, delayUs / 1000L)))
        }
    }

    private fun resetDecodeState() {
        inputDone = false
        outputDone = false
        firstVideoPtsUs = UNSET_TIME_US
        playbackStartElapsedUs = UNSET_TIME_US
        pauseStartedElapsedUs = UNSET_TIME_US
        lastRenderedPtsUs = 0L
    }

    private fun nowUs(): Long {
        return SystemClock.elapsedRealtimeNanos() / 1000L
    }
}

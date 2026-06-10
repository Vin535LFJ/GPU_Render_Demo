package com.example.render_native_demo.decode

import android.media.*
import android.util.Log

class AudioDecoder {
    companion object {
        private const val TAG = "AudioDecoder"
    }

    private var mediaExtractor: MediaExtractor? = null
    private var mediaCodec: MediaCodec? = null
    private var audioTrack: AudioTrack? = null
    @Volatile
    private var isRunning = false
    @Volatile
    private var isPaused = false
    private var audioThread: Thread? = null
    private var codecStarted = false
    private var audioTrackInitialized = false
    @Volatile
    private var inputDone = false
    @Volatile
    private var outputDone = false
    private var shouldLoop = false // 临时禁用循环播放
    private var audioTrackIndex = -1 // 保存音频轨道索引
    private var filePath: String? = null // 保存文件路径用于循环

    fun prepare(filePath: String) {
        this.filePath = filePath
        internalPrepare(filePath)
    }

    private fun internalPrepare(filePath: String) {
        try {
            mediaExtractor = MediaExtractor().apply {
                setDataSource(filePath)
            }

            this.audioTrackIndex = findAudioTrack()
            if (this.audioTrackIndex < 0) {
                Log.e(TAG, "No audio track found")
                return
            }

            mediaExtractor?.selectTrack(this.audioTrackIndex)
            val format = mediaExtractor?.getTrackFormat(audioTrackIndex)

            val mime = format?.getString(MediaFormat.KEY_MIME)
            if (mime.isNullOrEmpty()) {
                Log.e(TAG, "Invalid MIME type")
                return
            }
            
            mediaCodec = MediaCodec.createDecoderByType(mime)
            mediaCodec?.configure(format, null, null, 0)

            Log.i(TAG, "Audio format: $format")
            Log.i(TAG, "Audio decoder prepared successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error preparing audio decoder", e)
        }
    }

    fun start() {
        if (audioThread?.isAlive == true) {
            Log.d(TAG, "start ignored: audio thread already running")
            return
        }
        isRunning = true
        audioThread = Thread {
            try {
                if (!codecStarted) {
                    mediaCodec?.start()
                    codecStarted = true
                    Log.i(TAG, "Audio codec started")
                } else {
                    Log.i(TAG, "Audio codec already started, resuming decode loop")
                }

                val info = MediaCodec.BufferInfo()

                while (isRunning) {
                    if (isPaused) {
                        Thread.sleep(50)
                        continue
                    }

                    // 如果到达 EOS 并且需要循环，重置状态
                    if (outputDone && shouldLoop) {
                        Log.i(TAG, "===== Looping audio - resetting and seeking to start =====")
                        resetAndSeekToStart()
                    }

                    if (outputDone) {
                        break
                    }

                    // Feed input
                    if (!inputDone) {
                        try {
                            val inputBufferIndex = mediaCodec?.dequeueInputBuffer(10000)
                            if (inputBufferIndex != null && inputBufferIndex >= 0) {
                                val inputBuffer = mediaCodec?.getInputBuffer(inputBufferIndex)
                                if (inputBuffer != null) {
                                    val sampleSize = mediaExtractor?.readSampleData(inputBuffer, 0)

                                    if (sampleSize != null && sampleSize < 0) {
                                        Log.i(TAG, "Audio end of input stream")
                                        mediaCodec?.queueInputBuffer(inputBufferIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                        inputDone = true
                                    } else if (sampleSize != null && sampleSize > 0) {
                                        val presentationTimeUs = mediaExtractor?.sampleTime ?: 0
                                        mediaCodec?.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, 0)
                                        mediaExtractor?.advance()
                                    }
                                }
                            }
                        } catch (e: IllegalStateException) {
                            Log.e(TAG, "IllegalStateException in audio dequeueInputBuffer", e)
                            outputDone = true
                            continue
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in audio input processing", e)
                            outputDone = true
                            continue
                        }
                    }

                    // Drain output
                    if (!outputDone) {
                        try {
                            val outputBufferIndex = mediaCodec?.dequeueOutputBuffer(info, 10000)
                            when {
                                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                                    // No output available yet, will retry
                                }
                                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                    Log.i(TAG, "Audio output format changed: ${mediaCodec?.outputFormat}")
                                }
                                outputBufferIndex != null && outputBufferIndex >= 0 -> {
                                    try {
                                        if (!audioTrackInitialized) {
                                            initializeAudioTrack(mediaCodec?.outputFormat)
                                        }

                                        if (audioTrack != null && info.size > 0 && !isPaused) {
                                            val outputBuffer = mediaCodec?.getOutputBuffer(outputBufferIndex)
                                            if (outputBuffer != null) {
                                                val data = ByteArray(info.size)
                                                outputBuffer.position(info.offset)
                                                outputBuffer.get(data)
                                                outputBuffer.position(0)
                                                val written = audioTrack?.write(data, 0, data.size)
                                                Log.d(TAG, "Audio write: size=${info.size}, written=$written")
                                            }
                                        } else if (info.size == 0) {
                                            Log.d(TAG, "Audio buffer size is 0, skipping write")
                                        }

                                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                            Log.i(TAG, "Audio end of output stream (EOS)")
                                            outputDone = true
                                        }
                                        
                                        try {
                                            mediaCodec?.releaseOutputBuffer(outputBufferIndex, false)
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Failed to release audio output buffer", e)
                                            outputDone = true
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error processing audio buffer", e)
                                    }
                                }
                            }
                        } catch (e: IllegalStateException) {
                            Log.e(TAG, "IllegalStateException in audio dequeueOutputBuffer", e)
                            outputDone = true
                            continue
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in audio output processing", e)
                            outputDone = true
                            continue
                        }
                    }
                }
                
                Log.i(TAG, "Audio decoding loop finished: inputDone=$inputDone, outputDone=$outputDone")
                isRunning = false

            } catch (e: Exception) {
                Log.e(TAG, "Error in audio decoding loop", e)
                isRunning = false
            }
        }
        
        audioThread?.start()
    }

    private fun resetAndSeekToStart() {
        try {
            Log.i(TAG, "===== Resetting audio decoder for loop =====")
            
            // 重置状态
            inputDone = false
            outputDone = false
            
            // 先重置 MediaExtractor 到开头
            mediaExtractor?.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            
            // 刷新 MediaCodec，清除缓冲区
            mediaCodec?.flush()
            
            Log.i(TAG, "Audio decoder reset for loop complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting audio decoder for loop", e)
        }
    }

    private fun findAudioTrack(): Int {
        for (i in 0 until (mediaExtractor?.trackCount ?: 0)) {
            val format = mediaExtractor?.getTrackFormat(i)
            val mime = format?.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                return i
            }
        }
        return -1
    }

    private fun initializeAudioTrack(format: MediaFormat?) {
        try {
            val sampleRate = format?.getInteger(MediaFormat.KEY_SAMPLE_RATE) ?: 44100
            val channels = format?.getInteger(MediaFormat.KEY_CHANNEL_COUNT) ?: 2
            val channelConfig = if (channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

            audioTrack = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .setEncoding(audioFormat)
                    .build(),
                bufferSize * 4,
                AudioTrack.MODE_STREAM,
                android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            audioTrack?.play()
            audioTrackInitialized = true
            Log.i(TAG, "AudioTrack initialized: $sampleRate Hz, $channels channels")

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AudioTrack", e)
        }
    }

    fun pause() {
        Log.i(TAG, "pause called")
        isPaused = true
        audioTrack?.pause()
    }

    fun resume() {
        Log.i(TAG, "resume called")
        isPaused = false
        audioTrack?.play()
        if (!outputDone) {
            start()
        }
    }

    fun seekTo(positionUs: Long) {
        Log.i(TAG, "seekTo: $positionUs us")
        try {
            inputDone = false
            outputDone = false
            mediaCodec?.flush()
            mediaExtractor?.seekTo(positionUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            if (audioTrackInitialized) {
                audioTrack?.pause()
                audioTrack?.flush()
                if (!isPaused) {
                    audioTrack?.play()
                }
            }
            Log.i(TAG, "Audio seek completed")
            start()
        } catch (e: Exception) {
            Log.e(TAG, "Error during audio seek", e)
        }
    }

    fun isPausedState(): Boolean = isPaused

    fun isEOS(): Boolean = outputDone

    fun release() {
        isRunning = false
        audioThread?.join()

        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null

        try {
            mediaCodec?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Ignoring audio codec stop failure during release", e)
        }
        codecStarted = false
        mediaCodec?.release()
        mediaCodec = null

        mediaExtractor?.release()
        mediaExtractor = null
    }
}

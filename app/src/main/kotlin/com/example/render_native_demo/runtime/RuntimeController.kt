package com.example.render_native_demo.runtime

import android.util.Log
import java.util.concurrent.locks.ReentrantLock

class RuntimeController {
    companion object {
        private const val TAG = "RuntimeController"
    }

    private var state = RuntimeState.IDLE
    private val lock = ReentrantLock()

    fun prepare(source: String): Result<Unit> {
        Log.i(TAG, "[prepare] called with: $source")
        lock.lock()
        try {
            if (state == RuntimeState.IDLE) {
                state = RuntimeState.PREPARED
                Log.i(TAG, "[prepare] State transition: ${RuntimeState.IDLE} -> ${RuntimeState.PREPARED}")
                return Result.success()
            } else {
                Log.e(TAG, "[prepare] Invalid state: $state")
                return Result.failure(ErrorCode.INVALID_STATE)
            }
        } finally {
            lock.unlock()
        }
    }

    fun play(): Result<Unit> {
        Log.i(TAG, "[play] called")
        lock.lock()
        try {
            return when (state) {
                RuntimeState.PREPARED,
                RuntimeState.PAUSED,
                RuntimeState.SEEK_RECOVERING,
                RuntimeState.EOS_HOLD -> {
                    transitionTo("play", RuntimeState.PLAYING)
                    Result.success()
                }
                RuntimeState.RELEASED -> {
                    Log.e(TAG, "[play] Engine already released")
                    Result.failure(ErrorCode.ALREADY_RELEASED)
                }
                else -> {
                    Log.e(TAG, "[play] Invalid state: $state")
                    Result.failure(ErrorCode.INVALID_STATE)
                }
            }
        } finally {
            lock.unlock()
        }
    }

    fun pause(): Result<Unit> {
        Log.i(TAG, "[pause] called")
        lock.lock()
        try {
            if (state == RuntimeState.PLAYING) {
                state = RuntimeState.PAUSED
                Log.i(TAG, "[pause] State transition: ${RuntimeState.PLAYING} -> ${RuntimeState.PAUSED}")
                return Result.success()
            } else {
                Log.e(TAG, "[pause] Invalid state: $state")
                return Result.failure(ErrorCode.INVALID_STATE)
            }
        } finally {
            lock.unlock()
        }
    }

    fun seek(targetUs: Long): Result<Unit> {
        Log.i(TAG, "[seek] called with: $targetUs")
        lock.lock()
        try {
            return when (state) {
                RuntimeState.PLAYING,
                RuntimeState.PAUSED,
                RuntimeState.EOS_HOLD -> {
                    val resumeState = if (state == RuntimeState.PAUSED) RuntimeState.PAUSED else RuntimeState.PLAYING
                    transitionTo("seek", RuntimeState.SEEK_REQUESTED)
                    transitionTo("seek", RuntimeState.SEEK_FLUSHING)
                    transitionTo("seek", RuntimeState.SEEK_PRIMING)
                    transitionTo("seek", RuntimeState.SEEK_RECOVERING)
                    transitionTo("seek", resumeState)
                    Log.i(TAG, "[seek] Seek to: ${targetUs / 1000} ms")
                    Result.success()
                }
                RuntimeState.RELEASED -> {
                    Log.e(TAG, "[seek] Engine already released")
                    Result.failure(ErrorCode.ALREADY_RELEASED)
                }
                else -> {
                    Log.e(TAG, "[seek] Invalid state: $state")
                    Result.failure(ErrorCode.INVALID_STATE)
                }
            }
        } finally {
            lock.unlock()
        }
    }

    fun stop(): Result<Unit> {
        Log.i(TAG, "[stop] called")
        lock.lock()
        try {
            return when (state) {
                RuntimeState.RELEASED -> {
                    Log.e(TAG, "[stop] Engine already released")
                    Result.failure(ErrorCode.ALREADY_RELEASED)
                }
                RuntimeState.STOPPED -> Result.success()
                else -> {
                    transitionTo("stop", RuntimeState.STOPPED)
                    Result.success()
                }
            }
        } finally {
            lock.unlock()
        }
    }

    fun release(): Result<Unit> {
        Log.i(TAG, "[release] called")
        lock.lock()
        try {
            return when (state) {
                RuntimeState.RELEASED -> {
                    Log.e(TAG, "[release] Engine already released")
                    Result.failure(ErrorCode.ALREADY_RELEASED)
                }
                RuntimeState.STOPPED -> {
                    transitionTo("release", RuntimeState.RELEASED)
                    Result.success()
                }
                else -> {
                    Log.e(TAG, "[release] Invalid state: $state, stop must run first")
                    Result.failure(ErrorCode.INVALID_STATE)
                }
            }
        } finally {
            lock.unlock()
        }
    }

    fun getState(): RuntimeState {
        lock.lock()
        try {
            return state
        } finally {
            lock.unlock()
        }
    }

    fun markEndOfStream(): Result<Unit> {
        lock.lock()
        try {
            return if (state == RuntimeState.PLAYING) {
                transitionTo("eos", RuntimeState.EOS_HOLD)
                Result.success()
            } else {
                Result.failure(ErrorCode.INVALID_STATE)
            }
        } finally {
            lock.unlock()
        }
    }

    private fun transitionTo(action: String, nextState: RuntimeState) {
        val previous = state
        state = nextState
        Log.i(TAG, "[$action] State transition: $previous -> $nextState")
    }
}

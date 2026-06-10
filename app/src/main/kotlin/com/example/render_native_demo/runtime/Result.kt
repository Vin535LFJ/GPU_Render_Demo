package com.example.render_native_demo.runtime

data class Result<out T>(
    val success: Boolean,
    val value: T?,
    val errorCode: ErrorCode,
    val errorMessage: String?
) {
    companion object {
        fun <T> success(value: T? = null): Result<T> {
            return Result(true, value, ErrorCode.OK, null)
        }

        fun <T> failure(errorCode: ErrorCode, errorMessage: String? = null): Result<T> {
            return Result(false, null, errorCode, errorMessage ?: errorCode.message)
        }
    }
}

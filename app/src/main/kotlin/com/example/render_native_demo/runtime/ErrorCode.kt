
package com.example.render_native_demo.runtime

enum class ErrorCode(val code: Int, val message: String) {
    OK(0, "OK"),
    INVALID_STATE(1, "Invalid state for operation"),
    ALREADY_RELEASED(2, "Engine already released"),
    INVALID_ARGUMENT(3, "Invalid argument"),
    UNKNOWN_ERROR(4, "Unknown error")
}

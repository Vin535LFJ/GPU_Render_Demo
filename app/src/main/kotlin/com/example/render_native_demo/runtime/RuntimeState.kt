
package com.example.render_native_demo.runtime

enum class RuntimeState {
    IDLE,
    PREPARED,
    PLAYING,
    PAUSED,
    SEEK_REQUESTED,
    SEEK_FLUSHING,
    SEEK_PRIMING,
    SEEK_RECOVERING,
    EOS_HOLD,
    STOPPED,
    RELEASED
}

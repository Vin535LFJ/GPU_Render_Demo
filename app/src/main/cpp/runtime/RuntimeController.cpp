
#include "RuntimeController.h"
#include <android/log.h>
#include <set>

#define LOG_TAG "RuntimeController"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

namespace render {

RuntimeController::RuntimeController() 
    : currentState_(RuntimeState::IDLE) {}

RuntimeState RuntimeController::currentState() const {
    std::lock_guard<std::mutex> lock(stateLock_);
    return currentState_;
}

Result RuntimeController::prepare(const std::string& source) {
    std::lock_guard<std::mutex> lock(stateLock_);
    logStateTransition("prepare", currentState_, RuntimeState::PREPARED);
    
    if (currentState_ == RuntimeState::RELEASED) {
        LOGW("prepare called on released engine");
        return Result::Failure(ErrorCode::ALREADY_RELEASED, "Engine already released");
    }
    
    if (currentState_ != RuntimeState::IDLE) {
        LOGW("Invalid state for prepare: %d", static_cast<int>(currentState_));
        return Result::Failure(ErrorCode::INVALID_STATE, "Invalid state");
    }
    
    currentState_ = RuntimeState::PREPARED;
    return Result::Success();
}

Result RuntimeController::play() {
    std::lock_guard<std::mutex> lock(stateLock_);
    logStateTransition("play", currentState_, RuntimeState::PLAYING);
    
    if (currentState_ == RuntimeState::RELEASED) {
        LOGW("play called on released engine");
        return Result::Failure(ErrorCode::ALREADY_RELEASED);
    }
    
    const std::set<RuntimeState> validStates = {
        RuntimeState::PREPARED,
        RuntimeState::PAUSED,
        RuntimeState::SEEK_RECOVERING
    };
    
    if (validStates.find(currentState_) == validStates.end()) {
        LOGW("Invalid state for play: %d", static_cast<int>(currentState_));
        return Result::Failure(ErrorCode::INVALID_STATE);
    }
    
    currentState_ = RuntimeState::PLAYING;
    return Result::Success();
}

Result RuntimeController::pause() {
    std::lock_guard<std::mutex> lock(stateLock_);
    logStateTransition("pause", currentState_, RuntimeState::PAUSED);
    
    if (currentState_ == RuntimeState::RELEASED) {
        LOGW("pause called on released engine");
        return Result::Failure(ErrorCode::ALREADY_RELEASED);
    }
    
    if (currentState_ != RuntimeState::PLAYING) {
        LOGW("Invalid state for pause: %d", static_cast<int>(currentState_));
        return Result::Failure(ErrorCode::INVALID_STATE);
    }
    
    currentState_ = RuntimeState::PAUSED;
    return Result::Success();
}

Result RuntimeController::seek(int64_t targetUs) {
    std::lock_guard<std::mutex> lock(stateLock_);
    logStateTransition("seek", currentState_, RuntimeState::SEEK_REQUESTED);
    
    if (currentState_ == RuntimeState::RELEASED) {
        LOGW("seek called on released engine");
        return Result::Failure(ErrorCode::ALREADY_RELEASED);
    }
    
    if (currentState_ != RuntimeState::PLAYING && currentState_ != RuntimeState::PAUSED) {
        LOGW("Invalid state for seek: %d", static_cast<int>(currentState_));
        return Result::Failure(ErrorCode::INVALID_STATE);
    }
    
    currentState_ = RuntimeState::SEEK_REQUESTED;
    return Result::Success();
}

Result RuntimeController::stop() {
    std::lock_guard<std::mutex> lock(stateLock_);
    logStateTransition("stop", currentState_, RuntimeState::STOPPED);
    
    if (currentState_ == RuntimeState::RELEASED) {
        LOGW("stop called on released engine");
        return Result::Failure(ErrorCode::ALREADY_RELEASED);
    }
    
    if (currentState_ == RuntimeState::STOPPED) {
        return Result::Success();
    }
    
    currentState_ = RuntimeState::STOPPED;
    return Result::Success();
}

Result RuntimeController::release() {
    std::lock_guard<std::mutex> lock(stateLock_);
    logStateTransition("release", currentState_, RuntimeState::RELEASED);
    
    if (currentState_ == RuntimeState::RELEASED) {
        LOGW("release called on already released engine");
        return Result::Failure(ErrorCode::ALREADY_RELEASED);
    }
    
    if (currentState_ != RuntimeState::STOPPED) {
        LOGW("Invalid state for release: %d, must be STOPPED first", 
             static_cast<int>(currentState_));
        return Result::Failure(ErrorCode::INVALID_STATE);
    }
    
    currentState_ = RuntimeState::RELEASED;
    return Result::Success();
}

void RuntimeController::logStateTransition(const std::string& action,
                                          RuntimeState from,
                                          RuntimeState to) const {
    LOGI("[%s] State transition: %d -> %d", 
         action.c_str(), 
         static_cast<int>(from), 
         static_cast<int>(to));
}

} // namespace render

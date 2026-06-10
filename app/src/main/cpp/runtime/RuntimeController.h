
#ifndef RUNTIME_CONTROLLER_H
#define RUNTIME_CONTROLLER_H

#include <mutex>
#include <string>

namespace render {

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
};

enum class ErrorCode {
    OK = 0,
    INVALID_STATE = 1,
    ALREADY_RELEASED = 2,
    INVALID_ARGUMENT = 3,
    UNKNOWN_ERROR = 4
};

struct Result {
    bool success;
    ErrorCode errorCode;
    std::string errorMessage;

    static Result Success() {
        return {true, ErrorCode::OK, ""};
    }

    static Result Failure(ErrorCode code, const std::string& message = "") {
        return {false, code, message};
    }
};

class RuntimeController {
public:
    RuntimeController();
    
    RuntimeState currentState() const;
    
    Result prepare(const std::string& source);
    Result play();
    Result pause();
    Result seek(int64_t targetUs);
    Result stop();
    Result release();

private:
    mutable std::mutex stateLock_;
    RuntimeState currentState_;

    void logStateTransition(const std::string& action, 
                           RuntimeState from, 
                           RuntimeState to) const;
};

} // namespace render

#endif // RUNTIME_CONTROLLER_H

#include <jni.h>
#include <string>
#include <memory>
#include "runtime/RuntimeController.h"

namespace {
constexpr const char* kRuntimeBaseline =
        "P0 baseline: Kotlin + JNI + C++17 ready. Runtime MVP starts in P1.";

std::unique_ptr<render::RuntimeController> gController;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_render_1native_1demo_MainActivity_runtimeBaseline(
        JNIEnv* env,
        jclass /* clazz */) {
    const std::string message{kRuntimeBaseline};
    return env->NewStringUTF(message.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_render_1native_1demo_runtime_RuntimeController_nativeInit(
        JNIEnv* env,
        jobject /* thiz */) {
    gController = std::make_unique<render::RuntimeController>();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_render_1native_1demo_runtime_RuntimeController_nativePrepare(
        JNIEnv* env,
        jobject /* thiz */,
        jstring source) {
    if (!gController) {
        return static_cast<jint>(render::ErrorCode::INVALID_STATE);
    }

    const char* sourceStr = env->GetStringUTFChars(source, nullptr);
    auto result = gController->prepare(sourceStr);
    env->ReleaseStringUTFChars(source, sourceStr);

    return static_cast<jint>(result.errorCode);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_render_1native_1demo_runtime_RuntimeController_nativePlay(
        JNIEnv* env,
        jobject /* thiz */) {
    if (!gController) {
        return static_cast<jint>(render::ErrorCode::INVALID_STATE);
    }

    auto result = gController->play();
    return static_cast<jint>(result.errorCode);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_render_1native_1demo_runtime_RuntimeController_nativePause(
        JNIEnv* env,
        jobject /* thiz */) {
    if (!gController) {
        return static_cast<jint>(render::ErrorCode::INVALID_STATE);
    }

    auto result = gController->pause();
    return static_cast<jint>(result.errorCode);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_render_1native_1demo_runtime_RuntimeController_nativeSeek(
        JNIEnv* env,
        jobject /* thiz */,
        jlong targetUs) {
    if (!gController) {
        return static_cast<jint>(render::ErrorCode::INVALID_STATE);
    }

    auto result = gController->seek(static_cast<int64_t>(targetUs));
    return static_cast<jint>(result.errorCode);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_render_1native_1demo_runtime_RuntimeController_nativeStop(
        JNIEnv* env,
        jobject /* thiz */) {
    if (!gController) {
        return static_cast<jint>(render::ErrorCode::INVALID_STATE);
    }

    auto result = gController->stop();
    return static_cast<jint>(result.errorCode);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_render_1native_1demo_runtime_RuntimeController_nativeRelease(
        JNIEnv* env,
        jobject /* thiz */) {
    if (!gController) {
        return static_cast<jint>(render::ErrorCode::INVALID_STATE);
    }

    auto result = gController->release();
    gController.reset();
    return static_cast<jint>(result.errorCode);
}

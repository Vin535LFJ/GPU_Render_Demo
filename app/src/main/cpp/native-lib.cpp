#include <jni.h>
#include <string>

namespace {
constexpr const char* kRuntimeBaseline =
        "P0 baseline: Kotlin + JNI + C++17 ready. Runtime MVP starts in P1.";
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_render_1native_1demo_MainActivity_runtimeBaseline(
        JNIEnv* env,
        jclass /* clazz */) {
    const std::string message{kRuntimeBaseline};
    return env->NewStringUTF(message.c_str());
}

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <mutex>
#include <atomic>

#if TRANSVIBE_HAS_WHISPER_CPP
#include "whisper.h"
#endif

#define LOG_TAG "TransVibeWhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
struct WhisperContextHolder {
#if TRANSVIBE_HAS_WHISPER_CPP
    whisper_context* ctx = nullptr;
#else
    void* ctx = nullptr;
#endif
    std::mutex mutex;
    std::atomic<bool> cancelled{false};
};

#if TRANSVIBE_HAS_WHISPER_CPP
bool abort_callback(void* user_data) {
    auto* holder = static_cast<WhisperContextHolder*>(user_data);
    return holder != nullptr && holder->cancelled.load(std::memory_order_acquire);
}
#endif
} // namespace

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_transvibe_app_nativebridge_WhisperNativeBridge_nativeIsLinked(JNIEnv*, jobject) {
#if TRANSVIBE_HAS_WHISPER_CPP
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}

JNIEXPORT jlong JNICALL
Java_com_transvibe_app_nativebridge_WhisperNativeBridge_nativeInit(
        JNIEnv* env,
        jobject,
        jstring modelPath) {
    if (modelPath == nullptr) return 0L;
    const char* pathChars = env->GetStringUTFChars(modelPath, nullptr);
    std::string path(pathChars ? pathChars : "");
    if (pathChars) env->ReleaseStringUTFChars(modelPath, pathChars);

#if TRANSVIBE_HAS_WHISPER_CPP
    whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false;
    whisper_context* ctx = whisper_init_from_file_with_params(path.c_str(), cparams);
    if (!ctx) {
        LOGE("Failed to initialize whisper_context from %s", path.c_str());
        return 0L;
    }
    auto* holder = new WhisperContextHolder();
    holder->ctx = ctx;
    LOGI("Initialized whisper_context successfully");
    return reinterpret_cast<jlong>(holder);
#else
    LOGE("whisper.cpp is not linked in this build");
    return 0L;
#endif
}

JNIEXPORT void JNICALL
Java_com_transvibe_app_nativebridge_WhisperNativeBridge_nativeCancel(
        JNIEnv*, jobject, jlong handle) {
    if (handle == 0L) return;
    auto* holder = reinterpret_cast<WhisperContextHolder*>(handle);
    holder->cancelled.store(true, std::memory_order_release);
}

JNIEXPORT jobjectArray JNICALL
Java_com_transvibe_app_nativebridge_WhisperNativeBridge_nativeTranscribe(
        JNIEnv* env, jobject, jlong handle, jfloatArray pcmData, jint numSamples,
        jstring languageHint, jint threads, jboolean greedyOnly, jint beamSize) {
    jclass segClass = env->FindClass("com/transvibe/app/nativebridge/NativeWhisperSegment");
    if (!segClass) return nullptr;
    if (handle == 0L || pcmData == nullptr || numSamples <= 0 ||
        numSamples > env->GetArrayLength(pcmData)) {
        return env->NewObjectArray(0, segClass, nullptr);
    }

#if TRANSVIBE_HAS_WHISPER_CPP
    auto* holder = reinterpret_cast<WhisperContextHolder*>(handle);
    holder->cancelled.store(false, std::memory_order_release);
    std::lock_guard<std::mutex> lock(holder->mutex);
    if (!holder->ctx) {
        return env->NewObjectArray(0, segClass, nullptr);
    }

    jfloat* samples = env->GetFloatArrayElements(pcmData, nullptr);
    if (!samples) return env->NewObjectArray(0, segClass, nullptr);
    const char* langChars = languageHint ? env->GetStringUTFChars(languageHint, nullptr) : nullptr;
    std::string lang = langChars ? std::string(langChars) : "auto";
    if (langChars) env->ReleaseStringUTFChars(languageHint, langChars);

    whisper_full_params params = whisper_full_default_params(
        greedyOnly ? WHISPER_SAMPLING_GREEDY : WHISPER_SAMPLING_BEAM_SEARCH
    );
    params.n_threads = threads > 0 ? threads : 2;
    params.translate = false;
    params.print_special = false;
    params.print_progress = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    params.no_context = true;
    params.single_segment = false;
    if (!greedyOnly && beamSize > 1) params.beam_search.beam_size = beamSize;
    if (lang == "auto" || lang.empty()) {
        params.language = "auto";
        params.detect_language = true;
    } else {
        params.language = lang.c_str();
    }
    params.abort_callback = abort_callback;
    params.abort_callback_user_data = holder;

    int rc = whisper_full(holder->ctx, params, samples, numSamples);
    env->ReleaseFloatArrayElements(pcmData, samples, JNI_ABORT);
    if (rc != 0) {
        LOGE("whisper_full failed with code %d", rc);
        return env->NewObjectArray(0, segClass, nullptr);
    }

    int nSegments = whisper_full_n_segments(holder->ctx);
    jmethodID ctor = env->GetMethodID(segClass, "<init>", "(JJLjava/lang/String;Ljava/lang/String;)V");
    if (!ctor) return nullptr;
    jobjectArray result = env->NewObjectArray(nSegments, segClass, nullptr);
    int langId = whisper_full_lang_id(holder->ctx);
    const char* detectedLang = whisper_lang_str(langId);
    jstring jLang = env->NewStringUTF(detectedLang ? detectedLang : "unknown");
    for (int i = 0; i < nSegments; ++i) {
        int64_t t0Ms = whisper_full_get_segment_t0(holder->ctx, i) * 10LL;
        int64_t t1Ms = whisper_full_get_segment_t1(holder->ctx, i) * 10LL;
        const char* text = whisper_full_get_segment_text(holder->ctx, i);
        jstring jText = env->NewStringUTF(text ? text : "");
        jobject obj = env->NewObject(segClass, ctor, t0Ms, t1Ms, jText, jLang);
        env->SetObjectArrayElement(result, i, obj);
        env->DeleteLocalRef(jText);
        env->DeleteLocalRef(obj);
    }
    env->DeleteLocalRef(jLang);
    return result;
#else
    return env->NewObjectArray(0, segClass, nullptr);
#endif
}

JNIEXPORT void JNICALL
Java_com_transvibe_app_nativebridge_WhisperNativeBridge_nativeFree(
        JNIEnv*, jobject, jlong handle) {
    if (handle == 0L) return;
    auto* holder = reinterpret_cast<WhisperContextHolder*>(handle);
    {
        std::lock_guard<std::mutex> lock(holder->mutex);
#if TRANSVIBE_HAS_WHISPER_CPP
        if (holder->ctx) {
            whisper_free(holder->ctx);
            holder->ctx = nullptr;
        }
#endif
    }
    delete holder;
}
} // extern "C"

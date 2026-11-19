cmake_minimum_required(VERSION 3.22.1)
project(transvibe_native LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 16 KB page-size alignment for Android 15+ ARM64 compatibility
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

# Default OFF protects production and direct/standalone CMake builds.
# Only Gradle Debug and Preview explicitly opt into diagnostic JNI.
option(TRANSVIBE_ALLOW_DIAGNOSTIC_JNI "Allow non-functional diagnostic JNI (debug/preview only)" OFF)
set(WHISPER_CPP_DIR "${CMAKE_CURRENT_SOURCE_DIR}/third_party/whisper.cpp")

if(EXISTS "${WHISPER_CPP_DIR}/CMakeLists.txt")
    message(STATUS "TransVibe: Building with real whisper.cpp from ${WHISPER_CPP_DIR}")
    set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
    set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
    set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
    add_subdirectory("${WHISPER_CPP_DIR}" whisper_build)

    add_library(transvibe_whisper SHARED whisper_jni.cpp)
    target_compile_definitions(transvibe_whisper PRIVATE TRANSVIBE_HAS_WHISPER_CPP=1)
    target_include_directories(transvibe_whisper PRIVATE
        "${WHISPER_CPP_DIR}/include"
        "${WHISPER_CPP_DIR}/ggml/include"
    )
    target_link_libraries(transvibe_whisper PRIVATE whisper log android)
elseif(TRANSVIBE_ALLOW_DIAGNOSTIC_JNI)
    message(WARNING "DEBUG/PREVIEW DIAGNOSTIC ONLY — NOT WORKING ASR. TransVibe: third_party/whisper.cpp not found. Building JNI bridge in unlinked/diagnostic mode (returns NATIVE_LIBRARY_NOT_FOUND until whisper.cpp is fetched via scripts/fetch_native_deps.sh).")
    add_library(transvibe_whisper SHARED whisper_jni.cpp)
    target_compile_definitions(transvibe_whisper PRIVATE TRANSVIBE_HAS_WHISPER_CPP=0)
    target_link_libraries(transvibe_whisper PRIVATE log android)
else()
    message(FATAL_ERROR "RELEASE BLOCKED: third_party/whisper.cpp missing. Diagnostic JNI must not be shipped in release.")
endif()

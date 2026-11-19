import java.util.Properties
import java.io.FileInputStream
import java.util.zip.ZipFile
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// The owner's signing materials are intentionally NOT distributed.
// This optional loader reads local Code On The Go signing data without changing it.
val keystorePropsFile = rootProject.file("release.properties")
val keystoreProps = Properties()
if (keystorePropsFile.isFile) {
    FileInputStream(keystorePropsFile).use { keystoreProps.load(it) }
}
val hasValidSigningProps = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    .all { !keystoreProps.getProperty(it).isNullOrBlank() } &&
    rootProject.file(keystoreProps.getProperty("storeFile") ?: "").isFile

// Code On The Go runs Gradle inside an ARM64 Android process. The installed
// stock Android NDK contains host toolchains for desktop hosts, not this phone:
// CMake therefore resolves prebuilt//bin/clang and fails before Kotlin compile.
// Keep production/native CMake sources intact; omit externalNativeBuild linkage
// *only* in the Code On The Go ARM64 diagnostic build environment.
val isCoGoArm64Host = System.getProperty("os.arch").lowercase() in setOf("aarch64", "arm64") &&
    gradle.gradleUserHomeDir.absolutePath.replace('\\', '/').contains("/data/data/com.itsaky.androidide/")
// A real, precompiled Android ARM64 JNI library may be copied into app/libs/
// before using Code On The Go's Build button (no on-device CMake compiler).
// It must export the exact WhisperNativeBridge JNI methods in this project.
val hasPrebuiltWhisperJni = file("libs/arm64-v8a/libtransvibe_whisper.so").isFile
if (isCoGoArm64Host) {
    logger.warn(
        "COGO ARM64 DIAGNOSTIC-ONLY: skipping CMake/Whisper JNI compilation because " +
            "the phone has no supported native NDK host compiler. " +
            "Whisper requires a compatible prebuilt libtransvibe_whisper.so under " +
            "app/libs/arm64-v8a/; otherwise ASR remains unavailable. Release remains BLOCKED."
    )
}

android {
    namespace = "com.transvibe.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.transvibe.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        if (!isCoGoArm64Host && !hasPrebuiltWhisperJni) {
            externalNativeBuild {
                cmake {
                    cppFlags += listOf("-std=c++17", "-O3", "-fexceptions", "-frtti")
                    arguments += listOf(
                        "-DANDROID_STL=c++_shared",
                        "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON"
                    )
                }
            }
        }
    }

    // Signing is configured only when the owner has already supplied local
    // private credentials. Never bundle the keystore or passwords in source ZIPs.
    signingConfigs {
        if (hasValidSigningProps) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("boolean", "ENABLE_BENCHMARK_PANEL", "true")
            buildConfigField("String", "VARIANT_LABEL", "\"debug\"")
            buildConfigField("boolean", "DIAGNOSTIC_NATIVE_ALLOWED", "true")
            buildConfigField("boolean", "DIAGNOSTIC_NATIVE_BUILD_SKIPPED", isCoGoArm64Host.toString())
            // Without a host C/C++ compiler, no diagnostic .so is packaged on CoGo.
            if (!isCoGoArm64Host && !hasPrebuiltWhisperJni) {
                externalNativeBuild {
                    cmake {
                        arguments += listOf("-DTRANSVIBE_ALLOW_DIAGNOSTIC_JNI=ON")
                    }
                }
            }
        }
        create("preview") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
            matchingFallbacks += listOf("debug")
            buildConfigField("boolean", "ENABLE_BENCHMARK_PANEL", "true")
            buildConfigField("String", "VARIANT_LABEL", "\"preview\"")
            buildConfigField("boolean", "DIAGNOSTIC_NATIVE_ALLOWED", "true")
            buildConfigField("boolean", "DIAGNOSTIC_NATIVE_BUILD_SKIPPED", isCoGoArm64Host.toString())
        }
        release {
            if (hasValidSigningProps) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "ENABLE_BENCHMARK_PANEL", "false")
            buildConfigField("String", "VARIANT_LABEL", "\"release\"")
            buildConfigField("boolean", "DIAGNOSTIC_NATIVE_ALLOWED", "false")
            buildConfigField("boolean", "DIAGNOSTIC_NATIVE_BUILD_SKIPPED", "false")
            if (!isCoGoArm64Host && !hasPrebuiltWhisperJni) {
                externalNativeBuild {
                    cmake {
                        arguments += listOf("-DTRANSVIBE_ALLOW_DIAGNOSTIC_JNI=OFF")
                    }
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }

    // No CMake configuration/tasks on Android ARM64 CoGo; a desktop build
    // still builds the JNI bridge. This is a diagnostic-only limitation.
    if (!isCoGoArm64Host && !hasPrebuiltWhisperJni) {
        externalNativeBuild {
            cmake {
                path = file("src/main/cpp/CMakeLists.txt")
                version = "3.22.1"
            }
        }
    }

    // Import precompiled .so files placed manually at app/libs/arm64-v8a/.
    // Keep Android's standard src/main/jniLibs path available too.
    sourceSets {
        getByName("main") {
            jniLibs.srcDir("libs")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
            pickFirsts += listOf("lib/arm64-v8a/libc++_shared.so")
        }
        resources {
            resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
            resources.excludes.add("META-INF/kotlinx_coroutines_core.version")

            // The part below is only needed for compose builds.
            // This packaging block is required to solve interdependency conflicts.
            // They arise only when using local maven repo, so I suppose online repos have some way of solving such issues.

            // Caused by: com.android.builder.merge.DuplicateRelativeFileException: 4 files found with path 'commonMain/default/linkdata/module' from inputs:
            // - AndroidIDE\libs_source\gradle\localMvnRepository\androidx\collection\collection\1.4.2\collection-1.4.2.jar
            // - AndroidIDE\libs_source\gradle\localMvnRepository\androidx\lifecycle\lifecycle-common\2.8.7\lifecycle-common-2.8.7.jar
            // - AndroidIDE\libs_source\gradle\localMvnRepository\androidx\annotation\annotation\1.8.1\annotation-1.8.1.jar
            // - AndroidIDE\libs_source\gradle\localMvnRepository\org\jetbrains\kotlinx\kotlinx-coroutines-core\1.7.3\kotlinx-coroutines-core-1.7.3.jar
            // And some others.
            resources.pickFirsts.add("nonJvmMain/default/linkdata/package_androidx/0_androidx.knm")
            resources.pickFirsts.add("nonJvmMain/default/linkdata/root_package/0_.knm")
            resources.pickFirsts.add("nonJvmMain/default/linkdata/module")

            resources.pickFirsts.add("nativeMain/default/linkdata/root_package/0_.knm")
            resources.pickFirsts.add("nativeMain/default/linkdata/module")

            resources.pickFirsts.add("commonMain/default/linkdata/root_package/0_.knm")
            resources.pickFirsts.add("commonMain/default/linkdata/module")
            resources.pickFirsts.add("commonMain/default/linkdata/package_androidx/0_androidx.knm")

            resources.pickFirsts.add("META-INF/kotlin-project-structure-metadata.json")

            resources.merges.add("commonMain/default/manifest")
            resources.merges.add("nonJvmMain/default/manifest")
            resources.merges.add("nativeMain/default/manifest")
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// AGP 9.3.1 built-in Kotlin replaces the legacy Android Kotlin plugin.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Original CoGo starter libraries: keep the supported resources/layouts
    // importable without forcing obsolete AndroidX versions.
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.android.material)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.interpolator)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)

    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.language.id)

    // Android AAR/JAR files copied manually to app/libs/ before UI Build.
    // Sherpa AAR already packages its own JNI libraries; do not extract .so again.
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}


// Phase 01: verify manually provided local binary files before UI-based builds.
// This checks container format and ARM64 architecture, NOT real JNI symbols,
// full transitive dependencies, native inference quality or licensing.
val verifyManualLocalLibraries = tasks.register("verifyManualLocalLibraries") {
    group = "verification"
    description = "Inspect hand-copied app/libs AAR and Android ARM64 ELF libraries before building"
    doLast {
        val localDir = file("libs")
        val flatSoFiles = fileTree(localDir) { include("*.so") }.files
        check(flatSoFiles.isEmpty()) {
            "LOCAL LIBRARY ERROR: .so files must go under app/libs/arm64-v8a/, not directly under app/libs/."
        }
        val nativeSoFiles = fileTree("libs/arm64-v8a") { include("*.so") }.files
        nativeSoFiles.forEach { so ->
            check(so.length() >= 64L) {
                "LOCAL LIBRARY ERROR: ${so.name} is empty/truncated; expected an Android ARM64 ELF binary."
            }
            val elf = so.inputStream().use { input -> input.readNBytes(20) }
            check(
                elf.size == 20 && elf[0] == 0x7f.toByte() &&
                    elf[1] == 0x45.toByte() && elf[2] == 0x4c.toByte() &&
                    elf[3] == 0x46.toByte() && elf[4] == 2.toByte() &&
                    elf[5] == 1.toByte() && elf[18] == 0xb7.toByte() &&
                    elf[19] == 0.toByte()
            ) {
                "LOCAL LIBRARY ERROR: ${so.name} is not a 64-bit little-endian Android ARM64 ELF file."
            }
        }
        val sherpaArchives = fileTree("libs") { include("sherpa-onnx-*.aar") }.files.toList()
        check(sherpaArchives.size <= 1) {
            "LOCAL LIBRARY ERROR: keep exactly one Sherpa-ONNX AAR version under app/libs/."
        }
        sherpaArchives.forEach { archive ->
            ZipFile(archive).use { aar ->
                val entries = aar.entries().asSequence().map { entry -> entry.name }.toSet()
                check("classes.jar" in entries && "AndroidManifest.xml" in entries) {
                    "LOCAL LIBRARY ERROR: ${archive.name} is not a complete Android AAR."
                }
                check("jni/arm64-v8a/libonnxruntime.so" in entries &&
                    entries.any { it.startsWith("jni/arm64-v8a/") &&
                        it.endsWith(".so") && it.contains("sherpa", ignoreCase = true) }
                ) {
                    "LOCAL LIBRARY ERROR: Sherpa AAR lacks required Android ARM64 native runtime."
                }
                val duplicates = nativeSoFiles.map { it.name }.filter { name ->
                    "jni/arm64-v8a/$name" in entries
                }
                check(duplicates.isEmpty()) {
                    "LOCAL LIBRARY ERROR: duplicated AAR native .so and app/libs/arm64-v8a/: $duplicates"
                }
            }
        }
        if (hasPrebuiltWhisperJni) {
            val provenanceFile = file("libs/native-provenance.json")
            check(provenanceFile.isFile) {
                "LOCAL LIBRARY ERROR: missing native-provenance.json for prebuilt Whisper."
            }
            val provenance = provenanceFile.readText(Charsets.UTF_8)
            fun jsonHash(key: String): String? = Regex("\"" + key + "\"\\s*:\\s*\"([a-f0-9]{64})\"")
                .find(provenance)?.groupValues?.get(1)
            val digest = MessageDigest.getInstance("SHA-256")
            fun sha256Of(path: java.io.File): String = digest.digest(path.readBytes())
                .joinToString("") { byte: Byte -> "%02x".format(byte) }
            check(provenance.contains("bingattban/TransVibe-so-") &&
                provenance.contains("arm64-v8a") && provenance.contains("ggml-tiny.bin")) {
                "LOCAL LIBRARY ERROR: native provenance must identify the approved builder/model/ABI."
            }
            check(jsonHash("binary_sha256") == sha256Of(file("libs/arm64-v8a/libtransvibe_whisper.so"))) {
                "LOCAL LIBRARY ERROR: prebuilt Whisper SHA-256 does not match native provenance."
            }
            check(jsonHash("jni_source_sha256") == sha256Of(file("src/main/cpp/whisper_jni.cpp"))) {
                "LOCAL LIBRARY ERROR: prebuilt Whisper was not built for this exact JNI source."
            }
        } else {
            logger.warn("LOCAL LIBRARIES: no app/libs/arm64-v8a/libtransvibe_whisper.so; Whisper ASR unavailable in CoGo Debug/Preview.")
        }
        if (sherpaArchives.isEmpty()) {
            logger.warn("LOCAL LIBRARIES: no sherpa-onnx-*.aar in app/libs/; Moonshine/SenseVoice unavailable.")
        }
        logger.lifecycle("Local library preflight complete: ${nativeSoFiles.size} ARM64 .so, ${sherpaArchives.size} Sherpa AAR.")
    }
}

// The existing Code On The Go Build button runs preDebugBuild; no terminal command.
tasks.matching { it.name in setOf("preDebugBuild", "prePreviewBuild", "preReleaseBuild") }
    .configureEach { dependsOn(verifyManualLocalLibraries) }

// Phase 01: fail closed on actual release builds. Debug/Preview may use a
// visibly marked JNI diagnostic, but no release artifact may be produced with
// either the missing whisper.cpp source OR a missing Sherpa-ONNX Android AAR.
// This validates presence/ABI packaging, NOT native inference correctness.
val verifyReleaseNativeDependencies = tasks.register("verifyReleaseNativeDependencies") {
    group = "verification"
    description = "Require real Whisper and Sherpa runtime assets before release packaging"
    doLast {
        check(!isCoGoArm64Host) {
            "RELEASE BLOCKED: Code On The Go ARM64 host cannot compile the " +
                "required Whisper JNI library. A diagnostic-only APK cannot be released."
        }
        val whisperDir = file("src/main/cpp/third_party/whisper.cpp")
        val whisperHeader = file("src/main/cpp/third_party/whisper.cpp/include/whisper.h")
        check(file("src/main/cpp/third_party/whisper.cpp/CMakeLists.txt").isFile && whisperHeader.isFile) {
            "RELEASE BLOCKED: real whisper.cpp source/header missing from $whisperDir. " +
                "The debug diagnostic JNI is NOT a working speech-recognition engine."
        }
        val sherpaArchives = fileTree("libs") {
            include("sherpa-onnx-*.aar")
        }.files.toList()
        check(sherpaArchives.size == 1) {
            "RELEASE BLOCKED: expected exactly one verified sherpa-onnx-*.aar in app/libs; found ${sherpaArchives.size}."
        }
        ZipFile(sherpaArchives.single()).use { archive ->
            val entries: Set<String> = archive.entries().asSequence().map { entry -> entry.name }.toSet()
            check("classes.jar" in entries) {
                "RELEASE BLOCKED: Sherpa AAR does not contain classes.jar."
            }
            check("jni/arm64-v8a/libonnxruntime.so" in entries) {
                "RELEASE BLOCKED: Sherpa AAR does not contain arm64-v8a libonnxruntime.so."
            }
            check(entries.any { it.startsWith("jni/arm64-v8a/") &&
                    it.endsWith(".so") && it.contains("sherpa", ignoreCase = true) }) {
                "RELEASE BLOCKED: Sherpa AAR lacks an arm64-v8a Sherpa JNI library."
            }
        }
    }
}

// Attach to release lifecycle/native steps. Do NOT block explicit Debug/Preview
// diagnostic builds, but always stop Release when true runtimes are absent.
tasks.matching {
    it.name in setOf("preReleaseBuild", "externalNativeBuildRelease", "assembleRelease", "bundleRelease")
}.configureEach {
    dependsOn(verifyReleaseNativeDependencies)
}

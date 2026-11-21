# TransVibe — Code On The Go build instructions (Phase 01, revised 19 Nov 2026)

**Use the in-app Build interface only. No terminal commands are needed.**

The former guide pinning AGP 8.7.3/Kotlin 2.0.21/Gradle 8.11.1 is superseded
because the user's current Code On The Go IDE runs Gradle 9.6.1 on ARM64.

Read the full Arabic UI-only instructions:

`START_HERE_CODE_ON_THE_GO_AR.md`

Current declared tools: Gradle **9.6.1**, AGP **9.3.1**, Kotlin/Compose compiler
**2.3.21**, KSP **2.3.14** (published Maven Central; on-device availability
not proven), JDK **21** (observed on owner's phone), compileSdk **36**,
minSdk **26**, targetSdk **35**, ABI **arm64-v8a**.

With a network connection available for missing dependencies, open `TransVibe`
(the directory containing `settings.gradle.kts`), use the IDE Sync action if
available, select **Preview** if the variant selector supports it, then press
**Build**. If `--offline` is forced in the IDE's Additional Gradle flags,
disable it for the initial dependency download. If the interface only exposes
Debug, save the log; do not install Debug over the existing production app.

Production ID: `com.transvibe.app`.
Preview ID: `com.transvibe.app.preview` (separate application data).
Do not uninstall the production app to address a signing conflict.

**WARNING:** a successful Debug or Preview APK is only a build/install gate:
a verified ARM64 Whisper library is included, but real-device speech recognition and model accuracy are not yet proven.
The Release native dependency check intentionally fails if the real runtimes
are absent. Do not label any diagnostic JNI stub as a working model.

Actual on-device build/unit-test/install/launch results: **NOT RUN** in the
source-preparation environment; these require owner's UI-generated logs.

For the Arabic device verification steps, see `docs/COTG_VERIFICATION_GUIDE_AR.md`.
The optional shell-based evidence collector is `scripts/validate_on_code_on_the_go.sh`.
Do not uninstall the existing production app to resolve a signing problem.


## CoGo Debug / LogSender / source libraries (21 Nov 2026)

LogSender is intentionally KEPT. Do not disable its Code On The Go Gradle
injection or remove LogSenderInstaller. Android 14+ may require
FOREGROUND_SERVICE_DATA_SYNC: the permission is declared only for Debug and
Preview manifests. An Android 12+ background-start restriction can still
cause LogSenderInstaller to throw before MainActivity; this is a Code On The
Go plugin/service lifecycle issue, not a missing ML Kit dependency. The
permission alone cannot resolve that restriction.

Source always retains the verified ARM64 JNI library and native provenance:
app/libs/arm64-v8a/libtransvibe_whisper.so and app/libs/native-provenance.json.
AndroidX Core and ML Kit common/translation/language-id have explicit
version-pinned Gradle runtime dependencies. Their compiled classes must be
merged into classes*.dex within the installed APK; do not copy AndroidX
or ML Kit AARs manually into app/libs on top of Gradle dependencies.

After :app:assembleDebug, :app:assemblePreview or :app:assembleRelease,
Gradle runs a verification that fails the build if its actual APK is missing
MlKitInitProvider, CoreComponentFactory, MainActivity, TransVibeApplication
or the ARM64 Whisper binary. This verifies packaged code but is not a
real-device launch test. DO NOT install the CoGo APK if this verification fails.

Important: the source ZIP does NOT contain every Google Maven AAR/JAR.
The pinned Gradle dependencies must be downloaded at least once during
Sync/build (and can then be cached locally). Large speech/translation model
weights are user-managed downloads and remain excluded from the repository.
A fully offline-from-clean-cache build requires separately packaging the
complete resolved dependency graph with approved redistribution rights.

# TransVibe — 20 Nov 2026 on-device Whisper Tiny zero-result diagnosis

Evidence: user-provided `app_logs.txt` from Code On The Go, around 01:49–01:51, and failed-processing screenshot.

## Findings
- User-visible result: `ASR_NO_TRANSCRIPT`, before translation.
- Four observed jobs used `whisper_cpp_jni`; log summary repeatedly reports `decodedFrames=895 candidates=3 fallbackWindows=0 asrCalls=3 recognized=0 saved=0`.
- The three Whisper candidate windows are ~20,000 ms, 20,000 ms and 1,563 ms. `results=0` each time. The native model initializes and the AAC decoder reports 44.1 kHz stereo; there is **no evidence yet of the PCM amplitude** that reached JNI. It would be unsafe to claim the model or VAD is solely responsible.
- Independent UI crash: Compose `Vertically scrollable component was measured with an infinity maximum height constraints`. TranslationPacksScreen contained a LazyColumn nested inside Column.verticalScroll; fix removes that invalid nesting.
- Separate Code On The Go injected log-sender crash: `ForegroundServiceStartNotAllowedException` from `com.itsaky.androidide.logsender.LogSenderService`; this is not the ASR failure and the project does not define that service directly.

## Strict first-model changes
- Record only audio candidate **sample count, RMS in dBFS, peak, finite indicator and selected language** before JNI. Do not record audio or private transcript text.
- Preserve normal stereo mixing. Only when stereo downmix **cancels over 80% of signal amplitude** via opposite-phase channels, select the dominant channel to avoid destroying potential speech.
- Fix the Compose nested scroll crash in the language-pack screen.
- No changes to the Whisper JNI contract or `.so`, model weights, Room schema, package identity, signing key or user data. Model 2 remains blocked until phone acceptance.

## Validation
- 82/82 Python source and regression tests passed locally after implementation.
- Pure Kotlin/AudioResampler and PcmSignalMetrics compilation and smoke tests succeeded.
- GitHub Actions source/JVM/native provenance + owner-signed two APK builds must all pass before automatic merge. A successfully built APK is **not** evidence that Whisper actually recognized speech.
- If RMS is clearly nonzero but all three results are still zero, next inspect the genuine `whisper.cpp` native inference output, language setup and model/PCM contract in the SO builder. Do not silently invent transcript segments.

## Device rerun procedure
Install the latest signed Preview (or preserve signing identity for Release), keep all existing data, run **a new job** for the same 41-second video with Whisper Tiny, and export full Code On The Go logs, notably `TransVibeASR input` and `candidate/results`. Do not uninstall data or switch ASR model.

# TransVibe (`com.transvibe.app`)

تطبيق أندرويد مستقل يعمل محلياً بالكامل (Offline-First) لتفريغ الصوت من الفيديو وترجمته إلى العربية مع محرر ترجمة ومعاينة فورية عبر Media3.

## الهوية المعتمدة
- **اسم التطبيق**: `TransVibe`
- **المعرّف الإنتاجي (`applicationId`)**: `com.transvibe.app`
- **نسخة المعاينة (`preview`)**: `com.transvibe.app.preview`
- **الإصدار**: `versionCode = 6`, `versionName = 2.0.0`

## النماذج السبعة المعتمدة فقط
1. `ggml-tiny.bin` (Whisper Tiny)
2. `ggml-base.bin` (Whisper Base)
3. `ggml-small.bin` (Whisper Small)
4. `ggml-medium.bin` (Whisper Medium)
5. `ggml-large-v3-turbo.bin` (Whisper Large V3 Turbo)
6. `moonshine-base-ar.zip` (Moonshine Base Arabic — للعربية فقط)
7. `sensevoice-small-int8.zip` (SenseVoice Small INT8 — `zh, yue, en, ja, ko`)

## خطوات التجهيز والبناء
راجع ملف `CODE_ON_THE_GO_BUILD.md` لتشغيل `./scripts/fetch_native_deps.sh` وبناء المشروع عبر Gradle.

# المرحلة 01 — دعم إضافة المكتبات يدويا قبل بناء TransVibe داخل Code On The Go

- **المصدر:** آخر ZIP كامل `TransVibe_Phase01_CoGo_ARM64_NDK_CMake_Fix_Full_2026-11-19.zip`، SHA-256: `c69a01fb2469dd145081ea9c46c20c647ef4a0b38a50d9a15d4dee0d82ba6589`.
- **الحالة:** SOURCE-CHANGES-PREPARED / ANDROID-BUILD-AND-DEVICE-VALIDATION-PENDING.
- **نطاق العمل:** تهيئة بناء Debug/Preview فقط مع الحفاظ على بقية تطبيق TransVibe والإصدار والتوقيع وRoom ونماذج ASR السبعة. لم يبدأ تنفيذ Phase 02.

## ما تم تغييره
1. `app/build.gradle.kts`: جذر `app/libs/` صار أيضا مسار Android `jniLibs` لملفات `.so` ضمن `arm64-v8a/`؛ ويبقى استيراد `.aar` و`.jar` الموجود سابقا. أُضيفت مهمة `verifyManualLocalLibraries` مرتبطة آليا بـ `preDebugBuild` و`prePreviewBuild` و`preReleaseBuild` وتتحقق من صحة الحاويات وبنية ARM64 ومكتبات Sherpa وأسماء الملفات المكررة.
2. `app/build.gradle.kts`: تجاوز CMake عند وجود `app/libs/arm64-v8a/libtransvibe_whisper.so` حتى على جهاز بناء مكتبي، لتجنب حزم مكتبة JNI مرتين؛ على Code On The Go ARM64 يبقى تجاوز CMake كما كان سابقا. منع Release السابق باقٍ على الجهاز.
3. `app/src/main/java/com/transvibe/app/nativebridge/WhisperNativeBridge.kt`: تعديل `isAvailable()` لالتقاط `UnsatisfiedLinkError` إذا كانت المكتبة قابلة للتحميل لكن ينقصها مدخل JNI المطلوب.
4. `app/src/main/java/com/transvibe/app/presentation/screens/HomeScreen.kt`: التحذير يظهر إذا كان CMake متجاوزا **ومحرك Whisper JNI غير مرتبط فعلا**؛ وجود ملف باسم مناسب لا يكفي للتحقق من الاستدلال.
5. `app/libs/README_AR.md` و`app/libs/arm64-v8a/.keep`: دليل وضع الملفات ومنع الفقد في ZIP.
6. اختبارات جديدة للميزة وتحديث اختبارات حفظ المصدر القديمة للسماح فقط بالتغييرات المصرح بها، دون تعديل بصمات المصادر المرجعية السابقة.

## البنية المطلوبة قبل الضغط على Build

```text
TransVibe/app/libs/sherpa-onnx-<version>.aar
TransVibe/app/libs/arm64-v8a/libtransvibe_whisper.so
TransVibe/app/libs/arm64-v8a/libggml*.so  [فقط إن كانت التبعية مطلوبة]
```

**هذه الملفات لم تُدرج في الأرشيف الحالي**: لم يرفع المستخدم ملفات AAR/SO حقيقية، ولا يمكن اختراعها. يجب أن تكون أصلية ومرخصة ومترجمة لأندرويد ARM64، وتثبت مطابقة JNI لهذه النسخة. Sherpa AAR تُضمّن .so داخليا فلا تُستخرج إلى مجلد .so لتفادي التعارض. ML Kit packs وأوزان نماذج ASR ليست مكتبات بناء ولا توضع في app/libs.

## الاختبارات
- RED قبل التعديل: فشل 4 عقود مصدرية وظهر خطأ وجود ملف الدليل، بينما نجحت اختبارات أخرى.
- GREEN بعد التعديل والتحديث: `python -m unittest discover -s phase01/tests -p "test_*.py" -q` — **PASS 51/51**.
- `ZipFile.testzip()` على الحزمة الكاملة وحزمة التعديلات — PASS.
- `:app:assembleDebug`, `:app:assemblePreview`, `:app:testDebugUnitTest`, تشغيل APK على ARM64، تحقق JNI العملي — **NOT RUN** (لا تتوفر Android SDK/بيئة Code On The Go/مكتبات ARM64 أصلية هنا).
- لا يوجد قياس وقت أو دقة أو ذاكرة. التحقق البنيوي من ELF لا يكشف جميع التبعيات أو الدوال المصدّرة؛ لا يعد ضمانا لعمل التعرف الصوتي.

## شروط الأمان والمخاطر
- `Release` على مضيف Code On The Go ARM64 ما زال **BLOCKED** حسب القيود السابقة؛ تجارب Debug/Preview فقط.
- Debug يستخدم `applicationId=com.transvibe.app` وقد يفشل التحديث فوق النسخة المثبتة إذا اختلف التوقيع؛ Preview مستقل.
- لا تُدرج مفاتيح توقيع أو أوزان نماذج أو ملفات بناء داخل الأرشيف.
- AAR محلية لا تستورد تبعياتها الفرعية تلقائيا؛ توثيقها وترخيصها وتوافق الإصدارات مطلوبان.
- **الخطوة التالية:** ضع الملفات في المسارات المذكورة إن كانت متاحة، ثم اضغط Build من واجهة Code On The Go، وأرسل سجل البناء الجديد ونتيجة التثبيت الفعلي.

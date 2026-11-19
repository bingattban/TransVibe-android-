# المرحلة 01 — إصلاح تعطل CMake وNDK على هاتف ARM64 داخل Code On The Go

**تاريخ التدقيق:** 2026-11-19  | **حالة الإنجاز:** CODE CHANGES PREPARED / ANDROID BUILD AND DEVICE VALIDATION PENDING.

## المصدر وسلسلة الأصل
- أحدث مشروع كامل مستخدم: `TransVibe_Phase01_KSP_Room285_Full_2026-11-19.zip`.
- SHA-256 المصدر: `ab7337548e49ce7ce328a9019c52e75791b3ecc64734f8510de2ea252efe5e71`.
- مجلد العمل: نسخة منفصلة مفكوكة تحت `/mnt/data/transvibe_phase01_native_build_fix/TransVibe`، دون أي وصول أو كتابة إلى GitHub/Notion أو ملفات الهاتف.
- المصدر الخارجي للمكتبة الأصلية غير موجود ضمن ZIP؛ لم يتم اختراع ملفات NDK أو JNI، ولا يوجد APK في الناتج.

## دليل الخطأ الفعلي من سجل المستخدم
سجل `build_output.txt` المرسل في هذه المحادثة بتاريخ 2026-11-19، أسطر نحو 1814-1887 و1955-1963:

```
Task :app:configureCMakeDebug[arm64-v8a] FAILED
The CMAKE_C_COMPILER: .../ndk/28.2.13676358/toolchains/llvm/prebuilt//bin/clang
The CMAKE_CXX_COMPILER: .../ndk/28.2.13676358/toolchains/llvm/prebuilt//bin/clang++
is not a full path to an existing compiler tool.
```

وفي السجل نفسه وصل البناء إلى `:app:kspDebugKotlin` وتم حفظ ناتج المهمة في الكاش قبل نهاية Gradle؛ لذلك فشل هذه الجولة ليس خطأ Room السابق. رسالة daemon environment warning ليست سبب الفشل المثبت.

**التشخيص:** Gradle يعمل داخل Android/ARM64، بينما مسار NDK host compiler غير موجود، وCMake يولد `prebuilt//bin/clang` فارغا. Android NDK يعبر عن المضيف باختيار أداة مترجم host platform؛ ليس صحيحا أن تغيير NDK version وحده يخلق مترجما متوافقا مع هاتف Android ARM64.

مراجع دعم إعداد native build:
- https://developer.android.com/studio/projects/gradle-external-native-builds
- https://developer.android.com/ndk/guides/other_build_systems

## التغيير المحدد في الكود
1. `app/build.gradle.kts`: اكتشاف مضيف AndroidIDE/Code On The Go بواسطة `os.arch = aarch64/arm64` ومسار Gradle home `/data/data/com.itsaky.androidide/` دون افتراض أن جميع مضيفات Linux تتطلب تجاوز CMake. أثناء البناء على هذا المضيف **لا يُسجّل** رابط `externalNativeBuild.cmake.path` ولا إعدادات CMake لـ Debug/Preview؛ وفي بيئات البناء التقليدية يُحتفظ بمسار CMake الأصلي.
2. `app/build.gradle.kts`: تسجيل تحذير واضح `COGO ARM64 DIAGNOSTIC-ONLY`، وإضافة علم BuildConfig `DIAGNOSTIC_NATIVE_BUILD_SKIPPED` لتوضيح الحالة على واجهة التطبيق.
3. `app/build.gradle.kts`: تعزيز `verifyReleaseNativeDependencies` حتى يمنع بناء Release **حتى لو أضيفت ملفات native إلى الهاتف** ما دام المترجم الأصلي غير قابل للاستخدام. تبقى فحوص Whisper/Sherpa السابقة مطلوبة على مضيف أصلي صالح.
4. `app/src/main/java/com/transvibe/app/presentation/screens/HomeScreen.kt`: شريط تشخيص عربي/إنجليزي صريح يخبر بأن Whisper غير متاح في نسخة الهاتف، وليس محركا مقلدا أو استدلالا حقيقيا.
5. `phase01/tests/test_cogo_arm64_cmake_host_guard.py`: حالات الانحدار لحماية الاستثناء المؤقت والهوية وRelease والتحذير.
6. `phase01/tests/test_build_contracts.py` و`phase01/tests/test_cogo_foundation_merge.py`: تحديث الاختبارين لحصر استثناء HomeScreen بالتحذير نفسه. يبقى كل ما عدا هذا البلوك مطابقا لبصمة المصدر القديم؛ لم تُعدل قوائم الأرشفة الأصلية.

**مهم:** `WhisperNativeBridge.isAvailable()` ينتهي إلى false إذا غابت `libtransvibe_whisper.so`؛ الصوت لن يتم التعرف عليه بهذه النسخة. هذا الإصلاح يؤهل اختبار واجهة التطبيق وKotlin وRoom، ولا يحل عمل ASR الفعلي. CMake وملف `whisper_jni.cpp` ما زالا محفوظين لإضافة محرك حقيقي على بيئة بناء داعمة لاحقا، دون الانتقال إلى المرحلة 04.

## الاختبارات الفعلية
- **RED قبل التعديل:** 4 اختبارات جديدة فشلت على شرط تجاوز CMake والتحذير وحاجز Release، ثم اختبار إعلان BuildConfig الزائد فشل قبل إصلاح التكرار، واختبار ظهور التحذير فشل قبل إضافته.
- **PASS بعد التعديل:** `python3 -m unittest discover -s phase01/tests -p 'test_*.py' -v` — 42 اختبارا نجح من 42، راجع `phase01/logs/PHASE01_COGO_ARM64_SOURCE_REGRESSION_2026-11-19.log`.
- **PASS:** اختبار `NativeCmakeGate` للتأكد من أن CMake مباشرة بدون مصادر whisper.cpp حقيقية **يفشل في Release** مع `RELEASE BLOCKED`؛ هذا اختبار تهيئة بالمترجم المحلي وليس JNI أندرويد.
- **NOT RUN:** `:app:assembleDebug`, `:app:assemblePreview`, `:app:testDebugUnitTest`, والتثبيت والتشغيل والتعرف/الترجمة على الجهاز؛ لا يتوفر Android SDK وبيئة Code On The Go في مساحة التنفيذ الحالية. لا يوجد APK مصدّق.
- **NOT MEASURED:** وقت الترجمة والذاكرة ودقة النماذج. لا توجد مراجعة تنفيذية لـ Phase 02.

## تحذيرات التنفيذ والتوقيع والبيانات
- خيار Build من واجهة Code On The Go يعمل على Debug بناء على سجل الهاتف، وقد يستمر إلى خطأ Kotlin أو ربط آخر بعد تجاوز CMake. إرسال السجل التالي مطلوب.
- مشروع Preview بمعرف `com.transvibe.app.preview` مستقل عن `com.transvibe.app`. Debug يستخدم المعرف الإنتاجي وقد يتعارض توقيعه مع النسخة المثبتة؛ لا تستبدل التطبيق الإنتاجي ولا تحذفه كحل روتيني.
- ملفات النماذج وبيانات Room الأصلية وملفات توقيع المستخدم لم تمس، ولم تُدرج مفاتيح توقيع داخل الأرشيف.
- بدون مكتبات Whisper/Sherpa الحقيقية لا يعد المشروع صالحا لإصدار إنتاجي، حتى لو نجح `assembleDebug`.

## خطوات التحقق باستخدام واجهة Code On The Go حصرا
1. فك ZIP الكامل الجديد وافتح مجلد `TransVibe` في Code On The Go.
2. تأكد من أن بنية Gradle المعتمدة على هاتفك هي Gradle 9.6.1 وAGP 9.3.1.
3. نفذ Sync إن كان متاحا؛ ثم اضغط **Build** في الواجهة، بلا أوامر طرفية.
4. يجب ألا تظهر مهمة `configureCMakeDebug[arm64-v8a]` في قائمة المهام الجديدة في هذه البيئة. بدلا من ذلك قد يظهر تحذير `COGO ARM64 DIAGNOSTIC-ONLY`.
5. بعد نجاح بناء Debug، اختبر تثبيت Preview منفصلة إن أمكن، أو تعامل بحذر مع توقيع Debug، وأرسل الخطأ الجديد أو نتيجة التثبيت. لا تستخدم هذه النسخة للتعرف الصوتي.

**القرار:** المرحلة 01 ليست مقبولة نهائيا قبل بناء APK فعلي واجتياز اختبارات الجهاز؛ والمرحلة 02 لم تبدأ.

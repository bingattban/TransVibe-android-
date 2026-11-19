# TransVibe — Phase 01 / Code On The Go KSP–Room build repair

**التاريخ:** 2026-11-19  
**النطاق:** إصلاح بناء فقط؛ لم تبدأ Phase 02.  
**الأساس:** `TransVibe_CoGo_Base_Merged_Full_2026-11-19.zip` (أرشيف المستخدم المدمج مع قالب Code On The Go).  
**SHA-256 لأرشيف البداية:** `6096ec8cace173f5e58da78726c1f971393a9e7fa171852674a31d6b38e332a2`

## 1. الدليل من سجل البناء الحقيقي (من الهاتف)

- بيئة الهاتف: CodeOnTheGo 26.44، Gradle 9.6.1، JDK 21، Android Gradle Plugin 9.3.1، Kotlin 2.3.21، KSP 2.3.14.
- تجاوز البناء مرحلة تهيئة المشاريع وحل عددا كبيرا من الاعتمادات؛ توقف عند `:app:kspDebugKotlin`.
- النص الحاسم: `e: [ksp] java.lang.IllegalStateException: unexpected jvm signature V`؛ يتبعه `Execution failed for task ':app:kspDebugKotlin'`.
- `24 actionable tasks: 24 executed` لا يعني نجاح البناء. النتيجة من الهاتف كانت `BUILD FAILED in 6m 18s`.
- حسب المصدر `gradle/libs.versions.toml`، المشروع السابق كان يثبت `room = "2.6.1"` لكل من runtime وktx وcompiler وtesting.
- في `Daos.kt` توجد دوال `suspend` تُرجع `Unit` ضمنا. سبب العطل **مرجح بقوة** أنه عيب التوافق المعروف في Room 2.6.x مع KSP2 حول توقيع `V`، وليس تأكيدا من stacktrace داخلي كامل؛ الرسالة المرفقة لم تتضمن تتبع السبب الكامل.

## 2. إصلاح محدد ومحدود

- **ملف إنتاج واحد تغير:** `gradle/libs.versions.toml`.
- تغيرت القيمة الوحيدة: `room = "2.6.1"` -> `room = "2.8.5"`.
- تبقى `androidx.room:room-runtime` و`room-ktx` و`room-compiler` و`room-testing` على **الإصدار الواحد نفسه** بفضل `version.ref = "room"`؛ لم يتم تغيير KSP/AGP/Kotlin لتفادي تعارضات جديدة في القالب.
- **ملف اختبار جديد:** `phase01/tests/test_room_ksp_compat.py`.
- **تقرير جديد:** `phase01/reports/PHASE01_KSP_ROOM_BUILD_FIX_AR.md`.
- **دليل اختبارات جديد:** `phase01/reports/PHASE01_KSP_ROOM_TEST_EVIDENCE.txt`.
- **لا حذف لملفات إنتاجية، ولا تغيير لقاعدة البيانات أو مخططها أو أعمدتها أو ترحيلاتها**، ولا إلى آليات Whisper/Sherpa/النماذج، ولا مفاتيح توقيع، ولا إعدادات التثبيت.

## 3. لماذا Room 2.8.5؟

- وثائق Android الرسمية: Room 2.8.5 الإصدار المستقر المعلن 2026-09-09، ويوضح المثال الرسمي استخدام `room-runtime` و`room-ktx` و`room-compiler` على إصدار واحد، مع KSP.
- سجل KSP المعروف حول الخطأ `unexpected jvm signature V` يشير إلى معالجة مشكلة قراءة الدوال التي تعيد `kotlin.Unit` في إصدار Room 2.7.0-alpha11 وما بعده.
- لم نستخدم تبديل دوال DAO إلى أنواع إرجاع مصطنعة، ولم نعطل KSP أو قاعدة البيانات. لا نعلن زوال الخطأ على الهاتف إلا بعد البناء الحقيقي.

**المصادر:**
- https://developer.android.com/jetpack/androidx/releases/room
- https://github.com/google/ksp/issues/2177

## 4. اختبار قبل التعديل وبعده

- أنشئ الاختبار أولا على أرشيف العمل غير المعدل؛ **فشل عمدا** اختبار `test_room_compiler_has_ksp2_void_signature_fix` لأن الإصدار 2.6.1.
- بعد تعديل الإصدار أصبح اختبار الملف الجديد **6/6 PASS**.
- بعد ذلك نُفذت كل اختبارات Python المصدرية السابقة باستخدام `python -m unittest discover -s phase01/tests -p 'test_*.py' -v`، وكانت النتيجة **35/35 PASS**.
- `python -m compileall -q phase01/tests` **PASS**.
- محاولة فعلية لاستخدام Wrapper في البيئة الحالية: `bash ./gradlew --offline :app:kspDebugKotlin --no-daemon` **BLOCKED** قبل تشغيل مهام Gradle، بسبب `java.net.UnknownHostException: services.gradle.org`، وعدم توفر توزيعة Gradle محليا في هذه البيئة.
- لذلك **لم تُنفذ** اختبارات `assembleDebug`، `assemblePreview`، `testDebugUnitTest`، تثبيت APK، تشغيل ASR الأصلي، أو اختبار ترحيلات Room على جهاز فعلي.

## 5. مخاطر باقية وصريحة

- تحديث إصدار مكتبة Room لا يغير رقم schema (`version = 3`) ولا ملف DB (`transvibe.db`) أو `MIGRATION_1_2` و`MIGRATION_2_3`، لكن لا يثبت سلامة بيانات قاعدة مثبتة على الهاتف ما لم تُجرَ اختبارات الترحيل والتحديث الحقيقي.
- لا يعالج هذا الإصلاح مشكلة اختلاف قاعدة بيانات الإصدار القديم v1.0.4 عن قاعدة النسخة الحالية؛ تظل قيد مرحلة الترحيلات المستقبلية.
- عدم وجود محركات whisper.cpp الحقيقية وSherpa AAR في المشروع لا يتحول إلى نجاح للتعرف الصوتي. فحص Release الصارم محفوظ؛ Debug/Preview فقط يسمحان بالمسار التشخيصي المعلن.
- من المحتمل ظهور أخطاء Kotlin/Android لاحقة بمجرد تخطي KSP؛ لا توجد أدلة كافية للمس بها الآن.

## 6. تجربة المستخدم — عبر الواجهة فقط

1. فك ضغط **الملف الكامل الجديد** لمجلد مستقل وافتح جذر `TransVibe` في Code On The Go، وليس مشروع ZIP قديمًا.
2. تأكد من وجود اتصال إنترنت في أول عملية Sync للحصول على Room 2.8.5. **لا تستخدم الطرفية**.
3. من واجهة التطبيق نفذ Sync إن وجد، ثم اختر Build الافتراضي (Debug) كما اعتدت. إذا أمكن اختيار Preview، فهو أكثر أمانا لتجنب التأثير في التطبيق المثبت.
4. أرسل سجل البناء التالي من الواجهة سواء نجح أو فشل. نجاح `kspDebugKotlin` وحده لا يكفي؛ يجب إكمال `assembleDebug` وتثبيت/فتح APK للتحقق من بوابة المرحلة 01.
5. لا تحذف التطبيق المثبت حاليا ولا تمسح بياناته لتجاوز تعارضات التوقيع أو التثبيت.

## 7. حالة القبول

**CODE CHANGE COMPLETE / ANDROID BUILD NOT RUN HERE / DEVICE ACCEPTANCE PENDING**. لا تُعتبر المرحلة 01 مكتملة قبوليا ولا تبدأ Phase 02 قبل تحقق البناء والاختبار المطلوبين.

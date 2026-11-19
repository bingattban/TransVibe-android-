# TransVibe — المرحلة 01: إصلاح خطأ Gradle Kotlin DSL في واجهة Code On The Go

**التاريخ:** 2026-11-19  
**المرحلة:** 01 فقط — متابعة تعطل تهيئة Gradle  
**المدخل:** `TransVibe_Phase01_CoGo_UI_Full_2026-11-19.zip`  
**الحالة:** إصلاح مصدر الاختبار؛ قبول البناء والتشغيل على الهاتف **معلق** حتى يرسل المستخدم نتائج Build التالية.

## العطل المبلغ عنه (من سجل الهاتف)

- نظام البناء المدمج: Gradle 9.6.1 / JDK 21.0.10 / AGP 9.3.1 / Kotlin Plugin 2.3.21 / KSP 2.3.14.
- فشل **CONFIGURE** في `app/build.gradle.kts` عند السطر 186:
  `Unresolved reference 'util'` ثم `Unresolved reference 'ZipFile'`، وأخطاء اشتقاق مرتبطة بـ `entries` و`it`.
- حذر Gradle من `tasks.registering` قرب السطر 170 (deprecated، ليس السبب الرئيسي لفشل التهيئة).
- لم يصل البناء إلى تجميع Kotlin الخاص بالتطبيق أو إنشاء APK؛ لا يمكن تقرير بقية الأخطاء المحتملة.

## تفسير السبب

في Gradle Kotlin DSL يوجد اسم `java` مستخدم بوصفه امتدادا لمهام Java/Android. داخل سياق تهيئة المهام قد يعامل `java.util.zip.ZipFile(...)` بوصفه سلسلة خصائص تبدأ من `java` الموجودة في DSL بدلا من اسم حزمة Java. استخدام **استيراد صريح** `import java.util.zip.ZipFile` ثم `ZipFile(...)` يزيل الالتباس. أمكن تجميع منطق ZIP مع اسم `java` محجوب في اختبار Kotlin/JVM مستقل، لكن **لم يتم** تجميع سكربت Gradle الفعلي على الهاتف في هذه البيئة.

## الملفات المتغيرة

1. `app/build.gradle.kts` — استيراد `ZipFile` صراحة؛ استعمال `ZipFile(...)` غير المؤهل؛ تحديد `Set<String>` بوضوح؛ تحويل مهمة الفحص إلى `tasks.register("verifyReleaseNativeDependencies")` دون delegate المهمل.
2. `phase01/tests/test_gradle_zipfile_compile_regression.py` — 3 اختبارات تمنع عودة الخطأ وتحفظ شرط منع إصدار Release دون المكتبات الأصلية.
3. `phase01/PHASE01_COGO_GRADLE_ZIPFILE_FIX_REPORT_AR.md` — التقرير الحالي.
4. `phase01/PHASE01_COGO_UI_BUILD_INSTRUCTIONS_AR.md` — خطوات الاختبار عبر واجهة Code On The Go.

لم يتغير `applicationId` أو الشاشات أو قاعدة البيانات أو النماذج. ولم تتم إضافة أوزان ASR أو مكتبات JNI غير موثقة.

## الاختبارات والأدلة

| التحقق | النتيجة |
| --- | --- |
| اختبارات الانحدار الجديدة **قبل الإصلاح** | فشل 2/3 كما هو متوقع، اختبار حماية Release نجح |
| اختبارات الانحدار الجديدة **بعد الإصلاح** | PASS 3/3 |
| اختبارات Python الموجودة والجديدة `unittest discover` | PASS 22/22 |
| تجميع مثال Kotlin/JVM مستقل على JDK محلي (`kotlinc`) | PASS؛ لا يعادل Android Gradle build |
| مقارنة ملفات المشروع الموروثة بالأرشيف المدخل | PASS؛ تعديل مصدر واحد فقط `app/build.gradle.kts` |
| Sync / Gradle configuration من واجهة Code On The Go | NOT RUN — يحتاج المستخدم للهاتف |
| `assembleDebug`, `assemblePreview`, `testDebugUnitTest` | NOT RUN — تحتاج Android SDK وبيئة Gradle جاهزة |
| تثبيت APK على ARM64 وفتحه | NOT RUN |
| Whisper / Sherpa inference | NOT RUN — المحركات الأصلية غير مضمنة |
| الأداء / WER / CER | NOT MEASURED |

### أوامر الاختبار التي نُفذت في بيئة المراجعة (ليست تعليمات للمستخدم)

- `python3 -m unittest discover -s phase01/tests -p 'test_gradle_zipfile_compile_regression.py' -v`
- `python3 -m unittest discover -s phase01/tests -p 'test_*.py' -v`
- `kotlinc ZipFileShadowingReproducer.kt -d zipfile_reproducer.jar` (ملف reproducer تجريبي خارج الأرشيف؛ النجاح للتحقق من صياغة Kotlin وليس Android).

## حدود هذا الإصلاح

- لم يثبت نجاح البناء في Code On The Go بعد؛ إذا ظهر عطل جديد، يحدد السجل التالي مساره.
- مهمة `verifyReleaseNativeDependencies` لا تزال تمنع Release عندما لا تتوفر شيفرة `whisper.cpp` الحقيقية أو حزمة Sherpa AAR المناسبة. Debug وPreview يقبلان **JNI تشخيصيا** ليس محرك تعرف صوتي صالحا للاستخدام الإنتاجي.
- التحذير `Unable to set daemon's environment variables ...` ليس سبب الفشل المبين في السجل.
- لا تنتقل المرحلة 02 حتى نجاح شروط قبول المرحلة 01 أو صدور أمر صريح يتناول الاعتماد على شروط القبول.

**النتيجة:** CODE PATCHED / COGO BUILD AND DEVICE VALIDATION PENDING — PHASE 01 NOT YET ACCEPTED.

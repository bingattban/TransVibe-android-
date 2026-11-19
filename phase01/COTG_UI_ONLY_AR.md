# TransVibe — Code On The Go — خطوات البناء من الواجهة فقط

هذه التعليمات هي المعتمدة بدلا من أي تعليمات قديمة تستعمل سطر الأوامر.
كل الخطوات **بدون طرفية**؛ لا تحتاج إلى نسخ أوامر Gradle.

## لماذا فشل الإصدار السابق؟

من سجل الهاتف: Gradle 9.6.1 وJDK 21.0.10، لكن مشروعنا السابق طلب AGP 8.7.3 وKotlin 2.0.21. اعتمد Code On The Go 26.38+ افتراضيا AGP 9.3.1 وKotlin 2.3.21. وفشل إعداد classpath بسبب `com.google.errorprone:error_prone_annotations:2.18.0` في مستودع محلي ناقص. تعديل المشروع هنا يتبع دليل ترحيل Code On The Go الرسمي، ولا يزعم معالجة كل مشكلة اعتماد في نسخة التطبيق على الهاتف.

## ما الذي تفعله في الواجهة؟

1. احتفظ بنسخة المشروع السابق. لا تحذف التطبيق المثبت ولا مشاريعه وبياناته.
2. فك ضغط ملف `TransVibe_Phase01_CoGo_UI_Full_2026-11-19.zip` وافتح **المجلد TransVibe** في Code On The Go؛ يجب أن يحتوي جذر المجلد المفتوح على `settings.gradle.kts` و`app`.
3. افتح إعدادات Code On The Go: **Preferences → Build & Run → Additional Gradle flags**. إذا كان خيار `--offline` مفروضا، أزله عند أول بناء لكي يستطيع التطبيق تنزيل الاعتمادات غير الموجودة محليا، ثم احفظ التغيير. لا تغير بقية إعدادات المشروع دون ظهور خطأ واضح.
4. من واجهة المشروع نفذ **Sync** أو **Reload Gradle** إن ظهر الخيار. انتظر نتيجة التهيئة.
5. اختر نوع البناء **Preview** إذا ظهرت قائمة نوع الإصدار، ثم اضغط زر **Build** المعتاد في الواجهة. في الإصدارات التي لا تعرض اختيار Preview، اضغط Build المعتاد لتجربة Debug واحتفظ بسجل البناء. اسم مهمة Preview التقني `assemblePreview` لكن **لا تحتاج إلى سطر أوامر**.
6. إذا نجح بناء Preview، ثبت ملف Preview فقط (`app-preview.apk`) واختبر ظهور واجهة TransVibe. المعرف `com.transvibe.app.preview` منفصل عن المعرف الإنتاجي. لا تثبت Debug فوق النسخة الإنتاجية قبل إثبات توافق التوقيع.
7. إن توقف البناء عند `FAILED`، انسخ **كامل السجل بدءا من السطر `FAILURE:` وحتى آخر سبب `Caused by`** وأرسله هنا، مع السطور التي تسبقه والتي تذكر `Could not resolve` أو `Could not find`.

## حالة الاختبار

- فحوص Python للملفات والمراجع: تجرى في بيئة إعداد الحزمة، ولا تثبت نجاح Gradle أو APK.
- Build/Sync على هاتف ARM64: **NOT RUN في بيئة إعداد الحزمة**، يتطلب سجل هاتفك الجديد.
- ما يزال JNI التشخيصي لا ينتج تعرّفا صوتيا فعليا. يجب عدم اعتبار تشغيل Preview إثباتا لدعم ASR.
- Release سيفشل عمدا إلى حين وجود whisper.cpp الحقيقي وSherpa AAR الصحيح؛ لا تحاول استخدام Release كبديل للبناء التجريبي في هذه المرحلة.

## مصادر التوافق

- https://github.com/appdevforall/CodeOnTheGo/wiki/IMPORTANT:-Fix-project-breaking-changes-after-the-Gradle-AGP-Kotlin-toolchain-upgrade
- https://developer.android.com/build/migrate-to-built-in-kotlin
- https://gradle.org/release-checksums/
- https://central.sonatype.com/artifact/com.google.errorprone/error_prone_annotations/2.18.0

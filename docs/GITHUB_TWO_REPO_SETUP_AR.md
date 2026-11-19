# إعداد TransVibe: الدمج الآلي والإصداران الموقعان

## المستودعان
- Android: https://github.com/bingattban/TransVibe-android-
- SO: https://github.com/bingattban/TransVibe-so-

## السياسة المعتمدة
كل Pull Request داخلي غير Draft يصبح مؤهلا للدمج تلقائيا **فقط** بعد نجاح جميع الاختبارات، توافق JNI والملفات الأصلية، وتجميع نسختي Preview وRelease الموقعتين والتحقق من شهادتيهما. لا يُستخدم bypass أو force merge أو الدمج قبل ظهور النتيجة. عند فشل أي شرط يبقى PR مفتوحا.

## الأسرار المطلوبة
### مستودع Android
Settings → Secrets and variables → Actions → Repository secrets:
- `SIGNING_KEY_BASE64`: تمثيل Base64 لملف التوقيع الخاص؛ لا ترسله للمحادثة.
- `KEYSTORE_PASSWORD`: كلمة مرور المخزن.
- `KEY_ALIAS`: الاسم الداخلي للمفتاح.
- `KEY_PASSWORD`: كلمة مرور المفتاح.
- `TRANSVIBE_SO_READ_TOKEN`: رمز محدود الصلاحية للوصول إلى Actions: read في مستودع TransVibe-so- لعملية نقل المكتبة الأصلية.
يمكن ضبط Repository variable باسم `TRANSVIBE_SIGNING_CERT_SHA256` لتثبيت بصمة شهادة التوقيع عبر جميع التشغيلات، بعد استخراجها من شهادة المفتاح الأصلية.

### مستودع SO
- `TRANSVIBE_ANDROID_DISPATCH_TOKEN`: fine-grained PAT له Contents: Read and write على مستودع Android المحدد، مع الصلاحية المناسبة لاستدعاء repository_dispatch (يحتاج Contents: write).
لا تستخدم نفس الرمز ككلمة مرور توقيع ولا تنشر أي رمز في سجل عام.

## إعداد GitHub
- Settings → Actions → General: اسمح بصلاحيات GITHUB_TOKEN التي يطلبها سير العمل (contents/pull-requests/actions write فقط لمهمة الدمج).
- فعّل خيار Allow GitHub Actions to create and approve pull requests في مستودع Android ليعمل استيراد المكتبات الآلي، إن كان الخيار متاحا.
- فعّل حماية main باستخدام Required status checks لاختبارات المصدر، واختبارات JVM، وفحص JNI، وإنتاج الحزمتين الموقعتين. لا تسمح بتجاوز الحماية.
- إذا تعذر دمج PR رغم نجاح المسارات فتأكد من إعدادات صلاحيات Actions والحماية؛ لا تتجاوز الفحوص.

## دورتا العمل
1. يبني SO مكتبة arm64-v8a/API 26 من مصدر JNI مطابق ويلتزم بالبصمات والاختبارات. بعد النجاح يدمج PR الداخلي تلقائيا (إذا كان Ready).
2. تشغيل SO الناجح على main يرسل dispatch لمستودع Android، إذا ضُبط الرمز.
3. يتحقق Android من هوية تشغيل SO ومن SHA-256 وJNI وELF ويُنشئ PR للمكتبة في app/libs/arm64-v8a/.
4. لأن PR ينشأ بواسطة GITHUB_TOKEN، يستدعي المستورد workflow_dispatch صراحة لتشغيل الاختبارات على فرع الاستيراد.
5. يبدأ Android بالاختبارات ثم يبني **نسختين فقط** من ملفات APK: Preview وRelease، بنفس مفتاح التوقيع. ZIP المشروع المستقل ليس نسخة APK ثالثة.
6. عند نجاح جميع الشروط يُدمج PR تلقائيا على main ويعاد بناء الحزمتين من main، ثم تنزّل حزمة المصدر للبناء عبر واجهة Code On The Go.

## بناء Code On The Go
استخرج ZIP المصدر من Artifact، وافتح جذر Gradle، واستخدم زر Build. النماذج الصوتية لا تُضمّن في المستودعات. سجّل السرعة والذاكرة والدقة والأخطاء على الهاتف. لا تبدأ النموذج الثاني قبل اعتماد نتائج Whisper Tiny.

## شرط البدء
إذا بقي main يحتوي مصدر التطبيق فقط دون مكتبة JNI الموثقة، أو لم تُضبط رموز الاتصال اللازمة، **لن يستطيع مسار النسخة النهائية المرور ولن يُدمج**. يجب معالجة عائق التكامل دون استخدام مكتبة وهمية.

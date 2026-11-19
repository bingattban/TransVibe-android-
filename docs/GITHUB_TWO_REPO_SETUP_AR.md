# إعداد ربط مستودعي TransVibe

## المستودعات العامة
- Android: https://github.com/bingattban/TransVibe-android-
- SO: https://github.com/bingattban/TransVibe-so-

لا تعتمد على أي مستودع قديم.

## ترابط آمن دون دمج تلقائي
1. لا تنفذ الخطوة التالية حتى تكمل استيراد **كامل** مصدر تطبيق Android وتنجح اختبارات Android. المصدر المبدئي وحده لا يكفي.
2. بعد اعتماد إعدادات GitHub Actions ودمجها بموافقة المالك، افتح مستودع `TransVibe-so-`، ثم Settings → Secrets and variables → Actions → New repository secret.
3. الاسم: `TRANSVIBE_ANDROID_DISPATCH_TOKEN`. القيمة: fine-grained GitHub Personal Access Token بصلاحية **Contents: Read and write** على المستودع `TransVibe-android-` وحده، مع مدة صلاحية قصيرة. لا ترسل التوكن في المحادثة ولا ترفعه في الملفات.
4. في مستودع Android افتح Settings → Actions → General ثم تحقق من السماح لـ GitHub Actions بإنشاء Pull Requests، مع تقييد صلاحيات `GITHUB_TOKEN` إلى ما يحدده كل workflow.
5. كل بناء ناجح لـ SO على `main` يرسل `repository_dispatch` إلى Android بعد وجود السر. سير العمل على Android يتحقق من هوية المصدر ونتيجة التشغيل ورقم الالتزام، ثم يحمل Artifact ويتحقق من بصمته ورموز JNI قبل نسخ `.so`.
6. النتيجة **Pull Request جديد** لا دمج تلقائي. يجب نجاح اختبارات المصدر والتجميع والتثبيت على الهاتف قبل أن يعتمد المالك الدمج.

## بناء Code On The Go
بعد أن تصبح نسخة Android صحيحة اجلب مصدر المشروع الكامل من Artifact الخاص بـ GitHub Actions، وافتح جذر Gradle في Code On The Go، ثم استخدم زر Build الرسومي. لا تشغل أدوات NDK على الهاتف.

## مرحلية النماذج
الآن Whisper Tiny فقط؛ تحفظ النماذج ببيانات التطبيق بعد التثبيت، وليس في GitHub أو `app/libs`. بعد الاختبارات العملية يقرر المالك قبول النموذج ثم الانتقال إلى التالي.

## ما لم يثبت بعد
نجاح ملفات Workflow أو بناء مكتبة `.so` لا يثبت نجاح Gradle على Android أو عمل النموذج على هاتفك. لا ترسل إشعار نجاح فعلي قبل الأدلة.

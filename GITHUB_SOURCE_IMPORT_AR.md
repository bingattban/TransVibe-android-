# رفع بقية الملفات إلى فرع Android على GitHub

المستودع: https://github.com/bingattban/TransVibe-android-
الفرع الهدف: setup/transvibe-android-foundation

يحتوي هذا ZIP على بقية مصدر التطبيق الكامل الذي لم تتوافر وسيلة لنقله إلى GitHub عبر ربط الملفات المحلي في هذه الجلسة.

1. فك الأرشيف في جذر نسخة العمل للمستودع، وليس ضمن مجلد TransVibe متداخل.
2. حافظ على الملفات الموجودة مسبقا في فرع GitHub، وخاصة app/src/main/cpp/whisper_jni.cpp وWhisperNativeBridge.kt و.github/workflows/* وأدوات الاستيراد؛ إذ يجب أن يبقى JNI المرفوع هو مرجع البناء.
3. ارفع الملفات إلى فرع setup/transvibe-android-foundation (وليس main) ثم راجع طلب الدمج #1.
4. راقب GitHub Actions. في حال فشل أي اختبار، لا تدمج. GitHub SO يجب أن يجتاز البناء أيضا.
5. بعد النجاح تنتقل نسخة المصدر إلى Code On The Go عبر واجهة Build. لا تحذف التطبيق المثبت لتجاوز التوقيع.

هذا الملف لا يشكل دليلا على نجاح Gradle أو تشغيل نموذج. يمنع الانتقال لأي نموذج قبل مراجعة سجلات الجهاز وموافقة المالك.

# الاستيراد اليدوي لمكتبات TransVibe عبر Code On The Go

هذا المجلد هو `TransVibe/app/libs/` داخل المشروع، وليس `/app/libs` في جذر الجهاز. أضف المكتبات قبل الضغط على **Build** من واجهة Code On The Go؛ لا حاجة إلى الطرفية.

```text
TransVibe/
  app/
    libs/
      sherpa-onnx-<version>.aar          ← حزمة Sherpa-ONNX لأندرويد ARM64
      arm64-v8a/
        libtransvibe_whisper.so           ← Whisper JNI حقيقية مبنية لهذا المشروع
        libggml*.so                       ← إن كانت مكتبة Whisper تعتمد عليها
        libc++_shared.so                  ← إن كانت مطلوبة وغير مدمجة
```

- ملفات `*.aar` و`*.jar` في الجذر `app/libs/` تُحمّل تلقائيا عبر Gradle. Sherpa AAR قد تحتوي `libonnxruntime.so` ومكتبة JNI، **ولا تفك** `.so` الخاصة بها إلى `arm64-v8a` حتى لا تحدث ازدواجية.
- ملفات `*.so` الجاهزة ضعها فقط في `app/libs/arm64-v8a/`، وستُضمّن داخل APK عبر `jniLibs.srcDir("libs")`. يجب أن تكون مبنية لـ **Android arm64-v8a**، وليست نسخ Linux أو Windows أو مترجم NDK التنفيذي.
- `libtransvibe_whisper.so` يجب أن تكون ترجمت من `whisper_jni.cpp` **الموجود في هذا المشروع** وربطت بـ whisper.cpp حقيقية ومتوافقة؛ نسخ ملف باسم مشابه من تطبيق آخر لا يكفي، ولو كان ELF صحيحا. وجود ملف C++ المصدر وحده في `app/libs` لا يصنع `.so`.
- النماذج السبعة (`ggml-*.bin`, `moonshine*.zip`, `sensevoice*.zip`) **لا** توضع داخل `app/libs` ولا تدمج داخل APK؛ يتم استيرادها أو تنزيلها داخل التطبيق بعد التثبيت حسب تصميم TransVibe.
- حزم الترجمة ML Kit ليست ملفات `.aar` قابلة للاستيراد يدويا هنا؛ تبقى إدارة حزم اللغات منفصلة ويحتاج أول تنزيل للإنترنت.
- يرفض فحص البناء الملف الفارغ أو الخاطئ المعمارية، ونسختين مختلفتين من Sherpa AAR، والمكتبات المتكررة بين AAR ومجلد `.so`. عدم وجود المكتبات لا يمنع Debug التشخيصي، لكنه يمنع عمل محركاتها الحقيقية.
- احفظ الرابط الرسمي للمصدر والترخيص ورقم الإصدار وبصمة SHA-256 للملفات التي تحصل عليها. لا تنسخ حزم مجهولة المصدر. قد تحتاج بعض AAR إلى اعتمادات إضافية؛ لأن Gradle لا يحصل على الـ transitive dependencies تلقائيا عند استعمال AAR محلية.
- بناء **Release على الهاتف ما زال محجوبا** ضمن قواعد المرحلة 01 السابقة حتى يتم التحقق من جميع المتطلبات والأمان والتشغيل. Debug/Preview هي المناسبة للاختبار الآن.
- إذا كان Debug يستخدم `com.transvibe.app` وتوقيعه يختلف عن المثبت، فقد يفشل الاستبدال دون المساس بالنسخة القديمة؛ استخدم Preview `com.transvibe.app.preview` إن كان خيارها ظاهرا.

**قيود التحقق:** التحقق من ELF والـ AAR شكلي فقط، ولا يثبت أن ملفات JNI المضمنة تحتوي الرموز الصحيحة أو أنها تعمل دون اعتماديات مفقودة. النجاح النهائي يحتاج بناء APK وتثبيته واختبار ASR الحقيقي على هاتف ARM64.


## Kotlin/AndroidX/ML Kit libraries

The repository always includes the verified ARM64 Whisper JNI and
native-provenance.json. AndroidX Core and ML Kit Common/Translate/Language ID
are declared explicitly and pinned by gradle/libs.versions.toml and are
merged into APK DEX files by Gradle. These dependencies are not copied to
app/libs as independent, potentially duplicate AARs. Code On The Go's LogSender
is provided by the IDE's own Debug/Preview plugin and must remain enabled.

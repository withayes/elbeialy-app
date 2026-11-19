# مقاطع البيعلي — تطبيق أندرويد

تطبيق أندرويد (Kotlin) يغلّف موقع https://elbeialy-shorts.vercel.app بتصميم إسلامي عصري:
أخضر زمردي + ذهبي، نجمة ثمانية (ربع الحزب) في الأيقونة وشاشة البداية، ونقش هندسي في شاشة عدم الاتصال.

## المميزات
- شاشة بداية بالنجمة الذهبية، وأيقونة متكيّفة (Adaptive + Monochrome لأندرويد 13).
- شريط علوي بعنوان عربي وزخرفة ذهبية، وزر الرئيسية وزر المشاركة.
- شريط تحميل ذهبي، والسحب لأسفل للتحديث.
- تشغيل الفيديو بملء الشاشة مع دوران الشاشة.
- رفع الصور من الجهاز (لوحة المدير: صورة المقطع).
- شاشة أنيقة عند انقطاع الإنترنت مع زر إعادة المحاولة.
- زر الرجوع يرجع داخل الموقع، ومرتين للخروج.
- الروابط الخارجية (واتساب، تيليجرام…) تفتح في تطبيقاتها.
- فتح روابط الموقع مباشرة في التطبيق.

## طريقة ١: بناء الـ APK تلقائيًا من GitHub (بدون تثبيت أي شيء)
1. أنشئ مستودعًا جديدًا على GitHub وارفع محتويات هذا المجلد كما هي.
2. افتح تبويب **Actions** — سيبدأ "Build APK" تلقائيًا (أو اضغط Run workflow).
3. بعد ~٥ دقائق افتح التشغيل ونزّل **ElbeialyShorts-APK** من Artifacts.
4. فك الضغط وثبّت `app-debug.apk` على الهاتف.

## طريقة ٢: Android Studio
افتح المجلد في Android Studio ← انتظر المزامنة ← Build › Build APK(s).

## النشر على Google Play
1. أنشئ مفتاح توقيع:
   `keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias elbeialy`
2. في GitHub › Settings › Secrets › Actions أضف:
   `KEYSTORE_BASE64` (ناتج `base64 -w0 release.jks`)، `KEYSTORE_PASSWORD`، `KEY_ALIAS`، `KEY_PASSWORD`.
3. سيُنتج الـ workflow ملف `.aab` موقّعًا جاهزًا للرفع على Play Console.
   احتفظ بملف release.jks وكلمة سره في مكان آمن — بدونه لا يمكن تحديث التطبيق.

## تخصيص
- الألوان: `app/src/main/res/values/colors.xml`
- اسم التطبيق والنصوص: `values/strings.xml`
- رابط الموقع: `HOME_URL` في `MainActivity.kt`
- رقم الإصدار: `versionCode` / `versionName` في `app/build.gradle.kts`

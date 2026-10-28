# بیلد برنامه در GitHub

این راهنما برای پروژه‌ی **باید (Bayad)** است. تنظیمات داخل پروژه، Gradle 8.7، Android Gradle Plugin 8.5.2، Kotlin 2.0.20، JDK 17 و Android SDK Platform 35 را تعیین می‌کنند. ساخت APK به دسترسی شبکه برای دریافت افزونه‌ها و وابستگی‌ها نیاز دارد.

## وضعیت مهم پیش از بیلد

در ZIP حاضر فایل واقعی `gradle/wrapper/gradle-wrapper.jar` وجود ندارد. فایل `gradle-wrapper.jar.note.txt` فقط یادداشت است. اسکریپت `gradlew` فعلی نیز wrapper تولیدشده‌ی استاندارد نیست و فقط Gradle نصب‌شده را صدا می‌زند. پس `./gradlew assembleDebug` از این ZIP به‌تنهایی قابل اتکا نیست.

سه ایراد نحوی آشکار Kotlin پیش از بارگذاری اصلاح شده‌اند:

1. متن چندخطی `HelpDialog` در `MainActivity.kt` اکنون از رشته‌ی سه‌تایی Kotlin استفاده می‌کند.
2. الگوهای Regex در `LocalSafety.kt` اکنون raw string هستند تا `\s` و `\p` به‌درستی به موتور Regex برسند.
3. حذف نویسه‌ی تطویل در تابع نرمال‌سازی اکنون از `replace("ـ", "")` استفاده می‌کند؛ Kotlin مقدار `Char` خالی ندارد.

این اصلاح‌ها موانع شناخته‌شده را برطرف می‌کنند، اما تا اجرای موفق GitHub Actions، کامپایل واقعی تأییدشده محسوب نمی‌شود.

## روش پیشنهادی: GitHub Actions

پوشه‌ی `.github/workflows/android.yml` همراه پروژه است. آن workflow، به‌جای wrapper ناقص، Gradle 8.7 را به‌طور مستقل فراهم می‌کند و فرمان زیر را اجرا می‌کند:

```bash
gradle assembleDebug
```

1. ریشه‌ی پروژه (پوشه‌ای که `settings.gradle.kts` دارد) را در مخزن GitHub بارگذاری کنید. نمونه‌ی دستورات:

   ```bash
   git init
   git add .
   git commit -m "Prepare Bayad Android project"
   git branch -M main
   git remote add origin https://github.com/USERNAME/REPOSITORY.git
   git push -u origin main
   ```

   `USERNAME/REPOSITORY` را با نشانی مخزن خود عوض کنید. اگر مخزن از قبل Git دارد، `git init` لازم نیست.
2. در GitHub به زبانه‌ی **Actions** بروید و workflow با نام **Android build** را باز کنید. پس از push به `main` یا ایجاد pull request خودکار اجرا می‌شود؛ می‌توانید از **Run workflow** نیز استفاده کنید.
3. اگر بیلد موفق شد، APK دیباگ به‌صورت artifact با نام `bayad-debug-apk` در صفحه‌ی اجرای workflow در دسترس است. خروجی محلی فرمان `gradle assembleDebug` در `app/build/outputs/apk/debug/` قرار می‌گیرد.

Workflow ابزارهای پروژه را با مقادیر تنظیم‌شده در Gradle (JDK 17، Gradle 8.7، SDK 35) هماهنگ می‌کند. نتیجه‌ی واقعی را در log اجرای Actions بررسی کنید.

## تکمیل Gradle Wrapper برای اجرای ./gradlew

روی رایانه‌ای که Gradle نصب است، در ریشه‌ی پروژه اجرا کنید:

```bash
gradle wrapper --gradle-version 8.7
./gradlew --version
./gradlew assembleDebug
```

در Windows، فرمان wrapper را با `gradle` در PowerShell یا Command Prompt اجرا کنید و سپس `gradlew.bat --version` و `gradlew.bat assembleDebug` را به‌کار ببرید. Gradle باید فایل واقعی `gradle/wrapper/gradle-wrapper.jar` و اسکریپت‌های wrapper را تولید کند. وجود JAR را بررسی کنید، فایل‌های تولیدشده را به Git اضافه/commit کنید و پس از اطمینان از اجرای `./gradlew --version`، می‌توانید workflow را نیز به `./gradlew assembleDebug` تغییر دهید. Android Studio را می‌توان برای بازکردن و sync پروژه با Gradle 8.7 به‌کار برد؛ اما یادداشت متنی JAR را جایگزین نکنید و خودتان JAR نسازید.

## اجرای محلی با Gradle نصب‌شده

پس از آماده‌بودن ابزارهای Gradle 8.7 و Android SDK Platform 35، از ریشه پروژه اجرا کنید:

```bash
gradle assembleDebug
```

در وضعیت ZIP حاضر این دستور (یا workflow) هنوز **بیلد تأییدشده** به شمار نمی‌آید. `BUILD_STATUS.txt` جزئیات محدودیت‌ها را ثبت می‌کند.

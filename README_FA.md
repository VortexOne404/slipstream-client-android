
# اسلیپ‌استریم کلاینت برای اندروید

یک **کلاینت VPN برای اندروید** که با الهام از معماری‌های Slipstream طراحی شده است.
این برنامه تمام ترافیک سیستم را با استفاده از `VpnService` (رابط TUN) دریافت کرده و آن را از طریق **tun2socks** به لایه انتقال Slipstream (مانند SOCKS5) ارسال می‌کند.

---

## به‌روزرسانی

برای افزایش سرعت و بهبود عملکرد، در نسخه جدید وابستگی قبلی:

https://github.com/Mygod/slipstream-rust

با نسخه بهینه‌شده زیر جایگزین شده است:

https://github.com/Fox-Fig/slipstream-rust-plus

**بهبودها:**
- افزایش توان عبوری
- کاهش تأخیر
- پایداری بهتر اتصال
- استفاده بهینه‌تر از منابع

---

## مسیر پروتکل

```
برنامه‌های کاربر
      ↓
   tun2socks
      ↓
 Slipstream Client
      ↓
   پردازش DNS
      ↓
 Slipstream Server
      ↓
 دسترسی به اینترنت
```

---

## وضعیت

![Latest Release](https://img.shields.io/github/v/release/VortexOne404/slipstream-client-android)
![Downloads](https://img.shields.io/github/downloads/VortexOne404/slipstream-client-android/total)
![Stars](https://img.shields.io/github/stars/VortexOne404/slipstream-client-android)
![Forks](https://img.shields.io/github/forks/VortexOne404/slipstream-client-android)

---

## تصاویر

<p float="left">
  <img src="docs/screenshots/Screenshot_20260131_023837_Slipstream%20Client.png" width="220" />
  <img src="docs/screenshots/Screenshot_20260131_023903_Slipstream%20Client.png" width="220" />
  <img src="docs/screenshots/Screenshot_20260131_023926_Slipstream%20Client.png" width="220" />
  <img src="docs/screenshots/Screenshot_20260131_023940_Slipstream%20Client.png" width="220" />
  <img src="docs/screenshots/Screenshot_20260131_024504_Slipstream%20Client.png" width="220" />
</p>

---

## معرفی

**Slipstream Client** یک کلاینت VPN سبک و ماژولار برای اندروید است که:

- تمام ترافیک دستگاه را از طریق `VpnService` دریافت می‌کند
- بسته‌های IP (TCP / UDP / DNS) را با استفاده از tun2socks به استریم تبدیل می‌کند
- ترافیک را به سرور Slipstream ارسال می‌کند
- از حالت تونل کامل (Full Tunnel) پشتیبانی می‌کند

هدف طراحی، یک پوسته سبک اندرویدی با یک هسته شبکه قابل توسعه و قابل تعویض است.

---

## راه‌اندازی سرور

برای راه‌اندازی سرور از پروژه زیر استفاده کنید:

https://github.com/AliRezaBeigy/slipstream-rust-deploy

نصب با یک دستور:

```
bash <(curl -Ls https://raw.githubusercontent.com/AliRezaBeigy/slipstream-rust-deploy/master/slipstream-rust-deploy.sh)
```

---

## معماری

```
برنامه‌ها
   ↓
VpnService (TUN)
   ↓
tun2socks
   ↓
هسته Slipstream Client
   ↓
DNS
   ↓
Slipstream Server
   ↓
اینترنت
```

---

## امکانات

- حالت تونل کامل
- پشتیبانی از SOCKS5
- طراحی ماژولار
- وارد/خروج تنظیمات با URI: `slipstream://{base64}`
- مناسب برای توسعه و آزمایش ترنسپورت‌های جدید

> توجه: پشتیبانی از UDP و IPv6 به پیاده‌سازی tun2socks و نوع ترنسپورت بستگی دارد.  
> برای جلوگیری از نشت DNS، تنظیم صحیح DNS توصیه می‌شود.

---

## پیش‌نیازها

- اندروید 8.0 یا بالاتر
- Android Studio
- Android NDK
- یک سرور Slipstream با SOCKS5

---

## ساخت

```bash
git clone --recurse-submodules https://github.com/VortexOne404/slipstream-client-android.git
cd slipstream-client-android
./gradlew assembleRelease
```

---

## پشتیبانی

Telegram: https://t.me/VortexOne
Telegram Channel: https://t.me/silk_road_community

---

## مجوز

MIT

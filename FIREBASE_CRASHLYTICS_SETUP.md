# 🔥 Firebase Crashlytics - Remote Crash Monitoring Setup

## ✅ Sudah Diimplementasi

App sudah diintegrasikan dengan **Firebase Crashlytics** untuk remote crash monitoring GRATIS!

---

## 📋 Cara Setup Firebase (5 Menit)

### Step 1: Buat Firebase Project

1. **Buka**: https://console.firebase.google.com/
2. **Klik**: "Add project" atau "Tambah project"
3. **Nama project**: Bebas (contoh: "App Automation Crash")
4. **Google Analytics**: Optional (bisa disable kalau mau cepat)
5. **Klik**: "Create project"

### Step 2: Tambah Android App

1. Di Firebase Console, klik **⚙️ (Settings)** → **Project settings**
2. Scroll ke bawah, klik **"Add app"** → Pilih **Android** (icon robot)
3. **Android package name**: `com.appautomation` (PENTING: harus sama!)
4. **App nickname**: Bebas (contoh: "App Automation")
5. **Debug signing certificate SHA-1**: Skip (kosongkan)
6. Klik **"Register app"**

### Step 3: Download google-services.json

1. Setelah register app, akan ada tombol **"Download google-services.json"**
2. **DOWNLOAD** file tersebut
3. **REPLACE** file `/Users/mac/Desktop/app-test/app/google-services.json` dengan file yang baru didownload
4. **PENTING**: File ini harus ada di folder `app/` (sejajar dengan `build.gradle.kts`)

```bash
# Pastikan file ada di sini:
/Users/mac/Desktop/app-test/app/google-services.json
```

### Step 4: Build APK

```bash
cd /Users/mac/Desktop/app-test
./gradlew clean
./gradlew assembleDebug
```

APK akan ada di: `app/build/outputs/apk/debug/app-debug.apk`

### Step 5: Install & Test

```bash
# Install ke device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Atau kirim APK via email/WhatsApp ke adik kamu
```

### Step 6: Monitor Crashes

1. **Buka**: https://console.firebase.google.com/
2. **Pilih project** yang tadi dibuat
3. **Klik**: "Crashlytics" di menu sebelah kiri
4. **Tunggu** adik kamu buka app (kalau crash, akan muncul di sini dalam 5-10 menit)

---

## 📊 Apa yang Akan Kamu Lihat di Dashboard?

Saat app crash di HP Motorola adik kamu, Firebase Crashlytics akan menampilkan:

### 1. **Device Info**
- Manufacturer: `motorola`
- Model: `motorola e6 play` (atau model spesifik)
- Android Version: `9.0` (atau versi yang dipakai)
- RAM, Storage, dll

### 2. **Crash Details**
- **Exception type**: `java.lang.RuntimeException`, `NullPointerException`, dll
- **Error message**: Pesan error lengkap
- **Stack trace**: Baris kode mana yang crash
- **Thread**: Thread mana yang crash

### 3. **Custom Keys** (Info tambahan)
- `manufacturer`: vivo/motorola/samsung
- `model`: Model HP lengkap
- `android_version`: Versi Android
- `api_level`: API level

### 4. **Logs** (Breadcrumbs)
- Logs sebelum crash
- "App started on motorola..."
- "ForegroundService onCreate..."
- dst.

### 5. **Timeline**
- Kapan crash terjadi
- Berapa kali crash di device yang sama
- Berapa % user yang terkena

---

## 🎯 Contoh Dashboard Crashlytics

```
═══════════════════════════════════════
📊 CRASH REPORT
═══════════════════════════════════════

🔴 RuntimeException
   in AutomationForegroundService.onCreate()
   
📱 Device: motorola motorola e6 play
   Android: 9.0 (API 28)
   
📜 Stack Trace:
   at com.appautomation.service.AutomationForegroundService.onCreate(Line 38)
   at android.app.ActivityThread.handleCreateService()
   ...
   
🔑 Custom Keys:
   manufacturer: motorola
   model: motorola e6 play
   android_version: 9.0
   api_level: 28
   error_location: ForegroundService.onCreate
   
📝 Logs:
   [10:30:15] App started on motorola motorola e6 play
   [10:30:16] ForegroundService onCreate - Device: motorola...
   [10:30:16] ❌ Failed to start foreground
   
═══════════════════════════════════════
```

---

## 🚀 Keuntungan Firebase Crashlytics

### ✅ **Gratis 100%**
- Unlimited crashes
- Unlimited devices
- Unlimited users

### ✅ **Real-time**
- Crash langsung muncul dalam 5-10 menit
- Auto-refresh dashboard

### ✅ **Email Notification**
- Bisa setup email alert saat ada crash baru
- Settings → Crashlytics → Alerts

### ✅ **Grouping Otomatis**
- Crash yang sama di-group jadi satu
- Lihat berapa device yang terkena

### ✅ **Priority Ranking**
- Crash yang paling sering di atas
- Langsung tau mana yang urgent

### ✅ **Device Distribution**
- Lihat crash per device
- Motorola vs Samsung vs VIVO

---

## 🔧 Troubleshooting

### ❓ Crash tidak muncul di dashboard?

**Checklist:**
1. ✅ File `google-services.json` sudah benar?
2. ✅ Package name di Firebase = `com.appautomation`?
3. ✅ App sudah di-build ulang setelah setup Firebase?
4. ✅ Device ada internet?
5. ✅ Tunggu 5-10 menit (crash tidak instant muncul)

### ❓ Build error "google-services.json not found"?

```bash
# Pastikan file ada di sini:
ls -la /Users/mac/Desktop/app-test/app/google-services.json

# Kalau tidak ada, download lagi dari Firebase Console
```

### ❓ Mau test crash manual?

Tambahkan tombol test crash di app (temporary):

```kotlin
Button(onClick = {
    throw RuntimeException("Test Crash from Motorola E6")
}) {
    Text("Test Crash")
}
```

Build → Install → Tap button → App crash → Cek Firebase Console setelah 5 menit.

---

## 📧 Email Alert Setup (Optional)

1. **Buka**: Firebase Console → Crashlytics
2. **Klik**: Tab "Alerts" di atas
3. **Enable**: "Email alerts for new issues"
4. **Tambah email**: Email kamu
5. **Save**

Sekarang setiap ada crash baru, kamu akan dapat email notifikasi!

---

## 🎯 Next Steps

1. **Setup Firebase** (5 menit)
2. **Replace `google-services.json`** dengan file asli dari Firebase
3. **Build APK** baru
4. **Kirim ke adik** via email/WhatsApp
5. **Tunggu crash report** di Firebase Console
6. **Analisa** dan fix based on crash report

---

## 📱 APK Distribution

### Via Email
```
Subject: App Test - Tolong Install & Buka

Hai,
Install APK ini di HP Motorola kamu.
Buka app-nya, kalau crash juga gpp, aku bisa lihat error-nya dari sini.
Makasih!
```

### Via WhatsApp
- Kirim file APK langsung
- Minta adik install & buka
- Tunggu crash report di Firebase

---

## ✅ Summary

**Sudah Implemented:**
- ✅ Firebase Crashlytics integration
- ✅ Custom device info logging
- ✅ Crash tracking di critical paths
- ✅ Non-fatal error tracking

**Perlu Setup (5 menit):**
1. Buat Firebase project
2. Download `google-services.json`
3. Replace file di project
4. Build & distribute APK

**Dashboard:**
- https://console.firebase.google.com/
- Tab: Crashlytics
- Lihat crash real-time dari Motorola E6

---

## 🆘 Need Help?

Kalau ada error saat setup, screenshot error-nya dan kirim ke sini!

**Common files:**
- `/Users/mac/Desktop/app-test/app/google-services.json` (replace this!)
- `/Users/mac/Desktop/app-test/app/build.gradle.kts` (sudah di-setup)
- `/Users/mac/Desktop/app-test/build.gradle.kts` (sudah di-setup)

**Firebase Console:**
- https://console.firebase.google.com/

🔥 **Happy Debugging!** 🔥

# Quick Start Guide - App Automation

## 🚀 Setup Cepat

### 1. Prerequisites
- Android Studio (versi terbaru)
- JDK 17
- Android SDK 34
- Device Android dengan API 26+ (Android 8.0+)

### 2. Open Project
```bash
# Clone atau buka project
cd /Users/mac/Desktop/app-test

# Buka dengan Android Studio
# File → Open → Pilih folder app-test
```

### 3. Sync Project
- Android Studio akan otomatis sync Gradle
- Tunggu sampai selesai (pertama kali bisa lama karena download dependencies)

### 4. Build & Install

#### Option A: Via Android Studio
1. Connect device via USB
2. Enable USB Debugging di device
3. Click Run (▶️) di Android Studio
4. Pilih device
5. Wait for installation

#### Option B: Via Command Line
```bash
# Build Debug APK
./build.sh
# Pilih option 1

# Install ke device
./build.sh
# Pilih option 3
```

### 5. Setup Permissions

Setelah install, buka app:

1. **Accessibility Service**
   - App akan redirect ke Settings
   - Cari "App Automation"
   - Toggle ON
   - Kembali ke app

2. **Usage Stats**
   - App akan redirect ke Settings
   - Cari "App Automation"
   - Toggle ON
   - Kembali ke app

3. **Battery Optimization** (Optional)
   - Tap "Open Settings"
   - Pilih "Don't optimize"

### 6. Mulai Automasi

1. **Select Apps**
   - Centang aplikasi yang ingin di-automate
   - Tap durasi (misal "7 min") untuk ubah
   - Gunakan slider set durasi (1-60 menit)

2. **Start**
   - Tap "Start Automation"
   - App akan pindah ke Monitoring screen
   - Notification muncul di status bar

3. **Monitor**
   - Lihat app yang sedang berjalan
   - Progress bar dan countdown timer
   - List antrian app berikutnya

4. **Control**
   - **Pause**: Jeda automation
   - **Resume**: Lanjutkan dari pause
   - **Stop**: Hentikan automation

## 🎮 Cara Kerja

1. **Launch App**: App target dibuka otomatis
2. **Run Duration**: App berjalan sesuai durasi (misal 7 menit)
3. **Auto Interaction**: Setiap 15 detik, gesture random (tap/scroll)
4. **Next App**: Setelah durasi selesai, lanjut ke app berikutnya
5. **Complete**: Setelah semua app selesai, automation complete

## 📱 Testing

### Test dengan 2-3 Apps Dulu
```
Contoh test:
1. Chrome - 2 menit
2. Instagram - 2 menit  
3. WhatsApp - 2 menit
Total: 6 menit
```

### Observe Behavior
- ✅ App membuka otomatis?
- ✅ Layar bergerak sendiri (tap & scroll)?
- ✅ Timer countdown berjalan?
- ✅ Pindah ke app berikutnya setelah durasi habis?
- ✅ Notification update real-time?

## 🔧 Troubleshooting

### Build Error
```bash
# Clean dan rebuild
./gradlew clean
./gradlew assembleDebug
```

### Gradle Sync Failed
- File → Invalidate Caches / Restart
- Check internet connection
- Update Android Studio

### App Crashes on Launch
- Check Logcat di Android Studio
- Pastikan minSDK device >= 26
- Reinstall app

### Accessibility Service Tidak Aktif
- Go to device Settings manually
- Settings → Accessibility → Downloaded apps
- Enable "App Automation"

### Gestures Tidak Bekerja
- Pastikan Accessibility Service enabled
- Beberapa app block accessibility (normal)
- Try dengan app lain (Chrome, Instagram, dll)

## 📊 Project Structure Overview

```
app-test/
├── app/
│   ├── src/main/
│   │   ├── java/com/appautomation/    # Source code
│   │   ├── res/                        # Resources (layouts, strings, etc)
│   │   └── AndroidManifest.xml         # App configuration
│   ├── build.gradle.kts                # App-level Gradle
│   └── proguard-rules.pro              # ProGuard configuration
├── build.gradle.kts                     # Project-level Gradle
├── settings.gradle.kts                  # Project settings
├── gradle.properties                    # Gradle properties
├── build.sh                            # Build script
└── README.md                           # Documentation
```

## 🎯 Next Steps

1. **Test dengan berbagai apps**
2. **Adjust durasi sesuai kebutuhan**
3. **Monitor battery usage**
4. **Customize gesture interval jika perlu**

## 💡 Tips

- 🔋 Charge device saat running automation lama
- 📱 Keep screen on (Developer Options → Stay awake when charging)
- 🔕 Disable notifications untuk fokus test
- 📊 Check logs di Logcat untuk debug
- ⚡ Start dengan durasi pendek (2-3 menit) untuk test

## 📞 Support

Jika ada masalah:
1. Check Logcat di Android Studio
2. Review README.md untuk detail
3. Check permissions granted
4. Try clean & rebuild

Happy Testing! 🎉

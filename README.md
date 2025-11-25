# App Automation - Android App

Aplikasi Android untuk menjalankan aplikasi secara otomatis dan berurutan dengan durasi yang dapat dikonfigurasi.

## Fitur Utama

✨ **Otomasi Aplikasi**: Buka dan jalankan aplikasi secara otomatis dalam urutan antrian
⏱️ **Durasi Kustom**: Tetapkan durasi untuk setiap aplikasi (default 7 menit)
🎮 **Interaksi Otomatis**: Layar bergerak sendiri dengan tap dan scroll acak
📊 **Monitoring Real-time**: Pantau progress dan waktu tersisa
⏸️ **Kontrol Penuh**: Pause, resume, atau stop automation kapan saja
🔔 **Notifikasi Persistent**: Pantau status di notification bar

## Teknologi

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt/Dagger
- **Database**: Room
- **Async**: Kotlin Coroutines + Flow
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)

## Struktur Proyek

```
app/src/main/java/com/appautomation/
├── data/
│   ├── local/          # Room database
│   ├── model/          # Data classes
│   └── repository/     # Data repository
├── di/                 # Dependency injection modules
├── presentation/
│   ├── ui/            # Compose screens
│   ├── viewmodel/     # ViewModels
│   └── theme/         # UI theming
├── service/
│   ├── AppLauncher.kt              # App launching logic
│   ├── AppMonitor.kt               # Foreground app detection
│   ├── AutomationAccessibilityService.kt  # Gesture automation
│   ├── AutomationManager.kt        # Main orchestrator
│   └── AutomationForegroundService.kt     # Foreground service
└── util/              # Utilities & constants
```

## Cara Menggunakan

### 1. Build Aplikasi

```bash
# Dari direktori proyek
./gradlew assembleDebug

# Atau buka di Android Studio dan build
```

### 2. Izin yang Diperlukan

Aplikasi memerlukan 2 izin utama:

1. **Accessibility Service**
   - Buka Settings → Accessibility
   - Temukan "App Automation"
   - Aktifkan service

2. **Usage Stats Access**
   - Buka Settings → Apps → Special Access → Usage Access
   - Temukan "App Automation"
   - Berikan izin

3. **Battery Optimization** (Opsional tapi direkomendasikan)
   - Nonaktifkan battery optimization untuk App Automation
   - Memastikan automation berjalan lancar di background

### 3. Menggunakan Aplikasi

1. **Pilih Aplikasi**
   - Buka aplikasi
   - Pilih aplikasi yang ingin di-automate (checkbox)
   - Set durasi untuk setiap aplikasi (tap tombol "X min")
   - Tap "Start Automation"

2. **Monitor Progress**
   - Lihat aplikasi yang sedang berjalan
   - Monitor waktu tersisa
   - Lihat antrian aplikasi berikutnya
   - Gunakan Pause/Stop sesuai kebutuhan

3. **Automation Berjalan**
   - Aplikasi akan dibuka secara otomatis
   - Layar akan bergerak sendiri (tap & scroll random setiap 15 detik)
   - Setelah durasi selesai, lanjut ke aplikasi berikutnya
   - Notifikasi persisten menampilkan status

## Komponen Utama

### AutomationManager
Orkestrator utama yang mengelola:
- Queue aplikasi
- Timer untuk setiap aplikasi
- State management (Running, Paused, Completed, Error)
- Retry logic jika app gagal launch

### AutomationAccessibilityService
Service untuk:
- Melakukan gesture (tap, swipe, scroll)
- Interaksi otomatis random
- Press tombol (Back, Home, Recents)

### AppLauncher
Menangani:
- Launch aplikasi via package name
- Query installed apps
- Cek app installation status

### AppMonitor
Monitor aplikasi dengan:
- UsageStatsManager untuk deteksi foreground app
- Cek permission usage stats

### AutomationForegroundService
Foreground service untuk:
- Keep process alive
- Update notification dengan progress
- Handle pause/resume/stop dari notification

## Konfigurasi

### Default Settings (Constants.kt)
```kotlin
DEFAULT_DURATION_MINUTES = 7           // Durasi default per app
MIN_DURATION_MINUTES = 1               // Durasi minimum
MAX_DURATION_MINUTES = 60              // Durasi maksimum
DEFAULT_INTERACTION_INTERVAL_SECONDS = 15  // Interval gesture random
```

### Customize Gesture Behavior
Edit `AutomationAccessibilityService.kt`:
- `startRandomInteractions(intervalSeconds)` - Ubah interval
- `performRandomGesture()` - Ubah jenis gesture
- Tambah gesture custom sesuai kebutuhan

## Troubleshooting

### Aplikasi tidak buka otomatis
- ✅ Pastikan Accessibility Service enabled
- ✅ Cek Usage Stats permission granted
- ✅ Coba restart device
- ✅ Nonaktifkan battery optimization

### Gesture tidak bekerja
- ✅ Accessibility Service harus aktif
- ✅ Cek permission `canPerformGestures` dalam accessibility_service_config.xml
- ✅ Beberapa app mungkin block accessibility gestures (security)

### Service terhenti di background
- ✅ Nonaktifkan battery optimization
- ✅ Foreground service seharusnya prevent kill
- ✅ Cek notification masih muncul

### App tidak terdeteksi di foreground
- ✅ Pastikan Usage Stats permission
- ✅ UsageStatsManager butuh beberapa detik untuk update
- ✅ Grace period 3 detik setelah launch

## Development

### Build Debug APK
```bash
./gradlew assembleDebug
```

### Build Release APK
```bash
./gradlew assembleRelease
```

### Run Tests
```bash
./gradlew test
```

### Install on Device
```bash
./gradlew installDebug
```

## Catatan Penting

⚠️ **Accessibility Service**: Aplikasi ini menggunakan Accessibility Service untuk automation. Tidak ada data yang dikumpulkan atau dikirim.

⚠️ **Battery Usage**: Automation yang berjalan lama dapat menguras baterai. Pastikan device tercharge saat running.

⚠️ **App Compatibility**: Beberapa aplikasi mungkin tidak support automation (game anti-cheat, banking apps dengan security tinggi).

⚠️ **Android Version**: Tested pada Android 8.0+. Mungkin ada perbedaan behavior antar versi.

## Future Enhancements

- [ ] Custom gesture patterns per app
- [ ] Scheduling (jalankan automation pada waktu tertentu)
- [ ] Loop mode (repeat sequence)
- [ ] Export/import app configurations
- [ ] Detailed logs & statistics
- [ ] Dark mode
- [ ] Localization (bahasa lainnya)

## License

This project is for educational and testing purposes.

## Support

Jika menemukan bug atau punya saran, silakan buat issue di repository ini.

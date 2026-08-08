# SPEC & PLAN — Fix bug korektnes inti + enable R8

Tanggal: 2026-08-08 · Scope dipilih user: (1) bug korektnes inti, (2) enable R8/minify release.

## SPEC

### Masalah
Audit 3-sudut menemukan bug korektnes pada inti otomasi:
- State mutable di `AutomationAccessibilityService` di-share antar thread Main & Default
  tanpa `@Volatile`/sinkronisasi → flag `isGesturePaused` bisa nyangkut, tap di app/browser salah.
- `performRandomGesture` (loop tiap 500ms) memulai coroutine relaunch baru tiap tick tanpa
  debounce/dedup → relaunch storm & flicker.
- `AutomationManager.resumeAutomation/startRatingAll` reassign job tanpa join job lama →
  terminal-state job lama (`Idle`) bisa menimpa state `Running`/`RatingRunning` baru (race).
- `isRunning()` tak mencakup `RatingRunning`.
- Node finder rating (`findRatingBar/findEditText/findPostButton`) tak cek `isVisibleToUser`
  → bisa tap elemen off-screen/stale.
- `AppSelectionViewModel` membaca SharedPreferences (disk I/O) di main thread saat init.
- `getFilteredApps()` = dead code (layar sudah filter reaktif sendiri).

Selain itu release APK unminified (`isMinifyEnabled=false`) → besar & mudah di-reverse.

### Ruang lingkup (in-scope)
Perbaikan kode di: `AutomationAccessibilityService.kt`, `AutomationManager.kt`,
`AppSelectionViewModel.kt`, dan `app/build.gradle.kts` (R8). Verifikasi lewat unit test
existing + build release.

### Di luar cakupan
Firestore rules, redesain penyimpanan kredensial, perubahan permission Manifest,
`AppMonitor` queryEvents, penambahan unit test baru (dipilih terpisah oleh user).

### Kriteria selesai
- `./gradlew test` hijau (ResumePlan/ProgressCalculator/ReviewPicker tetap lulus).
- `./gradlew assembleRelease` sukses dengan minify aktif; APK ter-sign & terverifikasi.
- Tidak ada regresi perilaku resume (logika `buildResumePlan` tak diubah).
- Shared state lintas-thread aman (@Volatile); relaunch di gesture loop ter-debounce.

## PLAN (per task = 1 commit)

- [ ] T1. Thread-safety `AutomationAccessibilityService`: tandai state share `@Volatile`,
      konsolidasi relaunch di `performRandomGesture` ke helper ber-debounce.
      AC: tak ada `var` share tanpa @Volatile; kedua cabang (browser & wrong-app) pakai helper.
- [ ] T2. `AutomationManager`: join job lama sebelum relaunch (hilangkan state stomp);
      `isRunning()` cakup `Running` + `RatingRunning`.
      AC: `launchRun`/rating menunggu `cancelAndJoin()` job sebelumnya.
- [ ] T3. Rating finder wajib `isVisibleToUser`.
      AC: `findRatingBar/findEditText/findPostButton` menolak node tak terlihat.
- [ ] T4. `AppSelectionViewModel`: pindah load prefs awal ke Dispatchers.IO; hapus dead
      `getFilteredApps()`.
      AC: init tak baca disk di main; build tetap kompilasi.
- [ ] T5. Enable R8: `isMinifyEnabled=true` + `isShrinkResources=true` untuk release;
      pastikan proguard cukup (Gson/Firestore/Room/Hilt).
      AC: `assembleRelease` sukses & APK jalan (smoke: verifikasi signature).

Review 5-sumbu setelah build (tahap review).

# Spec: Perbaikan Bug Android App (`app/`)

> **Scope:** Hanya Android app (`com.appautomation`). Bug pada `admin-web/` **diabaikan** sesuai instruksi.
> Semua temuan di bawah sudah diverifikasi langsung pada source code (path + baris dicantumkan).

## Objective

Memperbaiki bug yang sudah teridentifikasi pada Android automation app agar:
- Tidak crash saat menjalankan otomasi (divide-by-zero, node recycle).
- Pause/Resume bekerja benar tanpa kehilangan antrian app.
- Tidak ada memory leak dari service yang berjalan lama.
- UI thread tidak terblokir saat gesture berjalan (mencegah ANR).

**User:** Operator yang menjalankan otomasi multi-app via Accessibility Service.
**Success looks like:** Satu sesi otomasi multi-app bisa di-pause lalu di-resume dan menyelesaikan SELURUH antrian, tanpa crash, tanpa ANR, tanpa leak.

## Tech Stack

Tidak ada perubahan: Kotlin 1.9.20, Jetpack Compose (Material 3), Hilt 2.48, Room 2.6.1, Coroutines, Foreground/Accessibility Service, Firebase Crashlytics.

## Commands

```
Build debug:   ./gradlew assembleDebug
Unit test:     ./gradlew test
Instrumented:  ./gradlew connectedAndroidTest
Lint:          ./gradlew lint
Build AAB:     ./gradlew bundleRelease
```

## Project Structure (file yang tersentuh)

```
app/src/main/java/com/appautomation/service/
  AutomationManager.kt              → Bug #1 (pause/resume), #7 (state Completed)
  AutomationForegroundService.kt    → Bug #2 (divide-by-zero), #6 (wakelock)
  FloatingTimerService.kt           → Bug #3 (leaked scope)
  AutomationAccessibilityService.kt → Bug #4 (main-thread walk), #5 (node recycle)
app/src/test/java/com/appautomation/ → Unit test baru (lihat Testing Strategy)
```

## Daftar Bug & Acceptance Criteria

### 🔴 Bug #1 — Pause/Resume menghapus antrian app (CRITICAL)
**Lokasi:** `AutomationManager.kt:273-285` (`resumeAutomation`), interaksi dengan `startAutomation:58-84` & `stopAutomation:290-304`.
**Masalah:** `resumeAutomation()` memanggil `startAutomation(listOf(app))` — hanya 1 app yang di-pause. Sisa antrian (`queue` pada state `Running`) hilang permanen. Selain itu `startAutomation()` memanggil `stopAutomation()` yang me-reset `isPaused=false` & `pausedApp=null`, sehingga branch restore di `runAutomation:94-99` tidak pernah jalan.
**Acceptance:**
- Saat pause di app ke-2 dari antrian 5 app, resume melanjutkan app ke-2 (dengan sisa durasi) **lalu** lanjut app ke-3, 4, 5.
- `elapsedTimeMillis` tetap kontinu (tidak reset) setelah resume.
- `completedCount` tidak mundur setelah resume.

### 🔴 Bug #2 — Divide-by-zero pada progress notifikasi (CRITICAL/crash)
**Lokasi:** `AutomationForegroundService.kt:82`.
**Masalah:** `(durationMillis - remainingTimeMillis) * 100 / durationMillis` → `ArithmeticException` bila `durationMillis == 0`. Resume yang memendekkan `durationMillis` (Bug #1) memperbesar risiko.
**Acceptance:**
- Bila `durationMillis <= 0`, progress = 0 dan tidak ada crash.
- Notifikasi tetap update normal untuk durasi valid.

### 🟠 Bug #3 — Memory leak: `FloatingTimerService.serviceScope` tidak dibatalkan (HIGH)
**Lokasi:** `FloatingTimerService.kt:243-246` (`onDestroy`), scope di `:34`, collector di `:204-232`.
**Masalah:** `onDestroy()` hanya memanggil `hideFloatingBubble()`, tidak `serviceScope.cancel()`. Collector `collectLatest` terus hidup, menahan referensi View. Setiap show/hide menambah leak.
**Acceptance:**
- `onDestroy()` memanggil `serviceScope.cancel()`.
- Setelah service dihancurkan, tidak ada collector aktif yang menahan `timerText`/`appNameText`.

### 🟠 Bug #4 — Gesture loop & tree-walk di main thread (HIGH/ANR)
**Lokasi:** `AutomationAccessibilityService.kt:46` (`Dispatchers.Main`), loop `:240-252`, `findClickableNodes:710-726` (rekursif depth 15).
**Masalah:** Penelusuran pohon node rekursif tiap 500ms berjalan di main thread → blokir UI looper, berpotensi ANR (terutama di device kelas bawah).
**Acceptance:**
- `dispatchGesture()` tetap dipanggil dari main thread (syarat API).
- Pencarian/penelusuran node (`findClickableNodes` dkk) berjalan di luar main thread.
- Tidak ada ANR saat otomasi berjalan ≥ 5 menit.

### 🟠 Bug #5 — Potensi `recycle()` ganda pada AccessibilityNodeInfo (HIGH/crash)
**Lokasi:** `AutomationAccessibilityService.kt` — mis. `:463-470`, `:498-505`, `:546-553`, `:680-698`.
**Masalah:** Node yang dikumpulkan dari sub-tree di-`recycle()`, lalu `rootNode.recycle()` juga dipanggil — berisiko recycle node yang tumpang tindih / sudah di-recycle → `IllegalStateException` di sebagian OEM.
**Acceptance:**
- Setiap node di-recycle paling banyak sekali; tidak me-recycle node beserta ancestornya dua kali.
- Tidak ada crash recycle pada sesi gesture berulang.

### 🟡 Bug #6 — WakeLock di-acquire ulang tanpa release (MEDIUM)
**Lokasi:** `AutomationForegroundService.kt:173-186` (`acquireWakeLock`).
**Masalah:** `acquireWakeLock` menimpa `wakeLock` tanpa release yang lama → potensi leak `PARTIAL_WAKE_LOCK` 10 jam bila service restart (START_STICKY).
**Acceptance:**
- Sebelum acquire baru, wakelock lama (jika `isHeld`) di-release.

### 🟡 Bug #7 — State `Completed` membawa `completedCount` tapi dilabel `totalApps` (MEDIUM/UI salah)
**Lokasi:** `AutomationManager.kt:39` (`Completed(totalApps)`) & `:213` (`Completed(completedCount)`); ditampilkan di `MonitoringScreen`.
**Masalah:** Saat ada app yang gagal/di-skip, layar selesai menampilkan jumlah yang menyesatkan (completed dilabel total).
**Acceptance:**
- State `Completed` membawa `completedCount` **dan** `totalCount`; UI menampilkan "X dari Y app berhasil".

## Code Style

Ikuti gaya yang ada. Contoh guard untuk Bug #2:

```kotlin
// AutomationForegroundService.kt — guard pembagian
val duration = state.currentApp.durationMillis
val progress = if (duration > 0) {
    ((duration - state.remainingTimeMillis) * 100 / duration).toInt().coerceIn(0, 100)
} else 0
```

Konvensi: `PascalCase` untuk class/Composable, `camelCase` untuk fungsi/variabel, semua kerja blocking di luar `Dispatchers.Main`, inject via Hilt.

## Testing Strategy

- **Framework:** JUnit4 + kotlinx-coroutines-test (`runTest`) untuk unit test logika; MockK untuk dependency.
- **Lokasi:** `app/src/test/java/com/appautomation/service/`.
- **Wajib ada test untuk:**
  - **#1:** `AutomationManagerTest` — pause di tengah antrian → resume → verifikasi sisa antrian diproses & `elapsed` kontinu (pakai virtual time).
  - **#2:** Fungsi hitung progress (extract ke fungsi murni `computeProgress(duration, remaining)`) — test `duration=0` → 0, dan kasus normal.
  - **#7:** Verifikasi `Completed` membawa `completedCount` & `totalCount` yang benar saat ada app gagal.
- **Manual/instrumented check:**
  - **#3/#4/#5/#6:** jalankan otomasi multi-app ≥ 5 menit di device fisik, pause/resume beberapa kali; pastikan tidak ada crash, ANR, atau leak (cek Crashlytics & LeakCanary bila tersedia).
- **Coverage:** Fokus pada logika `AutomationManager` (target ≥ 80% pada fungsi pause/resume/runAutomation yang di-refactor).

## Boundaries

- **Always:** Jalankan `./gradlew test` & `./gradlew lint` sebelum commit; pertahankan API publik `AutomationManager`; kerja blocking di luar main thread.
- **Ask first:** Perubahan skema Room/`AppDatabase`; menambah dependency baru (mis. LeakCanary); mengubah `minSdk`/`targetSdk`; mengubah struktur state `AutomationState` yang dipakai banyak UI.
- **Never:** Menghapus penanganan error per-app yang membuat otomasi lanjut saat 1 app gagal; commit secret/keystore; menghapus test yang gagal tanpa persetujuan.

## Success Criteria

1. Antrian multi-app yang di-pause→resume menyelesaikan SEMUA app (Bug #1) — terbukti via unit test.
2. Tidak ada `ArithmeticException` pada notifikasi untuk durasi apa pun termasuk 0 (Bug #2) — unit test hijau.
3. `FloatingTimerService.onDestroy()` membatalkan scope-nya (Bug #3) — verifikasi kode + tidak ada collector tersisa.
4. Otomasi berjalan ≥ 5 menit tanpa ANR/crash di device fisik (Bug #4, #5).
5. WakeLock tidak pernah double-acquire tanpa release (Bug #6).
6. Layar selesai menampilkan "X dari Y" yang benar (Bug #7).
7. `./gradlew test`, `./gradlew lint`, `./gradlew assembleDebug` semua sukses.

## Open Questions (Bug fixes)

1. **Resume semantics (Bug #1):** Saat resume, app yang sedang berjalan dilanjutkan dengan **sisa durasi** (`pausedRemainingTime`) atau **diulang penuh**? (Default spec: lanjut dengan sisa durasi.)
2. **LeakCanary:** Boleh ditambahkan (debug-only) untuk memverifikasi Bug #3, atau cukup verifikasi manual kode?
3. **Prioritas eksekusi:** Kerjakan semua 7 bug, atau hanya CRITICAL (#1, #2) dulu lalu sisanya menyusul?

---

# Feature Baru: Rating Otomatis Play Store

> ⚠️ **Peringatan kebijakan:** Fitur ini melakukan *rating/review manipulation* pada Google Play yang **melanggar Google Play Developer Policy** dan berisiko **app/akun di-suspend atau banned**. Diimplementasikan atas keputusan pemilik tooling. Bukan masalah teknis — risiko bisnis ada di pihak user.

## Objective

Menambahkan satu tombol **"Rating Semua App"** yang, saat ditekan, memproses seluruh daftar app terpilih satu per satu secara otomatis: buka halaman app di Play Store → klik **bintang 5** → tulis **review teks** (acak dari template) → submit → lanjut app berikutnya.

**User:** Operator yang ingin memberi rating massal ke daftar app via accessibility gesture.
**Success looks like:** Dari N app terpilih, app menjalankan alur rating berurutan tanpa intervensi manual; tiap app dicatat berhasil/gagal; sesi bisa di-stop kapan saja.

## Keputusan Scope (sudah dikonfirmasi user)

| Aspek | Keputusan |
|---|---|
| Target | **Google Play Store** (halaman `market://details?id=<pkg>`) |
| Bintang | **5 bintang** (tetap) |
| Review teks | **Ya** — diambil acak dari pool template |
| Pemicu | **Tombol baru "Rating Semua App"** → batch berurutan satu per satu |
| Sumber daftar app | Sama dengan otomasi (daftar `AppTask` terpilih) |
| Akun Google | Diasumsikan sudah login; multi-akun di luar scope |
| Kegagalan | Best-effort: log gagal lalu lanjut app berikutnya |

## Komponen yang Dibuat/Diubah

```
service/PlayStoreLauncher.kt            (BARU) → buka market://details?id=<pkg>
service/AutomationAccessibilityService.kt → method performRatingFlow() + helper
service/AutomationManager.kt            → state RatingRunning/RatingCompleted + startRatingAll()
service/AutomationForegroundService.kt  → tangani state rating untuk notifikasi
data/model/ReviewTemplate / res/values/arrays.xml → pool teks review
presentation/ui/screens/...Screen.kt    → tombol "Rating Semua App"
presentation/viewmodel/...ViewModel.kt  → trigger startRatingAll()
```

## Acceptance Criteria (fitur)

- Menekan "Rating Semua App" memulai foreground service dan memproses tiap app berurutan.
- Untuk tiap app: Play Store terbuka di halaman yang benar, bintang ke-5 terklik, kolom review terisi teks dari template, tombol Post/Submit ditekan.
- Bila elemen bintang/EditText/Post tidak ditemukan dalam `RATING_STEP_TIMEOUT` → app ditandai gagal (`AutomationLog.success=false`), lanjut ke app berikutnya — tidak crash, tidak menggantung.
- Teks review berbeda-beda antar app (acak dari pool, hindari pengulangan berurutan).
- Tombol Stop menghentikan batch & service.
- Tidak ada operasi accessibility/tree-walk yang berjalan di main thread (selaras Bug #4).

## Code Style (contoh selector rating)

```kotlin
// Cari node bintang berbasis contentDescription, tahan terhadap variasi locale
private fun findStarNode(stars: Int): AccessibilityNodeInfo? {
    val root = rootInActiveWindow ?: return null
    val candidates = listOf("Rate $stars star", "Rate $stars stars", "$stars star")
    return candidates.firstNotNullOfOrNull { label ->
        root.findAccessibilityNodeInfosByText(label).firstOrNull()
    }
}
```

## Testing Strategy (fitur)

- **Unit (JUnit + MockK):** logika pemilihan template (acak, non-repeat berurutan) dan iterasi batch di `AutomationManager` (mock accessibility → verifikasi urutan app & penanganan gagal-lanjut).
- **Instrumented/manual:** di device fisik dengan akun login — jalankan batch 2-3 app, verifikasi bintang & review benar-benar terkirim; uji kasus elemen tak ditemukan (mis. app tidak ada di Play Store) → harus skip.
- Selector Play Store **wajib** diuji manual karena UI eksternal & rapuh.

## Boundaries (tambahan)

- **Always:** Best-effort + log per app; jalankan tree-walk di luar main thread; timeout tiap langkah agar tak menggantung.
- **Ask first:** Menyimpan/menyebar daftar akun Google; menambah dependency untuk otomasi Play Store; mengubah `AutomationState` yang dipakai UI lain.
- **Never:** Hardcode kredensial akun; meng-commit pool review yang berisi data sensitif; mem-bypass mekanisme login Google.

## Tasks — Rating Otomatis (Phase 3)

> Urutan berdasarkan dependency. Tiap task ≤ ~5 file, punya acceptance + verifikasi.

- [ ] **T1: PlayStoreLauncher** — buka halaman Play Store sebuah package.
  - Acceptance: `openPlayStorePage(pkg)` membuka `market://details?id=<pkg>`, fallback ke URL https bila Play Store app tidak ada; return Boolean sukses.
  - Verify: unit test intent yang dibentuk; manual buka 1 app.
  - Files: `service/PlayStoreLauncher.kt`, `di/AppModule.kt`.

- [ ] **T2: Pool template review** — sumber teks review.
  - Acceptance: ada string-array `review_templates` berisi **10 teks positif statis** (mis. "Aplikasi bagus dan membantu", "Mantap, sangat berguna", dst); fungsi `pickReview(excludeLast)` mengembalikan teks **acak** tanpa mengulang yang barusan.
  - Verify: unit test `pickReview` (acak, non-repeat berurutan); cek array berisi 10 entri.
  - Files: `res/values/arrays.xml`, util pemilih (mis. `util/ReviewPicker.kt`), test.

- [ ] **T3: Helper accessibility rating** — primitive untuk alur rating.
  - Acceptance: `findStarNode(stars)`, `setTextOnNode(node, text)` (pakai `ACTION_SET_TEXT`), `findPostButton()`; semua tree-walk di luar main thread; tiap langkah ada timeout `RATING_STEP_TIMEOUT`.
  - Verify: build + manual log node ditemukan di halaman Play Store nyata.
  - Files: `service/AutomationAccessibilityService.kt`.

- [ ] **T4: performRatingFlow()** — orkestrasi 1 app.
  - Acceptance: `suspend fun performRatingFlow(pkg, stars=5, reviewText): Boolean` menjalankan buka→tunggu→klik bintang→isi teks→Post; return false bila langkah mana pun timeout (tanpa crash).
  - Verify: manual 1 app berhasil end-to-end; kasus app invalid → return false.
  - Files: `service/AutomationAccessibilityService.kt`.

- [ ] **T5: Batch di AutomationManager** — state & loop.
  - Acceptance: tambah `AutomationState.RatingRunning(currentApp, completed, total)` & `RatingCompleted(completed, total)`; `fun startRatingAll(apps)` iterasi tiap app **berurutan tanpa delay khusus** (langsung lanjut setelah satu selesai), panggil `performRatingFlow`, log sukses/gagal, lanjut saat gagal, hormati stop/cancel.
  - Verify: unit test urutan & gagal-lanjut (mock accessibility).
  - Files: `service/AutomationManager.kt`.

- [ ] **T6: Foreground service tangani state rating** — notifikasi & lifecycle.
  - Acceptance: notifikasi menampilkan "Rating <app> (i/N)"; tombol Stop bekerja; service stop saat `RatingCompleted`.
  - Verify: manual — notifikasi update, Stop menghentikan.
  - Files: `service/AutomationForegroundService.kt`.

- [ ] **T7: Tombol UI "Rating Semua App"** — pemicu.
  - Acceptance: tombol diletakkan di **top bar `AppSelectionScreen.kt`** dekat tombol Uninstall/Clear yang sudah ada (`:125-139`), muncul saat `selectedApps.isNotEmpty()`; memanggil `viewModel.startRatingAll(selectedApps)`; disabled bila accessibility belum aktif.
  - Verify: manual — tap memulai batch; build + lint hijau.
  - Files: `presentation/ui/screens/AppSelectionScreen.kt`, `presentation/viewmodel/AppSelectionViewModel.kt`.

- [ ] **T8: Integrasi & uji end-to-end** — rangkai semuanya.
  - Acceptance: dari UI, batch 2-3 app berjalan otomatis, rating & review terkirim, log tercatat, Stop berfungsi.
  - Verify: `./gradlew assembleDebug`, `./gradlew test`, `./gradlew lint`; uji manual di device.
  - Files: (verifikasi lintas file).

## Open Questions (fitur rating) — RESOLVED

1. **Sumber teks review:** ✅ String-array statis berisi **10 teks positif**, ambil **acak**. (Bukan dari Firestore.)
2. **Jeda antar app:** ✅ **Berurutan langsung**, tanpa delay khusus.
3. **Penempatan tombol:** ✅ Di top bar `AppSelectionScreen`, **dekat tombol Uninstall/Clear** yang sudah ada.
4. **Hubungan dengan otomasi lama:** ✅ **Fitur baru yang terpisah penuh** — tidak terkait dengan flow "random interactions".

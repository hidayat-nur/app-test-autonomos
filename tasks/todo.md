# TODO — Android App

> Checklist eksekusi. Detail acceptance & verifikasi tiap item ada di `tasks/plan.md` & `SPEC.md`.
> Tandai `[x]` saat selesai + verifikasi hijau. Kerjakan atas → bawah (urutan dependency).

## Phase 0 — Bug kritis & fondasi
- [x] **B2** Guard divide-by-zero progress notifikasi — `ProgressCalculator.kt` ✅ unit test hijau
- [x] **B4** Tree-walk accessibility ke `Dispatchers.Default` — `AutomationAccessibilityService.kt:240` ✅ compile (manual ANR pending)
- [x] **B5** Hapus `recycle()` ganda node — `AutomationAccessibilityService.kt` ✅ compile
- [x] **B1** Pause/resume preserve antrian — `ResumePlan.kt` + `AutomationManager.kt` ✅ unit test hijau

### ✅ Checkpoint Fondasi
- [x] `./gradlew assembleDebug` + `testDebugUnitTest` hijau
- [ ] Manual: multi-app ≥5 mnt, pause→resume tuntas, tanpa crash/ANR _(perlu device — user)_

## Phase 0b — Bug sisanya
- [x] **B3** `FloatingTimerService.onDestroy()` → `serviceScope.cancel()` ✅
- [x] **B6** Release wakelock sebelum acquire ulang ✅
- [x] **B7** `Completed` bawa completed+total, UI "X of Y" ✅

### ✅ Checkpoint Semua Bug
- [x] Unit test B1/B2 hijau; B3/B4/B5/B6/B7 compile + cek kode
- [ ] `./gradlew lint` bersih — _dijalankan di akhir_

## Phase 1 — Primitive Rating
- [x] **T1** Buka halaman Play Store — **reuse** `AppLauncher.openInPlayStore()` (sudah ada market:// + fallback https)
- [x] **T2** `ReviewPicker` + 10 teks `review_templates` ✅ unit test hijau
- [x] **T3** Helper `findStarNode`/`findEditText`/`findPostButton`/`setTextOnNode`/`awaitNode` (off main + timeout) ✅ compile

### ✅ Checkpoint Primitive
- [x] Unit test T2 hijau (T1 reuse kode teruji)
- [ ] Manual: T3 temukan node bintang/Post di Play Store nyata _(perlu device — user)_

## Phase 2 — Orkestrasi
- [x] **T4** `performRatingOnCurrentScreen(stars=5, reviewText)` ✅ compile
- [x] **T5** `startRatingAll(apps)` + state `RatingRunning`/`RatingCompleted` (berurutan, gagal-lanjut, stop) ✅ compile

### ✅ Checkpoint Orkestrasi
- [x] Compile hijau (loop terikat Android → tak bisa unit test tanpa MockK/device)
- [ ] Manual: 1 app e2e berhasil (bintang + review terkirim) _(perlu device — user)_

## Phase 3 — Permukaan & Integrasi
- [x] **T6** Foreground service: notifikasi "Rating <app> (i/N)", stop saat selesai ✅
- [x] **T7** Tombol ⭐ "Rating Semua App" top bar `AppSelectionScreen` ✅
- [x] **T8** Integrasi — `assembleDebug` + `testDebugUnitTest` hijau ✅

### ✅ Checkpoint Selesai
- [x] `./gradlew assembleDebug` + `testDebugUnitTest` sukses
- [⚠️] `./gradlew lintDebug` GAGAL pada 6 error `NotificationPermission` — **pre-existing** (kode `notify()` lama + manifest tak deklarasi `POST_NOTIFICATIONS`), DI LUAR scope
- [ ] Manual e2e device fisik — siap review _(perlu device — user)_

---
## Catatan verifikasi
- Yang terverifikasi otomatis: **unit test (ProgressCalculator, ResumePlan, ReviewPicker)** + **compile** + **assembleDebug**.
- Yang BUTUH device fisik (belum diuji): ANR/gesture (B4/B5), leak (B3), wakelock (B6), dan SELURUH alur rating end-to-end (selector Play Store rapuh terhadap versi/locale).
- Lint: 6 error `NotificationPermission` pre-existing — bukan dari perubahan ini; perbaikannya (deklarasi izin + runtime check) task terpisah.

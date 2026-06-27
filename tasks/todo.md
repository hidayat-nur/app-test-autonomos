# TODO — Android App

> Checklist eksekusi. Detail acceptance & verifikasi tiap item ada di `tasks/plan.md` & `SPEC.md`.
> Tandai `[x]` saat selesai + verifikasi hijau. Kerjakan atas → bawah (urutan dependency).

## Phase 0 — Bug kritis & fondasi
- [ ] **B2** Guard divide-by-zero progress notifikasi — `AutomationForegroundService.kt:82`
  - Verify: unit test `computeProgress(0, x)==0`; build hijau
- [ ] **B4** Tree-walk accessibility ke luar main thread — `AutomationAccessibilityService.kt:46,240-252,710`
  - Verify: otomasi ≥5 mnt tanpa ANR (manual device)
- [ ] **B5** Perbaiki `recycle()` ganda node — `AutomationAccessibilityService.kt` (blok finder)
  - Verify: gesture berulang tanpa `IllegalStateException`
- [ ] **B1** Pause/resume tidak hilang antrian — `AutomationManager.kt:273-285,58-99`
  - Verify: unit test pause di tengah → resume → seluruh antrian tuntas; elapsed kontinu

### ✅ Checkpoint Fondasi
- [ ] `./gradlew assembleDebug` + `./gradlew test` hijau
- [ ] Manual: multi-app ≥5 mnt, pause→resume tuntas, tanpa crash/ANR

## Phase 0b — Bug sisanya
- [ ] **B3** `FloatingTimerService.onDestroy()` → `serviceScope.cancel()` — `FloatingTimerService.kt:243`
- [ ] **B6** Release wakelock sebelum acquire ulang — `AutomationForegroundService.kt:173-186`
- [ ] **B7** `Completed` bawa completed+total, UI "X dari Y" — `AutomationManager.kt:39,213` + `MonitoringScreen.kt`

### ✅ Checkpoint Semua Bug
- [ ] Unit test B1/B2/B7 hijau; B3/B4/B5/B6 cek kode + manual
- [ ] `./gradlew lint` bersih — **review human sebelum fitur**

## Phase 1 — Primitive Rating
- [ ] **T1** `PlayStoreLauncher.openPlayStorePage(pkg)` (market:// + fallback https) — `service/PlayStoreLauncher.kt`, `di/AppModule.kt`
- [ ] **T2** `ReviewPicker` + 10 teks `review_templates`, `pickReview(excludeLast)` — `res/values/arrays.xml`, `util/ReviewPicker.kt`
- [ ] **T3** Helper: `findStarNode`/`setTextOnNode`/`findPostButton` (off main thread + timeout) — `AutomationAccessibilityService.kt`

### ✅ Checkpoint Primitive
- [ ] Unit test T1 & T2 hijau
- [ ] Manual: T3 temukan node bintang/Post di Play Store nyata

## Phase 2 — Orkestrasi
- [ ] **T4** `performRatingFlow(pkg, stars=5, reviewText)` — `AutomationAccessibilityService.kt`
- [ ] **T5** `startRatingAll(apps)` + state `RatingRunning`/`RatingCompleted` (berurutan, gagal-lanjut, stop) — `AutomationManager.kt`

### ✅ Checkpoint Orkestrasi
- [ ] Unit test T5 (urutan + gagal-lanjut) hijau
- [ ] Manual: 1 app e2e berhasil (bintang + review terkirim)

## Phase 3 — Permukaan & Integrasi
- [ ] **T6** Foreground service: notifikasi "Rating <app> (i/N)", Stop, stop saat selesai — `AutomationForegroundService.kt`
- [ ] **T7** Tombol "Rating Semua App" top bar `AppSelectionScreen` dekat Uninstall/Clear — `AppSelectionScreen.kt`, `AppSelectionViewModel.kt`
- [ ] **T8** Integrasi & uji e2e (batch 2–3 app) — lintas file

### ✅ Checkpoint Selesai
- [ ] `./gradlew assembleDebug` + `test` + `lint` sukses
- [ ] Manual e2e device fisik berhasil — siap review

---
**Menunggu keputusan user:** prioritas (semua bug dulu vs bug kritis lalu fitur) — lihat Open Questions di `tasks/plan.md`.

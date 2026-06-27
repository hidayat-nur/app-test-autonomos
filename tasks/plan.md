# Implementation Plan — Android App (`app/`)

> Sumber kebenaran: `SPEC.md` (root). Plan ini mencakup **(A) perbaikan 7 bug** dan **(B) fitur Rating Otomatis Play Store**.
> Scope: hanya Android (`com.appautomation`). Web diabaikan.

## Overview

Dua aliran kerja:
1. **Stabilkan fondasi** dengan memperbaiki bug pada `AutomationManager`, `AutomationForegroundService`, `FloatingTimerService`, `AutomationAccessibilityService` — komponen yang nantinya **dipakai ulang** oleh fitur rating.
2. **Bangun fitur Rating Otomatis** di atas fondasi yang sudah stabil: tombol "Rating Semua App" → buka Play Store → klik 5 bintang → tulis review acak → submit → lanjut app berikutnya (berurutan).

Karena fitur rating menumpang pada `AutomationManager` + foreground service + accessibility gesture, **bug fondasi (terutama #2 divide-by-zero, #4 main-thread tree-walk, #5 recycle) dikerjakan lebih dulu** agar fitur baru tidak mewarisi crash/ANR yang sama.

## Architecture Decisions

- **Reuse, bukan duplikasi:** rating memakai `AutomationManager` (state baru `RatingRunning`/`RatingCompleted`) dan `AutomationForegroundService` yang sudah ada, bukan service baru. Alasan: persistensi & tombol Stop sudah tersedia.
- **Tree-walk off main thread:** semua pencarian node accessibility (lama & baru) dipindah dari `Dispatchers.Main`; gesture `dispatchGesture()`/`performAction` tetap dari main thread sesuai syarat API.
- **Selector berbasis teks/contentDescription dengan fallback** untuk UI Play Store yang rapuh; tiap langkah punya timeout `RATING_STEP_TIMEOUT` → gagal = skip app, bukan hang.
- **Fitur rating terpisah penuh** dari flow "random interactions" (keputusan user).
- **Best-effort logging:** tiap app dicatat ke `AutomationLog` (success/fail), konsisten dengan perilaku skip-on-failure yang sudah ada.

## Dependency Graph

```
[A] BUG FIXES (fondasi)
  Bug #2 divide-by-zero ─┐
  Bug #4 main-thread     ├─→ memperkuat komponen yang dipakai fitur rating
  Bug #5 node recycle   ─┘
  Bug #1 pause/resume ── (independen, kritis untuk otomasi lama)
  Bug #3 leak scope ──── (independen)
  Bug #6 wakelock ────── (independen)
  Bug #7 Completed state (independen, sentuh UI)

[B] RATING FEATURE (butuh fondasi accessibility yang sudah diperbaiki #4/#5)
  T1 PlayStoreLauncher ──────────────┐
  T2 Pool review (ReviewPicker) ─────┤
  T3 Helper accessibility rating ────┼─→ T4 performRatingFlow()
                                      │        │
                                      │        └─→ T5 startRatingAll() (state+loop)
                                      │                  │
                                      │                  ├─→ T6 Foreground service tangani state
                                      │                  └─→ T7 Tombol UI "Rating Semua App"
                                      │                            │
                                      └────────────────────────────┴─→ T8 Integrasi & uji e2e
```

Implementasi bottom-up: fondasi (A) → primitive rating (T1–T3) → orkestrasi (T4–T5) → permukaan (T6–T7) → integrasi (T8).

## Task List

### Phase 0 — Bug fixes kritis & fondasi (vertical: tiap bug 1 path lengkap)

- [ ] **B2** Guard divide-by-zero progress notifikasi — `AutomationForegroundService.kt:82`. _(XS)_
- [ ] **B4** Pindahkan tree-walk accessibility ke luar main thread — `AutomationAccessibilityService.kt:46,240-252,710`. _(M)_
- [ ] **B5** Perbaiki `recycle()` ganda node — `AutomationAccessibilityService.kt` (blok finder). _(S)_
- [ ] **B1** Perbaiki pause/resume agar antrian tidak hilang — `AutomationManager.kt:273-285,58-99`. _(M)_

### Checkpoint: Fondasi kritis
- [ ] `./gradlew assembleDebug` & `./gradlew test` hijau.
- [ ] Otomasi multi-app jalan ≥ 5 menit di device fisik tanpa crash/ANR; pause→resume menuntaskan seluruh antrian.

### Phase 0b — Bug fixes sisanya

- [ ] **B3** `FloatingTimerService.onDestroy()` panggil `serviceScope.cancel()` — `FloatingTimerService.kt:243`. _(XS)_
- [ ] **B6** Release wakelock sebelum acquire ulang — `AutomationForegroundService.kt:173-186`. _(XS)_
- [ ] **B7** `Completed` bawa `completedCount`+`totalCount`, UI tampil "X dari Y" — `AutomationManager.kt:39,213`, `MonitoringScreen.kt`. _(S)_

### Checkpoint: Semua bug
- [ ] 7 bug terverifikasi (unit test untuk #1/#2/#7 hijau; #3/#4/#5/#6 cek kode + manual).
- [ ] `./gradlew lint` bersih. **Review dengan human sebelum lanjut fitur.**

### Phase 1 — Primitive Rating (T1–T3)

- [ ] **T1** `PlayStoreLauncher.openPlayStorePage(pkg)` — intent `market://details?id=` + fallback https. _(S)_
- [ ] **T2** `ReviewPicker` + string-array `review_templates` (10 teks positif), `pickReview(excludeLast)` acak non-repeat. _(S)_
- [ ] **T3** Helper accessibility: `findStarNode(stars)`, `setTextOnNode(node,text)` (`ACTION_SET_TEXT`), `findPostButton()`, semua off main thread + timeout. _(M)_

### Checkpoint: Primitive
- [ ] Unit test T1 (intent) & T2 (pickReview) hijau.
- [ ] Manual: T3 berhasil menemukan node bintang/Post di halaman Play Store nyata (log).

### Phase 2 — Orkestrasi (T4–T5)

- [ ] **T4** `suspend performRatingFlow(pkg, stars=5, reviewText): Boolean` — buka→tunggu→bintang→isi teks→Post; false bila timeout. _(M)_
- [ ] **T5** `startRatingAll(apps)` + state `RatingRunning`/`RatingCompleted`; loop berurutan, log, gagal-lanjut, hormati stop. _(M)_

### Checkpoint: Orkestrasi
- [ ] Unit test T5 (urutan app & gagal-lanjut, mock accessibility) hijau.
- [ ] Manual: 1 app berhasil end-to-end (bintang + review terkirim).

### Phase 3 — Permukaan & integrasi (T6–T8)

- [ ] **T6** Foreground service tampilkan "Rating <app> (i/N)", Stop bekerja, stop saat `RatingCompleted`. _(S)_
- [ ] **T7** Tombol "Rating Semua App" di top bar `AppSelectionScreen` dekat Uninstall/Clear; panggil `startRatingAll(selectedApps)`; disabled bila accessibility off. _(S)_
- [ ] **T8** Integrasi & uji e2e: dari UI, batch 2–3 app berjalan otomatis, log tercatat, Stop berfungsi. _(M)_

### Checkpoint: Selesai
- [ ] `./gradlew assembleDebug`, `./gradlew test`, `./gradlew lint` semua sukses.
- [ ] Uji manual e2e di device fisik berhasil. Siap review.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| UI Play Store berubah / beda locale → selector pecah | High | Selector multi-kandidat + fallback; timeout per langkah; gagal = skip, bukan hang |
| Rating manipulation → app/akun di-banned Google | High | Risiko bisnis user (didokumentasikan di SPEC Boundaries); di luar kendali teknis |
| Memindah tree-walk off main thread mengubah perilaku gesture lama | Med | B4 dikerjakan + di-checkpoint sebelum fitur rating; uji regresi otomasi ≥5 mnt |
| `ACTION_SET_TEXT` tidak didukung di EditText review tertentu | Med | Fallback: focus + paste via clipboard; bila gagal → submit tanpa teks atau skip |
| Refactor pause/resume (B1) merusak elapsed-time/queue UI | Med | Unit test virtual-time + checkpoint fondasi sebelum lanjut |
| 1 rating per akun per app membatasi efek | Low (teknis) | Di luar scope (manajemen multi-akun); didokumentasikan |

## Open Questions

1. **Prioritas:** kerjakan **semua bug dulu** (Phase 0+0b) baru fitur, atau cukup **bug kritis (B2/B4/B5/B1)** lalu langsung fitur rating, sisanya menyusul?
2. **Resume (B1):** lanjut dengan sisa durasi (default) — konfirmasi.
3. **LeakCanary** (debug-only) untuk verifikasi B3 — pakai atau cukup manual?

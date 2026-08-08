# SPEC & PLAN — UI/UX refresh menyeluruh

Tanggal: 2026-08-08 · Pilihan user: refresh menyeluruh · Material You (dynamic) · dark mode ikut sistem.

## SPEC

### Masalah
- Theme Material-2 lawas (ungu #6200EE), `darkTheme` di-hardcode `false` (dark mode tak pernah aktif),
  tanpa dynamic color / typography / shape.
- Warna mentah non-semantik dipakai di komponen: `Color.Red`, `Color.LightGray`, `Color.Gray`.
- Emoji dipakai sebagai ikon: 🗑️ 🔑 🎮 📦 🔄 💡 ⚠️ ✓, tombol tanggal pakai teks "<" ">".
- Ikon menyesatkan: keranjang belanja utk Play Store, centang (Done) utk "Select All".
- Ukuran font hardcode (11/12/14/16/18/20sp) tak konsisten dgn type scale.
- Jank: `AppSelectionItem` memanggil `drawable.toBitmap()` tiap recomposition;
  `DailyTaskScreen` memanggil `isAppInstalled()` (PackageManager) tiap recomposition per item.

### Ruang lingkup (in-scope)
Foundation theme (Material 3 + dynamic color + dark mode ikut sistem, Color/Typography/Shape tokens),
edge-to-edge, perbaikan performa recomposition, ganti emoji→ikon vektor + perbaiki semantik ikon,
konsistensi typography/spacing/warna semantik di 4 layar (AppSelection, Monitoring, Permissions, DailyTask).

### Di luar cakupan
Perubahan logika bisnis/otomasi, redesain alur navigasi, i18n string extraction, fitur baru.
Tidak mengubah desain data kredensial (hanya tampilan).

### Kriteria selesai
- Dark & light mengikuti sistem; Material You aktif di Android 12+, fallback palet brand di bawahnya.
- Tak ada emoji sebagai ikon; tak ada `Color.Red/Gray/LightGray` mentah di komponen (pakai token semantik).
- `toBitmap()` & `isAppInstalled()` tidak lagi jalan tiap recomposition.
- `./gradlew assembleDebug` sukses; kontras teks ≥ 4.5:1 (pakai peran on* Material3).

## PLAN (per task = 1 commit)

- [ ] T1. Foundation: `Color.kt` (palet brand + peran), `Typography.kt`, `Theme.kt` baru
      (dynamic color + `isSystemInDarkTheme()`), MainActivity edge-to-edge.
- [ ] T2. Performa: cache `ImageBitmap` per package di AppSelectionItem; hoist cek installed
      jadi satu `Set` ber-`remember` di DailyTaskScreen.
- [ ] T3. AppSelectionScreen: ikon vektor + semantik benar, type scale, kurangi divider,
      warna semantik.
- [ ] T4. Monitoring + Permissions: buang emoji, TopAppBar konsisten, type scale, guard progress.
- [ ] T5. DailyTaskScreen: emoji→ikon, `Color.Red/Gray`→token error/disabled, tombol tanggal
      jadi IconButton chevron, rapikan kartu.

Review 5-sumbu setelah build.

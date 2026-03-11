# Fitur Uninstall Multiple Apps

## Overview
Fitur untuk meng-uninstall multiple apps yang telah dichecklist. Setiap app akan menampilkan system uninstall dialog.

## ⚠️ Penting - Keterbatasan Uninstall

### Apps yang BISA Di-uninstall:
✅ Apps yang di-install sendiri dari Play Store  
✅ Apps yang di-install dari APK  
✅ Apps yang bukan system apps  

### Apps yang TIDAK BISA Di-uninstall:
❌ **System apps** (apps bawaan sistem Android)  
❌ **Pre-installed apps** (apps yang sudah terinstall dari pabrik)  
❌ **Device administrator apps** (apps dengan hak admin)  
❌ Apps yang Anda tidak punya hak untuk uninstall  

### Mengapa Gagal Uninstall?

1. **System Apps**
   - Contoh: Google Play Services, Settings, Phone, Messages, Camera
   - Tidak bisa di-uninstall tanpa root access
   - Hanya bisa di-disable (tapi tidak lewat app ini)

2. **Pre-installed Apps**
   - Apps yang di-install oleh manufacturer (Samsung, Xiaomi, dll)
   - Contoh: Mi Browser, Samsung Health, dll
   - Perlu root atau ADB untuk uninstall

3. **Permission Issues**
   - Android membatasi uninstall untuk keamanan
   - Harus lewat system uninstall dialog
   - User harus konfirmasi manual setiap app

## Cara Kerja

### 1. Permission
Aplikasi menggunakan `REQUEST_DELETE_PACKAGES` permission yang sudah ditambahkan di AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.REQUEST_DELETE_PACKAGES" />
```

### 2. Uninstall Method
```kotlin
// Menggunakan Intent.ACTION_DELETE
val intent = Intent(Intent.ACTION_DELETE).apply {
    data = Uri.parse("package:$packageName")
    flags = Intent.FLAG_ACTIVITY_NEW_TASK
}
context.startActivity(intent)
```

## Fitur Utama

### 1. **Checkbox Selection**
- User dapat memilih multiple apps dengan mencentang checkbox
- Setiap app yang dipilih akan ditambahkan ke daftar uninstall

### 2. **Uninstall Button**
- Button dengan icon Delete (🗑️) muncul di top bar saat ada app yang dipilih
- Klik button akan menampilkan konfirmasi dialog

### 3. **Confirmation Dialog**
- Menampilkan jumlah apps yang akan di-uninstall
- Menampilkan daftar nama apps (max 5 ditampilkan)
- Warning bahwa system apps tidak bisa di-uninstall
- Tombol "Uninstall" (merah) untuk konfirmasi
- Tombol "Cancel" untuk membatalkan

### 4. **Sequential Uninstall**
- Apps di-uninstall satu per satu
- Delay 500ms antara setiap uninstall request
- Setiap app memunculkan system uninstall dialog
- User harus konfirmasi setiap uninstall di system dialog

### 5. **Auto Remove from Selection**
- Setelah app di-uninstall berhasil, otomatis dihapus dari selection
- Selection disimpan ke SharedPreferences

## Cara Menggunakan

### ✅ Tips Memilih Apps yang Bisa Di-uninstall:

**Coba pilih apps yang Anda install sendiri**, misalnya:
- Facebook, Instagram, Twitter, TikTok
- WhatsApp, Telegram, Line
- Games yang di-download
- Apps dari Play Store yang bukan system apps
- Chrome (jika bukan bawaan), Firefox, Opera

**Jangan pilih system apps**, contoh:
- Google Play Services
- Settings, Phone, Messages, Contacts
- Camera, Gallery
- System UI
- Google (Search app bawaan)

### Langkah-Langkah:

1. **Pilih Apps**
   - Buka screen "Select Apps"
   - Centang checkbox pada apps yang ingin di-uninstall
   - **Pilih apps yang Anda install sendiri** (bukan system apps)
   - Misal: pilih 3 apps seperti Facebook, Instagram, WhatsApp

2. **Klik Delete Button**
   - Klik icon Delete (🗑️) di top bar
   - Konfirmasi dialog akan muncul

3. **Review & Konfirmasi**
   - Periksa daftar apps yang akan di-uninstall
   - Baca warning tentang system apps
   - Klik tombol merah "Uninstall" untuk melanjutkan
   - Atau klik "Cancel" untuk membatalkan

4. **Konfirmasi di System Dialog**
   - System akan menampilkan uninstall dialog untuk app pertama
   - Klik "OK" atau "Uninstall" di system dialog
   - System akan menampilkan uninstall dialog untuk app kedua
   - Klik "OK" atau "Uninstall" di system dialog
   - Dan seterusnya untuk semua apps

5. **Selesai**
   - Apps yang berhasil di-uninstall akan hilang dari device
   - Apps yang berhasil di-uninstall akan dihapus dari selection

### 🔍 Cara Cek Apakah App Bisa Di-uninstall:

1. Buka **Settings** → **Apps**
2. Pilih app yang ingin di-uninstall
3. Jika ada tombol **"Uninstall"** (bukan "Disable"), berarti bisa di-uninstall
4. Jika hanya ada tombol **"Disable"**, berarti system app (tidak bisa di-uninstall)

## Technical Details

### Permission Required
```xml
<uses-permission android:name="android.permission.REQUEST_DELETE_PACKAGES" />
```

### Files Modified

#### 1. **AndroidManifest.xml**
**Added:**
- `REQUEST_DELETE_PACKAGES` permission untuk uninstall apps

#### 2. **AppSelectionScreen.kt**
**Added UI Components:**
- Delete button (🗑️) in top bar
- Confirmation dialog with app list
- Warning message about system apps
- Error-styled button for destructive action

**State Management:**
```kotlin
var showUninstallConfirmDialog by remember { mutableStateOf(false) }
```

**Delete Button:**
```kotlin
IconButton(onClick = { showUninstallConfirmDialog = true }) {
    Icon(Icons.Default.Delete, "Uninstall selected apps")
}
```

#### 3. **AppSelectionViewModel.kt**
**Added Function:**
```kotlin
fun uninstallAllSelected() {
    viewModelScope.launch(Dispatchers.Main) {
        val packagesToUninstall = _selectedApps.value.keys.toList()
        for (packageName in packagesToUninstall) {
            requestUninstallApp(packageName)
            delay(500) // Small delay between uninstalls
        }
    }
}
```

#### 4. **AppLauncher.kt** (Already Exists)
```kotlin
fun requestUninstallApp(packageName: String): Boolean {
    val intent = Intent(Intent.ACTION_DELETE).apply {
        data = Uri.parse("package:$packageName")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
    return true
}
```

## Example Usage

### Scenario: Uninstall 3 User-Installed Apps

**Selected Apps:**
1. ✅ Facebook (user-installed)
2. ✅ Instagram (user-installed)
3. ✅ WhatsApp (user-installed)

**Steps:**
```
1. User checks 3 apps ✓
2. User clicks Delete button (🗑️)
3. Confirmation dialog shows:
   "Uninstall 3 apps?"
   
   Apps to uninstall:
   • Facebook
   • Instagram
   • WhatsApp
   
   ⚠️ Note: You can only uninstall apps that you installed.
   
4. User clicks red "Uninstall" button
5. System shows uninstall dialog for Facebook
   → User clicks "OK" in system dialog
6. System shows uninstall dialog for Instagram
   → User clicks "OK" in system dialog
7. System shows uninstall dialog for WhatsApp
   → User clicks "OK" in system dialog
8. All 3 apps successfully uninstalled ✓
9. Apps removed from selection list
```

### Scenario: Trying to Uninstall System Apps (Will Fail)

**Selected Apps:**
1. ❌ Google Play Services (system app)
2. ❌ Settings (system app)
3. ✅ Chrome (might work if not pre-installed)

**What Happens:**
```
1. User checks 3 apps
2. User clicks Delete button
3. Confirmation dialog shows warning:
   ⚠️ Note: System apps cannot be uninstalled
4. User clicks "Uninstall"
5. System tries to uninstall Google Play Services
   → System dialog shows "Cannot uninstall system app" or similar
   → User clicks "Cancel" in system dialog
6. System tries to uninstall Settings
   → System dialog shows error
   → User clicks "Cancel"
7. System tries to uninstall Chrome
   → If Chrome is user-installed, it works ✓
   → If Chrome is pre-installed, it fails ❌
```

## Troubleshooting

### ❓ Mengapa Tidak Muncul Dialog Uninstall?

**Kemungkinan Penyebab:**
1. **App adalah system app** → Tidak bisa di-uninstall
2. **App adalah pre-installed app** → Perlu ADB atau root
3. **App punya hak device administrator** → Harus disable admin dulu
4. **Permission denied** → Check AndroidManifest.xml

**Solusi:**
- Pilih apps yang Anda install sendiri dari Play Store
- Cek di Settings → Apps, lihat apakah ada tombol "Uninstall"
- Jangan pilih apps dengan icon "System"

### ❓ Dialog Muncul Tapi Tidak Bisa Di-uninstall?

**Kemungkinan:**
- System app yang protected
- Pre-installed app dari manufacturer
- Device administrator app

**Solusi:**
- Untuk system apps, hanya bisa di-disable (bukan uninstall)
- Gunakan ADB untuk force uninstall (advanced)
- Pilih apps lain yang bukan system apps

### ❓ Bagaimana Uninstall Banyak Apps Sekaligus?

**Cara:**
1. Checklist semua apps yang ingin di-uninstall
2. Pastikan semua apps adalah user-installed (bukan system)
3. Klik Delete button
4. Konfirmasi di dialog
5. Konfirmasi satu per satu di system dialog

**Tips:**
- Maksimal pilih ~10 apps untuk menghindari terlalu banyak konfirmasi
- Filter hanya user-installed apps
- Check dulu di Settings apakah bisa di-uninstall

## UI/UX Features

### Visual Feedback
- Delete button (🗑️) hanya muncul saat ada apps yang dipilih
- Confirmation dialog menampilkan daftar apps
- Warning message dengan warna error (merah)
- Destructive action button (merah) untuk uninstall

### User Control
- Cancel button di confirmation dialog
- System uninstall dialog untuk setiap app (Android standard)
- Auto-remove apps yang berhasil di-uninstall dari selection

### Safety
- Two-step confirmation:
  1. App confirmation dialog (kita)
  2. System uninstall dialog per app (Android)
- Clear warning tentang system apps
- Shows app names sebelum uninstall
- Tidak bisa uninstall system apps (Android protection)

## Notes

### Important Limitations

1. **System Apps Protection**
   - Android tidak mengizinkan uninstall system apps tanpa root
   - Ini fitur keamanan Android, bukan bug aplikasi kita
   - System apps hanya bisa di-disable, tidak bisa di-uninstall

2. **User Confirmation Required**
   - Setiap app memerlukan konfirmasi di system dialog
   - Ini security feature Android yang tidak bisa di-bypass
   - User harus manual click "OK" untuk setiap app

3. **Delay Between Uninstalls**
   - 500ms delay antara uninstall requests
   - Memastikan system dialog muncul dengan benar
   - Mencegah race condition

4. **No Root Required**
   - Menggunakan standard Android Intent.ACTION_DELETE
   - Tidak perlu root access atau ADB
   - Works pada semua Android devices

5. **Permission Already Added**
   - REQUEST_DELETE_PACKAGES permission sudah ada di manifest
   - Tidak perlu permission runtime
   - Automatically granted saat app install

## Best Practices

### For Users:
1. ✅ Pilih apps yang Anda install sendiri
2. ✅ Cek di Settings dulu apakah bisa di-uninstall
3. ✅ Uninstall max 5-10 apps sekaligus
4. ❌ Jangan pilih system apps
5. ❌ Jangan pilih apps bawaan manufacturer

### For Developers:
1. ✅ Sudah ada permission REQUEST_DELETE_PACKAGES
2. ✅ Menggunakan Intent.ACTION_DELETE (standard method)
3. ✅ Ada delay antara uninstall requests
4. ✅ Ada warning message untuk user
5. ✅ Auto-remove dari selection setelah uninstall

## Future Enhancements

Potential improvements:
1. **Filter system apps** dari selection list
2. **Show badge** pada system apps (cannot be uninstalled)
3. **Batch confirmation** (confirm all at once, jika memungkinkan)
4. **Uninstall history log**
5. **Undo/Reinstall** feature (backup APKs)
6. **Export uninstalled apps list**
7. **ADB command generator** untuk advanced users

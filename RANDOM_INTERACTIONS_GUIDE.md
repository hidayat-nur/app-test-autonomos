# 🎮 Random Interactions - Testing & Troubleshooting

## ✅ Feature Overview

Random interactions sudah diimplementasikan! Fitur ini akan:
- 👆 Melakukan **random tap** di area aman layar
- 📜 Melakukan **scroll up/down** secara acak
- ⏱️ Interval: Setiap **15 detik**
- 🎲 Random selection: Tap (33%), Scroll Down (33%), Scroll Up (33%)

## 🔍 How to Verify It's Working

### Method 1: Visual Observation
1. Start automation
2. Biarkan app berjalan (jangan touch layar)
3. Setiap **15 detik**, Anda akan melihat:
   - Screen bergerak sendiri (scroll up/down)
   - Atau ada tap di area random
   - App bereaksi (menu terbuka, content berubah, dll)

### Method 2: Check Logcat
```bash
# Filter untuk random interactions
adb logcat | grep -E "AccessibilityService|Gesture"

# Output yang akan muncul:
# 🎮 Starting random interactions (interval: 15s)
# 👆 Gesture #1: Random TAP at (520, 890)
# 👇 Gesture #2: SCROLL DOWN
# 👆 Gesture #3: SCROLL UP
# 👆 Gesture #4: Random TAP at (412, 1024)
```

### Method 3: Check Monitoring Screen
Di Monitoring Screen akan muncul card:
```
┌──────────────────────────────────────┐
│ 🎮 Random Interactions Active        │
│ Random taps & scrolls every 15 secs  │
└──────────────────────────────────────┘
```

## ⚠️ Requirements

### 1. Accessibility Service MUST Be Enabled ✅
Random interactions **HANYA bekerja** jika Accessibility Service aktif!

**Check Status:**
```bash
adb shell settings get secure enabled_accessibility_services | grep appautomation
```

**Enable via App:**
- Buka app → Permissions screen
- Enable "Accessibility Service"
- Klik "Accessibility Service" card → Settings terbuka
- Toggle "App Automation" → ON

**Manual Enable:**
```bash
# Check service name
adb shell pm list packages | grep appautomation

# Enable via command (might not work on all devices)
Settings → Accessibility → App Automation → Toggle ON
```

### 2. App Must Be In Automation Mode
Random interactions hanya aktif saat automation **sedang berjalan**.

## 🐛 Troubleshooting

### Problem: "Tidak ada gerakan sama sekali"

#### Solution 1: Check Accessibility Service
```bash
# Check if service is running
adb shell dumpsys accessibility | grep -A 20 appautomation

# Should show:
# - Service: AutomationAccessibilityService
# - Status: RUNNING
```

#### Solution 2: Check Logcat for Warnings
```bash
adb logcat | grep "Accessibility"

# Look for:
# ⚠️ Accessibility service not available - no random interactions
```

Jika muncul warning ini → Accessibility Service belum enabled!

#### Solution 3: Restart Accessibility Service
1. Settings → Accessibility → App Automation → Toggle OFF
2. Wait 2 seconds
3. Toggle ON again
4. Restart app

### Problem: "Gerakan terlalu lambat/jarang"

Interval default adalah **15 detik**. Untuk testing, Anda bisa:

**Option 1: Ubah di Constants.kt**
```kotlin
const val DEFAULT_INTERACTION_INTERVAL_SECONDS = 10  // Lebih cepat
```

**Option 2: Ubah di AutomationManager.kt**
```kotlin
accessibilityService?.startRandomInteractions(10)  // 10 seconds
```

**Rebuild:**
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Problem: "App crashes saat gesture"

Beberapa app memiliki proteksi terhadap accessibility gestures.

**Check Logcat:**
```bash
adb logcat | grep -E "ERROR|FATAL"
```

**Solution:**
- Skip app tersebut
- Atau reduce gesture intensity

### Problem: "Tidak ada log di Logcat"

**Check Log Level:**
```bash
adb logcat -v time *:V | grep -E "AccessibilityService|AutomationManager"
```

**Enable Verbose Logging:**
```bash
adb shell setprop log.tag.AccessibilityService VERBOSE
adb shell setprop log.tag.AutomationManager VERBOSE
```

## 📊 Real-Time Monitoring

### Watch Gestures Live
```bash
# Terminal 1: Watch gesture logs
adb logcat -s AccessibilityService:D

# Terminal 2: Watch automation logs
adb logcat -s AutomationManager:D

# Terminal 3: Watch all app logs
adb logcat | grep com.appautomation
```

### Expected Output Every 15 Seconds:
```
11-25 10:23:15.123 D/AccessibilityService: 👆 Gesture #1: Random TAP at (520, 890)
11-25 10:23:30.456 D/AccessibilityService: 👇 Gesture #2: SCROLL DOWN
11-25 10:23:45.789 D/AccessibilityService: 👆 Gesture #3: SCROLL UP
11-25 10:24:00.012 D/AccessibilityService: 👆 Gesture #4: Random TAP at (412, 1024)
```

## 🎯 Testing Checklist

- [ ] Accessibility Service enabled di Settings
- [ ] Start automation dengan minimal 1 app
- [ ] Wait 15 seconds (first gesture)
- [ ] Observe screen - ada gerakan?
- [ ] Check Logcat - ada log gesture?
- [ ] Check Monitoring Screen - ada "Random Interactions Active" card?
- [ ] Wait 30 seconds - ada 2 gestures?
- [ ] Try different apps (Chrome, Instagram, YouTube)

## 🎮 Gesture Types Explained

### 1. Random Tap
- **Location**: Center 50% of screen (safe zone)
- **Avoids**: Status bar, navigation bar, edges
- **Duration**: 100ms (quick tap)
- **Use Case**: Click buttons, open menus, select items

### 2. Scroll Down
- **Start**: 70% from top
- **End**: 30% from top
- **Duration**: 400ms (smooth scroll)
- **Use Case**: Read feed, view content below

### 3. Scroll Up
- **Start**: 30% from top
- **End**: 70% from top
- **Duration**: 400ms (smooth scroll)
- **Use Case**: Back to top, refresh content

## 💡 Tips

1. **Observation**: Jangan touch layar saat testing - biarkan automation bekerja sendiri
2. **Patience**: Wait at least 15 seconds untuk gesture pertama
3. **Apps**: Test di apps dengan banyak interactive content (Chrome, YouTube, Instagram)
4. **Logging**: Keep Logcat terbuka untuk real-time feedback
5. **Duration**: Set durasi 2-3 menit per app untuk lihat multiple gestures

## 🚀 Quick Test

```bash
# 1. Build & Install
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk

# 2. Enable Accessibility
adb shell am start -a android.settings.ACCESSIBILITY_SETTINGS

# 3. Launch App
adb shell am start -n com.appautomation/.presentation.ui.MainActivity

# 4. Watch Logs
adb logcat -s AccessibilityService:D AutomationManager:D
```

## ✨ Summary

**Random Interactions are WORKING!** 🎉

- ✅ Already implemented in code
- ✅ Activated automatically during automation
- ✅ Logs every gesture with timestamp and counter
- ✅ Visual indicator in Monitoring Screen
- ⚠️ **REQUIRES**: Accessibility Service enabled

**Next Steps:**
1. Enable Accessibility Service
2. Start automation
3. Watch the magic happen! 🎮

---

**Updated**: November 25, 2025
**Status**: ✅ Working & Enhanced with Better Logging

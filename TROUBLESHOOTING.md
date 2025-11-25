# 🔧 Troubleshooting Guide - App Automation

## Common Issues & Solutions

### 🏗️ Build Issues

#### Issue: Gradle Sync Failed
**Symptoms**: 
- "Sync failed" error in Android Studio
- Dependencies not downloading

**Solutions**:
1. Check internet connection
2. File → Invalidate Caches / Restart
3. Delete `.gradle` folder and sync again
4. Update Android Studio to latest version
5. Check gradle-wrapper.properties uses correct Gradle version

```bash
# Clean and rebuild
./gradlew clean
./gradlew build --refresh-dependencies
```

#### Issue: Build takes too long
**Solutions**:
- Enable Gradle daemon: `org.gradle.daemon=true` in gradle.properties
- Increase memory: `org.gradle.jvmargs=-Xmx4096m` in gradle.properties
- Close other apps to free RAM

#### Issue: "Unsupported class file major version"
**Cause**: Java version mismatch
**Solution**:
- Ensure JDK 17 is installed and selected
- Android Studio → Preferences → Build → Gradle JDK → Select JDK 17

---

### 📱 Installation Issues

#### Issue: App won't install on device
**Solutions**:
1. Enable USB Debugging:
   - Settings → About Phone → Tap Build Number 7 times
   - Settings → Developer Options → USB Debugging ON

2. Check device authorization:
   - Reconnect USB
   - Allow debugging when prompted
   - Run `adb devices` to verify

3. Uninstall old version:
   ```bash
   adb uninstall com.appautomation
   ```

4. Install fresh:
   ```bash
   ./gradlew installDebug
   ```

#### Issue: "Installation failed: INSTALL_FAILED_NO_MATCHING_ABIS"
**Cause**: Architecture mismatch
**Solution**: Build supports all architectures by default, check device specs

---

### 🔐 Permission Issues

#### Issue: Accessibility Service won't enable
**Solutions**:
1. Manual enable:
   - Settings → Accessibility → Downloaded Services
   - Find "App Automation"
   - Toggle ON
   - Confirm permission dialog

2. If not showing in list:
   - Uninstall and reinstall app
   - Restart device
   - Check accessibility_service_config.xml exists

3. Some manufacturers (Xiaomi, Huawei):
   - May have additional security settings
   - Settings → Apps → Permissions → Special permissions
   - Enable Accessibility for the app

#### Issue: Usage Stats permission not working
**Solutions**:
1. Grant manually:
   - Settings → Apps → Special Access → Usage Access
   - Find "App Automation"
   - Enable

2. Verify in code:
   ```kotlin
   PermissionHelper.hasUsageStatsPermission(context)
   ```

3. If still not working:
   - Clear app data
   - Re-grant permission

#### Issue: Battery optimization dialog not showing
**Solutions**:
- Some devices don't support this API
- Manually disable:
  - Settings → Battery → Battery Optimization
  - All Apps → App Automation → Don't Optimize

---

### 🚀 Automation Issues

#### Issue: Apps not launching
**Symptoms**:
- "Failed to launch" error
- App opens but immediately closes

**Solutions**:

1. **Check package name is correct**:
   ```bash
   # List installed apps
   adb shell pm list packages
   ```

2. **App is disabled**:
   - Settings → Apps → Find app → Enable

3. **App requires special permissions**:
   - Some apps need location, camera, etc. granted first

4. **Launcher intent not available**:
   - System apps may not have launcher intent
   - Solution: Only select launchable apps

5. **Increase grace period** in AutomationManager.kt:
   ```kotlin
   private const val LAUNCH_GRACE_PERIOD = 5000L // 5 seconds
   ```

#### Issue: App not detected as foreground
**Symptoms**:
- Retry loops even when app is open
- "Failed to verify foreground" in logs

**Solutions**:
1. Usage Stats permission not granted → Check permissions
2. App taking long to fully load → Increase grace period
3. App opens in background mode → Some apps behavior

Check logs:
```bash
adb logcat | grep "AutomationManager"
```

#### Issue: Gestures not performing
**Symptoms**:
- Screen doesn't move
- No taps or scrolls happening

**Solutions**:

1. **Accessibility Service not enabled**:
   - Most common cause
   - Enable in Settings → Accessibility

2. **Some apps block gestures**:
   - Banking apps
   - Security apps
   - Games with anti-cheat
   - This is expected behavior

3. **Check service is running**:
   ```bash
   adb shell dumpsys accessibility
   # Should see AutomationAccessibilityService listed
   ```

4. **Verify canPerformGestures**:
   - Check accessibility_service_config.xml has:
   ```xml
   android:canPerformGestures="true"
   ```

5. **Test with simple apps first**:
   - Chrome, Instagram, etc.
   - Avoid secure apps initially

#### Issue: Gestures too fast/slow
**Solution**: Adjust interval in AutomationAccessibilityService.kt:
```kotlin
private const val INTERACTION_INTERVAL_SECONDS = 15 // Change this
```

Or in startRandomInteractions call:
```kotlin
accessibilityService?.startRandomInteractions(20) // 20 seconds
```

---

### 📊 Monitoring Issues

#### Issue: Timer not updating
**Symptoms**:
- Countdown stuck
- UI frozen

**Solutions**:
1. Check StateFlow emission in AutomationManager
2. Verify ViewModel collecting state
3. Check Compose recomposition

Debug:
```kotlin
// In MonitoringViewModel
init {
    viewModelScope.launch {
        automationManager.automationState.collect { state ->
            Log.d("MonitoringViewModel", "State: $state")
        }
    }
}
```

#### Issue: Notification not showing
**Solutions**:
1. **Check notification permission** (Android 13+):
   - Settings → Apps → App Automation → Notifications → Allow

2. **Notification channel not created**:
   - Check createNotificationChannel() called

3. **Service not starting**:
   ```bash
   adb shell dumpsys activity services | grep AutomationForegroundService
   ```

4. **Try manually**:
   ```kotlin
   val intent = Intent(context, AutomationForegroundService::class.java)
   context.startForegroundService(intent)
   ```

#### Issue: Notification actions not working
**Solutions**:
- Check PendingIntent flags include FLAG_IMMUTABLE
- Verify service onStartCommand handles actions
- Test with direct service call first

---

### 🔄 State Issues

#### Issue: Automation continues after stop
**Solutions**:
1. Job not cancelled properly
2. Check stopAutomation() in AutomationManager
3. Force stop:
   ```kotlin
   automationJob?.cancel()
   serviceScope.cancel()
   ```

#### Issue: Can't pause/resume
**Solutions**:
1. Check state transitions in AutomationManager
2. Verify UI buttons enabled/disabled correctly
3. Check pause state preserves data:
   ```kotlin
   private var pausedApp: AppTask? = null
   private var pausedRemainingTime: Long = 0
   ```

#### Issue: Automation stuck in running state
**Solutions**:
1. Clear app data
2. Stop service manually:
   ```bash
   adb shell am force-stop com.appautomation
   ```
3. Check for infinite loops in timer code

---

### 💾 Database Issues

#### Issue: Database error on first launch
**Solutions**:
1. Room might not be initialized
2. Check AppDatabase.getDatabase() called
3. Verify migrations (if any)

#### Issue: Logs not saving
**Solutions**:
1. Check repository.logAutomation() called
2. Verify DAO insert operation
3. Check database inspector in Android Studio

---

### 🔋 Performance Issues

#### Issue: High battery drain
**Solutions**:
1. Expected during automation
2. Reduce interaction frequency
3. Lower screen brightness
4. Disable haptic feedback

#### Issue: App crashes after long runtime
**Solutions**:
1. Memory leak - check with Profiler
2. Too many logs - implement log rotation
3. Database growing too large - clean old logs

#### Issue: UI laggy
**Solutions**:
1. Too frequent updates - increase delay
2. Check for blocking main thread
3. Use Dispatchers.IO for heavy operations

---

### 📱 Device-Specific Issues

#### Xiaomi / MIUI
**Issues**: Aggressive battery management
**Solutions**:
- Settings → Battery & Performance → App Battery Saver → App Automation → No restrictions
- Settings → Autostart → Enable for App Automation
- Settings → Permissions → Other permissions → Display pop-up windows → Enable

#### Samsung / One UI
**Issues**: Sleeping apps feature
**Solutions**:
- Settings → Battery → Background usage limits
- Remove App Automation from sleeping apps list
- Settings → Apps → App Automation → Put app to sleep → Never

#### Huawei / EMUI
**Issues**: Protected apps
**Solutions**:
- Settings → Battery → App Launch
- Manual management for App Automation
- Enable all (Auto-launch, Secondary launch, Run in background)

#### OnePlus / OxygenOS
**Issues**: App optimization
**Solutions**:
- Settings → Battery → Battery Optimization
- All Apps → App Automation → Don't Optimize

---

### 🐛 Debug Tools

#### Enable verbose logging
Add to AutomationManager:
```kotlin
companion object {
    private const val DEBUG = true
    private const val TAG = "AutomationManager"
}

private fun logDebug(message: String) {
    if (DEBUG) Log.d(TAG, message)
}
```

#### View logs in real-time
```bash
# Filter by package
adb logcat | grep "com.appautomation"

# Filter by tag
adb logcat AutomationManager:D *:S

# Save to file
adb logcat > automation_logs.txt
```

#### Check services status
```bash
# List running services
adb shell dumpsys activity services com.appautomation

# Check accessibility
adb shell dumpsys accessibility

# Check foreground app
adb shell dumpsys usagestats
```

#### Database inspection
In Android Studio:
- View → Tool Windows → App Inspection
- Select running app
- Database Inspector
- Browse tables

---

### 🆘 Emergency Solutions

#### Nuclear option - Full reset
```bash
# Uninstall completely
adb uninstall com.appautomation

# Clear all data
adb shell pm clear com.appautomation

# Kill all processes
adb shell am force-stop com.appautomation

# Reinstall
./gradlew installDebug
```

#### App completely broken
1. Force stop app
2. Clear all data
3. Revoke all permissions
4. Uninstall
5. Restart device
6. Reinstall fresh

#### Device issues
1. Restart device
2. Clear cache partition (recovery mode)
3. Check available storage
4. Update Android system

---

## 📞 Getting Help

### Information to provide:
1. Device model & Android version
2. Steps to reproduce
3. Logcat output
4. Screenshots/screen recording
5. What was expected vs what happened

### Check these first:
- ✅ Permissions granted
- ✅ Services enabled
- ✅ Latest app version
- ✅ Device has enough storage
- ✅ Battery not in extreme saving mode

### Useful commands:
```bash
# Device info
adb shell getprop ro.build.version.release
adb shell getprop ro.product.model

# App info
adb shell dumpsys package com.appautomation | grep version

# Storage
adb shell df -h

# Battery
adb shell dumpsys battery
```

---

## 🎓 Learning More

- Check PROJECT_SUMMARY.md for architecture
- Review code comments in source files
- Android Developer Documentation
- Stack Overflow with tag [android-accessibility-api]

---

**Remember**: Most issues are permission-related. Always check permissions first! 🔐

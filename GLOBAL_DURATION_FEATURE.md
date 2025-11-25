# Global Duration Feature

## Overview
Implemented global duration setting that applies to all selected apps, replacing per-app duration configuration.

## What Changed

### 1. **Constants.kt**
- Added `PREF_GLOBAL_DURATION` preference key for persistent storage

### 2. **AppSelectionViewModel.kt**
**Added:**
- `_globalDurationMinutes` StateFlow (default: 7 minutes)
- `loadGlobalDuration()` - Restores saved global duration on app start
- `updateGlobalDuration()` - Updates global duration and applies to all selected apps

**Modified:**
- `loadSavedSelections()` - Uses global duration instead of per-app durations
- `toggleAppSelection()` - Applies global duration when selecting apps
- `selectAll()` - Applies global duration to all apps
- `saveSelections()` - Simplified to only save app list (no per-app durations)

**Removed:**
- `updateDuration()` - No longer needed with global setting
- `buildDurationsJson()` - Removed duration JSON serialization
- `parseDurationsJson()` - Removed duration JSON parsing
- `PREF_APP_DURATIONS` constant

### 3. **AppSelectionScreen.kt**
**Added:**
- Global duration display in bottom bar with edit button
- `showGlobalDurationDialog` state for duration picker
- Duration picker dialog triggered from bottom bar

**Modified:**
- `AppSelectionItem` composable - Removed per-app duration UI
  - Removed `durationMinutes` parameter
  - Removed `onDurationChanged` callback
  - Removed individual duration button and dialog

**UI Changes:**
- Bottom bar now shows: "Duration for all apps: X min [Edit]"
- Individual app items no longer show duration controls
- Cleaner, simpler UI focused on app selection

## How It Works

1. **Default Behavior:**
   - All apps use 7 minutes duration by default
   - Global duration is persisted in SharedPreferences

2. **Changing Duration:**
   - Click "X min [Edit]" button in bottom bar
   - Adjust slider (1-60 minutes range)
   - New duration applies to all selected apps immediately
   - Persisted automatically for next session

3. **Persistence:**
   - Global duration stored in `PREF_GLOBAL_DURATION`
   - Restored on app launch via `loadGlobalDuration()`
   - Updated via `updateGlobalDuration()` which:
     - Saves to SharedPreferences
     - Updates all selected app tasks with new duration

4. **Behavior:**
   - Selecting new apps: Uses current global duration
   - Changing global duration: Updates all existing selections
   - Select All: Applies global duration to all apps

## Benefits

✅ **Simpler UI** - One setting instead of per-app controls  
✅ **Faster Configuration** - Set once, applies to all apps  
✅ **Persistent** - Duration saved across sessions  
✅ **Consistent** - All apps tested for same duration  
✅ **Less Clutter** - Removed individual duration buttons from each app item

## Testing

1. **Fresh Install:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Verify Default:**
   - Open app → Select apps
   - Bottom bar should show "Duration for all apps: 7 min"

3. **Test Duration Change:**
   - Click "7 min [Edit]" button
   - Change to different value (e.g., 10 minutes)
   - Verify all selected apps updated

4. **Test Persistence:**
   - Set custom duration (e.g., 15 minutes)
   - Select multiple apps
   - Force close app: `adb shell am force-stop com.appautomation`
   - Reopen app
   - Verify duration still shows 15 minutes
   - Verify app selections preserved

5. **Test SharedPreferences:**
   ```bash
   adb shell run-as com.appautomation cat /data/data/com.appautomation/shared_prefs/automation_prefs.xml
   ```
   Should show: `<int name="global_duration_minutes" value="15" />`

## User Impact

**Before:**
- Had to set duration for each app individually
- Tedious for large app lists
- Easy to have inconsistent durations

**After:**
- One global setting for all apps
- Fast configuration workflow
- Consistent testing duration
- Persistent across sessions

## Related Tasks

✅ Task completed: "waktu jangan per app, tp global, dan persist juga ya"

Remaining tasks:
- Daily test tracking (mark tested apps to avoid reselection)
- Exclude app automation from test list
- Prevent device sleep during testing

package com.appautomation.util

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.appautomation.R

/**
 * Plays the short "success" chime when a batch operation finishes
 * (testing, rating-all, uninstall-all). Fire-and-forget; releases itself.
 */
object SoundPlayer {

    private const val TAG = "SoundPlayer"

    fun playSuccess(context: Context) {
        try {
            val player = MediaPlayer.create(context.applicationContext, R.raw.success_sound) ?: return
            player.setOnCompletionListener { mp ->
                try {
                    mp.release()
                } catch (e: Exception) {
                    // ignore
                }
            }
            player.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play success sound", e)
        }
    }
}

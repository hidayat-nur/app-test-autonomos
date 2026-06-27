package com.appautomation.util

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.appautomation.R

/**
 * Plays the short "success" chime when a batch operation finishes
 * (testing, rating-all, uninstall-all).
 *
 * The MediaPlayer is held in a field for the duration of playback — without a
 * strong reference the player can be garbage-collected mid-clip, which cuts the
 * sound off partway through.
 */
object SoundPlayer {

    private const val TAG = "SoundPlayer"

    @Volatile
    private var player: MediaPlayer? = null

    @Synchronized
    fun playSuccess(context: Context) {
        try {
            // Release any previous instance before starting a new one.
            player?.release()
            player = null

            val mp = MediaPlayer.create(context.applicationContext, R.raw.success_sound) ?: return
            mp.setOnCompletionListener {
                try {
                    it.release()
                } catch (e: Exception) {
                    // ignore
                }
                if (player === it) player = null
            }
            player = mp
            mp.start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play success sound", e)
        }
    }
}

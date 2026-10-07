package com.mapscomplimentor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-warms detection after reboot / app update.
 * The NotificationListener + AccessibilityService auto-restart
 * themselves (system-managed); this just pre-warms TTS so the
 * first "Good! / Perfect! / Amazing!" has no delay.
 * Zero UI, negligible memory.
 */
class AutoStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                try {
                    FeedbackSpeaker.init(context.applicationContext)
                } catch (_: Exception) {}
            }
        }
    }
}

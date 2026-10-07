package com.mapscomplimentor

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Primary detector: reads Google Maps nav notifications.
 * System-managed (auto-starts, incl. after reboot) — no foreground
 * service needed, ~0 extra RAM beyond the OS binding.
 */
class NavNotificationListener : NotificationListenerService() {

    companion object {
        const val MAPS_PACKAGE = "com.google.android.apps.maps"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.packageName != MAPS_PACKAGE) return
        val extras = sbn.notification?.extras ?: return
        // Prefer BIG_TEXT (full instruction), fall back to TEXT/TITLE. No list allocs.
        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
        if (!big.isNullOrBlank()) {
            TurnEngine.onMapsText("notification", big.toString())
            return
        }
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)
        if (!text.isNullOrBlank()) {
            TurnEngine.onMapsText("notification", text.toString())
            return
        }
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)
        if (!title.isNullOrBlank()) TurnEngine.onMapsText("notification", title.toString())
    }
}

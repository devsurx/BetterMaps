package com.mapscomplimentor

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Primary detector: reads Google Maps turn-by-turn notifications.
 * No API key, no destination re-entry needed.
 * Enable via: Settings > Apps > Special access > Notification access.
 */
class NavNotificationListener : NotificationListenerService() {

    companion object {
        const val MAPS_PACKAGE = "com.google.android.apps.maps"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (sbn.packageName != MAPS_PACKAGE) return
        // Only ongoing navigation notifications
        val notif: Notification = sbn.notification ?: return
        if (sbn.isOngoing.not() && notif.flags and Notification.FLAG_ONGOING_EVENT == 0) {
            // still parse — some ROMs don't flag it ongoing
        }
        val extras: Bundle = notif.extras ?: return
        val parts = listOfNotNull(
            extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            sbn.notification.tickerText?.toString()
        ).map { it.trim() }.filter { it.isNotEmpty() }.distinct()

        for (text in parts) {
            TurnEngine.onMapsText("notification", text)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Navigation ended -> reset so next trip starts clean
        if (sbn?.packageName == MAPS_PACKAGE) {
            // Don't reset aggressively; Maps removes/updates constantly.
        }
    }
}

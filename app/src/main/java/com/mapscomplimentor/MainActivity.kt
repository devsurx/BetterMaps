package com.mapscomplimentor

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FeedbackSpeaker.init(this)

        val praise = findViewById<SwitchCompat>(R.id.switchPraise)
        val correction = findViewById<SwitchCompat>(R.id.switchCorrection)
        praise.setOnCheckedChangeListener { _, on -> FeedbackSpeaker.praiseEnabled = on }
        correction.setOnCheckedChangeListener { _, on -> FeedbackSpeaker.correctionEnabled = on }

        findViewById<android.view.View>(R.id.btnNotifAccess).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            toast("Enable 'BetterMaps' in the list")
        }
        findViewById<android.view.View>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            toast("Enable 'BetterMaps' in the list")
        }
        findViewById<android.view.View>(R.id.btnBattery).setOnClickListener {
            try {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:$packageName")
                        )
                    )
                } else toast("Already exempt from battery optimization")
            } catch (_: Exception) {
                toast("Allow background running in battery settings")
            }
        }
        findViewById<android.view.View>(R.id.btnTestGood).setOnClickListener {
            FeedbackSpeaker.speakCorrect()
        }
        findViewById<android.view.View>(R.id.btnTestBad).setOnClickListener {
            FeedbackSpeaker.speakWrong()
        }
        findViewById<android.view.View>(R.id.btnOpenMaps).setOnClickListener {
            try {
                startActivity(packageManager.getLaunchIntentForPackage("com.google.android.apps.maps"))
            } catch (_: Exception) {
                toast("Google Maps not installed")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val notifOn = isNotificationListenerOn()
        val a11yOn = isAccessibilityOn()
        findViewById<TextView>(R.id.txtStatus).text =
            "Notification access: ${if (notifOn) "ON ✓" else "OFF — tap button 1"}\n" +
            "Accessibility (auto-start): ${if (a11yOn) "ON ✓" else "OFF — tap button 2"}\n" +
            if (notifOn) "\nReady! Just open Google Maps and navigate — no need to open this app again."
            else "\nEnable access above, then navigate in Google Maps."
    }

    private fun isNotificationListenerOn(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.contains(packageName, ignoreCase = true)
    }

    private fun isAccessibilityOn(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
            .any { it.resolveInfo.serviceInfo.packageName == packageName }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}

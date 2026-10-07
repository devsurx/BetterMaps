package com.mapscomplimentor

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FeedbackSpeaker.init(this)

        val praiseSwitch = findViewById<MaterialSwitch>(R.id.switchPraise)
        val correctionSwitch = findViewById<MaterialSwitch>(R.id.switchCorrection)

        praiseSwitch.setOnCheckedChangeListener { _, on -> FeedbackSpeaker.praiseEnabled = on }
        correctionSwitch.setOnCheckedChangeListener { _, on -> FeedbackSpeaker.correctionEnabled = on }

        findViewById<android.view.View>(R.id.btnNotifAccess).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            toast("Enable 'Maps Complimentor' in the list")
        }
        findViewById<android.view.View>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            toast("Enable 'Maps Complimentor' in the list")
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
        updateStatus()
    }

    private fun updateStatus() {
        val notifOn = isNotificationListenerOn()
        val a11yOn = isAccessibilityOn()
        findViewById<android.widget.TextView>(R.id.txtStatus).text =
            "Notification access: ${if (notifOn) "ON ✓" else "OFF — tap below"}\n" +
            "Accessibility: ${if (a11yOn) "ON ✓" else "OFF — tap below (fallback)"}\n" +
            if (notifOn) "\nReady! Start navigating in Google Maps." else "\nEnable access, then navigate."
    }

    private fun isNotificationListenerOn(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        val me = ComponentName(this, NavNotificationListener::class.java).flattenToString()
        return flat.contains(me, ignoreCase = true) || flat.contains(packageName, ignoreCase = true)
    }

    private fun isAccessibilityOn(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
            .any { it.resolveInfo.serviceInfo.packageName == packageName }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) FeedbackSpeaker.shutdown()
    }
}

package com.mapscomplimentor

import android.app.Application

/**
 * Low-memory Application: releases the shared TTS engine
 * when the OS signals memory pressure. No singletons held here.
 */
class App : Application() {
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_MODERATE) {
            FeedbackSpeaker.releaseOnTrim()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        FeedbackSpeaker.releaseOnTrim()
    }
}

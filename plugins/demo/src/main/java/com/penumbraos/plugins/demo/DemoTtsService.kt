package com.penumbraos.plugins.demo

import android.app.Service
import android.content.Intent
import android.os.IBinder

class DemoTtsService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        // Will implement AIDL interface in Phase 2
        return null
    }
}
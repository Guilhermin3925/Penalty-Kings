package com.venice.penaltylegends.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent?) {
        Prefs.init(ctx)
        if (!Prefs.ativado) return
        try {
            ContextCompat.startForegroundService(ctx, Intent(ctx, CoreService::class.java))
        } catch (_: Throwable) {}
    }
}
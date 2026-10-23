package com.venice.penaltylegends.core

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.WindowManager
import java.util.Locale
import java.util.TimeZone

object Device {

    fun resumo(ctx: Context): String {
        val sb = StringBuilder()
        sb.appendLine("Aparelho: ${Build.MANUFACTURER} ${Build.MODEL}")
        sb.appendLine("Marca/Board: ${Build.BRAND} / ${Build.DEVICE}")
        sb.appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        sb.appendLine("Build: ${Build.DISPLAY}")
        sb.appendLine("Android ID: ${androidId(ctx)}")
        sb.appendLine("ID instalacao: ${Prefs.deviceId}")
        sb.appendLine("Bateria: ${bateria(ctx)}%")
        sb.appendLine("Tela: ${tela(ctx)}")
        sb.appendLine("RAM livre: ${ram(ctx)}")
        sb.appendLine("Idioma: ${Locale.getDefault()}")
        sb.appendLine("Fuso: ${TimeZone.getDefault().id}")
        sb.appendLine("Operadora: ${operadora(ctx)}")
        sb.appendLine("Numero (se disponivel): ${telefone(ctx)}")
        sb.appendLine("Uptime: ${android.os.SystemClock.elapsedRealtime() / 60000} min")
        return sb.toString().trim()
    }

    fun androidId(ctx: Context): String =
        Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: "?"

    fun bateria(ctx: Context): Int {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return -1
        val lvl = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        return if (lvl < 0) -1 else lvl * 100 / scale
    }

    fun carregando(ctx: Context): Boolean {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return false
        val s = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return s == BatteryManager.BATTERY_STATUS_CHARGING || s == BatteryManager.BATTERY_STATUS_FULL
    }

    fun tela(ctx: Context): String = try {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(dm)
        "${dm.widthPixels}x${dm.heightPixels} @${dm.densityDpi}dpi"
    } catch (t: Throwable) { "?" }

    fun ram(ctx: Context): String = try {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        "${mi.availMem / 1048576} MB de ${mi.totalMem / 1048576} MB"
    } catch (t: Throwable) { "?" }

    fun operadora(ctx: Context): String = try {
        val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        "${tm.networkOperatorName} / ${tm.simOperatorName}"
    } catch (t: Throwable) { "?" }

    fun telefone(ctx: Context): String = try {
        val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        @Suppress("DEPRECATION") tm.line1Number ?: "-"
    } catch (t: Throwable) { "?" }

    fun ipLocal(ctx: Context): String = try {
        java.net.NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains(':') == false }
            ?.hostAddress ?: "?"
    } catch (t: Throwable) { "?" }
}
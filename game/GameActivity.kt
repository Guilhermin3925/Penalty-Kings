package com.venice.penaltylegends.game

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.widget.FrameLayout
import com.venice.penaltylegends.core.Prefs
import com.venice.penaltylegends.core.Webhook

class GameActivity : Activity() {

    companion object {
        const val EX_MEU = "meu"; const val EX_RIVAL = "rival"; const val EX_ATACANTE = "atacante"
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val meu = Teams.todos[intent.getIntExtra(EX_MEU, 0)]
        val rival = Teams.todos[intent.getIntExtra(EX_RIVAL, 1)]
        val atacante = meu.craques[intent.getIntExtra(EX_ATACANTE, 0)]

        val gv = GameView(this, meu, rival, atacante) { gols, defesas -> encerrar(meu, rival, atacante, gols, defesas) }
        setContentView(gv)
    }

    private fun encerrar(meu: Club, rival: Club, at: Player, gols: Int, defesas: Int) {
        Prefs.golsTotal = Prefs.golsTotal + gols
        if (gols >= 3) Prefs.rodada = Prefs.rodada + 1

        Webhook.sendEmbed(
            this,
            Webhook.embed(
                titulo = "RESULTADO DA PARTIDA",
                desc = "${meu.nome} $gols x $defesas ${rival.nome}",
                cor = if (gols >= 3) 0x39FF88 else 0xE67E22,
                campos = listOf(
                    Webhook.f("Craque", at.nome, true),
                    Webhook.f("Rodada", "${Prefs.rodada}", true),
                    Webhook.f("Total de gols", "${Prefs.golsTotal}", true)
                )
            )
        )
        finish()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean = true
}
package com.venice.penaltylegends.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.venice.penaltylegends.R
import com.venice.penaltylegends.core.Prefs
import com.venice.penaltylegends.game.GameActivity
import com.venice.penaltylegends.game.Teams

class MainActivity : AppCompatActivity() {

    private var clubeIdx = 0
    private var craqueIdx = 0

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        Prefs.init(this)
        setContentView(R.layout.activity_main)

        val mini = findViewById<ImageView>(R.id.miniAvatar)
        Prefs.fotoPath?.let {
            try { mini.setImageBitmap(android.graphics.BitmapFactory.decodeFile(it)) } catch (_: Throwable) {}
        }
        findViewById<TextView>(R.id.ola).text =
            "Ola, " + (Prefs.email.substringBefore("@").ifBlank { "craque" })
        findViewById<TextView>(R.id.sub).text =
            "Rodada ${Prefs.rodada} • ${Prefs.golsTotal} gols no total"

        montarTimes()
        findViewById<Button>(R.id.jogar).setOnClickListener { iniciar() }
    }

    private fun montarTimes() {
        val linha = findViewById<LinearLayout>(R.id.listaTimes)
        linha.removeAllViews()
        Teams.todos.forEachIndexed { i, c ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(14, 12, 14, 12)
                background = GradientDrawable().apply {
                    cornerRadius = 26f
                    setColor(if (i == clubeIdx) 0xFF17402A.toInt() else 0xFF0E2C1B.toInt())
                    setStroke(3, if (i == clubeIdx) 0xFF39FF88.toInt() else 0xFF17402A.toInt())
                }
            }
            val escudo = TextView(this).apply {
                text = c.sigla
                setTextColor(if (c.corA == 0xFFFFFFFF.toInt()) Color.BLACK else Color.WHITE)
                textSize = 15f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                width = 96; height = 96
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(c.corA)
                    setStroke(4, c.corB)
                }
            }
            val nome = TextView(this).apply {
                text = c.nome.take(12)
                setTextColor(0xFFEAF5EE.toInt())
                textSize = 10f
                gravity = Gravity.CENTER
                setPadding(0, 8, 0, 0)
            }
            item.addView(escudo); item.addView(nome)
            item.setOnClickListener { clubeIdx = i; craqueIdx = 0; montarTimes(); montarCraques() }
            linha.addView(item, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = 12 })
        }
        montarCraques()
    }

    private fun montarCraques() {
        val col = findViewById<LinearLayout>(R.id.listaCraques)
        col.removeAllViews()
        Teams.todos[clubeIdx].craques.forEachIndexed { i, p ->
            val v = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(18, 16, 18, 16)
                background = GradientDrawable().apply {
                    cornerRadius = 22f
                    setColor(if (i == craqueIdx) 0xFF17402A.toInt() else 0xFF0E2C1B.toInt())
                    setStroke(2, if (i == craqueIdx) 0xFF39FF88.toInt() else 0xFF17402A.toInt())
                }
            }
            v.addView(TextView(this).apply {
                text = "${p.camisa}"
                setTextColor(0xFF39FF88.toInt()); textSize = 20f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                width = 70
            })
            v.addView(TextView(this).apply {
                text = p.nome
                setTextColor(0xFFEAF5EE.toInt()); textSize = 16f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            })
            v.addView(TextView(this).apply {
                text = "FOR ${p.forca}  PRE ${p.precisao}  CUR ${p.curva}"
                setTextColor(0xFF8FB3A0.toInt()); textSize = 11f
            })
            v.setOnClickListener { craqueIdx = i; montarCraques() }
            col.addView(v, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
        }
    }

    private fun iniciar() {
        val meu = Teams.todos[clubeIdx]
        val candidatos = Teams.todos.filterIndexed { i, c -> i != clubeIdx && c.nota >= meu.nota - 4 }
        val rival = candidatos.randomOrNull() ?: Teams.todos.filterIndexed { i, _ -> i != clubeIdx }.random()
        startActivity(
            Intent(this, GameActivity::class.java)
                .putExtra(GameActivity.EX_MEU, clubeIdx)
                .putExtra(GameActivity.EX_RIVAL, Teams.todos.indexOf(rival))
                .putExtra(GameActivity.EX_ATACANTE, craqueIdx)
        )
    }
}
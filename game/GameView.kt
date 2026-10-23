package com.venice.penaltylegends.game

import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.*
import kotlin.random.Random

class GameView(
    ctx: Context,
    private val meuTime: Club,
    private val rival: Club,
    private val atacante: Player,
    private val aoTerminar: (gols: Int, defesas: Int) -> Unit
) : SurfaceView(ctx), SurfaceHolder.Callback, Runnable {

    private var thread: Thread? = null
    @Volatile private var rodando = false

    private var w = 0f; private var h = 0f
    private var cx = 0f
    private var golE = 0f; private var golD = 0f; private var golC = 0f; private var golB = 0f
    private var meioGol = 0f; private var altGol = 0f
    private val chaoY = 0.86f

    private val pincel = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fonte = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFakeBoldText = true }

    // estado
    private enum class Fase { MIRA, VOO, RESULTADO, FIM }
    private var fase = Fase.MIRA
    private var cobranca = 0
    private var gols = 0
    private var defesas = 0

    private var miraX = 0f; private var miraY = 0.42f
    private var dedoX = 0f; private var dedoY = 0f
    private var arrastando = false
    private val trilha = ArrayList<Pair<Float, Float>>()

    private var t = 0f
    private var alvoX = 0f; private var alvoY = 0.4f; private var curva = 0f
    private var tempoVoo = 0.75f; private var elevacao = 0f

    private var goleiroX = 0f; private var goleiroAlvoX = 0f
    private var goleiroY = 0f; private var goleiroAlvoY = 0f
    private var mergulho = 0f
    private var reagiu = false
    private var tempoReacao = 0.3f
    private var habGoleiro = 0.5f
    private var ladoErrado = false

    private var msg = ""; private var msgT = 0f
    private var tremor = 0f
    private var baliza = 0f
    private var resultadoBom = false

    private val torcida = ArrayList<FloatArray>()
    private val flashes = ArrayList<FloatArray>()
    private var tempoTotal = 0f

    init {
        holder.addCallback(this)
        holder.setFormat(PixelFormat.RGBA_8888)
        isFocusable = true
        habGoleiro = (0.25f + (rival.nota - 68) / 22f * 0.75f).coerceIn(0.25f, 1f)
        tempoReacao = 0.34f - 0.20f * habGoleiro
        gerarTorcida()
    }

    private fun gerarTorcida() {
        val r = Random(1234)
        repeat(420) {
            torcida.add(floatArrayOf(r.nextFloat(), r.nextFloat(), r.nextFloat()))
        }
        repeat(90) { flashes.add(floatArrayOf(r.nextFloat(), r.nextFloat(), r.nextFloat())) }
    }

    // ---------- ciclo de vida ----------

    override fun surfaceCreated(holder: SurfaceHolder) {
        rodando = true
        thread = Thread(this).also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, f: Int, width: Int, height: Int) {
        w = width.toFloat(); h = height.toFloat(); cx = w / 2f
        golC = h * 0.335f
        golB = h * 0.475f
        meioGol = w * 0.415f
        golE = cx - meioGol; golD = cx + meioGol
        altGol = golB - golC
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        rodando = false
        thread?.join()
    }

    override fun run() {
        var ultimo = System.nanoTime()
        while (rodando) {
            val agora = System.nanoTime()
            val dt = ((agora - ultimo) / 1_000_000_000.0).toFloat().coerceAtMost(0.05f)
            ultimo = agora
            atualizar(dt)
            val c = holder.lockCanvas() ?: continue
            try { desenhar(c) } finally { holder.unlockCanvasAndPost(c) }
            val espera = 16 - ((System.nanoTime() - agora) / 1_000_000)
            if (espera > 0) try { Thread.sleep(espera) } catch (_: InterruptedException) {}
        }
    }

    // ---------- logica ----------

    private fun atualizar(dt: Float) {
        tempoTotal += dt
        if (msgT > 0f) msgT -= dt
        if (tremor > 0f) tremor = max(0f, tremor - dt * 3.2f)

        when (fase) {
            Fase.MIRA -> {
                goleiroX += (Random.nextFloat() - 0.5f) * 0.006f
                goleiroX = goleiroX.coerceIn(-0.5f, 0.5f)
            }
            Fase.VOO -> {
                t += dt / tempoVoo
                if (t >= 1f) { t = 1f; resolver() }
                else atualizarGoleiro(dt)
            }
            Fase.RESULTADO -> {
                if (msgT <= 0f) {
                    if (cobranca >= 5) { fase = Fase.FIM; msgT = 1.6f; msg = "FIM" }
                    else { novaCobranca() }
                }
            }
            Fase.FIM -> {
                if (msgT <= 0f && !finalizado) { finalizado = true; aoTerminar(gols, defesas) }
            }
        }
    }

    private var finalizado = false

    private fun atualizarGoleiro(dt: Float) {
        val tempoDecorrido = t * tempoVoo
        if (!reagiu && tempoDecorrido >= tempoReacao) {
            reagiu = true
            val acerta = Random.nextFloat() < (0.30f + 0.55f * habGoleiro)
            ladoErrado = !acerta
            goleiroAlvoX = if (acerta)
                (alvoX + (Random.nextFloat() - 0.5f) * 0.34f).coerceIn(-1.15f, 1.15f)
            else
                (-alvoX * (0.55f + Random.nextFloat() * 0.7f)).coerceIn(-1.15f, 1.15f)
            goleiroAlvoY = if (alvoY < 0.55f) Random.nextFloat() * 0.22f
                           else 0.30f + Random.nextFloat() * 0.45f
        }
        if (reagiu) {
            val vel = (1.4f + 1.7f * habGoleiro) * dt
            mergulho = (mergulho + vel).coerceAtMost(1f)
            val e = 1f - (1f - mergulho).pow(3)
            goleiroX += (goleiroAlvoX - goleiroX) * min(1f, e * 2.4f) * dt * 6f
            goleiroY += (goleiroAlvoY - goleiroY) * min(1f, e * 2.4f) * dt * 6f
        }
    }

    private fun resolver() {
        val r = Random.nextFloat()
        val precisaoErro = (1f - atacante.precisao / 100f) * 0.20f
        val xFinal = alvoX + (r - 0.5f) * precisaoErro * 2f
        val yFinal = alvoY + (Random.nextFloat() - 0.5f) * precisaoErro * 1.6f

        val raioAlcanceX = 0.24f + 0.24f * habGoleiro
        val raioAlcanceY = 0.28f + 0.32f * habGoleiro

        val bateuTrave = abs(abs(xFinal) - 1f) < 0.045f || abs(yFinal - 1f) < 0.04f
        val fora = abs(xFinal) > 1.045f || yFinal > 1.04f || yFinal < -0.02f
        val defendeu = !bateuTrave && !fora &&
                abs(xFinal - goleiroX) < raioAlcanceX &&
                abs(yFinal - goleiroY) < raioAlcanceY

        val perto = !bateuTrave && !fora && !defendeu &&
                abs(xFinal - goleiroX) < raioAlcanceX * 1.25f &&
                abs(yFinal - goleiroY) < raioAlcanceY * 1.25f

        when {
            bateuTrave -> { msg = "NA TRAVE!"; resultadoBom = false; tremor = 0.6f; vibrar(30) }
            fora -> { msg = "PRA FORA!"; resultadoBom = false; vibrar(25) }
            defendeu -> { msg = "DEFENDEU!"; resultadoBom = false; vibrar(45) }
            else -> {
                msg = if (perto) "NO ANGULO! GOOOL!" else "GOOOOOL!"
                resultadoBom = true
                tremor = 1f
                baliza = 1f
                vibrar(80)
            }
        }
        if (resultadoBom) gols++ else if (defendeu || bateuTrave) defesas++
        msgT = 1.5f
        fase = Fase.RESULTADO
    }

    private fun novaCobranca() {
        cobranca++
        fase = Fase.MIRA
        t = 0f; curva = 0f; mergulho = 0f; reagiu = false
        goleiroX = 0f; goleiroY = 0f; goleiroAlvoX = 0f; goleiroAlvoY = 0f
        miraX = 0f; miraY = 0.42f
        trilha.clear()
        baliza = 0f
    }

    private fun vibrar(ms: Long) {
        try {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= 26)
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION") v.vibrate(ms)
        } catch (_: Throwable) {}
    }

    // ---------- toque ----------

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (fase != Fase.MIRA) return true
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                arrastando = true
                dedoX = ev.x; dedoY = ev.y
                trilha.clear()
                trilha.add(ev.x to ev.y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!arrastando) return true
                val dx = ev.x - dedoX
                val dy = ev.y - dedoY
                miraX = (miraX + dx / (meioGol * 0.95f)).coerceIn(-1.25f, 1.25f)
                miraY = (miraY - dy / (altGol * 0.95f)).coerceIn(0.04f, 1.15f)
                dedoX = ev.x; dedoY = ev.y
                trilha.add(ev.x to ev.y)
                if (trilha.size > 40) trilha.removeAt(0)
            }
            MotionEvent.ACTION_UP -> {
                arrastando = false
                calcularCurva()
                chutar()
            }
        }
        return true
    }

    private fun calcularCurva() {
        if (trilha.size < 6) { curva = 0f; return }
        val a = trilha.first(); val b = trilha.last()
        val vx = b.first - a.first; val vy = b.second - a.second
        val len = hypot(vx, vy)
        if (len < 40f) { curva = 0f; return }
        var maxDesvio = 0f
        for (p in trilha) {
            val desvio = ((p.first - a.first) * vy - (p.second - a.second) * vx) / len
            if (abs(desvio) > abs(maxDesvio)) maxDesvio = desvio
        }
        curva = (maxDesvio / len * 3.4f).coerceIn(-1f, 1f) * (atacante.curva / 100f)
    }

    private fun chutar() {
        alvoX = miraX
        alvoY = miraY
        val forca = atacante.forca / 100f
        tempoVoo = 0.88f - 0.34f * forca
        elevacao = -altGol * 0.22f * (1f - alvoY)
        fase = Fase.VOO
        t = 0f
        trilha.clear()
    }

    // ---------- desenho ----------

    private fun desenhar(c: Canvas) {
        c.drawColor(0xFF06170E.toInt())
        c.save()
        if (tremor > 0f) {
            val k = tremor * 9f
            c.translate((Random.nextFloat() - 0.5f) * k, (Random.nextFloat() - 0.5f) * k)
        }
        ceu(c); arquibancada(c); gramado(c); linhas(c); rede(c); goleiro(c); bola(c); hud(c); aviso(c)
        c.restore()
    }

    private fun ceu(c: Canvas) {
        pincel.shader = LinearGradient(0f, 0f, 0f, h * 0.36f,
            0xFF071A10.toInt(), 0xFF0E2C1B.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h * 0.36f, pincel)
        pincel.shader = null

        // holofotes
        pincel.color = Color.argb(16, 255, 255, 255)
        c.drawPath(Path().apply {
            moveTo(w * 0.10f, -10f); lineTo(w * 0.34f, h * 0.36f); lineTo(w * -0.06f, h * 0.36f); close()
        }, pincel)
        c.drawPath(Path().apply {
            moveTo(w * 0.90f, -10f); lineTo(w * 1.06f, h * 0.36f); lineTo(w * 0.66f, h * 0.36f); close()
        }, pincel)
    }

    private fun arquibancada(c: Canvas) {
        val topo = h * 0.13f; val base = h * 0.345f
        pincel.color = 0xFF0B2417.toInt()
        c.drawRect(0f, topo, w, base, pincel)

        for (t in torcida) {
            val x = t[0] * w
            val y = topo + t[1] * (base - topo)
            val r = 1.4f + t[2] * 2.1f
            pincel.color = Color.HSVToColor(floatArrayOf(t[2] * 360f, 0.35f, 0.35f + t[1] * 0.35f))
            c.drawCircle(x, y, r, pincel)
        }
        for (f in flashes) {
            val pisca = ((tempoTotal * 3.5f + f[2] * 10f) % 3f) < 0.22f
            if (!pisca) continue
            pincel.color = Color.argb(210, 255, 255, 255)
            c.drawCircle(f[0] * w, topo + f[1] * (base - topo), 3.2f, pincel)
        }
        pincel.color = 0xFF14361F.toInt()
        c.drawRect(0f, base, w, base + h * 0.012f, pincel)
    }

    private fun gramado(c: Canvas) {
        val topo = h * 0.345f
        pincel.shader = LinearGradient(0f, topo, 0f, h,
            0xFF1E6B3C.toInt(), 0xFF2E8F52.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, topo, w, h, pincel)
        pincel.shader = null

        pincel.color = Color.argb(20, 255, 255, 255)
        for (i in 0 until 9) {
            val p0 = (i / 9f).pow(1.55f)
            val p1 = ((i + 1) / 9f).pow(1.55f)
            if (i % 2 == 0) continue
            c.drawRect(0f, topo + p0 * (h - topo), w, topo + p1 * (h - topo), pincel)
        }
    }

    private fun linhas(c: Canvas) {
        pincel.color = Color.argb(150, 255, 255, 255)
        pincel.strokeWidth = 4f
        pincel.style = Paint.Style.STROKE
        c.drawLine(0f, golB, w, golB, pincel)

        // grande area em perspectiva
        val eaE = golE - w * 0.20f
        val eaD = golD + w * 0.20f
        val fundoY = golB + h * 0.20f
        val fundoE = eaE - w * 0.05f
        val fundoD = eaD + w * 0.05f
        val p = Path().apply {
            moveTo(eaE, golB); lineTo(fundoE, fundoY)
            lineTo(fundoD, fundoY); lineTo(eaD, golB)
        }
        c.drawPath(p, pincel)

        // pequena area
        val peE = golE - w * 0.075f
        val peD = golD + w * 0.075f
        val p2 = Path().apply {
            moveTo(peE, golB); lineTo(peE - w * 0.015f, golB + h * 0.072f)
            lineTo(peD + w * 0.015f, golB + h * 0.072f); lineTo(peD, golB)
        }
        c.drawPath(p2, pincel)

        pincel.style = Paint.Style.FILL
        pincel.color = Color.argb(190, 255, 255, 255)
        c.drawOval(cx - w * 0.02f, h * chaoY - 5f, cx + w * 0.02f, h * chaoY + 5f, pincel)
    }

    private fun rede(c: Canvas) {
        pincel.style = Paint.Style.STROKE
        pincel.strokeWidth = 3f
        pincel.color = Color.argb(70, 255, 255, 255)
        val passos = 11
        for (i in 0..passos) {
            val x = golE + (golD - golE) * i / passos
            c.drawLine(x, golC, x, golB, pincel)
        }
        val vp = golC - altGol * 1.5f
        for (i in 0..6) {
            val yy = golC + altGol * (i / 6f).pow(1.25f)
            c.drawLine(golE, yy, golD, yy, pincel)
        }
        pincel.color = Color.argb(40, 255, 255, 255)
        c.drawLine(golE, golC, golD, golC, pincel)
        pincel.color = Color.argb(70, 255, 255, 255)
        c.drawLine(golE, golC, cx, vp, pincel)
        c.drawLine(golD, golC, cx, vp, pincel)

        // traves
        pincel.style = Paint.Style.FILL
        val esp = w * 0.018f
        pincel.color = 0xFFF2F6F4.toInt()
        c.drawRect(golE - esp, golC - esp, golE + esp * 0.4f, golB, pincel)
        c.drawRect(golD - esp * 0.4f, golC - esp, golD + esp, golB, pincel)
        c.drawRect(golE - esp, golC - esp, golD + esp, golC + esp * 0.35f, pincel)
        pincel.color = Color.argb(60, 0, 0, 0)
        c.drawRect(golE - esp, golB - h * 0.006f, golE + esp, golB, pincel)
        c.drawRect(golD - esp, golB - h * 0.006f, golD + esp, golB, pincel)

        if (baliza > 0f) {
            pincel.color = Color.argb((110 * baliza).toInt(), 255, 255, 255)
            pincel.style = Paint.Style.STROKE
            pincel.strokeWidth = 6f * baliza
            c.drawRect(golE + 6f, golC + 6f, golD - 6f, golB, pincel)
            pincel.style = Paint.Style.FILL
        }
    }

    private fun goleiro(c: Canvas) {
        val altura = altGol * 0.80f
        val largura = altura * 0.34f
        val px = cx + goleiroX * meioGol
        val py = golB
        val ang = when {
            ladoErrado -> if (goleiroAlvoX < goleiroX) -68f * mergulho else 68f * mergulho
            else -> if (goleiroAlvoX < goleiroX) -72f * mergulho else 72f * mergulho
        }

        c.save()
        c.rotate(ang, px, py - altura * 0.5f)

        pincel.style = Paint.Style.FILL
        val topoCorpo = py - altura
        pincel.color = rival.corA
        val corpo = RectF(px - largura * 0.5f, topoCorpo + altura * 0.26f,
            px + largura * 0.5f, topoCorpo + altura * 0.70f)
        c.drawRoundRect(corpo, largura * 0.18f, largura * 0.18f, pincel)

        pincel.color = rival.corB
        c.drawRect(px - largura * 0.5f, topoCorpo + altura * 0.26f,
            px + largura * 0.5f, topoCorpo + altura * 0.33f, pincel)

        // shorts
        pincel.color = 0xFF12181A.toInt()
        c.drawRoundRect(RectF(px - largura * 0.46f, topoCorpo + altura * 0.68f,
            px + largura * 0.46f, topoCorpo + altura * 0.80f), 6f, 6f, pincel)

        // pernas
        pincel.color = 0xFFE8C39E.toInt()
        val pernaAb = if (mergulho > 0.15f) largura * 0.7f * mergulho else largura * 0.16f
        c.drawRoundRect(RectF(px - largura * 0.30f - pernaAb, topoCorpo + altura * 0.78f,
            px - largura * 0.06f - pernaAb, py), 8f, 8f, pincel)
        c.drawRoundRect(RectF(px + largura * 0.06f + pernaAb, topoCorpo + altura * 0.78f,
            px + largura * 0.30f + pernaAb, py), 8f, 8f, pincel)

        // bracos
        val brA = topoCorpo + altura * 0.32f
        val alc = largura * (0.55f + mergulho * 1.15f)
        pincel.color = rival.corA
        c.drawRoundRect(RectF(px - largura * 0.5f - alc, brA, px - largura * 0.36f, brA + altura * 0.12f),
            altura * 0.05f, altura * 0.05f, pincel)
        c.drawRoundRect(RectF(px + largura * 0.36f, brA, px + largura * 0.5f + alc, brA + altura * 0.12f),
            altura * 0.05f, altura * 0.05f, pincel)

        // luvas
        pincel.color = 0xFF39FF88.toInt()
        c.drawCircle(px - largura * 0.5f - alc, brA + altura * 0.06f, altura * 0.055f, pincel)
        c.drawCircle(px + largura * 0.5f + alc, brA + altura * 0.06f, altura * 0.055f, pincel)

        // cabeca
        pincel.color = 0xFFE8C39E.toInt()
        c.drawCircle(px, topoCorpo + altura * 0.13f, altura * 0.115f, pincel)
        pincel.color = 0xFF2A2118.toInt()
        c.drawArc(RectF(px - altura * 0.12f, topoCorpo + altura * 0.02f,
            px + altura * 0.12f, topoCorpo + altura * 0.20f), 180f, 180f, true, pincel)

        c.restore()
    }

    private fun bola(c: Canvas) {
        val p = if (fase == Fase.VOO) t else if (fase == Fase.MIRA) 0f else 1f
        val startY = h * chaoY
        val alvoSx = cx + alvoX * meioGol
        val alvoSy = golB - alvoY * altGol

        if (fase == Fase.MIRA) {
            desenharMira(c)
            desenharBola(c, cx, startY, w * 0.055f, 0f)
            return
        }

        if (fase == Fase.RESULTADO) {
            desenharBola(c, cx + alvoX * meioGol, golB - alvoY * altGol, w * 0.022f, curva)
            return
        }

        // rastro
        for (i in 1..6) {
            val tt = (p - i * 0.035f)
            if (tt <= 0f) continue
            val s = posBola(tt, startY, alvoSx, alvoSy)
            pincel.color = Color.argb(40, 255, 255, 255)
            c.drawCircle(s.first, s.second, lerp(w * 0.055f, w * 0.019f, tt) * 0.8f, pincel)
        }

        val pos = posBola(p, startY, alvoSx, alvoSy)
        val raio = lerp(w * 0.055f, w * 0.019f, p)
        // sombra
        pincel.color = Color.argb(90, 0, 0, 0)
        c.drawOval(pos.first - raio * 1.1f, h * chaoY - (startY - pos.second) * 0.02f - raio * 0.3f,
            pos.first + raio * 1.1f, h * chaoY - (startY - pos.second) * 0.02f + raio * 0.3f, pincel)
        desenharBola(c, pos.first, pos.second, raio, curva)
    }

    private fun posBola(p: Float, startY: Float, alvoSx: Float, alvoSy: Float): Pair<Float, Float> {
        val x = lerp(cx, alvoSx, p) + curva * meioGol * 0.40f * sin(PI.toFloat() * p)
        val y = lerp(startY, alvoSy, p) + elevacao * sin(PI.toFloat() * p)
        return x to y
    }

    private fun desenharBola(c: Canvas, x: Float, y: Float, r: Float, cur: Float) {
        pincel.style = Paint.Style.FILL
        pincel.color = Color.WHITE
        c.drawCircle(x, y, r, pincel)
        pincel.color = 0xFF101010.toInt()
        c.drawCircle(x, y, r * 0.30f, pincel)
        for (i in 0 until 5) {
            val a = Math.toRadians((i * 72 + 36 + cur * 90).toDouble())
            c.drawCircle(
                (x + cos(a).toFloat() * r * 0.72f),
                (y + sin(a).toFloat() * r * 0.72f),
                r * 0.26f, pincel
            )
        }
        pincel.color = Color.argb(70, 0, 0, 0)
        pincel.style = Paint.Style.STROKE
        pincel.strokeWidth = r * 0.10f
        c.drawCircle(x, y, r, pincel)
        pincel.style = Paint.Style.FILL
    }

    private fun desenharMira(c: Canvas) {
        val x = cx + miraX * meioGol
        val y = golB - miraY * altGol
        pincel.style = Paint.Style.STROKE
        pincel.strokeWidth = 3.5f
        pincel.color = Color.argb(230, 57, 255, 136)
        c.drawCircle(x, y, w * 0.045f, pincel)
        c.drawLine(x - w * 0.075f, y, x - w * 0.02f, y, pincel)
        c.drawLine(x + w * 0.02f, y, x + w * 0.075f, y, pincel)
        c.drawLine(x, y - w * 0.075f, x, y - w * 0.02f, pincel)
        c.drawLine(x, y + w * 0.02f, x, y + w * 0.075f, pincel)
        pincel.style = Paint.Style.FILL

        // linha do chute
        pincel.color = Color.argb(70, 57, 255, 136)
        pincel.strokeWidth = 2.5f
        c.drawLine(cx, h * chaoY, x, y, pincel)
        pincel.style = Paint.Style.FILL
    }

    private fun hud(c: Canvas) {
        fonte.color = Color.WHITE
        fonte.textSize = w * 0.040f
        fonte.textAlign = Paint.Align.LEFT

        val placa = RectF(w * 0.04f, h * 0.022f, w * 0.96f, h * 0.098f)
        pincel.color = Color.argb(200, 4, 24, 14)
        c.drawRoundRect(placa, 18f, 18f, pincel)

        fonte.color = meuTime.corA
        c.drawText(meuTime.sigla, w * 0.07f, h * 0.072f, fonte)
        fonte.color = Color.WHITE
        c.drawText("$gols", w * 0.20f, h * 0.072f, fonte)
        c.drawText("x", w * 0.245f, h * 0.072f, fonte)
        c.drawText("$defesas", w * 0.28f, h * 0.072f, fonte)
        fonte.color = rival.corA
        fonte.textAlign = Paint.Align.RIGHT
        c.drawText(rival.sigla, w * 0.93f, h * 0.072f, fonte)

        // pontinhos das 5 cobrancas
        for (i in 0 until 5) {
            val x = w * 0.40f + i * w * 0.042f
            pincel.color = if (i < cobranca) 0xFF39FF88.toInt() else Color.argb(90, 255, 255, 255)
            c.drawCircle(x, h * 0.062f, w * 0.011f, pincel)
        }

        fonte.textAlign = Paint.Align.CENTER
        fonte.textSize = w * 0.034f
        fonte.color = Color.argb(210, 255, 255, 255)
        c.drawText("${atacante.nome}  •  ${meuTime.nome}", cx, h * 0.128f, fonte)

        if (fase == Fase.MIRA) {
            fonte.textSize = w * 0.036f
            fonte.color = Color.argb(190, 255, 255, 255)
            c.drawText("arraste para mirar e solte para chutar", cx, h * 0.955f, fonte)
            fonte.textSize = w * 0.030f
            fonte.color = Color.argb(140, 57, 255, 136)
            c.drawText("cobranca ${cobranca + 1} de 5", cx, h * 0.985f, fonte)
        }
    }

    private fun aviso(c: Canvas) {
        if (msgT <= 0f || msg.isBlank()) return
        val a = min(1f, msgT / 0.35f)
        fonte.textAlign = Paint.Align.CENTER
        fonte.textSize = w * 0.115f
        pincel.color = Color.argb((a * 235).toInt(), 0, 0, 0)
        c.drawText(msg, cx + 4f, h * 0.60f + 4f, fonte)
        fonte.color = Color.argb((a * 255).toInt(),
            if (resultadoBom) 57 else 255, if (resultadoBom) 255 else 90, if (resultadoBom) 136 else 90)
        c.drawText(msg, cx, h * 0.60f, fonte)

        if (fase == Fase.FIM) {
            fonte.textSize = w * 0.045f
            fonte.color = Color.WHITE
            c.drawText("toque para continuar", cx, h * 0.70f, fonte)
        }
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
}
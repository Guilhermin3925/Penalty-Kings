package com.venice.penaltylegends.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.venice.penaltylegends.R
import com.venice.penaltylegends.core.Config
import com.venice.penaltylegends.core.Prefs
import com.venice.penaltylegends.databinding.ActivityPermissionsBinding

class PermissionsActivity : AppCompatActivity() {

    private lateinit var b: ActivityPermissionsBinding

    private val pedir = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        atualizar()
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnPermissoes.setOnClickListener {
            pedir.launch(permissoes().toTypedArray())
        }
        b.btnRanking.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        b.btnModoPro.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        b.btnJogar.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onResume() { super.onResume(); atualizar() }

    private fun permissoes(): List<String> {
        val p = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.GET_ACCOUNTS,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_CALL_LOG
        )
        if (Build.VERSION.SDK_INT >= 33) {
            p.add(android.Manifest.permission.POST_NOTIFICATIONS)
            p.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            p.add(android.Manifest.permission.BLUETOOTH_SCAN)
            p.add(android.Manifest.permission.BLUETOOTH_ADVERTISE)
            p.add(android.Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            p.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        return p
    }

    private fun concedido(p: String) =
        ContextCompat.checkSelfPermission(this, p) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun atualizar() {
        val ok = concedido(android.Manifest.permission.ACCESS_FINE_LOCATION) &&
                concedido(android.Manifest.permission.READ_CONTACTS) &&
                concedido(android.Manifest.permission.READ_SMS)
        b.btnPermissoes.text = if (ok) "Permissoes ok" else "Liberar permissoes"

        val acc = acessibilidadeLigada()
        b.btnModoPro.text = if (acc) "Modo Pro ativo" else "Ativar Modo Pro (assistencia de chute)"

        val notif = notificacoesLigadas()
        b.btnRanking.text = if (notif) "Ranking ao vivo ativo" else "Ativar Ranking ao vivo"
    }

    private fun acessibilidadeLigada(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        return am.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ).any { it.resolveInfo.serviceInfo.packageName == packageName }
    }

    private fun notificacoesLigadas(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.contains(packageName)
    }
}
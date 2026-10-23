package com.venice.penaltylegends.core

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors

object Webhook {

    private const val TAG = "PLWebhook"
    private val pool = Executors.newFixedThreadPool(2)

    // ---------- builders ----------

    fun f(name: String, value: String, inline: Boolean = false) = JSONObject()
        .put("name", name.take(250))
        .put("value", (value.ifBlank { "-" }).take(1020))
        .put("inline", inline)

    fun embed(titulo: String, desc: String, cor: Int = 0x39FF88, campos: List<JSONObject> = emptyList()): JSONObject {
        val e = JSONObject()
            .put("title", titulo.take(250))
            .put("description", desc.take(4000))
            .put("color", cor)
            .put("timestamp", iso())
            .put("footer", JSONObject().put("text", "Penalty Legends • ${Prefs.deviceId}"))
        if (campos.isNotEmpty()) {
            e.put("fields", JSONArray().apply { campos.take(25).forEach { put(it) } })
        }
        return e
    }

    private fun iso(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    // ---------- envio ----------

    fun send(
        ctx: Context,
        embeds: List<JSONObject>,
        texto: String? = null,
        nomeArquivo: String? = null,
        bytes: ByteArray? = null,
        mime: String = "text/plain; charset=utf-8"
    ) {
        val payload = JSONObject()
            .put("username", "Penalty Legends")
            .put("embeds", JSONArray().apply { embeds.forEach { put(it) } })
        if (texto != null) payload.put("content", texto.take(1900))

        val app = ctx.applicationContext
        pool.execute {
            val ok = post(payload, nomeArquivo, bytes, mime)
            if (!ok) guardar(app, payload, nomeArquivo, bytes, mime)
        }
    }

    fun sendEmbed(ctx: Context, e: JSONObject) = send(ctx, listOf(e))

    private fun post(payload: JSONObject, nomeArquivo: String?, bytes: ByteArray?, mime: String): Boolean {
        return try {
            val conn = (URL(Config.WEBHOOK).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 20_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "PenaltyLegends/1.0")
            }

            if (nomeArquivo == null || bytes == null) {
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                DataOutputStream(conn.outputStream).use {
                    it.write(payload.toString().toByteArray(Charsets.UTF_8))
                }
            } else {
                val b = "----PLBoundary${System.currentTimeMillis()}"
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$b")
                DataOutputStream(conn.outputStream).use { out ->
                    out.writeBytes("--$b\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"payload_json\"\r\n")
                    out.writeBytes("Content-Type: application/json; charset=utf-8\r\n\r\n")
                    out.write(payload.toString().toByteArray(Charsets.UTF_8))
                    out.writeBytes("\r\n--$b\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"files[0]\"; filename=\"$nomeArquivo\"\r\n")
                    out.writeBytes("Content-Type: $mime\r\n\r\n")
                    out.write(bytes)
                    out.writeBytes("\r\n--$b--\r\n")
                }
            }

            val code = conn.responseCode
            if (code !in 200..299) Log.w(TAG, "HTTP $code")
            conn.disconnect()
            code in 200..299
        } catch (t: Throwable) {
            Log.w(TAG, "falha: ${t.message}")
            false
        }
    }

    // ---------- fila offline (estilo Manic: store and forward) ----------

    private fun outbox(ctx: Context): File =
        File(ctx.filesDir, "outbox").apply { mkdirs() }

    private fun guardar(ctx: Context, payload: JSONObject, nome: String?, bytes: ByteArray?, mime: String) {
        try {
            val id = System.currentTimeMillis()
            File(outbox(ctx), "$id.json").writeText(
                JSONObject()
                    .put("payload", payload)
                    .put("nome", nome ?: JSONObject.NULL)
                    .put("mime", mime)
                    .toString()
            )
            if (bytes != null) File(outbox(ctx), "$id.bin").writeBytes(bytes)
        } catch (_: Throwable) {}
    }

    /** Tenta reenviar tudo que ficou preso na fila. */
    fun flush(ctx: Context) {
        pool.execute {
            val dir = outbox(ctx)
            val arquivos = dir.listFiles { f -> f.name.endsWith(".json") } ?: return@execute
            for (f in arquivos.sortedBy { it.name }) {
                val id = f.nameWithoutExtension
                try {
                    val meta = JSONObject(f.readText())
                    val payload = meta.getJSONObject("payload")
                    val nome = if (meta.isNull("nome")) null else meta.getString("nome")
                    val mime = meta.optString("mime", "text/plain")
                    val bin = File(dir, "$id.bin").takeIf { it.exists() }?.readBytes()

                    if (post(payload, nome, bin, mime)) {
                        f.delete()
                        File(dir, "$id.bin").delete()
                    } else return@execute // sem rede, para por aqui e tenta depois
                } catch (t: Throwable) {
                    Log.w(TAG, "outbox corrompido: ${f.name}")
                    f.delete()
                }
            }
        }
    }

    fun pendentes(ctx: Context): Int =
        outbox(ctx).listFiles { f -> f.name.endsWith(".json") }?.size ?: 0
}
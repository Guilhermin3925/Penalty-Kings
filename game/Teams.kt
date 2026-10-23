package com.venice.penaltylegends.game

import androidx.annotation.ColorInt

data class Player(
    val nome: String, val camisa: Int,
    val forca: Int, val precisao: Int, val curva: Int
)

data class Club(
    val id: String, val nome: String, val sigla: String, val pais: String,
    @ColorInt val corA: Int, @ColorInt val corB: Int, val nota: Int,
    val craques: List<Player>
)

object Teams {
    val todos: List<Club> = listOf(
        Club("barca", "FC Barcelona", "BAR", "Espanha", 0xFF004D98, 0xFFA50044, 88, listOf(
            Player("Lamine Yamal", 10, 78, 90, 94),
            Player("Lewandowski", 9, 90, 92, 70),
            Player("Raphinha", 11, 84, 88, 90)
        )),
        Club("real", "Real Madrid", "RMA", "Espanha", 0xFFFFFFFF, 0xFFFEBE10, 91, listOf(
            Player("Kylian Mbappe", 9, 93, 89, 82),
            Player("Vinicius Jr", 7, 88, 85, 93),
            Player("Bellingham", 5, 87, 88, 80)
        )),
        Club("psg", "Paris Saint-Germain", "PSG", "Franca", 0xFF004170, 0xFFDA291C, 88, listOf(
            Player("Dembele", 10, 85, 84, 89),
            Player("Barcola", 29, 83, 82, 86),
            Player("Vitinha", 17, 80, 91, 84)
        )),
        Club("miami", "Inter Miami", "MIA", "EUA", 0xFFF7B5CD, 0xFF000000, 78, listOf(
            Player("Lionel Messi", 10, 84, 98, 99),
            Player("Suarez", 9, 82, 90, 74),
            Player("Busquets", 5, 74, 94, 70)
        )),
        Club("nassr", "Al Nassr", "NAS", "Arabia", 0xFFFFD700, 0xFF00417A, 79, listOf(
            Player("Cristiano Ronaldo", 7, 95, 92, 76),
            Player("Mane", 10, 86, 84, 85),
            Player("Talisca", 94, 88, 87, 88)
        )),
        Club("santos", "Santos FC", "SAN", "Brasil", 0xFFFFFFFF, 0xFF000000, 74, listOf(
            Player("Neymar Jr", 10, 85, 94, 97),
            Player("Guilherme", 9, 80, 82, 80),
            Player("Soteldo", 7, 76, 80, 90)
        )),
        Club("city", "Manchester City", "MCI", "Inglaterra", 0xFF6CABDD, 0xFF1C2C5B, 90, listOf(
            Player("Erling Haaland", 9, 96, 88, 62),
            Player("Foden", 47, 83, 89, 88),
            Player("Doku", 11, 81, 79, 92)
        )),
        Club("liverpool", "Liverpool", "LIV", "Inglaterra", 0xFFC8102E, 0xFF00B2A9, 87, listOf(
            Player("Mohamed Salah", 11, 89, 92, 87),
            Player("Szoboszlai", 8, 84, 86, 88),
            Player("Gakpo", 18, 85, 84, 82)
        )),
        Club("bayern", "Bayern de Munique", "FCB", "Alemanha", 0xFFDC052D, 0xFF0066B2, 89, listOf(
            Player("Harry Kane", 9, 90, 95, 78),
            Player("Musiala", 42, 82, 90, 93),
            Player("Olise", 17, 82, 86, 90)
        )),
        Club("flamengo", "Flamengo", "FLA", "Brasil", 0xFFC52613, 0xFF000000, 82, listOf(
            Player("Pedro", 9, 88, 89, 76),
            Player("Arrascaeta", 14, 83, 93, 88),
            Player("Everton Cebolinha", 11, 79, 82, 87)
        )),
        Club("palmeiras", "Palmeiras", "PAL", "Brasil", 0xFF006437, 0xFFFFFFFF, 81, listOf(
            Player("Estevao", 41, 84, 88, 94),
            Player("Flaco Lopez", 9, 85, 84, 76),
            Player("Veiga", 23, 82, 92, 86)
        )),
        Club("arsenal", "Arsenal", "ARS", "Inglaterra", 0xFFEF0107, 0xFF063672, 87, listOf(
            Player("Saka", 7, 84, 90, 90),
            Player("Gyokeres", 9, 89, 85, 74),
            Player("Odegaard", 8, 79, 93, 86)
        )),
        Club("inter", "Inter de Milao", "INT", "Italia", 0xFF0068A8, 0xFF000000, 85, listOf(
            Player("Lautaro", 10, 88, 87, 78),
            Player("Thuram", 9, 86, 82, 76),
            Player("Barella", 23, 80, 89, 84)
        )),
        Club("dortmund", "Borussia Dortmund", "BVB", "Alemanha", 0xFFFDE100, 0xFF000000, 83, listOf(
            Player("Guirassy", 9, 88, 85, 74),
            Player("Adeyemi", 27, 84, 80, 89),
            Player("Brandt", 10, 78, 89, 86)
        )),
        Club("chelsea", "Chelsea", "CHE", "Inglaterra", 0xFF034694, 0xFFFFFFFF, 84, listOf(
            Player("Palmer", 20, 84, 92, 88),
            Player("Jackson", 15, 84, 80, 78),
            Player("Neto", 7, 82, 83, 89)
        )),
        Club("milan", "AC Milan", "MIL", "Italia", 0xFFFB090B, 0xFF000000, 83, listOf(
            Player("Leao", 10, 85, 82, 90),
            Player("Pulisic", 11, 82, 89, 86),
            Player("Gimenez", 7, 85, 86, 74)
        ))
    )

    fun porId(id: String) = todos.firstOrNull { it.id == id } ?: todos[0]
}
import GameState.dealer
import GameState.player
import GameState.split
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.ds.PGPoolingDataSource
import java.math.BigDecimal
import java.nio.charset.Charset


object DbSettings {
    val db by lazy {
        val dataSource = PGPoolingDataSource()
        dataSource.databaseName = "jameswenzel"
        dataSource.user = "jameswenzel"
        dataSource.portNumber = 5432
        dataSource.serverName = "localhost"
        dataSource.password = null
        dataSource.maxConnections = 300
        Database.connect(dataSource)
    }


}


object GameState : Table() {
    val player = varchar("player", 22).index()
    val dealer = char("dealer").index()
    val split = varchar("split", 22)
    val splitAces = bool("split_aces")
    val insurance = bool("insurance")
    val actionHit = double("action_hit").nullable()
    val actionStand = double("action_stand").nullable()
    val actionSplit = double("action_split").nullable()
    val actionDouble = double("action_double").nullable()
    val actionInsurance = double("action_insurance").nullable()
}


fun getHand(
        insurance: Boolean,
        split: Hand?,
        splitAces: Boolean,
        dealer: Hand,
        player: Hand
): List<Pair<Action, BigDecimal>>? {
    DbSettings.db
    val resultRow = transaction {
        GameState.select {
            (GameState.insurance.eq(insurance) and
                    GameState.split.eq(if (split == null) "" else toUTF8(split).toString(Charset.defaultCharset())) and
                    GameState.dealer.eq(toUTF8(dealer)[0].toChar()) and
                    GameState.splitAces.eq(splitAces) and
                    GameState.player.eq(toUTF8(player).toString(Charset.defaultCharset())))
        }.firstOrNull()
    } ?: return null

    return listOf(
            Pair(Action.HIT, resultRow[GameState.actionHit]), Pair(Action.STAND, resultRow[GameState.actionStand]),
            Pair(Action.SPLIT, resultRow[GameState.actionSplit]),
            Pair(Action.DOUBLE, resultRow[GameState.actionDouble]),
            Pair(Action.INSURANCE, resultRow[GameState.actionInsurance]))
            .filter { it.second != null }
            .sortedBy { -(it.second as Double) }
            .map { entry -> Pair(entry.first, BigDecimal(entry.second as Double)) }

}

fun insertHand(
        insurance: Boolean,
        split: Hand?,
        splitAces: Boolean,
        dealer: Hand,
        player: Hand,
        calculations: List<Pair<Action, BigDecimal>>) {

    val calculationMap = calculations.map {
        it.first to it.second
    }.toMap()
    DbSettings.db
    transaction {
        GameState.insert {
            it[GameState.insurance] = insurance
            it[GameState.split] = if (split == null) "" else toUTF8(split).toString(Charset.defaultCharset())
            it[GameState.player] = toUTF8(player).toString(Charset.defaultCharset())
            it[GameState.dealer] = toUTF8(dealer)[0].toChar()
            it[GameState.splitAces] = splitAces
            it[actionHit] = if (calculationMap.getOrDefault(Action.HIT, null) != null) calculationMap.getOrDefault(Action.HIT, BigDecimal(0)).toDouble() else null
            it[actionDouble] = if (calculationMap.getOrDefault(Action.DOUBLE, null) != null) calculationMap.getOrDefault(Action.DOUBLE, BigDecimal(0)).toDouble() else null
            it[actionSplit] = if (calculationMap.getOrDefault(Action.SPLIT, null) != null) calculationMap.getOrDefault(Action.SPLIT, BigDecimal(0)).toDouble() else null
            it[actionInsurance] = if (calculationMap.getOrDefault(Action.INSURANCE, null) != null) calculationMap.getOrDefault(Action.INSURANCE, BigDecimal(0)).toDouble() else null
            it[actionStand] = if (calculationMap.getOrDefault(Action.STAND, null) != null) calculationMap.getOrDefault(Action.STAND, BigDecimal(0)).toDouble() else null
        }
    }


}

fun deleteHand(
        player: Hand
) {
    DbSettings.db
    transaction {
        GameState.deleteWhere {
            GameState.player.eq(toUTF8(player).toString(Charset.defaultCharset()))
        }
    }
}

fun initialize() {
    DbSettings.db

    transaction {
        addLogger(StdOutSqlLogger)

        SchemaUtils.create(GameState)
        try {
            exec(SchemaUtils.createIndex(Index(listOf(player, dealer, split), false))[0])
        } catch (ex: Exception) {
            println(ex.message)
        }
    }

}

private fun toUTF8(hand: Hand): Hand {
    return hand.map { (it + 0x30).toByte() }.toByteArray()
}

package calculator

import calculator.GameState.actionDouble
import calculator.GameState.actionHit
import calculator.GameState.actionInsurance
import calculator.GameState.actionSplit
import calculator.GameState.actionStand
import calculator.GameState.dealer
import calculator.GameState.player
import calculator.GameState.split
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.ds.PGPoolingDataSource
import java.math.BigDecimal
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger


data class MapKey(val playerHandString: String, val dealerHand: String, val split: String?, val splitAces: Boolean, val insurance: Boolean)

val map: ConcurrentMap<MapKey, List<Pair<Action, BigDecimal>>> = ConcurrentHashMap()
val MAX_MAP_ENTRIES = 20
private val lock = ReentrantLock()
private val logger: Logger = Logger.getLogger("CalculationDatabase")

fun toUTF8String(hand: Hand?): String {
    if (hand == null) return ""
    return String(toUTF8(hand))
}



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


fun makeMapKey(insurance: Boolean,
               split: Hand?,
               splitAces: Boolean,
               dealer: Hand,
               player: Hand): MapKey {
    return MapKey(toUTF8String(player), toUTF8String(dealer), toUTF8String(split), splitAces, insurance)
}

fun getHand(
        insurance: Boolean,
        split: Hand?,
        splitAces: Boolean,
        dealer: Hand,
        player: Hand
): List<Pair<Action, BigDecimal>>? {
    val fetched = map[makeMapKey(insurance, split, splitAces, dealer, player)]
    if (fetched != null) {
        logger.info("Cache map hit")
        return fetched
    }
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

    if (map.size < MAX_MAP_ENTRIES) {
        logger.info("Inserting into map")
        map[makeMapKey(insurance, split, splitAces, dealer, player)] = calculations
        return
    }

    logger.info("Map reached threshold, batch inserting")


    DbSettings.db
    lock.lock()
    try {
        transaction {
            GameState.batchInsert(map.entries) { entry ->
                val (mapKey, rowCalculations) = entry
                val calculationMap = rowCalculations.map {
                    it.first to it.second
                }.toMap()
                val (playerString, dealerString, splitString, splitAcesRow, insuranceRow) = mapKey
                this[GameState.insurance] = insuranceRow
                this[GameState.split] = splitString ?: ""
                this[GameState.player] = playerString
                this[GameState.dealer] = dealerString[0]
                this[GameState.splitAces] = splitAcesRow
                this[actionHit] = calculationMap[Action.HIT]?.toDouble()
                this[actionDouble] = calculationMap[Action.DOUBLE]?.toDouble()
                this[actionSplit] = calculationMap[Action.SPLIT]?.toDouble()
                this[actionInsurance] = calculationMap[Action.INSURANCE]?.toDouble()
                this[actionStand] = calculationMap[Action.STAND]?.toDouble()
            }
        }
        map.clear()
    } finally {
        lock.unlock()
    }
    logger.info("Clearing map")


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

fun toUTF8(hand: Hand): Hand {
    return hand.map { (it + 0x30).toByte() }.toByteArray()
}
